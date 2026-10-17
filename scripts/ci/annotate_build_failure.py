#!/usr/bin/env python3
"""Turn a failed Gradle log into GitHub annotations (readable without downloading raw logs)."""
import re, sys
log = open(sys.argv[1], encoding='utf-8', errors='replace').read().splitlines()

def emit(title, lines):
    msg = '%0A'.join(l.replace('%', '%25').replace('\r', '') for l in lines)[:3800]
    print(f'::error title={title}::{msg}')

errors = [l for l in log if re.match(r'^(e: |ERROR:|\[ksp\] |.*error: |.*Error: )', l) or ' error: ' in l]
lint = [l for l in log if 'Lint found' in l or re.search(r'\.(kt|xml):\d+: Error:', l)]
tests = [l for l in log if re.search(r'(FAILED|Test.*>.*FAILED|AssertionFailedError|expected:)', l)]
wrong, grab = [], 0
for l in log:
    if 'What went wrong' in l: grab = 25
    if grab: wrong.append(l); grab -= 1
chunks = [errors[i:i + 25] for i in range(0, min(len(errors), 175), 25)]
for n, c in enumerate(chunks, 1):
    emit(f'Compiler errors {n}', c)
if lint: emit('Lint', lint[:40])
if tests: emit('Tests', tests[:40])
if wrong: emit('What went wrong', wrong)
if not (errors or lint or tests or wrong):
    emit('Build failed', log[-40:])
