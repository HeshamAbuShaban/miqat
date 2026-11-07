# Engine parity — Miqat vs the Salatuk donor (decoded 2026-11-07)

This document is the single source of truth for the prayer-time engine decode.
Reproduce any number:

```bash
cd ~/WorkingOn/SalatukPlus
python3 -I tools/verify_engine.py --tolerance 1                # fitted mode (corpus reproduction)
python3 -I tools/verify_engine.py --params donor --tolerance 1 # donor-live mode
python3 -I tools/fit_methods.py                                # refit fitted tables
python3 -I tools/gen_kotlin_table.py && python3 -I tools/gen_parity_fixtures.py
```

## 1. The donor pipeline — fully decoded (evidence: decompile, donor §2)

The engine lives in `t5/a.java` (3,033 lines) + `y5/` + `x5/` + `v5/`. It is a
PrayTimes-style core (the solar math `t5.a.q` is line-for-line our port: same
julian day, same low-precision declination/eqt series, same double-shift
convention). Pipeline for one (date, location, method):

1. **Params** `y5.b.a(method, city, country, date)`:
   - base = `y5.g` method table (59 methods, 11-element arrays — extracted verbatim
     into `tools/verify_engine.py METHOD_PARAMS`);
   - per-city overrides (`y5.b` switch: oman→muscat, malaysia→MY/ID variants,
     switzerland→fribourg, morocco→azrou);
   - **seasonal correction layer** — first row of the method's `QiblaAdjust`
     table whose MMDD window (`b`/`e`) contains today, applied to the slots
     (source: JSON embedded in `y5/b.java`, byte-identical to DB table `qibla`;
     extracted to `tools/qibla_adjusts.json`, 112 rows).
2. **computeTimes** `t5.a.g` — 7 slots, single pass at fixed day-fractions
   (5/24, 6/24, 12/24, 13/24, 18/24, 18/24, 18/24):
   `fajr = f(180−P0)`, `sunrise = f(180−0.833)`, `dhuhr = transit`,
   `asr = d(shadow+1)`, `sunset = f(0.833)`, then two "slot" computations
   `f(P2)` / `f(P4)`. Horizon depression 0.833°, plus
   `0.0347·√altitude` for methods iraq/morocco/tunisia/jordan/orleans/sudan/
   belgium/kazakhstan and countries PS/IL/CZ/CH.
3. **adjustTimes** `t5.a.a` — civil shift `utc − lng/15` (SY/JO static path
   uses `lng/18.3`; not the computed path), per-prayer ihtiyat margins, then
   mode overrides, then per-city minute tweaks (the huge switch: 109 cities
   across belgium/france/birmingham-method guards — extracted to
   `tools/city_tweaks.json`), then the high-latitude rule.
4. **format** `t5.a.j` — round half **up** to the displayed minute
   (`floor(t + 0.5 min)`).

### 1.1 The 11-slot method array (donor y5.g) — decoded

| slot | meaning | example (makkah / mwl / tehran) |
|---|---|---|
| [0] | fajr depression, degrees | 18.5 / 18.0 / 17.7 |
| [1] | maghrib mode: 1 = minutes, 0 = angle | 1 / 1 / 0 |
| [2] | maghrib value (minutes if [1]=1, degrees if [1]=0) | 0 / 0 / 4.5 |
| [3] | isha mode: 1 = minutes after maghrib, 0 = angle | 1 / 0 / 0 |
| [4] | isha value (minutes if [3]=1, degrees if [3]=0) | 90 / 17 / 14 |
| [5..10] | ihtiyat minutes: fajr, sunrise, dhuhr, asr, maghrib, isha | 0… / −4… (emirates) |

Proven (not guessed): `makkah` = isha maghrib+90 ✓ Umm al-Qura; `mwl` = isha
angle 17 ✓; `tehran` = maghrib angle 4.5° + isha angle 14 ✓; `jafari` = 4°/14° ✓.
Asr shadow: `x5.b.a(SHAFII)=0 → +1`, `HANAFI=1 → +2` (t5.a.g).

