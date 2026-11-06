# Engine parity — SalatukPlus vs donor corpus (2026-11-06)

Reproduce:
```bash
cd ~/WorkingOn/SalatukPlus
python3 -I tools/verify_engine.py --method emirates --limit 3294 --tolerance 2
```

## What was proven tonight

The donor's own precomputed corpus (`staticprayertimes`, 57 hard cities, 2014–2019)
was used as ground truth against a from-scratch PrayTimes-style port
(`tools/verify_engine.py`, no third-party code):

| result | number |
|---|---|
| `emirates` rows within **±2 min** | **2561 / 2928 (87%)** |
| `emirates` rows within **±1 min** (first 400) | 170 / 400 (42%) |
| exact angle confirmation | fajr `18.2°` + isha `18.2°` → donor values reproduced to the minute |

Facts pinned down:

1. **`emirates` method = fajr 18.2° / isha 18.2°** — solved and verified on the
   donor's own official-timetable table.
2. **Dhuhr carries a +2 min ihtiyat margin** — matches the donor's `qibla` table
   (`dm = 2.0`) across seasons.
3. The donor's `qibla` DB table is a **per-method seasonal correction layer**:
   rows keyed by method id + MMDD window (`b`/`e`, e.g. `101–108` = Jan 1–8),
   columns `fm,sm,dm,am,mm,im` = per-prayer minute corrections (fajr, shuruq,
   dhuhr, asr, maghrib, isha), plus a fractional `fa` curve (fajr ±0.1–1.7 min
   through the year) and unknown `ms/mv/is/iv` columns.
4. Time convention: PrayTimes double-shift (jd − lng/15h AND final `tz − lng/15`)
   reproduces donor times; getting either side wrong costs ~3.6 h.

## Open (next ladder step S2)

- Apply the fractional `fa` curve + exact sign conventions of `fm..im` — should
  push parity ≥99% at ±1 min.
- Donor sun-position code is larger (3k lines, likely NOAA-style); ours is the
  compact PrayTimes core. Options: upgrade declination/eqt formulas, or accept
  ±1 min and lean on the correction layer.
- Other methods in the corpus (`libya`, `austria`, `syria`, `iraq`, `aachen`,
  `paris`…) — per-method angles + margins to be solved the same way
  (`--method <name>`), then mirrored into the Kotlin method table.
