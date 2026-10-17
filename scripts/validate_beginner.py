"""Original beginner course contracts and reproducibility checks."""
import json, re
from pathlib import Path
from build_beginner import build

root=Path(__file__).resolve().parents[1]
data=json.loads((root/'app/src/main/assets/beginner.json').read_text(encoding='utf-8'))
assert data==build(), 'Run build_beginner.py after editing course sources'
assert len(data)==21 and sum(len(t['questions']) for t in data)==105
assert len({t['id'] for t in data})==len(data)
assert data[0]['group']=='reading' and data[-1]['group']=='time'
for topic in data:
    assert topic['level'] in ('Pre-A1','A1')
    assert topic['rule'] and len(topic['examples'])>=2
    assert len(topic['questions'])==5
    for q in topic['questions']:
        assert len(q['choices'])==len(set(q['choices']))==3
        assert 0<=q['answer']<3 and q['speech'] and q['explanation']
        assert q['prompt'] and len(q['speech'])<250
# Count speech targets across every planned course; full phrases are exempt.
from collections import Counter
targets=Counter()
for name in ['lessons.json','grammar.json','beginner.json']:
    content=json.loads((root/'app/src/main/assets'/name).read_text(encoding='utf-8'))
    for topic in content:
        for q in topic['questions']:
            value=q.get('speech','').strip().casefold().strip('.!?')
            if re.fullmatch(r"[a-z]+(?:['’-][a-z]+)*",value): targets[value]+=1
assert max(targets.values(),default=0)<=5
print('PASS: 21 original beginner topics / 105 questions; progression, reproducibility and standalone-word cap')
