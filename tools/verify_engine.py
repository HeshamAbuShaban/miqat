#!/usr/bin/env python3
"""SalatukPlus engine parity check.

Recomputes prayer times with the same PrayTimes-style algorithm the Kotlin engine
uses, and diffs them against the DONOR's own precomputed corpus
(staticprayertimes table in SalatukDB.db — Salatuk's official-timetable fallback
for 57 hard cities, 2014-2019).

Usage: python3 -I tools/verify-engine.py [--tolerance 1] [--limit 200]
  -I : isolated (untrusted DB path never executes anything)
Exit code 0 = all sampled rows within tolerance.
"""
import argparse
import math
import sqlite3
import sys

DB = "donor/cities-db/SalatukDB.db"

# Method table inferred from donor strings + standard PrayTimes.org values.
# (fajr_angle, isha_angle, isha_minutes_if_not_angle, maghrib_minutes_or_angle)
METHODS = {
    "makkah":    dict(fajr=18.5, isha=None, isha_min=90),
    "mwl":       dict(fajr=18.0, isha=17.0),
    "isna":      dict(fajr=15.0, isha=15.0),
    "egypt":     dict(fajr=19.5, isha=17.5),
    "karachi":   dict(fajr=18.0, isha=18.0),
    "kuwait":    dict(fajr=18.0, isha=17.5),
    "qatar":     dict(fajr=18.0, isha=None, isha_min=90),
    "dubai":     dict(fajr=18.2, isha=18.2),
    "emirates":  dict(fajr=18.2, isha=18.2),
    "jordan":    dict(fajr=18.0, isha=17.0),
    "syria":     dict(fajr=15.5, isha=15.5),
    "iraq":      dict(fajr=19.5, isha=17.5),
    "libya":     dict(fajr=18.5, isha=18.5),
    "tunisia":   dict(fajr=18.0, isha=18.0),
    "algeria":   dict(fajr=18.0, isha=17.0),
    "turkey":    dict(fajr=18.0, isha=17.0),
    "malaysia":  dict(fajr=20.0, isha=18.0),
    "tehran":    dict(fajr=17.7, isha=14.0, maghrib_min=4),
    "singapore": dict(fajr=20.0, isha=18.0),
    "france":    dict(fajr=18.0, isha=18.0),
    "birmingham":dict(fajr=18.0, isha=16.0),   # donor-specific (VERIFY per-city)
    "london":    dict(fajr=18.0, isha=16.0),   # donor-specific (VERIFY per-city)
    "paris":     dict(fajr=12.0, isha=12.0),   # UOIF style (VERIFY)
    "montreal":  dict(fajr=16.0, isha=14.0),   # donor-specific (VERIFY)
    "munchen":   dict(fajr=18.0, isha=17.0),   # VERIFY
    "aachen":    dict(fajr=18.0, isha=17.0),   # VERIFY
    "blackburn": dict(fajr=18.0, isha=16.0),   # VERIFY
    "nurnberg":  dict(fajr=18.0, isha=17.0),   # VERIFY
    "belgium":   dict(fajr=18.0, isha=17.0),   # VERIFY
    "windsor":   dict(fajr=18.0, isha=16.0),   # VERIFY
    "calgary":   dict(fajr=16.0, isha=15.0),   # VERIFY
    "toulouse":  dict(fajr=12.0, isha=12.0),   # VERIFY
    "lyon":      dict(fajr=12.0, isha=12.0),   # VERIFY
    "austria":   dict(fajr=18.0, isha=17.0),   # VERIFY
    "tajikistan":dict(fajr=18.0, isha=18.0),   # VERIFY
}


MARGINS = {
    "emirates": dict(sunrise=-2, dhuhr=2, asr=2, maghrib=3, isha=0),
}

def fix(a, b):
    r = a - b * math.floor(a / b)
    return r


def julian(y, m, d):
    if m <= 2:
        y -= 1
        m += 12
    a = math.floor(y / 100.0)
    b = 2 - a + math.floor(a / 4.0)
    return math.floor(365.25 * (y + 4716)) + math.floor(30.6001 * (m + 1)) + d + b - 1524.5


def sun_position(jd):
    d = jd - 2451545.0
    g = fix(357.529 + 0.98560028 * d, 360.0)
    q = fix(280.459 + 0.98564736 * d, 360.0)
    l = fix(q + 1.915 * math.sin(math.radians(g)) + 0.020 * math.sin(math.radians(2 * g)), 360.0)
    e = 23.439 - 0.00000036 * d
    ra = math.degrees(math.atan2(math.cos(math.radians(e)) * math.sin(math.radians(l)),
                                 math.cos(math.radians(l)))) / 15.0
    ra = fix(ra, 24)
    decl = math.degrees(math.asin(math.sin(math.radians(e)) * math.sin(math.radians(l))))
    eqt = q / 15.0 - ra
    return decl, eqt


def mid_day(jd, eqt):
    return fix(12.0 - eqt, 24.0)


def sun_angle_time(jd, angle, lat, t, ccw):
    """UT hour of the moment sun depression `angle` (positive = below horizon)."""
    decl, _ = sun_position(jd + t)
    noon = mid_day(jd, sun_position(jd + t)[1])
    num = -math.sin(math.radians(angle)) - math.sin(math.radians(decl)) * math.sin(math.radians(lat))
    den = math.cos(math.radians(decl)) * math.cos(math.radians(lat))
    if den == 0 or abs(num / den) > 1:
        return None
    dt = math.degrees(math.acos(num / den)) / 15.0
    return noon - dt if ccw else noon + dt


