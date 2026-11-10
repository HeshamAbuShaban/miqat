#!/usr/bin/env python3
"""Build the Miqat assets from the donor DB (teardown §3).

Outputs
-------
app/src/main/assets/cities-v<VERSION>.db — the city store, opened read-only at
runtime (decisions.md 2026-11-10 (2)). The FILENAME carries the asset version;
bump VERSION whenever the schema or rows change so the app's copy-on-first-open
logic replaces stale copies.

  cities(_id, en, ar, en_lower, ar_lower, country, lat, lon, tz, zone, zone_id,
         method)
    en_lower — pre-folded (lowercase) for ASCII prefix/contains search
    ar_lower — Arabic name NORMALIZED (hamza/alef/ta-marbuta folded, diacritics
               stripped) so a user who types «الاهرام» finds «الأهرام» — the
               R2 search-normalization item
    zone     — the IANA zone id (e.g. Asia/Riyadh); zone_id — the donor's
               integer key into the timezone table, so the app can compute the
               DST-correct UTC offset for any date (the S2 "own tz table")
  meta(key, value) — provenance (source db sha, row count, built date)

app/src/main/assets/miqat-data-v<VERSION>.db — the companion data:
  countries(code, name_ar, name_en, method) — Arabic country names + the
        country's default calculation method
  day_events(id, month, start_day, end_day, name_ar, name_en) — the Islamic
        days worth knowing (59 rows) — the calendar's events
  prayer_events(id, prayer_index, start_min, end_min, message_ar, message_en,
        priority) — the donor's own reminder texts with their time windows
  zones(id, code, name) and tz_rules(zone_id, abbr, start_epoch, gmt_offset,
        dst) — the timezone table (28k rules, DST-aware)

Usage: python3 -I tools/build_miqat_assets.py
"""
import hashlib
import os
import re
import sqlite3
import unicodedata
from datetime import date

DB = "donor/cities-db/SalatukDB.db"
OUT = "app/src/main/assets/cities-v3.db"
VERSION = 3
DATA_OUT = "app/src/main/assets/miqat-data-v1.db"
DATA_VERSION = 1

DIACRITICS = re.compile("[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED]")
AR_FOLD = {
    "\u0623": "\u0627", "\u0625": "\u0627", "\u0622": "\u0627", "\u0671": "\u0627",
    "\u0629": "\u0647", "\u0649": "\u064A", "\u0626": "\u064A", "\u0624": "\u0648",
    "\u0640": "",
}


def clean(s):
    if s is None:
        return ""
    s = unicodedata.normalize("NFC", str(s)).replace("|", "/").strip()
    return s


def fold_ar(s):
    """Normalize an Arabic string for tolerant search (R2)."""
    if not s:
        return ""
    s = unicodedata.normalize("NFD", s)
    s = DIACRITICS.sub("", s)
    s = "".join(AR_FOLD.get(c, c) for c in s)
    s = unicodedata.normalize("NFC", s)
    return s.strip().lower()


