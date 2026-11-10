#!/usr/bin/env python3
"""Build the Miqat city store from the donor cities DB (teardown §3).

Output: app/src/main/assets/cities-v<VERSION>.db — SQLite, opened read-only at
runtime (decisions.md 2026-11-10 (2)). The FILENAME carries the asset version
(`cities-v2.db` today); bump VERSION whenever the schema or rows change so the
app's copy-on-first-open logic can replace stale copies.

  cities(_id, en, ar, en_lower, country, lat, lon, tz, method)
    en_lower — pre-folded (lowercase) for ASCII prefix/contains search
    ar       — Arabic name; contains-match via LIKE (leading wildcard = scan,
               acceptable at 218k rows; prefix queries use the en_lower index)
  meta(key, value) — provenance (source db sha, row count, built date)

The old v0 cities.tsv asset is retired (whole-file scan replaced by indexed
queries; nothing loads 218k rows into memory).

Usage: python3 -I tools/build_miqat_assets.py
"""
import hashlib
import os
import sqlite3
import unicodedata
from datetime import date

DB = "donor/cities-db/SalatukDB.db"
OUT = "app/src/main/assets/cities-v2.db"
VERSION = 2


def clean(s):
    if s is None:
        return ""
    s = unicodedata.normalize("NFC", str(s)).replace("|", "/").strip()
    return s


def main():
    src = sqlite3.connect("file:" + DB + "?mode=ro", uri=True)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    if os.path.exists(OUT):
        os.remove(OUT)
    out = sqlite3.connect(OUT)
    out.executescript("""
      PRAGMA journal_mode=OFF;
      CREATE TABLE cities(
        _id INTEGER PRIMARY KEY,
        en TEXT NOT NULL, ar TEXT NOT NULL, en_lower TEXT NOT NULL,
        country TEXT NOT NULL,
        lat REAL NOT NULL, lon REAL NOT NULL,
        tz REAL NOT NULL, method TEXT NOT NULL);
      CREATE TABLE meta(key TEXT PRIMARY KEY, value TEXT NOT NULL);
    """)
    n = 0
    rows = []
    for (en, ar, lat, lon, tz, cc, method) in src.execute(
            "SELECT city_name, city_name_ar, latitude, longitude, utc_offset,"
            " country_code, calc_method FROM cities"):
        en, ar = clean(en), clean(ar)
        if not en or lat is None or lon is None:
            continue
        try:
            lat, lon, tz = float(lat), float(lon), float(tz)
        except (TypeError, ValueError):
            continue
        rows.append((en, ar, en.lower(), clean(cc), round(lat, 5), round(lon, 5),
                     tz, clean(method) or "makkah"))
        n += 1
    out.executemany(
        "INSERT INTO cities(en, ar, en_lower, country, lat, lon, tz, method)"
        " VALUES (?,?,?,?,?,?,?,?)", rows)
    sha = hashlib.sha256(open(DB, "rb").read()).hexdigest()
    out.executemany("INSERT INTO meta(key, value) VALUES (?,?)", [
        ("asset_version", str(VERSION)),
        ("source", "donor cities DB (teardown §3)"),
        ("source_sha256", sha[:16]),
        ("rows", str(n)),
        ("built", date.today().isoformat()),
    ])
    out.commit()
    out.executescript("""
      CREATE INDEX idx_cities_en_lower ON cities(en_lower);
      CREATE INDEX idx_cities_ar ON cities(ar);
      ANALYZE;
      VACUUM;
    """)
    out.close()
    size = os.path.getsize(OUT)
    print(f"wrote {OUT}: {n} cities, {size/1e6:.1f} MB, source sha {sha[:16]}")


if __name__ == "__main__":
    main()
