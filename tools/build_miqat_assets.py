#!/usr/bin/env python3
"""Build the Miqat v1 city asset from the donor cities DB (teardown §3).

Output: app/src/main/assets/cities.tsv.gz
  columns (TSV, header included):
    en | ar | cc | lat | lon | utc_offset | calc_method
Rows: every city with a usable name and coordinates. Names are NFC-normalized,
pipe-free (| is the separator — replaced inside names).

Usage: python3 -I tools/build_miqat_assets.py
"""
import gzip
import sqlite3
import unicodedata

DB = "donor/cities-db/SalatukDB.db"
OUT = "app/src/main/assets/cities.tsv.gz"


def clean(s):
    if s is None:
        return ""
    s = unicodedata.normalize("NFC", str(s)).replace("|", "/").strip()
    return s


def main():
    con = sqlite3.connect("file:" + DB + "?mode=ro", uri=True)
    n = 0
    with gzip.open(OUT, "wt", encoding="utf-8", compresslevel=9) as f:
        f.write("en|ar|cc|lat|lon|tz|method\n")
        for (en, ar, lat, lon, tz, cc, method) in con.execute(
                "SELECT city_name, city_name_ar, latitude, longitude, utc_offset,"
                " country_code, calc_method FROM cities"):
            en, ar = clean(en), clean(ar)
            if not en or lat is None or lon is None:
                continue
            try:
                lat, lon, tz = float(lat), float(lon), float(tz)
            except (TypeError, ValueError):
                continue
            f.write(f"{en}|{ar}|{clean(cc)}|{lat:.5f}|{lon:.5f}|{tz:g}|{clean(method) or 'makkah'}\n")
            n += 1
    print(f"wrote {OUT}: {n} cities")


if __name__ == "__main__":
    main()
