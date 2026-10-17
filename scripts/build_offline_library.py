"""Build a bundled, indexed EN/RU reference. Never downloads at app runtime."""
import argparse, hashlib, io, json, re, sqlite3, urllib.request, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
URL = 'https://www.manythings.org/anki/rus-eng.zip'
SHA256 = '1534e267976f43ae97e966d2ac9dc1e9128fdd0efdf08eceee21cd88ff20682c'
TOTAL = 150_000
# Broad corpus is for explicit reference searches, never automatic children's lessons.
EXCLUDE = re.compile(r'\b(fuck\w*|shit\w*|bitch\w*|porn\w*|sex\w*|rape\w*|suicid\w*|kill\w*|murder\w*|naked|nude|cocaine|heroin|vodka|beer|wine|drunk|gun\w*|bomb\w*|terror\w*)\b|\b(секс\w*|порн\w*|уби\w*|убь\w*|убей\w*|самоуб\w*|наркот\w*|изнасил\w*|пистолет\w*|бомб\w*|водк\w*|пив\w*|пьян\w*|голый|голая)\b', re.I)

def build(archive, output):
    request=urllib.request.Request(URL,headers={'User-Agent':'Mozilla/5.0 LinguaFree-offline-build/0.9','Accept':'application/zip,*/*'})
    raw = Path(archive).read_bytes() if archive else urllib.request.urlopen(request, timeout=120).read()
    assert hashlib.sha256(raw).hexdigest() == SHA256, 'Source changed: review before updating pinned checksum'
    core = json.loads((ROOT/'core/data/src/main/assets/library/dictionary.json').read_text(encoding='utf-8'))
    rows = []; seen = set()
    for item in core:
        key = (item['en'].casefold(), item['ru'].casefold())
        assert key not in seen
        seen.add(key)
        rows.append((item['en'], item['ru'], item['source'], 'core'))
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        for line in z.read('rus.txt').decode('utf-8-sig').splitlines():
            parts = line.split('\t')
            if len(parts) != 3: continue
            en, ru, attribution = parts
            key = (en.casefold(), ru.casefold())
            if key in seen or not re.search('[A-Za-z]', en) or not re.search('[А-Яа-яЁё]', ru): continue
            if len(en)>180 or len(ru)>240 or EXCLUDE.search(en+' '+ru): continue
            if not attribution.startswith('CC-BY 2.0 (France) Attribution: tatoeba.org #'): continue
            seen.add(key); rows.append((en, ru, attribution, 'corpus'))
            if len(rows) == TOTAL: break
    assert len(rows) == TOTAL, f'Only {len(rows)} unique pairs; do not pad with generated duplicates'
    memory = str(output) == ':memory:'
    if not memory:
        output = Path(output); output.parent.mkdir(parents=True, exist_ok=True)
        output.unlink(missing_ok=True)
    db = sqlite3.connect(':memory:' if memory else str(output))
    db.execute('CREATE TABLE entries(id INTEGER PRIMARY KEY,en TEXT NOT NULL,ru TEXT NOT NULL,credit TEXT NOT NULL,kind TEXT NOT NULL)')
    db.executemany('INSERT INTO entries(en,ru,credit,kind) VALUES(?,?,?,?)', rows)
    db.execute("CREATE VIRTUAL TABLE search USING fts4(en,ru,tokenize=unicode61)")
    db.execute('INSERT INTO search(docid,en,ru) SELECT id,en,ru FROM entries')
    db.execute('CREATE TABLE metadata(key TEXT PRIMARY KEY,value TEXT NOT NULL)')
    db.executemany('INSERT INTO metadata VALUES(?,?)', [('count',str(TOTAL)),('core',str(len(core))),('source',URL),('source_sha256',SHA256),('license','Tatoeba CC BY 2.0 FR; per-entry author credit retained; core: LinguaFree original')])
    db.commit()
    assert db.execute('PRAGMA integrity_check').fetchone()[0] == 'ok'
    assert db.execute("SELECT count(*) FROM search WHERE search MATCH 'hello*'").fetchone()[0] > 0
    assert db.execute("SELECT count(*) FROM search WHERE search MATCH 'привет*'").fetchone()[0] > 0
    db.execute('VACUUM')
    size=len(db.serialize()) if memory else output.stat().st_size
    db.close()
    print(f'Offline library: {len(core)} core + {TOTAL-len(core)} corpus = {TOTAL}; {size} bytes; integrity and EN/RU search passed')

if __name__ == '__main__':
    p=argparse.ArgumentParser(); p.add_argument('--archive'); p.add_argument('--output',default=str(ROOT/'core/data/src/main/assets/offline-reference.db'))
    a=p.parse_args(); build(a.archive,a.output)
