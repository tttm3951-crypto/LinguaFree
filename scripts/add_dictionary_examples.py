#!/usr/bin/env python3
"""Fill previously missing examples without reordering entries or changing vocabulary identities."""
import json
import re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]

def fill_examples(dictionary):
    examples={}
    for line in (ROOT/'scripts/dictionary_examples.tsv').read_text(encoding='utf-8').splitlines():
        key,en,ru=line.split('|')
        assert key not in examples, f'Duplicate source key: {key}'
        assert len(en.split())>=3 and ru.strip()
        examples[key]=(en,ru)
    for entry in dictionary:
        if entry['example_en'].casefold()!=entry['en'].casefold():
            continue
        key=entry['en']+':'+entry['ru']
        en,ru=examples.get(key,examples.get(entry['en'],(None,None)))
        assert en and ru, f'Missing example: {key}'
        # Words may be separated by possessives: brush OUR teeth is natural contextual use.
        words=re.findall(r"[a-z]+(?:'[a-z]+)*",entry['en'].casefold())
        example_words=re.findall(r"[a-z]+(?:'[a-z]+)*",en.casefold())
        pos=0
        for word in words:
            pos=example_words.index(word,pos)+1
        entry['example_en']=en
        entry['example_ru']=ru
    return dictionary

if __name__=='__main__':
    original=json.loads((ROOT/'core/data/src/main/assets/library/dictionary.json').read_text(encoding='utf-8'))
    print(json.dumps(fill_examples(original),ensure_ascii=False,indent=2))
