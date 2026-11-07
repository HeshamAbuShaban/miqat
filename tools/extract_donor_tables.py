#!/usr/bin/env python3
"""Extract the donor's correction tables from the decompiled sources into JSON.

Outputs (repo-relative, committed):
  tools/qibla_adjusts.json  — seasonal correction rows (donor class v5.f "QiblaAdjust",
                              embedded in y5/b.java; identical to donor DB table `qibla`)
  tools/city_tweaks.json    — per-city minute tweaks from t5/a.adjustTimes switch
                              (guarded by method: belgium / paris(FR) / birmingham(UK))

Read-only over donor/ ; rerun after any future donor refresh.
Usage: python3 -I tools/extract_donor_tables.py
"""
import json
import re

B = "donor/extracted-code/sources/y5/b.java"
A = "donor/extracted-code/sources/t5/a.java"


def extract_qibla():
    src = open(B, encoding="utf-8").read()
    a = src.index("Iterable iterable = (Iterable) aVar.a(")
    s = src.index('"', a)
    j = s + 1
    while True:  # find closing unescaped quote
        j = src.index('"', j)
        k, nb = j - 1, 0
        while src[k] == "\\":
            nb += 1
            k -= 1
        if nb % 2 == 0:
            break
        j += 1
    rows = json.loads(json.loads(src[s:j + 1]))
    for r in rows:  # normalize key order
        keys = ["m", "mn", "c", "b", "e", "fa", "ms", "mv", "is", "iv",
                "fm", "sm", "dm", "am", "mm", "im", "ism"]
        rows[rows.index(r)] = {k: r[k] for k in keys if k in r}
    json.dump(rows, open("tools/qibla_adjusts.json", "w"), indent=1, ensure_ascii=False)
    print(f"qibla_adjusts.json: {len(rows)} rows, methods={sorted({r['mn'] for r in rows})}")


# t5/a.java a() city-tweak switch: three method-guarded regions by line range
REGIONS = [("belgium", 189, 677), ("paris", 677, 951), ("birmingham", 951, 1406)]
CASE = re.compile(
    r'equals\("([^"]+)"\)\)\s*\{((?:\s*dArr\[(\d)\] = dArr\[\3\] [+-] \d+\.\d+d;)+)\s*\}'
)
ADD = re.compile(r"dArr\[(\d)\] = dArr\[\1\] ([+-]) (\d+\.\d+)d;")


def extract_tweaks():
    lines = open(A, encoding="utf-8").readlines()
    out = {}
    for method, lo, hi in REGIONS:
        seg = "".join(lines[lo - 1:hi])
        for m in CASE.finditer(seg):
            city, body, _ = m.group(1), m.group(2), m.group(3)
            tweaks = [0.0] * 7
            for a in ADD.finditer(body):
                idx, sign, val = int(a.group(1)), a.group(2), float(a.group(3))
                tweaks[idx] = val if sign == "+" else -val
            out.setdefault(method, {})[city] = tweaks
    json.dump(out, open("tools/city_tweaks.json", "w"), indent=1, ensure_ascii=False)
    for k, v in out.items():
        print(f"city_tweaks.json[{k}]: {len(v)} cities")


if __name__ == "__main__":
    extract_qibla()
    extract_tweaks()
