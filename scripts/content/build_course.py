#!/usr/bin/env python3
"""Builds LinguaFree course content (schema v1) into core/data/src/main/assets/content/.

Sources:
  * scripts/content/a1_start.py               — hand-written section «Старт A1» (3 units × 5 lessons)
  * core/data/src/main/assets/library/*.json  — content of LinguaFree 0.9 (300 lessons, 70 stories,
                                                20 grammar topics, dictionary), converted, nothing dropped.
Output is deterministic (fixed seeds) so diffs stay readable.
Usage: python3 scripts/content/build_course.py
"""
import json, os, random, re, sys
from collections import OrderedDict

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
sys.path.insert(0, os.path.dirname(__file__))
import a1_start  # noqa: E402

LIB = os.path.join(ROOT, 'core/data/src/main/assets/library')
OUT = os.path.join(ROOT, 'core/data/src/main/assets/content')
SCHEMA_VERSION = 1
CONTENT_VERSION = 1

PUNCT = re.compile(r"[^\w\s'’-]", re.UNICODE)
UNIT_COLORS = ['#4CC417', '#FF8A1F', '#2EB8F2', '#A56BF7', '#FF4B6E', '#34D1A2', '#FFC21F', '#1FA9E6', '#FF7A45', '#9B6BF2']


def first(v):
    return v.split('|')[0].strip()


def variants(v):
    return [x.strip() for x in v.split('|') if x.strip()]


def tokens(sentence):
    """Word tiles: punctuation removed, case kept."""
    return [t for t in PUNCT.sub(' ', sentence).replace('’', "'").split() if t.strip("'-")]


def norm(s):
    return ' '.join(PUNCT.sub(' ', s.lower().replace('ё', 'е').replace('’', "'")).split())


def slug(text):
    s = re.sub(r"[^a-z0-9]+", '_', text.lower().replace("'", '')).strip('_')
    return s[:48] or 'x'


def short_ru(translation):
    return translation.split(',')[0].strip()


