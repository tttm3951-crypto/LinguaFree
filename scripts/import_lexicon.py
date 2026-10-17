#!/usr/bin/env python3
"""Import a properly licensed bilingual lexicon into sharded LinguaFree JSON files.

Usage:
  python3 scripts/import_lexicon.py source.csv --license CC-BY-4.0 --source-name "Dataset title"
CSV required columns: english,russian,level,example_en,example_ru.
Only import sources whose license permits redistribution and attribution.
This script does NOT manufacture translations or inflate vocabulary counts.
"""
import argparse
import csv
import json
import re
from pathlib import Path

def main():
    p=argparse.ArgumentParser()
    p.add_argument("csv",type=Path)
    p.add_argument("--license",required=True)
    p.add_argument("--source-name",required=True)
    p.add_argument("--output",type=Path,default=Path("app/src/main/assets/lexicon"))
    args=p.parse_args()
    if args.license.upper() not in {"CC0","CC-BY-4.0","CC-BY-SA-4.0","PUBLIC-DOMAIN"}:
        p.error("Review redistribution rights and explicitly allow the source license first")
    required={"english","russian","level","example_en","example_ru"}
    items={}
    with args.csv.open(encoding="utf-8-sig",newline="") as f:
        reader=csv.DictReader(f)
        if not required.issubset(reader.fieldnames or []):
            p.error("CSV must contain english,russian,level,example_en,example_ru")
        for rowno,row in enumerate(reader,2):
            row={k:(v or "").strip() for k,v in row.items()}
            if not all(row.get(k) for k in required):
                continue
            if row["level"] not in {"A1","A2","B1","B2","C1","C2"}:
                continue
            if any(len(row[k])>400 for k in required):
                continue
            key=(row["english"].casefold(),row["russian"].casefold())
            items.setdefault(key,{**{k:row[k] for k in required},"source":args.source_name,"license":args.license})
    values=sorted(items.values(),key=lambda x:x["english"].casefold())
    args.output.mkdir(parents=True,exist_ok=True)
    for old in args.output.glob("part-*.json"):
        old.unlink()
    for i in range(0,len(values),2500):
        (args.output/f"part-{i//2500+1:04d}.json").write_text(json.dumps(values[i:i+2500],ensure_ascii=False,indent=1),encoding="utf-8")
    (args.output/"manifest.json").write_text(json.dumps({"entries":len(values),"source":args.source_name,"license":args.license,"parts":(len(values)+2499)//2500},ensure_ascii=False,indent=2),encoding="utf-8")
    print(f"Imported {len(values)} verified-format unique bilingual entries in {(len(values)+2499)//2500} shards")
    print("Translations and examples still require human language-quality review.")

if __name__=="__main__":
    main()
