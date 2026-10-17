#!/usr/bin/env python3
"""Validates LinguaFree course content (schema v1). Mirrors ContentValidator.kt in :core:data.
Exit code 1 on any error. Usage: python3 scripts/content/validate_content.py
"""
import json, os, re, sys
from collections import Counter

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
CONTENT = os.path.join(ROOT, 'core/data/src/main/assets/content')
DRAWABLES = os.path.join(ROOT, 'core/ui/src/main/res/drawable')
SUPPORTED_SCHEMA = 1
TYPES = {'tiles', 'type', 'select_image', 'match', 'listen_tiles', 'listen_type', 'fill_gap', 'speak',
         'select_translation', 'dialogue'}
NODE_TYPES = {'lesson', 'chest', 'story', 'review', 'test'}
PUNCT = re.compile(r"[^\w\s']", re.UNICODE)
errors, warnings = [], []


def norm(s):
    return ' '.join(PUNCT.sub(' ', s.lower().replace('ё', 'е').replace('’', "'")).split())


def err(where, msg):
    errors.append(f'{where}: {msg}')


def check_exercise(e, where, images):
    t = e.get('type')
    if t not in TYPES:
        return err(where, f'unknown type {t!r}')
    for f in ('id', 'type'):
        if f not in e:
            err(where, f'missing {f}')
    ans = e.get('answers', [])
    opts = e.get('options', [])
    opt_texts = [o['text'] if isinstance(o, dict) else o for o in opts]
    if t in {'tiles', 'type', 'select_image', 'listen_tiles', 'listen_type', 'fill_gap', 'speak', 'select_translation'}:
        if not ans:
            err(where, 'no answers')
        if not e.get('text'):
            err(where, 'empty text')
    if t in {'select_image', 'fill_gap', 'select_translation'}:
        if len(opt_texts) < 2:
            err(where, 'needs ≥ 2 options')
        if ans and not any(norm(a) == norm(o) for a in ans for o in opt_texts):
            err(where, f'answer {ans[0]!r} is not among options {opt_texts}')
        if len(set(map(norm, opt_texts))) != len(opt_texts):
            err(where, f'duplicate options {opt_texts}')
    if t == 'select_image':
        if len(opts) != 4:
            err(where, 'select_image needs 4 cards')
        for o in opts:
            img = o.get('image') if isinstance(o, dict) else None
            if not img or img not in images:
                err(where, f'missing image {img!r}')
    if t in {'tiles', 'listen_tiles'}:
        bank = Counter(w for o in opt_texts for w in norm(o).split())
        need = Counter(norm(w) for w in norm(ans[0]).split()) if ans else Counter()
        # contractions: tiles must contain the exact words of the first answer
        for w, c in need.items():
            if bank[w] < c:
                err(where, f'tiles cannot build {ans[0]!r}: missing {w!r}')
    if t in {'listen_tiles', 'listen_type', 'speak'} and not e.get('audio'):
        err(where, 'audio missing')
    if t == 'fill_gap' and '___' not in e.get('text', ''):
        err(where, 'fill_gap text needs ___')
    if t == 'match':
        pairs = e.get('pairs', [])
        if len(pairs) != 5:
            err(where, f'match needs 5 pairs, has {len(pairs)}')
        if len({norm(p['en']) for p in pairs}) != len(pairs) or len({norm(p['ru']) for p in pairs}) != len(pairs):
            err(where, 'match pairs must be unique on both sides')
    if t == 'dialogue':
        if len(e.get('lines', [])) < 2:
            err(where, 'dialogue needs ≥ 2 lines')
        if not e.get('questions'):
            err(where, 'dialogue needs questions')
        for q in e.get('questions', []):
            if not (0 <= q.get('answer', -1) < len(q.get('options', []))):
                err(where, f'bad question answer index in {q.get("prompt")!r}')
    if t in {'tiles', 'type'} and e.get('direction') not in ('ru_en', 'en_ru'):
        err(where, 'direction must be ru_en or en_ru')