### 1.2 The seasonal layer — donor `qibla` table / QiblaAdjust — FULLY DECODED

`v5.f` ("com.masaratapp.salatukprayertimeslibrary.entity.QiblaAdjust") maps the
columns via its accessors (proven from the kotlinx-serialization ctor order):

| column | applied to | units |
|---|---|---|
| `fa` | slot [0] fajr **angle** | degrees |
| `ms` / `mv` | slot [1]/[2] maghrib mode/value | flag / min-or-deg |
| `ism` / `iv` | slot [3]/[4] isha mode/value | flag / min-or-deg (`iv` moves the isha ANGLE by up to **+8.36°** for birmingham in Nov!) |
| `fm` / `sm` / `dm` / `am` / `mm` / `im` | slots [5..10] | minutes (fajr/sunrise/dhuhr/asr/maghrib/isha) |

Rows: `b`/`e` = MMDD window (e.g. 101–108 = Jan 1–8), `mn` = method name,
`c` = city (all zeros in the shipped data — the schema anticipates per-city
rows). Gate: `y5.f.a()` lists 22 eligible methods; only `emirates`,
`birmingham` (+ `dubai`, whose rows are dead at runtime — dubai ∉ y5.f) carry
rows. This corrects teardown §3, which mis-mapped `fm=−3.0, dm=−1.0` — the real
Jan-1-8 emirates row is `fa=1.18, am=2.0, mm=−2.0`.

### 1.3 High-latitude rule (t5.a.a tail + x5.d) — decoded

`night = fix24(sunrise − sunset)` (crosses midnight = night length). Fajr:
if NaN or `fix24(sunrise − fajr) > (fajrAngle/60)·night` →
`fajr = sunrise − portion + fajrMargin`. Isha symmetric with
`fix24(isha − maghrib) > portion`. Portions: `ANGLE_NIGHT = angle/60`
(default), `MID_NIGHT = 0.5` (uoif/moscow), `ONE_SEVENTH_NIGHT = 0.14286`,
`NONE = 0`. **donor `b.l(a,b) = fix24(b − a)` — argument order is swapped vs a
natural reading** (the key gotcha of the decode).

## 2. What the corpus is — and the two parity tiers

The corpus (`staticprayertimes`, 57 cities, 2014–2019, 20,862 rows) is **not**
engine output: it is a hand-authored Excel table (`#VALEUR!` error values and
`12.29`-style typos are in the shipped data) assembled from per-city calendar
sources. Evidence:

- its effective parameters differ from y5.g (e.g. corpus emirates ≈
  **fajr 18.0° / isha 18.0°** with margins ≈ (−1, −3.3, +2.2, +2.5, +2.8, +0.5)
  vs live y5.g 18.5°/18.5° + seasonal layer);
- several countries' rows bake DST, others don't (per city AND per year —
  syria 2015 rows carry EEST, the aachen 2014/2018/2019 rows come from three
  different sources);
