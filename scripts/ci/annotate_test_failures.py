#!/usr/bin/env python3
"""Turn JUnit XML failures (unit or instrumented) into GitHub ::error annotations."""
import glob, sys
import xml.etree.ElementTree as ET

count = 0
patterns = sys.argv[1:] or ['**/build/outputs/androidTest-results/**/*.xml', '**/build/test-results/**/*.xml']
for pattern in patterns:
    for path in glob.glob(pattern, recursive=True):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        for case in root.iter('testcase'):
            for f in list(case.findall('failure')) + list(case.findall('error')):
                text = (f.get('message') or '') + '\n' + (f.text or '')
                text = text.strip()[:4500].replace('%', '%25').replace('\r', '').replace('\n', '%0A')
                print(f"::error title={case.get('classname')}.{case.get('name')}::{text}")
                count += 1
# Logcat crash lines help when the app dies before the assertion.
for path in glob.glob('device-artifacts/logcat.txt'):
    lines = open(path, errors='replace').read().splitlines()
    crash = [i for i, l in enumerate(lines) if 'FATAL EXCEPTION' in l]
    for i in crash[:2]:
        print('::error title=Crash in logcat::' + '%0A'.join(lines[i:i + 25])[:3000])
print(f'{count} failing test(s) annotated')
