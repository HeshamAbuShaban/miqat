#!/usr/bin/env python3
"""Generate Kotlin parity fixtures (app/src/test/java/app/miqat/core/
ParityFixtures.kt) from the donor corpus.

For each corpus-fitted method, sample dates across the year (two cities when
the corpus has several) that the PYTHON engine reproduces within ±1 min, and
emit them as compile-time fixtures. The Kotlin test then measures PrayerTimes
against the same donor ground truth. Regenerate after every fit:
    python3 -I tools/gen_parity_fixtures.py
"""
import json
import sqlite3
import sys

sys.path.insert(0, "tools")
import verify_engine as ve

OUT = "app/src/test/java/app/miqat/core/ParityFixtures.kt"
SAMPLE_DATES = []  # filled below: 6 dates spread over the year


def sample_dates():
    # fixed deterministic picks (15th of each other month + solstice-ish days)
    return [(2014, m, d) for m, d in ((1, 1), (2, 15), (4, 15), (6, 21), (9, 15), (11, 15))]


def main():
    ve.PARAMS_MODE = "fitted"
    con = sqlite3.connect("file:" + ve.DB + "?mode=ro", uri=True)
    cities = ve.load_cities(con)
    fitted = ve.fitted_table()
    dates = sample_dates()

    fixtures = []
    skipped = 0
    for method, entry in sorted(fitted.items()):
        country = entry.get("country") or ve.METHOD_COUNTRY.get(method)
        by_city = {}
        for (slug, date, f, sr, dh, asr, mg, isha) in con.execute(
                "SELECT city, date, fajr, sunrise, dhuhr, asr, maghrib, isha"
                " FROM staticprayertimes WHERE methodname = ?", (method,)):
            by_city.setdefault(slug, {})[date] = (f, sr, dh, asr, mg, isha)
        chosen = 0
        for slug in sorted(by_city):
            if chosen >= 8:
                break
            cc = ve.resolve(cities, slug.lower().replace(" ", "").replace("-", ""), country)
            if cc is None:
                fx = ve.SLUG_FIXES.get(slug.lower())
                cc = resolve_fixed(cities, fx) if fx else None
            if cc is None:
                continue
            lat, lon, tzh, alt = cc
            # (month, day) picks applied to each year the corpus actually has
            years = {d[:4] for d in by_city[slug]}
            want_dates = {f"{y}{m:02d}{d:02d}" for y in years for (m, d) in dates}
            for date in sorted(by_city[slug]):
                if date not in want_dates:
                    continue
                if chosen >= 8:
                    break
                want = by_city[slug][date]
                y, mo, dy = int(date[:4]), int(date[4:6]), int(date[6:8])
                try:
                    values = [ve.parse_hhmm(x) for x in want]
                except ValueError:
                    skipped += 1
                    continue
                tz = tzh + (ve.dst_hours(y, mo, dy, country)
                            if ve.fitted_dst_applies(method, slug.lower(), y) else 0.0)
                v = ve.compute(y, mo, dy, lat, lon, 0.0, tz, method,
                               slug.lower().replace(" ", "").replace("-", ""), country)
                slots = (v[0], v[1], v[2], v[3], v[5], v[6])
                ok = True
                exp = []
                for got_h, w in zip(slots, want):
                    if got_h is None or got_h != got_h:
                        ok = False
                        break
                    g = ve.fmt(got_h)
                    gmin = int(g[:2]) * 60 + int(g[3:])
                    if abs(gmin - w) > 1:
                        ok = False
                        break
                    exp.append(g)
                if not ok:
                    continue
                fixtures.append((method, slug, date, lat, lon, tz, exp))
                chosen += 1

    lines = []
    lines.append("""package app.miqat.core

/**
 * GENERATED parity fixtures — do not hand-edit.
 * Regenerate with: python3 -I tools/gen_parity_fixtures.py
 *
 * Rows sampled from the donor's shipped timetable corpus (staticprayertimes,
 * SalatukDB.db) across the fitted methods and the year; every row is reproduced
 * by the python engine (tools/verify_engine.py, fitted mode) within ±1 min.
 * `utcOffset` is the effective offset (DST baked, per the corpus's own convention
 * documented in docs/engine-parity.md).
 */
object ParityFixtures {

    data class Row(
        val method: String,
        val city: String,
        val year: Int,
        val month: Int,
        val day: Int,
        val lat: Double,
        val lon: Double,
        val utcOffset: Double,
        val fajr: String,
        val sunrise: String,
        val dhuhr: String,
        val asr: String,
        val maghrib: String,
        val isha: String,
    )

    val rows: List<Row> = listOf(
""")
    for (method, slug, date, lat, lon, tz, exp) in fixtures:
        y, mo, dy = int(date[:4]), int(date[4:6]), int(date[6:8])
        lines.append(
            f'        Row("{method}", "{slug}", {y}, {mo}, {dy}, '
            f"{lat}, {lon}, {tz}, "
            f'"{exp[0]}", "{exp[1]}", "{exp[2]}", "{exp[3]}", "{exp[4]}", "{exp[5]}"),')
    lines.append("""    )
}
""")
    open(OUT, "w").write("\n".join(lines))
    print(f"wrote {OUT}: {len(fixtures)} fixture rows (skipped {skipped} malformed)")


def resolve_fixed(cities, fx):
    name, country = fx[0], fx[1] if len(fx) > 1 else None
    key = name.replace(" ", "").replace("-", "").lower()
    return cities.get((country, key)) or cities.get((None, key))


if __name__ == "__main__":
    main()
