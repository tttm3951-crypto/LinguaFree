#!/usr/bin/env python3
"""Structural validation of LinguaFree lessons and original stories."""
import json
import hashlib
from pathlib import Path
from collections import defaultdict

root = Path(__file__).resolve().parents[1] / "app/src/main/assets"
lessons = json.loads((root / "lessons.json").read_text(encoding="utf-8"))
stories = json.loads((root / "stories.json").read_text(encoding="utf-8"))
errors = []
signatures = defaultdict(list)
unique = set()
if len(lessons) != 300:
    errors.append(f"Expected 300 lessons; got {len(lessons)}")
for i, lesson in enumerate(lessons, 1):
    qs = lesson.get("questions", [])
    if not lesson.get("title") or len(qs) != 7:
        errors.append(f"Lesson {i}: missing title or not exactly seven questions")
    sig = []
    for j, q in enumerate(qs, 1):
        options = q.get("choices", [])
        ans = q.get("answer")
        if not all(isinstance(q.get(k), str) and q[k].strip() for k in ("prompt", "speech", "explanation")):
            errors.append(f"Lesson {i}, question {j}: missing text")
        if not isinstance(ans, int) or not (0 <= ans < len(options)):
            errors.append(f"Lesson {i}, question {j}: invalid answer index")
            continue
        if len(options) < 2 or len(set(x.strip().casefold() for x in options)) != len(options):
            errors.append(f"Lesson {i}, question {j}: duplicate or missing options")
        signature = (q["prompt"], options[ans])
        sig.append(signature)
        unique.add(signature)
    if len(sig) != len(set(sig)):
        errors.append(f"Lesson {i}: repeated question")
    signatures[tuple(sig)].append(i)
if len(stories) < 20:
    errors.append(f"Too few stories: {len(stories)}")
for i, story in enumerate(stories, 1):
    if not story.get("title") or not story.get("level") or len(story.get("lines", [])) < 3:
        errors.append(f"Story {i}: invalid metadata")
    for j, line in enumerate(story.get("lines", []), 1):
        if not line.get("en") or not line.get("ru"):
            errors.append(f"Story {i}, line {j}: missing translation")
duplicates = [v for v in signatures.values() if len(v) > 1]
print(f"Lessons: {len(lessons)}; questions: {sum(len(x['questions']) for x in lessons)}")
print(f"Unique question-answer pairs: {len(unique)}; identical lesson groups: {len(duplicates)}")
print(f"Original stories: {len(stories)}")
for group in duplicates[:12]:
    print("CONTENT WARNING: same question set in lessons", ", ".join(map(str, group)))
if duplicates:
    errors.append(f"Repeated full lesson sets: {len(duplicates)}")
if len(unique) < 900:
    errors.append(f"Vocabulary diversity regression: only {len(unique)} distinct prompt-answer pairs; expected at least 900")
dictionary = json.loads((root / "dictionary.json").read_text(encoding="utf-8"))
identity_hash=hashlib.sha256(json.dumps([(x['en'],x['ru']) for x in dictionary],ensure_ascii=False,separators=(',',':')).encode()).hexdigest()
if identity_hash != '0b0d08b24e22c917ac090ffc1e9936b8e5e971d2d025e8f50822635dae8df12f':
    errors.append('Dictionary identities/order changed: migrate existing card progress before accepting this change')
seen_dictionary = set()
with_examples = 0
for i, entry in enumerate(dictionary, 1):
    for field in ("en", "ru", "level", "source", "example_en", "example_ru"):
        if not isinstance(entry.get(field), str) or not entry[field].strip():
            errors.append(f"Dictionary {i}: missing {field}")
    key = (entry.get("en", "").casefold(), entry.get("ru", "").casefold())
    if key in seen_dictionary:
        errors.append(f"Dictionary {i}: duplicate pair")
    seen_dictionary.add(key)
    if entry.get("example_en", "").casefold() != entry.get("en", "").casefold():
        with_examples += 1
if len(dictionary) < 600:
    errors.append(f"Dictionary too small: {len(dictionary)}")
if with_examples != len(dictionary):
    errors.append(f"Missing contextual examples: {len(dictionary)-with_examples}")
print(f"Offline dictionary: {len(dictionary)} entries; {with_examples} contextual examples")
if errors:
    print("\n".join("ERROR: " + error for error in errors))
    raise SystemExit(1)
print("Structural checks passed. This is not a linguistic accuracy certification.")