class Builder:
    def __init__(self):
        self.lexemes = OrderedDict()      # id -> {id, word, translation, image}
        self.lexicon = {}                  # lowercase word -> hint
        self.exercise_ids = set()
        self.images = []                   # (en word, image) pool for select_image distractors

    # ---------- helpers ----------
    def lexeme(self, lid, word, translation, image=None):
        if lid not in self.lexemes:
            self.lexemes[lid] = {'id': lid, 'word': word, 'translation': translation, 'image': image}
        if image and (word, image) not in self.images:
            self.images.append((word, image))
        return lid

    def hint(self, word, translation):
        k = word.lower().strip()
        if k and k not in self.lexicon:
            self.lexicon[k] = translation

    def ex(self, base, **kw):
        eid = base
        n = 2
        while eid in self.exercise_ids:
            eid = f'{base}-{n}'
            n += 1
        self.exercise_ids.add(eid)
        out = {'id': eid}
        out.update({k: v for k, v in kw.items() if v not in (None, [], {}, '')})
        return out

    def tiles(self, rnd, answer_first, accepted, distractor_pool, eid, text, direction, listen=False):
        toks = tokens(answer_first)
        own = {t.lower() for t in toks}
        seen_l = set()
        pool = []
        for w in distractor_pool:
            if w.strip() and w.lower() not in own and w.lower() not in seen_l:
                seen_l.add(w.lower())
                pool.append(w if w.isupper() and len(w) == 1 else (w.lower() if w[:1].isupper() and w.lower() in self.lexicon else w))
        rnd.shuffle(pool)
        extra = pool[:max(2, min(4, 7 - len(toks) // 2))]
        opts = toks + extra
        rnd.shuffle(opts)
        t = 'listen_tiles' if listen else 'tiles'
        return self.ex(eid, type=t, text=text, direction=direction, options=opts,
                       answers=[answer_first] if listen else accepted,
                       audio=answer_first if (listen or direction == 'en_ru') else None)

    # ---------- section 1: hand-written A1 ----------
    def build_start_units(self):
        for k, v in a1_start.FUNCTION_HINTS.items():
            self.hint(k, v)
        units = []
        prior_new = []
        for u in a1_start.UNITS:
            for l in u['lessons']:
                for lid, ru, img in l['new']:
                    word = lid.replace('_', ' ')
                    self.lexeme(lid, word, ru, img)
                    self.hint(word, ru)
                    for part in word.split():
                        self.hint(part, ru if ' ' not in word else self.lexicon.get(part, ru))
                    self.hint(short_ru(ru), word)
        for u in a1_start.UNITS:
            nodes = []
            lessons = []
            for l in u['lessons']:
                lessons.append(self.start_lesson(u, l, prior_new))
                prior_new += [n[0] for n in l['new']]
            story = self.story_node(u['id'] + '-story', u['story']['title'],
                                    [(s, en, ru) for s, en, ru in u['story']['lines']],
                                    [(q, o, a) for q, o, a in u['story']['questions']], seed=u['id'])
            order = [lessons[0], lessons[1], {'id': u['id'] + '-chest', 'type': 'chest', 'title': 'Сундук'},
                     lessons[2], story, lessons[3], lessons[4],
                     {'id': u['id'] + '-review', 'type': 'review', 'title': 'Повторение юнита'},
                     {'id': u['id'] + '-test', 'type': 'test', 'title': 'Проверка юнита'}]
            nodes.extend(order)
            units.append({'id': u['id'], 'title': u['title'], 'description': u['description'], 'color': u['color'],
                          'guidebook': [{'title': g['title'], 'text': g['text'],
                                         'examples': [{'en': e[0], 'ru': e[1]} for e in g['examples']]} for g in u['guidebook']],
                          'nodes': nodes})
        return units

    def start_lesson(self, unit, l, prior_new):
        rnd = random.Random(l['id'])
        new = l['new']
        words = [(lid, lid.replace('_', ' '), ru, img) for lid, ru, img in new]
        sents = l['sentences']
        en_first = [first(s[0]) for s in sents]
        ru_first = [first(s[1]) for s in sents]
        en_pool = [t for s in en_first for t in tokens(s)] + [w for _, w, _, _ in words if ' ' not in w]
        ru_pool = [t for s in ru_first for t in tokens(s)]
        lexemes_in = lambda en: [lid for lid, w, _, _ in words if re.search(r'\b' + re.escape(w) + r'\b', en.lower())]
        exs = []
        pid = l['id']

        def select_image(i):
            lid, w, ru, img = words[i]
            same = [(x[1], x[3]) for x in words if x[3] and x[3] != img]
            unit_imgs = [(self.lexemes[x[0]]['word'], self.lexemes[x[0]]['image']) for ll in unit['lessons'] for x in ll['new'] if x[2] and x[2] != img]
            rest = [p for p in self.images if p[1] != img]
            rnd.shuffle(same); rnd.shuffle(unit_imgs); rnd.shuffle(rest)
            others = list(dict.fromkeys(same + unit_imgs + rest))
            opts = [{'text': w, 'image': img}] + [{'text': p[0], 'image': p[1]} for p in others[:3]]
            rnd.shuffle(opts)
            return self.ex(f'{pid}-img{i}', type='select_image', text=short_ru(ru), options=opts, answers=[w], lexemes=[lid], audio=w)

        def select_word(i, en_to_ru):
            lid, w, ru, _ = words[i]
            if en_to_ru:
                pool = [short_ru(x[2]) for x in words if x[0] != lid]
                opts = [short_ru(ru)] + rnd.sample(pool, min(2, len(pool)))
                rnd.shuffle(opts)
                return self.ex(f'{pid}-w{i}', type='select_translation', direction='en_ru', text=w, options=opts,
                               answers=[short_ru(ru)], lexemes=[lid], audio=w)
            pool = [x[1] for x in words if x[0] != lid]
            opts = [w] + rnd.sample(pool, min(2, len(pool)))
            rnd.shuffle(opts)
            return self.ex(f'{pid}-w{i}', type='select_translation', direction='ru_en', text=short_ru(ru), options=opts,
                           answers=[w], lexemes=[lid])

        def word_or_image(i):
            return select_image(i) if words[i][3] else select_word(i, en_to_ru=i % 2 == 1)

        def tiles_ru_en(si):
            en, ru = sents[si]
            return dict(self.tiles(rnd, first(en), variants(en), en_pool, f'{pid}-t{si}', first(ru), 'ru_en'), lexemes=lexemes_in(first(en)))

        def tiles_en_ru(si):
            en, ru = sents[si]
            return dict(self.tiles(rnd, first(ru), variants(ru), ru_pool, f'{pid}-tr{si}', first(en), 'en_ru'), lexemes=lexemes_in(first(en)))

        def listen_tiles(si):
            en, _ = sents[si]
            return dict(self.tiles(rnd, first(en), [first(en)], en_pool, f'{pid}-lt{si}', first(en), 'ru_en', listen=True),
                        lexemes=lexemes_in(first(en)))

        def listen_type(text, lex):
            return self.ex(f'{pid}-ly', type='listen_type', text=text, audio=text, answers=[text], lexemes=lex)

        def type_tr(si):
            en, ru = sents[si]
            return self.ex(f'{pid}-ty{si}', type='type', direction='ru_en', text=first(ru), answers=variants(en), lexemes=lexemes_in(first(en)))

        def speak(si):
            en, _ = sents[si]
            return self.ex(f'{pid}-sp{si}', type='speak', text=first(en), audio=first(en), answers=[first(en)], lexemes=lexemes_in(first(en)))

        def fill_gap(si):
            en, ru = sents[si]
            e = first(en)
            for lid, w, _, _ in words:
                if ' ' in w:
                    continue
                m = re.search(r'\b' + re.escape(w) + r'\b', e, re.I)
                if m:
                    gap = e[:m.start()] + '___' + e[m.end():]
                    others = [x[1] for x in words if x[1] != w and ' ' not in x[1]]
                    if len(others) < 2:
                        others += [t for t in en_pool if t.lower() != w and t.lower() not in e.lower()]
                    cand = list(dict.fromkeys(o.lower() for o in others if o.lower() != w and o.lower() not in norm(e).split()))
                    picked = rnd.sample(cand, 2)
                    if m.group(0)[:1].isupper():
                        picked = [o[:1].upper() + o[1:] for o in picked]
                    opts = [m.group(0)] + picked
                    rnd.shuffle(opts)
                    return self.ex(f'{pid}-g{si}', type='fill_gap', text=gap, subtitle=first(ru), options=opts,
                                   answers=[m.group(0)], lexemes=[lid], audio=e)
            return None

        def select_sentence(si):
            en, ru = sents[si]
            others = [first(s[0]) for j, s in enumerate(sents) if j != si]
            opts = [first(en)] + rnd.sample(others, 2)
            rnd.shuffle(opts)
            return self.ex(f'{pid}-ss{si}', type='select_translation', direction='ru_en', text=first(ru), options=opts,
                           answers=[first(en)], lexemes=lexemes_in(first(en)))

        def match():
            pairs = [{'en': w, 'ru': short_ru(ru)} for _, w, ru, _ in words]
            lids = [x[0] for x in words]
            extra = [p for p in prior_new if p not in lids]
            rnd.shuffle(extra)
            for p in extra:
                if len(pairs) >= 5:
                    break
                lx = self.lexemes[p]
                pairs.append({'en': lx['word'], 'ru': short_ru(lx['translation'])})
                lids.append(p)
            ru_seen = set()
            pairs = [p for p in pairs if not (p['ru'] in ru_seen or ru_seen.add(p['ru']))][:5]
            return self.ex(f'{pid}-m', type='match', text='', pairs=pairs, lexemes=lids[:5])

        def dialogue():
            d = l['dialogue']
            return self.ex(f'{pid}-d', type='dialogue', text=l['title'],
                           lines=[{'speaker': s, 'en': en, 'ru': ru} for s, en, ru in d['lines']],
                           questions=[{'prompt': q, 'options': o, 'answer': a} for q, o, a in d['questions']])

        gap_idx = 1
        plan = [
            word_or_image(0),
            select_word(1, en_to_ru=True) if not words[1][3] else select_image(1),
            tiles_ru_en(0),
            word_or_image(2),
        ]
        g = None
        for si in (1, 2, 3, 4, 5, 6, 7):
            g = fill_gap(si)
            if g:
                gap_idx = si
                break
        plan.append(g or select_sentence(1))
        plan += [
            listen_tiles(2 if gap_idx != 2 else 3),
            match(),
            tiles_en_ru(3 if gap_idx != 3 else 4),
            type_tr(min(4, len(sents) - 1) if gap_idx != 4 else 6),
            speak(5),
            select_sentence(6),
            listen_type(words[3][1], [words[3][0]]),
            dialogue(),
            word_or_image(4),
            tiles_ru_en(7),
        ]
        for i, e in enumerate(plan):
            e['order'] = i
        return {'id': pid, 'type': 'lesson', 'title': l['title'], 'newWords': [w[0] for w in words], 'exercises': plan}

    def story_node(self, nid, title, lines, questions, seed):
        rnd = random.Random(seed)
        exs = [self.ex(f'{nid}-d', type='dialogue', text=title,
                       lines=[{'speaker': s, 'en': en, 'ru': ru} for s, en, ru in lines],
                       questions=[{'prompt': q, 'options': o, 'answer': a} for q, o, a in questions])]
        short = [x for x in lines if 2 <= len(tokens(x[1])) <= 7]
        if short:
            s, en, ru = short[0]
            pool = [t for x in lines for t in tokens(x[1])]
            exs.append(self.tiles(rnd, en, [en], pool, f'{nid}-t', ru, 'ru_en'))
        if len(short) > 1:
            s, en, ru = short[-1]
            exs.append(self.ex(f'{nid}-l', type='listen_type', text=en, audio=en, answers=[en]))
        for i, e in enumerate(exs):
            e['order'] = i
        return {'id': nid, 'type': 'story', 'title': title, 'exercises': exs}

    # ---------- legacy 0.9 content ----------
    def build_legacy(self):
        lessons = json.load(open(os.path.join(LIB, 'lessons.json'), encoding='utf-8'))
        stories = json.load(open(os.path.join(LIB, 'stories.json'), encoding='utf-8'))
        grammar = json.load(open(os.path.join(LIB, 'grammar.json'), encoding='utf-8'))
        dictionary = json.load(open(os.path.join(LIB, 'dictionary.json'), encoding='utf-8'))
        for d in dictionary:
            if ' ' not in d['en'].strip():
                self.hint(d['en'], d['ru'])
                self.hint(d['ru'], d['en'])

        seen = {norm(l['word']) for l in self.lexemes.values()}
        parts = []   # (lesson_index, part_suffix, title, items)
        for i, l in enumerate(lessons):
            items = []
            for qi, q in enumerate(l['questions']):
                quoted = re.search(r'«(.*)»', q['prompt']).group(1)
                ans = q['choices'][q['answer']]
                ru_en = q['prompt'].startswith('Как по-английски')
                en, ru = (ans, quoted) if ru_en else (quoted, ans)
                items.append({'q': q, 'qi': qi, 'en': en.strip(), 'ru': ru.strip(), 'ru_en': ru_en})
            new_items = []
            for it in items:
                k = norm(it['en'])
                if k not in seen:
                    seen.add(k)
                    new_items.append(it)
                it['new'] = it in new_items
            if len(new_items) > 5:
                # Keep ≤ 5 new items per lesson: split into two parts, each with its own new words.
                half = (len(new_items) + 1) // 2
                first_new = new_items[:half]
                a = [it for it in items if not it['new'] or it in first_new]
                b = [it for it in items if it['new'] and it not in first_new]
                review = [it for it in items if not it['new']]
                parts.append((i, 'a', l['title'] + ' · часть 1', a))
                parts.append((i, 'b', l['title'] + ' · часть 2', b + review + first_new[:max(0, 4 - len(b))]))
            else:
                parts.append((i, '', l['title'], items))

        # Lexemes for all legacy pairs
        for _, _, _, items in parts:
            for it in items:
                lid = slug(it['en'])
                if lid in self.lexemes and norm(self.lexemes[lid]['word']) != norm(it['en']):
                    lid = 'x_' + lid
                it['lid'] = self.lexeme(lid, it['en'], it['ru'])

        sections = [('s2', 'Раздел 2 · Закрепление', 'A1', 'Слова и фразы на каждый день: повторяем и расширяем', 0, 100),
                    ('s3', 'Раздел 3 · Уверенный старт', 'A2', 'Диалоги в городе, дома, в школе и в пути', 100, 200),
                    ('s4', 'Раздел 4 · Свободнее', 'B1', 'Мнения, планы и непростые ситуации', 200, 300)]
        level_of = {'A1': 'A1', 'A2': 'A2', 'B1': 'B1'}
        story_queue = {lv: [s for s in stories if s.get('level') == lv] for lv in ('A1', 'A2', 'B1')}
        grammar_by_level = {lv: [g for g in grammar if g['level'] == lv] for lv in ('A1', 'A2', 'B1', 'B2')}
        grammar_by_level['B1'] += grammar_by_level.pop('B2')
        out_sections = []
        color_i = 3
        unit_no = 4
        story_used = 0
        for sid, stitle, cefr, sdesc, lo, hi in sections:
            sec_parts = [p for p in parts if lo <= p[0] < hi]
            chunks = [sec_parts[i:i + 10] for i in range(0, len(sec_parts), 10)]
            units = []
            topics = grammar_by_level[cefr]
            for ci, chunk in enumerate(chunks):
                uid = f'u{unit_no:02d}'
                base_titles = list(dict.fromkeys(re.split(r'\s·\s', p[2])[0] for p in chunk))
                title = ', '.join(base_titles[:3])
                rnd = random.Random(uid)
                lesson_nodes = [self.legacy_lesson(p, rnd, sec_parts) for p in chunk]
                st_list = story_queue[cefr]
                story_node = None
                if st_list:
                    st = st_list.pop(0)
                    story_used += 1
                    story_node = self.legacy_story(f'{uid}-story', st, rnd)
                nodes = lesson_nodes[:4] + [{'id': f'{uid}-chest', 'type': 'chest', 'title': 'Сундук'}] + lesson_nodes[4:7]
                if story_node:
                    nodes.append(story_node)
                nodes += lesson_nodes[7:] + [{'id': f'{uid}-review', 'type': 'review', 'title': 'Повторение юнита'},
                                             {'id': f'{uid}-test', 'type': 'test', 'title': 'Проверка юнита'}]
                g = topics[ci % len(topics)] if topics else None
                guide = []
                if g:
                    guide.append({'title': g['title'], 'text': g['rule'],
                                  'examples': [self.split_example(e) for e in g['examples']]})
                g2 = topics[(ci + len(chunks)) % len(topics)] if len(topics) > 1 else None
                if g2 and g2 is not g:
                    guide.append({'title': g2['title'], 'text': g2['rule'],
                                  'examples': [self.split_example(e) for e in g2['examples']]})
                first_idx = chunk[0][0] + 1
                last_idx = chunk[-1][0] + 1
                units.append({'id': uid, 'title': title, 'color': UNIT_COLORS[color_i % len(UNIT_COLORS)],
                              'description': f'Уроки {first_idx}–{last_idx} программы LinguaFree 0.9 · {cefr}',
                              'guidebook': guide, 'nodes': nodes})
                color_i += 1
                unit_no += 1
            out_sections.append({'id': sid, 'title': stitle, 'cefr': cefr, 'description': sdesc, 'units': units})
        return out_sections

    @staticmethod
    def split_example(e):
        if ' — ' in e:
            en, ru = e.split(' — ', 1)
            return {'en': en.strip(), 'ru': ru.strip()}
        return {'en': e.strip(), 'ru': ''}

    def legacy_lesson(self, part, rnd, sec_parts):
        idx, suffix, title, items = part
        nid = f'legacy-L{idx:03d}{suffix}'
        exs = []
        # 1) Original questions, preserved as multiple-choice translation.
        for it in items:
            q = it['q']
            if it['ru_en']:
                exs.append(self.ex(f'{nid}-q{it["qi"]}', type='select_translation', direction='ru_en', text=it['ru'],
                                   options=q['choices'], answers=[it['en']], lexemes=[it['lid']]))
            else:
                exs.append(self.ex(f'{nid}-q{it["qi"]}', type='select_translation', direction='en_ru', text=it['en'],
                                   options=q['choices'], answers=[it['ru']], lexemes=[it['lid']], audio=it['en']))
        # 2) Generated practice of the same items in other formats (≥ 12 exercises in total).
        en_pool = [t for p in sec_parts[:40] for it2 in p[3] for t in tokens(it2['en'])]
        ru_pool = [t for p in sec_parts[:40] for it2 in p[3] for t in tokens(it2['ru'])]
        multi = [it for it in items if len(tokens(it['en'])) >= 2]
        short = [it for it in items if 1 <= len(tokens(it['en'])) <= 4]
        gen = []
        if multi:
            it = multi[0]
            gen.append(dict(self.tiles(rnd, it['en'], [it['en']], en_pool, f'{nid}-t', it['ru'], 'ru_en'), lexemes=[it['lid']]))
        if len(multi) > 1:
            it = multi[1]
            gen.append(dict(self.tiles(rnd, it['en'], [it['en']], en_pool, f'{nid}-lt', it['en'], 'ru_en', listen=True), lexemes=[it['lid']]))
        uniq = list({it['en'].lower(): it for it in items if len(tokens(it['en'])) <= 4}.values())
        if len(uniq) >= 5:
            pairs, ru_seen = [], set()
            for it in uniq:
                if it['ru'] not in ru_seen:
                    ru_seen.add(it['ru'])
                    pairs.append(it)
            if len(pairs) >= 5:
                gen.append(self.ex(f'{nid}-m', type='match', text='', pairs=[{'en': it['en'].rstrip('.!?'), 'ru': it['ru'].rstrip('.!?')} for it in pairs[:5]],
                                   lexemes=[it['lid'] for it in pairs[:5]]))
        if short:
            it = short[-1]
            gen.append(self.ex(f'{nid}-ty', type='type', direction='ru_en', text=it['ru'], answers=[it['en']], lexemes=[it['lid']]))
        gap_src = [it for it in multi if len(tokens(it['en'])) >= 3]
        if gap_src:
            it = gap_src[-1]
            toks = tokens(it['en'])
            word = max(toks, key=len)
            m = re.search(r'\b' + re.escape(word) + r'\b', it['en'])
            pool = [t for t in en_pool if t.lower() != word.lower() and len(t) > 2]
            if m and len(set(pool)) >= 2:
                opts = [word] + rnd.sample(sorted(set(pool)), 2)
                rnd.shuffle(opts)
                gen.append(self.ex(f'{nid}-g', type='fill_gap', text=it['en'][:m.start()] + '___' + it['en'][m.end():],
                                   subtitle=it['ru'], options=opts, answers=[word], lexemes=[it['lid']], audio=it['en']))
        sp = [it for it in items if 1 <= len(tokens(it['en'])) <= 7]
        if sp:
            it = sp[len(sp) // 2]
            gen.append(self.ex(f'{nid}-sp', type='speak', text=it['en'], audio=it['en'], answers=[it['en']], lexemes=[it['lid']]))
        ru_tiles = [it for it in items if 2 <= len(tokens(it['ru'])) <= 7]
        if ru_tiles:
            it = ru_tiles[0]
            gen.append(dict(self.tiles(rnd, it['ru'], [it['ru']], ru_pool, f'{nid}-tr', it['en'], 'en_ru'), lexemes=[it['lid']]))
        lt = [it for it in items if 1 <= len(tokens(it['en'])) <= 6]
        if lt:
            it = lt[0]
            gen.append(self.ex(f'{nid}-ly', type='listen_type', text=it['en'], audio=it['en'], answers=[it['en']], lexemes=[it['lid']]))
        # Interleave: originals and generated, starting with an original question.
        mixed = []
        g_i = 0
        for i, e in enumerate(exs):
            mixed.append(e)
            if i % 2 == 1 and g_i < len(gen):
                mixed.append(gen[g_i]); g_i += 1
        mixed += gen[g_i:]
        # Pad to 12 with other formats of this lesson's and the previous lesson's items (no exact repeats).
        used = {(e['type'], e.get('direction'), norm(e.get('text', ''))) for e in mixed}
        pad_pool = items + [x for x in getattr(self, '_prev_items', []) if x not in items]
        k = 0
        guard = 0
        while len(mixed) < 12 and pad_pool and guard < 60:
            guard += 1
            it = pad_pool[k % len(pad_pool)]
            kind = (k // len(pad_pool)) % 3
            k += 1
            if kind == 0:
                key = ('select_translation', 'en_ru', norm(it['en']))
                if key in used:
                    continue
                pool = [x['ru'] for x in pad_pool if norm(x['ru']) != norm(it['ru'])]
                if len(pool) < 2:
                    continue
                opts = [it['ru']] + rnd.sample(list(dict.fromkeys(pool)), 2)
                rnd.shuffle(opts)
                e = self.ex(f'{nid}-r{k}', type='select_translation', direction='en_ru', text=it['en'], options=opts,
                            answers=[it['ru']], lexemes=[it['lid']], audio=it['en'])
            elif kind == 1:
                key = ('type', 'ru_en', norm(it['ru']))
                if key in used or len(tokens(it['en'])) > 6:
                    continue
                e = self.ex(f'{nid}-r{k}', type='type', direction='ru_en', text=it['ru'], answers=[it['en']], lexemes=[it['lid']])
            else:
                key = ('listen_type', None, norm(it['en']))
                if key in used or len(tokens(it['en'])) > 6:
                    continue
                e = self.ex(f'{nid}-r{k}', type='listen_type', text=it['en'], audio=it['en'], answers=[it['en']], lexemes=[it['lid']])
            used.add(key)
            mixed.append(e)
        self._prev_items = items
        for i, e in enumerate(mixed):
            e['order'] = i
        new_words = [it['lid'] for it in items if it.get('new')][:5]
        return {'id': nid, 'type': 'lesson', 'title': title, 'newWords': new_words, 'exercises': mixed}

    def legacy_story(self, nid, story, rnd):
        lines = [('', ln['en'], ln['ru']) for ln in story['lines']]
        qs = []
        cand = list(range(len(lines)))
        rnd.shuffle(cand)
        all_ru = [ln[2] for ln in lines]
        for ci in cand[:min(3, len(lines))]:
            opts = [lines[ci][2]] + [r for r in all_ru if r != lines[ci][2]][:2]
            if len(opts) < 2:
                continue
            ans_text = opts[0]
            rnd.shuffle(opts)
            qs.append((f'Что значит «{lines[ci][1]}»?', opts, opts.index(ans_text)))
        return self.story_node(nid, story['title'], lines, qs, seed=nid)

    def write(self):
        os.makedirs(os.path.join(OUT, 'units'), exist_ok=True)
        for f in os.listdir(os.path.join(OUT, 'units')):
            os.remove(os.path.join(OUT, 'units', f))
        start_units = self.build_start_units()
        legacy_sections = self.build_legacy()
        sections = [{'id': 's1', 'title': 'Раздел 1 · Старт', 'cefr': 'A1', 'description': 'Первые слова и фразы: знакомство, еда, семья',
                     'units': start_units}] + legacy_sections
        course = {'schemaVersion': SCHEMA_VERSION, 'contentVersion': CONTENT_VERSION, 'id': 'en-ru',
                  'title': 'Английский для русскоязычных', 'sections': []}
        for s in sections:
            course['sections'].append({'id': s['id'], 'title': s['title'], 'cefr': s['cefr'], 'description': s['description'],
                                       'units': [u['id'] for u in s['units']]})
            for u in s['units']:
                u = dict(u, schemaVersion=SCHEMA_VERSION)
                with open(os.path.join(OUT, 'units', u['id'] + '.json'), 'w', encoding='utf-8') as f:
                    json.dump(u, f, ensure_ascii=False, separators=(',', ':'))
        with open(os.path.join(OUT, 'course.json'), 'w', encoding='utf-8') as f:
            json.dump(course, f, ensure_ascii=False, indent=1)
        with open(os.path.join(OUT, 'lexemes.json'), 'w', encoding='utf-8') as f:
            json.dump({'schemaVersion': SCHEMA_VERSION, 'lexemes': list(self.lexemes.values())}, f, ensure_ascii=False, separators=(',', ':'))
        with open(os.path.join(OUT, 'lexicon.json'), 'w', encoding='utf-8') as f:
            json.dump({'schemaVersion': SCHEMA_VERSION, 'hints': self.lexicon}, f, ensure_ascii=False, separators=(',', ':'))
        n_units = sum(len(s['units']) for s in sections)
        n_lessons = sum(1 for s in sections for u in s['units'] for nd in u['nodes'] if nd['type'] == 'lesson')
        n_ex = len(self.exercise_ids)
        print(f'Sections {len(sections)}, units {n_units}, lessons {n_lessons}, exercises {n_ex}, lexemes {len(self.lexemes)}, hints {len(self.lexicon)}')


if __name__ == '__main__':
    Builder().write()
