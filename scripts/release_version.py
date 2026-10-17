"""Read the single Android version source for release filenames and upgrade fixtures."""
import re
from pathlib import Path
s=(Path(__file__).resolve().parents[1]/'app/build.gradle.kts').read_text(encoding='utf-8')
version=re.search(r'versionName\s*=\s*"(\d+\.\d+(?:\.\d+)?)"',s).group(1)
code=int(re.search(r'versionCode\s*=.*?\?:\s*(\d+)',s).group(1))
assert code>1
print(f'APP_VERSION={version}')
print(f'APP_TAG=v{version}' + ('.0' if version.count('.')==1 else ''))
print(f'APP_CODE={code}')
print(f'PREVIOUS_CODE={code-1}')
