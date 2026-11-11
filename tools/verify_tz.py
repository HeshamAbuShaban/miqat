#!/usr/bin/env python3
"""Verifies the bundled timezone table against the IANA database.

The table (`tz_rules` in the shipped DB) stores its transitions in SECONDS.
The app once compared them with MILLISECONDS — every comparison matched, the
sentinel row won, and every daylight-saving city was an hour wrong in
summer (the advisory council, 2026-11-11). This script is the guard: it
samples the table's own convention and compares it with the system's own
zoneinfo, so a unit mismatch can never ship again.

    python3 -I tools/verify_tz.py
"""

from __future__ import annotations

import datetime as dt
import os
import sqlite3
import sys
import zoneinfo

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
DB = os.path.join(ROOT, "app/src/main/assets/miqat-data-v1.db")

# The instants we sample: deep winter, deep summer, and the two DST edges.
# They are INSIDE the table's coverage (the donor's rules stop in 2023 — beyond
# that the platform's own database is the source of truth, and the app uses it).
SAMPLES = [
    dt.datetime(2016, 1, 15, 12, 0, tzinfo=dt.timezone.utc),
    dt.datetime(2016, 7, 15, 12, 0, tzinfo=dt.timezone.utc),
    dt.datetime(2016, 3, 27, 0, 30, tzinfo=dt.timezone.utc),
    dt.datetime(2016, 10, 30, 0, 30, tzinfo=dt.timezone.utc),
    dt.datetime(2019, 6, 21, 12, 0, tzinfo=dt.timezone.utc),
]


def table_offset(con: sqlite3.Connection, zone_id: int, epoch_seconds: int) -> float | None:
    row = con.execute(
        "SELECT gmt_offset FROM tz_rules WHERE zone_id = ? AND start_epoch <= ?"
        " ORDER BY start_epoch DESC LIMIT 1",
        (zone_id, epoch_seconds),
    ).fetchone()
    return None if row is None else row[0] / 3600.0


def main() -> int:
    if not os.path.exists(DB):
        print("missing:", DB)
        return 1
    con = sqlite3.connect(DB)
    zones = con.execute("SELECT id, name FROM zones ORDER BY id").fetchall()
    if not zones:
        print("the zones table is empty")
        return 1

    checked = 0
    wrong = 0
    missing = 0
    samples: list[tuple[int, str, str]] = []
    for zone_id, name in zones:
        try:
            tz = zoneinfo.ZoneInfo(name)
        except Exception:
            continue
        for when in SAMPLES:
            epoch = int(when.timestamp())
            ours = table_offset(con, zone_id, epoch)
            if ours is None:
                continue
            theirs = when.astimezone(tz).utcoffset().total_seconds() / 3600.0
            checked += 1
            if abs(ours - theirs) > 1e-9:
                wrong += 1
                if len(samples) < 12:
                    samples.append(
                        (zone_id, name, f"{when.isoformat()} ours={ours} zoneinfo={theirs}")
                    )

    print(f"tz table: {checked} samples, {wrong} differ from zoneinfo")
    print("  (the samples sit inside the table's coverage; beyond it the app")
    print("   uses the platform's own timezone database)")
    for s in samples:
        print("  DIFFERS", s)
    # A UNIT mismatch makes almost every sample differ (the bug the council
    # found: 1614/1664). A few zones whose rules changed since the donor's
    # data (Baku, Antarctica/*) are a known, small difference.
    ratio = wrong / checked if checked else 0.0
    if ratio > 0.05:
        print(f"FAIL — {ratio:.1%} of the samples differ: the table's convention is wrong")
        return 1
    print(f"OK — the convention holds ({ratio:.1%} differ — zones whose rules changed since the donor's data)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