def build_cities(src):
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    for path in (OUT,):
        if os.path.exists(path):
            os.remove(path)
    out = sqlite3.connect(OUT)
    out.executescript("""
      PRAGMA journal_mode=OFF;
      CREATE TABLE cities(
        _id INTEGER PRIMARY KEY,
        en TEXT NOT NULL, ar TEXT NOT NULL,
        en_lower TEXT NOT NULL, ar_lower TEXT NOT NULL,
        country TEXT NOT NULL,
        lat REAL NOT NULL, lon REAL NOT NULL,
        tz REAL NOT NULL,
        zone TEXT NOT NULL, zone_id INTEGER,
        method TEXT NOT NULL);
      CREATE TABLE meta(key TEXT PRIMARY KEY, value TEXT NOT NULL);
    """)
    zone_ids = {}
    for zid, name in src.execute("SELECT zone_id, zone_name FROM zone"):
        zone_ids[name] = zid
    n = 0
    rows = []
    for (en, ar, lat, lon, tz, cc, method, zone) in src.execute(
            "SELECT city_name, city_name_ar, latitude, longitude, utc_offset,"
            " country_code, calc_method, timezone FROM cities"):
        en, ar = clean(en), clean(ar)
        if not en or lat is None or lon is None:
            continue
        try:
            lat, lon, tz = float(lat), float(lon), float(tz)
        except (TypeError, ValueError):
            continue
        zname = clean(zone)
        rows.append((en, ar, en.lower(), fold_ar(ar), clean(cc),
                     round(lat, 5), round(lon, 5), tz, zname, zone_ids.get(zname),
                     clean(method) or "makkah"))
        n += 1
    out.executemany(
        "INSERT INTO cities(en, ar, en_lower, ar_lower, country, lat, lon, tz,"
        " zone, zone_id, method) VALUES (?,?,?,?,?,?,?,?,?,?,?)", rows)
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
      CREATE INDEX idx_cities_ar_lower ON cities(ar_lower);
      ANALYZE;
      VACUUM;
    """)
    out.close()
    print(f"wrote {OUT}: {n} cities, {os.path.getsize(OUT)/1e6:.1f} MB, "
          f"source sha {sha[:16]}")


def build_data(src):
    if os.path.exists(DATA_OUT):
        os.remove(DATA_OUT)
    out = sqlite3.connect(DATA_OUT)
    out.executescript("""
      PRAGMA journal_mode=OFF;
      CREATE TABLE countries(
        code TEXT PRIMARY KEY, name_ar TEXT NOT NULL, name_en TEXT NOT NULL,
        method TEXT NOT NULL);
      CREATE TABLE day_events(
        id INTEGER PRIMARY KEY, month INTEGER NOT NULL,
        start_day INTEGER NOT NULL, end_day INTEGER NOT NULL,
        name_ar TEXT NOT NULL, name_en TEXT NOT NULL);
      CREATE TABLE prayer_events(
        id INTEGER PRIMARY KEY, prayer_index INTEGER NOT NULL,
        start_min INTEGER NOT NULL, end_min INTEGER NOT NULL,
        message_ar TEXT NOT NULL, message_en TEXT NOT NULL,
        priority INTEGER NOT NULL);
      CREATE TABLE zones(
        id INTEGER PRIMARY KEY, code TEXT NOT NULL, name TEXT NOT NULL);
      CREATE TABLE tz_rules(
        zone_id INTEGER NOT NULL, abbr TEXT NOT NULL,
        start_epoch INTEGER NOT NULL, gmt_offset INTEGER NOT NULL,
        dst INTEGER NOT NULL);
      CREATE INDEX idx_tz_zone ON tz_rules(zone_id, start_epoch);
      CREATE INDEX idx_day_events ON day_events(month, start_day);
      CREATE INDEX idx_prayer_events ON prayer_events(prayer_index);
    """)
    out.executemany(
        "INSERT INTO countries(code, name_ar, name_en, method) VALUES (?,?,?,?)",
        [(clean(c), clean(ar), clean(en), clean(m) or "makkah")
         for (c, ar, en, m) in src.execute(
             "SELECT country_code, country_name_ar, country_name, calc_method"
             " FROM countries")])
    out.executemany(
        "INSERT INTO day_events(id, month, start_day, end_day, name_ar, name_en)"
        " VALUES (?,?,?,?,?,?)",
        [(i, m, s, e, clean(ar), clean(en)) for (i, m, s, e, ar, en) in src.execute(
            "SELECT id, month_index, start_day, end_day, event_name_ar,"
            " event_name_en FROM day_events")])
    out.executemany(
        "INSERT INTO prayer_events(id, prayer_index, start_min, end_min,"
        " message_ar, message_en, priority) VALUES (?,?,?,?,?,?,?)",
        [(i, p, s, e, clean(ar), clean(en), pr) for (i, p, s, e, ar, en, pr) in src.execute(
            "SELECT id, prayer_index, start_time, end_time, message_ar,"
            " message_en, priority FROM prayer_events")])
    out.executemany(
        "INSERT INTO zones(id, code, name) VALUES (?,?,?)",
        [(i, clean(c), clean(n)) for (i, c, n) in src.execute(
            "SELECT zone_id, country_code, zone_name FROM zone")])
    out.executemany(
        "INSERT INTO tz_rules(zone_id, abbr, start_epoch, gmt_offset, dst)"
        " VALUES (?,?,?,?,?)",
        list(src.execute(
            "SELECT zone_id, abbreviation, time_start, gmt_offset, dst FROM timezone")))
    out.commit()
    out.executescript("ANALYZE; VACUUM;")
    out.close()
    print(f"wrote {DATA_OUT}: {os.path.getsize(DATA_OUT)/1e6:.1f} MB "
          "(countries, day_events, prayer_events, zones, tz_rules)")


def main():
    src = sqlite3.connect("file:" + DB + "?mode=ro", uri=True)
    build_cities(src)
    build_data(src)
    src.close()


if __name__ == "__main__":
    main()