- per-city offsets up to ±20 min inside one method (iraq's six cities).

Therefore parity has two tiers:

| tier | what it certifies | tool |
|---|---|---|
| **port parity** | our Kotlin engine ≡ the donor's LIVE pipeline (same params → same minutes) | `gen_parity_fixtures.py` (donor-mode expected values) → `PrayerTimesParityTest` |
| **corpus reproduction** | our pipeline + fitted correction tables reproduce the donor's shipped fallback data | `verify_engine.py --params fitted` + `fit_methods.py` |

The fitted tables (`tools/fitted_methods.json`) chain four additive layers,
mirroring the donor's own mechanisms: method params → seasonal windows
(`qibla` semantics) → per-city offsets (`c` column semantics) → per-(city,year)
weekly windows (the corpus's per-source calendars). The donor's shipped
equivalent is its 20,862-row static table; ours is the same data in
correction form, valid for any date, no per-day rows.

## 3. Numbers

Baseline before this decode (2026-11-06): emirates only, **87% @ ±2 min**
(our best-guess params 18.2°/18.2°). After the decode + corpus-fitted
correction chain (2026-11-07), **87.78% @ ±1 min** across ALL 21 corpus
methods (18,308 / 20,862 rows; 5 malformed corpus cells + 371 excluded
corrupt rows reported separately):

| method | rows | ±1 min parity | note |
|---|---|---|---|
| libya | 2928 | **100.0%** | engine-clean corpus |
| toulouse | 366 | **100.0%** | |
| emirates | 3294 | **98.9%** | flagship (UAE) — 37 misses, all ±2 min rounding wobble |
| mississauga | 366 | 98.9% | |
| syria | 2196 | 96.8% | EEST baked in corpus; fitted per city/year |
| iraq | 2196 | 93.9% | per-city offsets up to ±20 min (six calendar sources) |
| montreal / windsor / tajikistan / luxembourg | | 89.6 / 84.2 / 91.3 / 81.4% | |
| munchen / paris / calgary / nurnberg / aachen / lyon | | 82.8 / 82.5 / 82.5 / 78.7 / 72.0 / 76.4% | |
| austria / london / belgium | | 78.8 / 71.3 / 62.3% | |
| birmingham / blackburn | 366 each | 51.9% | UK mosque-calendar conventions differ structurally (see §2) |

Reading of the residual gap: the corpus rows for the UK/DE/CA methods are
hand-authored mosque calendars whose seasonal fajr/isha conventions do not
follow the donor's own ANGLE_NIGHT rule shape — the correction-chain tiers
absorb the smooth part (weekly windows) and what remains is per-calendar
structural difference, i.e. **data**, not engine math. Reaching ≥99% there
means shipping per-calendar correction data (S2 cities DB decision), not
further engine work. The donor's own runtime does something equivalent: it
serves those 57 cities from the static table, bypassing its engine entirely.

Port-parity tier (donor-live): `ParityFixtures.kt` (138 rows across 22
methods, donor-mode expected values from the reference python engine) gates
the Kotlin port at ±1 min — `PrayerTimesParityTest`. Note: not yet executed —
no Gradle/JDK toolchain configured locally; first `./gradlew` run pending
(zcode-playbook: idle-time task candidate).

Rounding convention proven from corpus: donor format = round half UP to the
displayed minute (`t5.a.j`), not banker's rounding.

## 4. Corpus data errors found (donor's own artifacts)

- `rasalkhaimah 20140312 dhuhr = "06:26"` (typo for 12:26) — 6 h off.
- `nurnberg 2014` × 4 cells with `.` instead of `:` (`12.29`, `6.53`, …).
- `lyon 20140713 asr = "#VALEUR!"` — uncaught Excel error in the shipped APK.
- lyon 2014-04-16..21: maghrib jumps +20 min for one week (source switch in
  the authoring spreadsheet).
- The fitter excludes cells it cannot parse and rows >25 min off the full
  model; they are reported at fit time and counted as corpus errors in parity.

## 5. Kotlin mirror (app/src/main/java/app/miqat/core/)

- `PrayerTimes.kt` — the pipeline above, dependency-free, JVM-testable.
  Naming: `CalcMethod` (donor `calcMethod`), `Margins`, `SeasonalCorrections`,
  `HighLatitudeRule` (donor enum names), `LocalPrayerTimes` (fractional local
  hours — the old `PrayerTimesUtc` name was wrong), `Location`.
  `format()` = donor round-half-up.
- `MethodTable.kt` — GENERATED (`tools/gen_kotlin_table.py`): donor-live
  y5.g params + the live seasonal rows for emirates/birmingham. Do not edit;
  regenerate.
- `ParityFixtures.kt` — GENERATED (`tools/gen_parity_fixtures.py`): corpus
  rows with donor-mode expected values; `PrayerTimesParityTest` gates the port
  at ±1 min.
- Known deviations from the donor, accepted for v1: altitude term omitted
  (≤ ±1 min, cities DB carries the column for S2); per-city minute tweaks
  (`tools/city_tweaks.json`) not wired (corpus cities only, sub-minute for
  corpus cities; wire at S2 with the cities DB); DST comes from the caller
  (S2 own-tz table).