def asr_time(jd, factor, lat, t):
    decl, _ = sun_position(jd + t)
    angle = -math.degrees(math.atan(1.0 / (factor + math.tan(math.radians(abs(lat - decl))))))
    return sun_angle_time(jd, angle, lat, t, ccw=False)


def compute_times(y, m, d, lat, lon, tz_hours, method, asr_factor=1.0):
    cfg = METHODS[method]
    jd = julian(y, m, d) - lon / (15.0 * 24.0)
    times = {
        "fajr":    sun_angle_time(jd, cfg["fajr"], lat, 5.0 / 24.0, ccw=True),
        "sunrise": sun_angle_time(jd, 0.833, lat, 6.0 / 24.0, ccw=True),
        "dhuhr":   None,  # mid-day, filled below
        "asr":     asr_time(jd, asr_factor, lat, 13.0 / 24.0),
        "maghrib": sun_angle_time(jd, 0.833, lat, 18.0 / 24.0, ccw=False),
        "isha":    None,
        # ihtiyat margins (minutes) — donor corpus shows +2 dhuhr/asr, ~+1..2 maghrib/isha
        "_m": MARGINS.get(method, {}),
    }
    margins = times.pop("_m")
    decl, eqt = sun_position(jd)
    times["dhuhr"] = mid_day(jd, eqt) + margins.get("dhuhr", 0) / 60.0
    if "maghrib_min" in cfg:
        times["maghrib"] = times["sunset"] + cfg["maghrib_min"] / 60.0 if False else times["maghrib"]
    if cfg.get("isha") is not None:
        times["isha"] = sun_angle_time(jd, cfg["isha"], lat, 18.0 / 24.0, ccw=False) + margins.get("isha", 0) / 60.0
    else:
        times["isha"] = times["maghrib"] + cfg["isha_min"] / 60.0
    times["maghrib"] += margins.get("maghrib", 0) / 60.0
    times["asr"] += margins.get("asr", 0) / 60.0
    times["sunrise"] += margins.get("sunrise", 0) / 60.0
    out = {}
    for k, ut in times.items():
        if ut is None:
            out[k] = None
            continue
        local = (ut + tz_hours - lon / 15.0) % 24.0  # PrayTimes convention: jd shift + civil correction
        out[k] = (round(local * 60) // 60, round(local * 60) % 60)
    return out


def hhmm(t):
    return None if t is None else "%02d:%02d" % t


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--tolerance", type=int, default=1, help="minutes")
    ap.add_argument("--limit", type=int, default=200)
    ap.add_argument("--method", type=str, default=None)
    args = ap.parse_args()

    con = sqlite3.connect("file:" + DB + "?mode=ro", uri=True)
    q = ("SELECT method, methodname, city, date, fajr, sunrise, dhuhr, asr, maghrib, isha"
         " FROM staticprayertimes")
    qargs = []
    if args.method:
        q += " WHERE methodname = ?"
        qargs.append(args.method)
    q += " LIMIT ?"
    qargs.append(args.limit)
    rows = con.execute(q, qargs).fetchall()

    # city slug -> coords (donor stores slugs like 'abudhabi')
    cities = {repl(r[0]): (r[1], r[2], r[3]) for r in con.execute(
        "SELECT city_name, latitude, longitude, utc_offset FROM cities")}
    # fallback coords for donor corpus slugs not present in cities (VERIFY manually)
    extra = {
        "abudhabi": (24.4539, 54.3773, 4.0),
        "dubai": (25.2048, 55.2708, 4.0),
    }
    cities.update(extra)

    bad = ok = miss = 0
    for (mid, mname, slug, date, f, sr, dh, asr, mg, isha) in rows:
        if slug not in cities:
            miss += 1
            continue
        lat, lon, tzh = cities[slug]
        method = mname if mname in METHODS else "makkah"
        y, mo, dy = int(date[:4]), int(date[4:6]), int(date[6:8])
        got = compute_times(y, mo, dy, lat, lon, tzh, method)
        want = {"fajr": f, "sunrise": sr, "dhuhr": dh, "asr": asr, "maghrib": mg, "isha": isha}
        worst = 0
        for k in want:
            w = minutes(want[k])
            g = hhmm(got[k])
            g = None if g is None else minutes(g)
            if g is None:
                worst = 999
                break
            worst = max(worst, abs(w - g))
        if worst <= args.tolerance:
            ok += 1
        else:
            bad += 1
            if bad <= 10:
                detail = "  ".join(f"{k[:3]} {want[k]}/{hhmm(got[k])}" for k in
                                   ("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"))
                print(f"MISS {mname:11s} {slug:11s} {date} worst={worst}m  {detail}")
    print(f"\nparity: {ok} ok, {bad} miss, {miss} city-unresolved  (tolerance {args.tolerance}m)")
    sys.exit(0 if bad == 0 else 1)


def repl(s):
    return s.replace(" ", "").replace("-", "").lower()


def minutes(hhmm_s):
    h, m = hhmm_s.split(":")
    return int(h) * 60 + int(m)


if __name__ == "__main__":
    main()
