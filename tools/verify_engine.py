#!/usr/bin/env python3
"""Miqat engine parity check — recomputes prayer times with the DONOR's exact
pipeline (decoded from the decompile: t5/a.java + y5/{a,b,c,f,g}.java + x5/d.java)
and diffs them against the donor's own precomputed corpus (staticprayertimes,
57 cities, 2014-2019).

Pipeline (1:1 with donor):
  y5.g METHOD_PARAMS  -> y5.b per-city overrides -> y5.b seasonal QiblaAdjust
  (tools/qibla_adjusts.json == donor DB table `qibla`)
  -> t5.a.g computeTimes (7 slots, single pass)
  -> t5.a.a adjustTimes (tz shift, mode overrides, city tweaks, high-lat rule)
  -> t5.a.j format (round half up to the minute)

Usage: python3 -I tools/verify_engine.py [--method NAME] [--limit N] [--tolerance M]
  -I : isolated mode (untrusted DB path never executes anything)
Exit code 0 = all sampled rows within tolerance.
"""
import argparse
import json
import math
import sqlite3
import sys

DB = "donor/cities-db/SalatukDB.db"
QIBLA_JSON = "tools/qibla_adjusts.json"
TWEAKS_JSON = "tools/city_tweaks.json"

# ---------------------------------------------------------------------------
# Method parameter table — donor y5/g.java, verbatim. Slot format:
# [0] fajr angle (deg)
# [1] maghrib mode: 0 = angle (P[2] = depression deg), 1 = minutes
# [2] maghrib value
# [3] isha mode:   0 = angle (P[4] = depression deg), 1 = minutes after maghrib
# [4] isha value
# [5..10] ihtiyat, minutes: fajr, sunrise, dhuhr, asr, maghrib, isha
# ---------------------------------------------------------------------------
METHOD_PARAMS = {
    "mwl":         [18.0, 1.0, 0.0, 0.0, 17.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "jafari":      [16.0, 0.0, 4.0, 0.0, 14.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "custom":      [18.0, 1.0, 0.0, 0.0, 17.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "none":        [18.0, 1.0, 0.0, 0.0, 17.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "morocco":     [19.09, 0.0, 0.0, 0.0, 17.0, 0.0, -2.0, 5.0, 0.0, 3.0, 0.0],
    "azrou":       [19.1, 0.0, 0.0, 0.0, 17.0, 0.0, -1.0, 5.0, 0.0, 1.0, 0.0],
    "algeria":     [18.0, 1.0, 3.0, 0.0, 17.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "sudan":       [18.12, 0.0, 0.0, 0.0, 17.88, 0.0, 3.0, 3.0, 0.0, -4.0, 0.0],
    "makkah":      [18.5, 1.0, 0.0, 1.0, 90.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "egypt":       [19.5, 1.0, 0.0, 0.0, 17.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "karachi":     [18.0, 1.0, 0.0, 0.0, 18.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "tehran":      [17.7, 0.0, 4.5, 0.0, 14.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "tunisia":     [18.0, 1.0, 1.0, 0.0, 18.0, -1.0, 0.0, 7.0, 0.0, 0.0, 1.0],
    "turkey":      [18.0, 0.0, 0.0, 0.0, 16.93, 0.0, -6.0, 6.0, 4.0, 5.0, 0.0],
    "malaysia":    [20.0, 1.0, 0.0, 0.0, 18.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0],
    "malaysia2":   [20.0, 1.0, 0.0, 0.0, 18.46, 0.0, 0.0, 3.0, 2.0, 1.0, 0.0],
    "indonesia":   [20.0, 1.0, 0.0, 0.0, 18.0, 2.0, -2.0, 2.0, 2.0, 2.0, 2.0],
    "palestine":   [20.11, 0.0, 0.0, 0.0, 17.9, 0.0, -5.0, 0.0, 0.0, 4.0, 0.0],
    "oman":        [18.0, 1.0, 5.0, 0.0, 18.0, 0.0, 0.0, 5.0, 5.0, 0.0, 1.0],
    "omanmuscat":  [17.74, 0.0, 0.0, 0.0, 18.229, 0.0, 1.0, 6.0, 6.0, 6.0, 0.0],
    "kazakhstan":  [14.97, 0.0, 0.0, 0.0, 14.96, 0.0, 0.0, 5.0, 5.0, 0.0, 0.0],
    "tajikistan":  [18.0, 1.0, 0.0, 0.0, 18.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "emirates":    [18.5, 1.0, 2.0, 0.0, 18.5, 1.0, -4.0, 2.0, 0.0, 0.0, -3.0],
    "jordan":      [18.12, 0.0, 0.0, 0.0, 17.975, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0],
    "kuwait":      [18.0, 1.0, 0.0, 0.0, 17.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "qatar":       [18.0, 1.0, 0.0, 1.0, 90.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0],
    "libya":       [18.3, 0.0, 0.0, 0.0, 18.35, 0.0, 0.0, 4.0, 0.0, 4.0, 0.0],
    "syria":       [19.5, 1.0, 0.0, 0.0, 17.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "iraq":        [18.0, 0.0, 0.0, 0.0, 17.0, 0.0, 0.0, 5.0, 3.0, 2.0, 0.0],
    "maldives":    [19.0, 1.0, 0.0, 0.0, 19.0, 0.0, -1.0, 4.0, 1.0, 1.0, 1.0],
    "moscow":      [16.0, 0.0, 0.0, 0.0, 15.1, 0.0, 0.0, 1.0, 1.0, 1.0, 2.0],
    "blackburn":   [18.0, 1.0, 0.0, 0.0, 17.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "london":      [18.0, 1.0, 0.0, 0.0, 17.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "birmingham":  [18.0, 1.0, 0.0, 0.0, 17.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "aachen":      [18.0, 1.0, 0.0, 0.0, 17.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "munchen":     [18.0, 1.0, 0.0, 0.0, 17.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "potsdam":     [18.0, 1.0, 0.0, 0.0, 17.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "nurnberg":    [18.0, 1.0, 0.0, 0.0, 17.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "austria":     [18.0, 1.0, 0.0, 0.0, 17.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "belgium":     [18.0, 0.0, 0.0, 0.0, 18.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "luxembourg":  [18.0, 1.0, 0.0, 0.0, 17.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "czech":       [12.04, 0.0, 0.0, 0.0, 12.04, 0.0, 0.0, 5.0, 0.0, -2.0, 0.0],
    "switzerland": [17.99, 0.0, 0.0, 1.0, 100.0, 1.0, 4.0, 0.0, 0.0, -4.0, 0.0],
    "fribourg":    [18.01, 0.0, 0.0, 1.0, 100.0, 1.0, 5.0, 0.0, 0.0, -5.0, 0.0],
    "uoif":        [12.0, 1.0, 0.0, 0.0, 12.0, -5.0, 0.0, 5.0, 0.0, 4.0, 5.0],
    "paris":       [12.0, 1.0, 0.0, 0.0, 12.0, -5.0, 0.0, 5.0, 0.0, 4.0, 5.0],
    "toulouse":    [12.0, 1.0, 0.0, 0.0, 12.0, -5.0, 0.0, 5.0, 0.0, 4.0, 5.0],
    "lyon":        [12.0, 1.0, 0.0, 0.0, 12.0, -5.0, 0.0, 5.0, 0.0, 4.0, 5.0],
    "orleans":     [15.0, 0.0, 0.0, 0.0, 12.34, 0.0, 0.0, 5.0, 0.0, 0.0, 0.0],
    "isna":        [15.0, 1.0, 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "montreal":    [15.0, 1.0, 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "windsor":     [15.0, 1.0, 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "calgary":     [15.0, 1.0, 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "mississauga": [15.0, 1.0, 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "southkorea":  [18.0, 1.0, 0.0, 0.0, 18.0, 1.0, -1.0, 0.0, 0.0, 0.0, -6.0],
    "rotterdam":   [15.0, 1.0, 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "dordrecht":   [15.0, 1.0, 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    "eindhoven":   [15.0, 1.0, 0.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
}

# y5.b.a(): per-city method-parameter overrides (city name lowercased, spaces stripped)
CITY_PARAM_OVERRIDES = {
    ("oman", "masqat"): "omanmuscat", ("oman", "muscat"): "omanmuscat",
    ("oman", "مسقط"): "omanmuscat", ("oman", "mascate"): "omanmuscat",
    ("morocco", "azro"): "azrou", ("morocco", "aẓro"): "azrou",
    ("morocco", "أزرو"): "azrou", ("morocco", "azrou"): "azrou",
    ("switzerland", "freiburg"): "fribourg", ("switzerland", "fribourg"): "fribourg",
    ("switzerland", "friburgo"): "fribourg", ("switzerland", "فريبورج"): "fribourg",
    ("switzerland", "فريبورغ"): "fribourg", ("switzerland", "französisch"): "fribourg",
}
MALAYSIA_OVERRIDES = {"MY": "malaysia2", "ID": "indonesia"}

# y5.f.a(): methods eligible for the seasonal correction layer (gate)
SEASONAL_METHODS = {
    "emirates", "iraq", "syria", "birmingham", "london", "munchen", "aachen",
    "potsdam", "nurnberg", "paris", "toulouse", "lyon", "montreal", "windsor",
    "calgary", "mississauga", "libya", "blackburn", "belgium", "tajikistan",
    "austria", "luxembourg",
}

# t5.a.g(): horizon elevation correction applies to these methods / countries
ELEVATION_METHODS = {"iraq", "morocco", "tunisia", "jordan", "orleans", "sudan",
                     "belgium", "kazakhstan"}
ELEVATION_COUNTRIES = {"PS", "IL", "CZ", "CH"}

# t5.a.m(): high-latitude rule. Default ANGLE_NIGHT; MID_NIGHT for uoif/moscow
# (resolved via the city default method — corpus decides empirically, see --hl-paris)
RULE_MID = {  # fraction 0.5 of night
    "uoif", "moscow",
}

# corpus method -> home country (for city disambiguation + DST)
METHOD_COUNTRY = {
    "emirates": "AE", "libya": "LY", "syria": "SY", "iraq": "IQ",
    "tajikistan": "TJ", "aachen": "DE", "munchen": "DE", "potsdam": "DE",
    "nurnberg": "DE", "austria": "AT", "belgium": "BE", "birmingham": "GB",
    "blackburn": "GB", "london": "GB", "paris": "FR", "toulouse": "FR",
    "lyon": "FR", "luxembourg": "LU", "montreal": "CA", "windsor": "CA",
    "calgary": "CA", "mississauga": "CA",
}

EU_DST = {"DE", "AT", "BE", "FR", "GB", "LU", "IE", "NL", "ES", "PT", "IT",
          "CH", "CZ", "DK", "SE", "NO", "FI", "HU", "PL", "SK", "SI", "GR",
          "MT", "CY", "EE", "LV", "LT", "RO", "BG", "HR"}
NA_DST = {"CA", "US"}

# corpus slug -> (city_name in DB to try, or explicit coords)
SLUG_FIXES = {
    "nurnberg": ("Nürnberg", None),
    "klagenfurt": ("Klagenfurt am Woerthersee", None),
    "zawia": ("Az Zawiyah", "LY"),
    "khawrfakkan": ("Khawr Fakkan", "AE"),
}


def fix(a, b):
    r = a - b * math.floor(a / b)
    return r


def julian(y, m, d):
    """Donor t5.a.p(y,m,d)."""
    if m <= 2:
        y -= 1
        m += 12
    a = math.floor(y / 100.0)
    b = 2 - a + math.floor(a / 4.0)
    return (math.floor(365.25 * (y + 4716)) + math.floor(30.6001 * (m + 1))
            + d + b - 1524.5)


def sun_position(jd):
    """Donor t5.a.q(jd): [declination, eqt] — identical to our Kotlin port."""
    d = jd - 2451545.0
    g = fix(357.529 + 0.98560028 * d, 360.0)
    q = fix(280.459 + 0.98564736 * d, 360.0)
    l = fix(q + 1.915 * math.sin(math.radians(g)) + 0.020 * math.sin(math.radians(2 * g)), 360.0)
    e = 23.439 - 0.00000036 * d
    ra = fix(math.degrees(math.atan2(math.cos(math.radians(e)) * math.sin(math.radians(l)),
                                     math.cos(math.radians(l)))) / 15.0, 24)
    decl = math.degrees(math.asin(math.sin(math.radians(e)) * math.sin(math.radians(l))))
    eqt = q / 15.0 - ra
    return decl, eqt


NAN = float("nan")


def sun_angle_time(jd, angle, lat, t):
    """Donor t5.a.f(angle, t): UT hour of sun crossing depression `angle`;
    angle in (90, 180] means before noon. NaN when the sun never reaches it."""
    decl, _ = sun_position(jd + t)
    noon = fix(12.0 - sun_position(jd + t)[1], 24.0)
    num = -math.sin(math.radians(angle)) - math.sin(math.radians(decl)) * math.sin(math.radians(lat))
    den = math.cos(math.radians(decl)) * math.cos(math.radians(lat))
    if den == 0:
        return NAN
    x = num / den
    if abs(x) > 1:
        return NAN
    dt = math.degrees(math.acos(x)) / 15.0
    return noon - dt if angle > 90 else noon + dt


def asr_time(jd, shadow, lat, t):
    """Donor t5.a.d(shadow, t): shadow = 1 (Shafi'i) or 2 (Hanafi)."""
    decl, _ = sun_position(jd + t)
    angle = -math.degrees(math.atan2(1.0, shadow + math.tan(math.radians(abs(lat - decl)))))
    return sun_angle_time(jd, angle, lat, t)


FA_MINUTES = False


def compute(y, mo, dy, lat, lon, alt, tz, method, city, country, shadow=None,
            maghrib_angle_mode_respects_slot=True):
    """Donor pipeline. Returns the 7-slot local-hour array
    [fajr, sunrise, dhuhr, asr, sunset, maghrib, isha] (slot 4 is internal).
    Param source selected by PARAMS_MODE (donor-live vs corpus-fitted)."""
    p, corr_rows, shadow_default, elev_enabled, city_off = effective_params(method)
    if shadow is None:
        shadow = shadow_default

    # --- y5.b: city overrides (donor mode only) -------------------------------
    if PARAMS_MODE == "donor":
        if method == "malaysia" and country in MALAYSIA_OVERRIDES:
            p = list(METHOD_PARAMS[MALAYSIA_OVERRIDES[country]])
        ov = CITY_PARAM_OVERRIDES.get((method, city))
        if ov:
            p = list(METHOD_PARAMS[ov])

    # --- seasonal QiblaAdjust layer (donor qibla table == fitted windows) -----
    mmdd = (mo * 100) + dy
    for row in corr_rows:
        if row["b"] <= mmdd <= row["e"]:
            # donor y5.b.java adds fa/iv to the ANGLE slots; empirically the
            # corpus fits fa/iv as MINUTES -- --fa-minutes selects that reading
            if FA_MINUTES:
                p[5] += row.get("fa", 0.0)
                p[10] += row.get("iv", 0.0)
            else:
                p[0] += row.get("fa", 0.0)
                p[4] += row.get("iv", 0.0)
            p[1] += row.get("ms", 0.0)
            p[2] += row.get("mv", 0.0)
            p[3] += row.get("ism", 0.0)
            p[5] += row.get("fm", 0.0)
            p[6] += row.get("sm", 0.0)
            p[7] += row.get("dm", 0.0)
            p[8] += row.get("am", 0.0)
            p[9] += row.get("mm", 0.0)
            p[10] += row.get("im", 0.0)
            break
    # per-city offsets (donor `qibla` `c` column semantics — corpus cities only)
    co = city_off.get(city) or {}
    if co:
        p[5] += co.get("fm", 0.0)
        p[6] += co.get("sm", 0.0)
        p[7] += co.get("dm", 0.0)
        p[8] += co.get("am", 0.0)
        p[9] += co.get("mm", 0.0)
        p[10] += co.get("im", 0.0)

    # --- t5.a.g computeTimes: 7 slots, single pass ---------------------------
    jd = julian(y, mo, dy) - lon / 360.0
    elev = elev_enabled and (method in ELEVATION_METHODS or country in ELEVATION_COUNTRIES) and alt
    dsqrt = 0.833 + (math.sqrt(alt) * 0.0347 if elev else 0.0)
    t = [5 / 24, 6 / 24, 12 / 24, 13 / 24, 18 / 24, 18 / 24, 18 / 24]
    v = [0.0] * 7
    v[0] = sun_angle_time(jd, 180 - p[0], lat, t[0])
    v[1] = sun_angle_time(jd, 180 - dsqrt, lat, t[1])
    _, eqt = sun_position(jd + t[2])
    v[2] = fix(12.0 - eqt, 24.0)
    v[3] = asr_time(jd, shadow, lat, t[3])
    v[4] = sun_angle_time(jd, dsqrt, lat, t[4])
    v[5] = sun_angle_time(jd, p[2], lat, t[5])
    v[6] = sun_angle_time(jd, p[4], lat, t[6])

    # --- t5.a.a adjustTimes ---------------------------------------------------
    shift = tz - lon / 15.0
    v = [x + shift for x in v]
    v[0] += p[5] / 60.0
    v[1] += p[6] / 60.0
    v[2] += p[7] / 60.0
    v[3] += p[8] / 60.0
    d10 = v[4] + p[9] / 60.0
    if p[1] == 1.0:
        v[5] = d10 + p[2] / 60.0
    elif maghrib_angle_mode_respects_slot:
        v[5] = v[5] + p[9] / 60.0  # angle-mode: keep the f(P[2]) slot (PrayTimes semantic)
    else:
        v[5] = d10
    if p[3] == 1.0:
        v[6] = v[5] + p[4] / 60.0
    v[6] += p[10] / 60.0

    # --- t5.a.a: per-city minute tweaks (method-guarded) ----------------------
    tw = TWEAKS.get(method, {}).get(city)
    if tw:
        for i in range(7):
            v[i] += tw[i]

    # --- t5.a.a: high-latitude rule (default ANGLE_NIGHT) ---------------------
    # donor b.l(a, b) = fix24(b - a)  (arg order swapped vs a natural reading):
    #   night = l(sunset, sunrise) = fix24(sunrise - sunset)  -> crosses midnight = night length
    #   fajr gap = l(fajr, sunrise) = fix24(sunrise - fajr)
    #   isha gap = l(maghrib, isha) = fix24(isha - maghrib)
    rule = "MID" if method in RULE_MID else "ANGLE"
    night = fix(v[1] - v[4], 24.0)
    if rule == "MID":
        pf, pi = 0.5 * night, 0.5 * night
    else:
        pf = (p[0] / 60.0) * night
        pi = ((p[4] if p[3] == 0.0 else 18.0) / 60.0) * night
    if math.isnan(v[0]) or fix(v[1] - v[0], 24.0) > pf:
        v[0] = v[1] - pf + p[5] / 60.0
    if math.isnan(v[6]) or fix(v[6] - v[5], 24.0) > pi:
        v[6] = v[5] + pi + p[10] / 60.0

    return v


def fmt(t):
    """Donor t5.a.j(t): round half up to the displayed minute."""
    if t is None or math.isnan(t):
        return None
    dj = fix(t + 0.008333333333333333, 24.0)
    h = math.floor(dj)
    m = math.floor((dj - h) * 60.0)
    return "%02d:%02d" % (h, m)


def fmt_min(t):
    s = fmt(t)
    return None if s is None else int(s[:2]) * 60 + int(s[3:])


# --- DST (per civil date; all prayer times fall after the transition instant) --
def _nth_sunday(y, mo, n):
    wd = (julian(y, mo, 1) + 1.5) % 7  # 0=Monday..? julian(.5)=midnight; weekday offset
    # 2000-01-01 was a Saturday. Compute weekday of 1st: days since known Sunday.
    known = julian(2000, 1, 2)  # a Sunday (2000-01-02)
    wd = int(fix(julian(y, mo, 1) - known, 7))  # 0=Sunday
    return 1 + (n - 1) * 7 + (7 - wd) % 7


def dst_hours(y, mo, dy, country):
    if country in EU_DST:
        start = (_nth_sunday(y, 3, 5), 3)
        end = (_nth_sunday(y, 10, 5), 10)
        cur = (dy, mo)
        return 1.0 if start <= cur < end else 0.0
    if country in NA_DST:
        start = (_nth_sunday(y, 3, 2), 3)
        end = (_nth_sunday(y, 11, 1), 11)
        cur = (dy, mo)
        return 1.0 if start <= cur < end else 0.0
    return 0.0


import unicodedata


def load_cities(con):
    def repl(s):
        s = unicodedata.normalize("NFC", str(s))
        return s.replace(" ", "").replace("-", "").lower()
    cities = {}
    for name, lat, lon, tzh, cc, alt in con.execute(
            "SELECT city_name, latitude, longitude, utc_offset, country_code, altitude FROM cities"):
        try:
            alt = float(alt) if alt not in (None, "") else None
        except (TypeError, ValueError):
            alt = None
        cities.setdefault((cc, repl(name)), (float(lat), float(lon), float(tzh), alt))
        cities.setdefault((None, repl(name)), (float(lat), float(lon), float(tzh), alt))
    return cities


def resolve(cities, slug, country):
    key = (country, unicodedata.normalize("NFC", str(slug)).lower())
    if key in cities:
        return cities[key]
    return cities.get((None, slug.lower()))


def parse_hhmm(s):
    s = str(s).strip().lower().replace(" ", "")
    if s.endswith("am") or s.endswith("pm"):
        s = s[:-2]
    if ":" not in s:
        raise ValueError(f"malformed corpus time: {s!r}")
    h, m = s.split(":")
    return int(h) * 60 + int(m)


QIBLA_ROWS = json.load(open(QIBLA_JSON))
TWEAKS = json.load(open(TWEAKS_JSON))

# --- parameter source modes -------------------------------------------------
# "donor":  y5.g live params + qibla seasonal layer + per-city overrides
#           + elevation term (the donor app as shipped, decoded)
# "fitted": corpus-fitted params + monthly windows (tools/fitted_methods.json)
#           — reproduces the donor's shipped timetable corpus (parity target)
PARAMS_MODE = "fitted"
_FITTED = None


def fitted_table():
    global _FITTED
    if _FITTED is None:
        _FITTED = json.load(open("tools/fitted_methods.json"))
    return _FITTED


def effective_params(method):
    """(slots, corrections_rows, asr_shadow, elevation_enabled, city_offsets)."""
    if PARAMS_MODE == "fitted":
        entry = fitted_table().get(method)
        if entry is None:
            slots = list(METHOD_PARAMS[method])
            rows = [r for r in QIBLA_ROWS if r["mn"] == method]
            return slots, rows, 1, method in ELEVATION_METHODS, {}
        return (list(entry["params"]),
                [dict(w, mn=method, fa=0.0, ms=0.0, mv=0.0, iv=0.0, ism=0.0)
                 for w in entry["windows"]],
                entry.get("asr_shadow", 1), False, entry.get("city_offsets", {}))
    slots = list(METHOD_PARAMS[method])
    rows = [r for r in QIBLA_ROWS if r["mn"] == method] if method in SEASONAL_METHODS else []
    return slots, rows, 1, True, {}


def fitted_dst_applies(method, slug, year):
    entry = fitted_table().get(method)
    if entry is None:
        return True  # donor mode: corpus generation assumed DST-aware
    keys = entry.get("dst_keys") or ([f"{slug}/{year}"] if entry.get("dst") else [])
    return f"{slug}/{year}" in keys


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--tolerance", type=int, default=1, help="minutes")
    ap.add_argument("--limit", type=int, default=0, help="rows per method (0 = all)")
    ap.add_argument("--method", type=str, default=None)
    ap.add_argument("--slot-maghrib", action="store_true", default=True,
                    help="angle-mode maghrib keeps its f(P[2]) slot (PrayTimes)")
    ap.add_argument("--no-slot-maghrib", dest="slot_maghrib", action="store_false")
    ap.add_argument("--alt", choices=["db", "zero"], default="db")
    ap.add_argument("--fa-minutes", action="store_true", default=False,
                    help="apply fa/iv as minutes on fajr/isha time instead of degrees")
    ap.add_argument("--params", choices=["fitted", "donor"], default="fitted",
                    help="parameter source: corpus-fitted (parity target) or donor-live")
    ap.add_argument("--misses", type=int, default=8, help="miss rows to print")
    args = ap.parse_args()
    global FA_MINUTES, PARAMS_MODE
    FA_MINUTES = args.fa_minutes
    PARAMS_MODE = args.params

    con = sqlite3.connect("file:" + DB + "?mode=ro", uri=True)
    cities = load_cities(con)

    q = ("SELECT methodname, city, date, fajr, sunrise, dhuhr, asr, maghrib, isha"
         " FROM staticprayertimes")
    qargs = []
    if args.method:
        q += " WHERE methodname = ?"
        qargs.append(args.method)
    if args.limit:
        q += " LIMIT ?"
        qargs.append(args.limit)
    rows = con.execute(q, qargs).fetchall()

    per_method = {}
    bad_total = ok_total = miss_city = corpus_err = 0
    for (mname, slug, date, f, sr, dh, asr, mg, isha) in rows:
        country = METHOD_COUNTRY.get(mname)
        cc = resolve(cities, slug.replace(" ", "").replace("-", ""), country) \
            if slug.lower().replace(" ", "").replace("-", "") not in SLUG_FIXES else None
        if cc is None:
            fx = SLUG_FIXES.get(slug.lower().replace(" ", "").replace("-", ""))
            if fx:
                cc = resolve(cities, fx[0].replace(" ", "").replace("-", ""),
                             fx[1] if len(fx) > 2 else country) or \
                    resolve(cities, fx[0], fx[1] if len(fx) > 2 else country)
        if cc is None:
            miss_city += 1
            continue
        lat, lon, tzh, alt = cc
        if args.alt == "zero" or alt is None:
            alt = 0.0
        y, mo, dy = int(date[:4]), int(date[4:6]), int(date[6:8])
        tz = tzh + (dst_hours(y, mo, dy, country)
                    if fitted_dst_applies(mname, slug.lower(), y) else 0.0)
        v = compute(y, mo, dy, lat, lon, alt, tz, mname,
                    slug.replace(" ", "").replace("-", "").lower(), country,
                    maghrib_angle_mode_respects_slot=args.slot_maghrib)
        try:
            want = {"fajr": f, "sunrise": sr, "dhuhr": dh, "asr": asr,
                    "maghrib": mg, "isha": isha}
            want = {k: parse_hhmm(v) for k, v in want.items()}
        except ValueError:
            corpus_err += 1
            continue
        worst = 0
        detail = []
        # output slots: 0..6 = fajr, sunrise, dhuhr, asr, sunset(internal), maghrib, isha
        slot_of = {"fajr": 0, "sunrise": 1, "dhuhr": 2, "asr": 3, "maghrib": 5, "isha": 6}
        for k, wstr in zip(("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"),
                           (f, sr, dh, asr, mg, isha)):
            w = parse_hhmm(wstr)
            g = fmt_min(v[slot_of[k]])
            if g is None:
                worst = 999
                detail.append(f"{k[:3]} {wstr}/None")
                continue
            d = abs(w - g)
            worst = max(worst, d)
            if d > 0:
                detail.append(f"{k[:3]} {wstr}|{fmt(v[slot_of[k]])}")
        per_method.setdefault(mname, [0, 0, 0])
        per_method[mname][2] += 1
        if worst <= args.tolerance:
            ok_total += 1
            per_method[mname][0] += 1
        else:
            bad_total += 1
            per_method[mname][1] += 1
            if per_method[mname][1] <= args.misses:
                print(f"MISS {mname:11s} {slug:12s} {date} worst={worst}m  {'  '.join(detail)}")

    print(f"\n{'method':<12} {'ok':>6} {'miss':>5} {'total':>6}  %")
    for mname, (ok, bad, tot) in sorted(per_method.items()):
        print(f"{mname:<12} {ok:>6} {bad:>5} {tot:>6}  {100.0*ok/tot:5.1f}%")
    n = ok_total + bad_total
    print(f"\nparity: {ok_total} ok, {bad_total} miss, {miss_city} city-unresolved,"
          f" {corpus_err} corpus-format-errors  ({100.0*ok_total/n:.2f}% @ ±{args.tolerance}m)")
    sys.exit(0 if bad_total == 0 else 1)


if __name__ == "__main__":
    main()