def main():
    images = {f[:-4] for f in os.listdir(DRAWABLES) if f.startswith('img_')}
    course = json.load(open(os.path.join(CONTENT, 'course.json'), encoding='utf-8'))
    if course.get('schemaVersion') != SUPPORTED_SCHEMA:
        err('course.json', f'unsupported schemaVersion {course.get("schemaVersion")}')
    lexemes = json.load(open(os.path.join(CONTENT, 'lexemes.json'), encoding='utf-8'))['lexemes']
    lex_ids = {l['id'] for l in lexemes}
    if len(lex_ids) != len(lexemes):
        err('lexemes.json', 'duplicate lexeme ids')
    for l in lexemes:
        if l.get('image') and l['image'] not in images:
            err('lexemes.json', f'missing image {l["image"]}')
    ids = set()
    type_count = Counter()
    lessons = 0
    unit_ids = []
    for s in course['sections']:
        for uid in s['units']:
            unit_ids.append(uid)
            path = os.path.join(CONTENT, 'units', uid + '.json')
            if not os.path.exists(path):
                err(uid, 'unit file missing')
                continue
            u = json.load(open(path, encoding='utf-8'))
            if u.get('schemaVersion') != SUPPORTED_SCHEMA:
                err(uid, 'unsupported schemaVersion')
            if not re.fullmatch(r'#[0-9A-Fa-f]{6}', u.get('color', '')):
                err(uid, 'color must be #RRGGBB')
            types_in_unit = Counter(n['type'] for n in u['nodes'])
            if types_in_unit['test'] != 1:
                err(uid, 'each unit needs exactly one test node (unit check / skip)')
            for n in u['nodes']:
                where = f'{uid}/{n.get("id")}'
                if n.get('type') not in NODE_TYPES:
                    err(where, f'unknown node type {n.get("type")!r}')
                if n['id'] in ids:
                    err(where, 'duplicate id')
                ids.add(n['id'])
                exs = n.get('exercises', [])
                if n['type'] == 'lesson':
                    lessons += 1
                    if len(exs) < 12:
                        err(where, f'lesson needs ≥ 12 exercises, has {len(exs)}')
                    if len(exs) > 16:
                        warnings.append(f'{where}: {len(exs)} exercises (target 10–15)')
                    if len(n.get('newWords', [])) > 5:
                        err(where, f'more than 5 new words: {n["newWords"]}')
                    for w in n.get('newWords', []):
                        if w not in lex_ids:
                            err(where, f'new word {w!r} not in lexemes.json')
                if n['type'] == 'story' and not any(e.get('type') == 'dialogue' for e in exs):
                    err(where, 'story needs a dialogue exercise')
                if n['type'] in ('chest', 'review', 'test') and exs:
                    err(where, f'{n["type"]} nodes are generated, exercises must be empty')
                for e in exs:
                    ew = f'{where}/{e.get("id")}'
                    if e.get('id') in ids:
                        err(ew, 'duplicate id')
                    ids.add(e.get('id'))
                    type_count[e.get('type')] += 1
                    for lid in e.get('lexemes', []):
                        if lid not in lex_ids:
                            err(ew, f'unknown lexeme {lid!r}')
                    check_exercise(e, ew, images)
    if len(set(unit_ids)) != len(unit_ids):
        err('course.json', 'duplicate unit ids')
    missing_types = TYPES - set(type_count)
    if missing_types:
        err('course', f'exercise types never used: {sorted(missing_types)}')
    for w in warnings[:10]:
        print('warning:', w)
    if errors:
        for e in errors[:80]:
            print('ERROR', e)
        print(f'{len(errors)} errors')
        sys.exit(1)
    print(f'OK: {len(unit_ids)} units, {lessons} lessons, {sum(type_count.values())} exercises; types: {dict(type_count)}')


if __name__ == '__main__':
    main()
