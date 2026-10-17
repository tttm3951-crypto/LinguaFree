#!/usr/bin/env python3
"""Curriculum contracts: at most five standalone targets; contextual phrases are exempt."""
import json
import re
from collections import Counter
from pathlib import Path


def standalone_target(question):
    text = question['speech'].strip().casefold().replace('’', "'").strip('.!?')
    return text if re.fullmatch(r"[a-z]+(?:['-][a-z]+)*", text) else None


def excess_words(questions):
    counts = Counter(filter(None, (standalone_target(q) for q in questions)))
    return {word: count for word, count in counts.items() if count > 5}, counts


def validate(root):
    assets = root / 'app/src/main/assets'
    lessons = json.loads((assets / 'lessons.json').read_text(encoding='utf-8'))
    grammar = json.loads((assets / 'grammar.json').read_text(encoding='utf-8'))
    all_questions = [q for lesson in lessons + grammar for q in lesson['questions']]
    excess, counts = excess_words(all_questions)
    assert not excess, f'Standalone word targets exceed five: {excess}'
    assert {t['level'] for t in grammar} == {'A1', 'A2', 'B1', 'B2'}
    topic_ids, question_ids = set(), set()
    for topic in grammar:
        assert topic['id'] not in topic_ids, 'Duplicate topic ID'
        topic_ids.add(topic['id'])
        assert topic['group'] in {'pronouns', 'verbs', 'tenses'}
        assert topic['title'].strip() and topic['rule'].strip() and topic['examples']
        assert len(topic['questions']) == 5
        for q in topic['questions']:
            assert q['id'] not in question_ids, 'Duplicate question ID'
            question_ids.add(q['id'])
            assert q['prompt'].strip() and q['explanation'].strip()
            assert len(q['choices']) == 3 and len(set(q['choices'])) == 3
            assert q['answer'] in range(3)
            assert q['speech'] == q['choices'][q['answer']]
            assert len(q['speech'].split()) > 1, 'Grammar is practised in sentences'
        for example in topic['examples']:
            assert any(q['speech'] == example for q in topic['questions'])
    for level in ['A1', 'A2', 'B1', 'B2']:
        assert {t['group'] for t in grammar if t['level'] == level} == {'pronouns', 'verbs', 'tenses'}
    # The tracked JSON must remain reproducible from the original content source.
    from build_grammar import topics
    assert grammar == topics, 'grammar.json differs from build_grammar.py'
    print(f'PASS: {len(counts)} standalone targets; maximum {max(counts.values(), default=0)} occurrences')
    print(f'PASS: {len(grammar)} grammar topics, {len(question_ids)} exercises across A1/A2/B1/B2')
    print('Phrases are exempt. This validates structure, not CEFR certification or linguistic accuracy.')


if __name__ == '__main__':
    validate(Path(__file__).resolve().parents[1])
