# Decisions log — Miqat

> One line per decision; newest first. `applicationId` `app.miqat` freezes at release.

## 2026-11-10 (5) — Settings v1 names: `UserSettings` + `PrayerAdjusts`

- `UserSettings` — SharedPreferences wrapper (F-Droid clean, no DataStore yet):
  selected city + per-prayer manual minute-adjusts; persisted selected city
  fixes the «resets to Makkah on every launch» gap.
- `PrayerAdjusts(fajr, sunrise, dhuhr, asr, maghrib, isha)` — user minutes per
  slot (donor `adjust_min_*` parity). Applied in the UI layer AFTER
  `PrayerTimes.compute` (engine untouched — margins stay ihtiyat, adjusts are
  user override). The detail sheet shows engine value vs adjusted value with
  the R3a register badge «تعديل يدوي على هذا الوقت» when ≠ 0 (both values
  rule, council copy).
- Settings entry point: gear in the header → `SettingsSheet` v0 (location block
  with coords/tz + adjusts steppers). The full settings tree (S5) grows from
  this sheet, mirroring the donor's 13-screen completeness checklist (R6).

## 2026-11-10 (4) — Long-run build session: entity names LOCKED before code

- `DayClock` — dual-clock computation (R1): given a day's appointed times + now,
  returns `ClockSegment` = current appointment + elapsed + next + remaining,
  seconds precision, wraparound-safe (before Fajr → current = Isha of yesterday).
- `Appointment(key, labelAr, hour)` — one appointed-time boundary entry
  (prayer or worship-window bound). `ClockSegment` — the result row.
- `WorshipWindows` — R5 computation object: `DuhaWindow` (sunrise+20min →
  dhuhr−20min, «تقديري» label), `NightInfo` (Maghrib→Fajr majority position:
  midnight + last-third start; endpoints documented in code), `SaharWindow`
  (last sixth of night → Fajr, «تقديري»). All NaN-safe (polar).
- Quran text allowed in UI v1 (certain + cited): وَبِالْأَسْحَارِ هُمْ
  يَسْتَغْفِرُونَ [الذاريات ١٨]. The Muslim-2702 hadith stays OUT of UI until
  verified per the guardrail (research-agenda §Guardrail).
- UI additions: ticking hero (HH:MM:SS + elapsed secondary line), worship cards
  (Duha / الأسحار / night), `TimeDetailSheet` (R3 v1 provenance: method,
  angles, seasonal window, margins, raw seconds), rich search rows (local time +
  distance), qibla "0 km" display fix.

## 2026-11-10 (3) — Research agenda opened; entity names proposed, NOT final

- `docs/research-agenda.md` opened per Hesham's «سيئة وفقيرة» brief: streams R1–R7
  + fiqh guardrail + council protocol. Names proposed there — `DualClock`,
  `OrbitWindow`, `TimedWorship`, `NightPortion`, `WorshipWindow` — are
  **provisional**; they lock here only when their stream reaches code (naming
  discipline). Council seats recorded in the agenda; council verdicts land as
  dated appendices there, never merged silently.

## 2026-11-10 (2) — S2 city storage: prebuilt SQLite asset, framework API, no Room

- Cities move from the v0 whole-file TSV scan to `assets/cities.db` (prebuilt by
  tools/build_miqat_assets.py from the donor DB, indexed en_lower/ar, `meta`
  version table; filename carries the asset version, e.g. `cities-v2.db`,
  copy-on-first-open to filesDir). Kotlin wrapper `CityStore` on framework
  `SQLiteDatabase` — **no Room** (no ksp/kapt, F-Droid-clean tree). Search =
  indexed SQL per keystroke; nothing loads 218k rows into memory; first frame
  no longer blocks on I/O (home renders with a hardcoded Makkah default,
  refined from the DB async). This `CityStore` is the vehicle the S2 tz /
  seasonal / day_events tables ride. Engine files untouched (parity state
  unchanged; 10/10 gates re-run).

## 2026-11-10 — Icon: geometric candidates dead; gold-wordmark v2 awaits pick

- All geometric icon candidates rejected by Hesham («كلهم سيء»); locked direction
  = gold «الميقات» wordmark on deep-navy squircle (his 6/10 reference, target ≥8.5).
  v2 four variants rendered in `docs/design/miqat-design-session.html`; pick pending;
  then fonttools vector conversion → adaptive icons. State recorded in design-brief §0.
- ZCode-era stranded state recovered: `SalatukPlus` path confirmed to be a leftover
  symlink → Miqat and deleted (per its own memory-file instruction); design session
  HTML rescued into `docs/design/`. Session env is now OpenCode; playbook rules
  (plan-first, mock-first, emulator loop) unchanged.

## 2026-11-07 (3) — Engine naming scheme locked (Hesham: "التسمية مهمة")

- Kotlin engine entities named for the donor-adjacent domain, future-proof for
  S2/S9: `CalcMethod` (donor `calcMethod`, not the generic "Method"),
  `Margins`, `SeasonalCorrections` (donor `qibla` columns fm/sm/dm/am/mm/im →
  readable prayer names), `HighLatitudeRule` (donor x5.c enum names),
  `LocalPrayerTimes` (the old `PrayerTimesUtc` name was factually wrong —
  values are fractional LOCAL hours), `Location`, `asrShadow` (1/2).
- Data-tier decision: live `MethodTable` = donor-live y5.g params (+ live
  seasonal rows for emirates/birmingham); corpus-fitted tables stay a harness
  artifact (several are degenerate by design — they encode the corpus's own
  per-calendar-source offsets). Port-parity test fixtures are donor-mode.
- Folder/repo renamed to `Miqat` (2026-11-07 (4), Hesham's explicit ask: zero
  traces of the working name anywhere); brand = folder = package now.

## 2026-11-07 (2) — NAME LOCKED + design direction locked

- **Name FINAL: الميقات / Miqat** (Hesham's decision after a counsel session). The
  broader word deliberately matches the broader product (S9: prayer, fasting, night
  thirds, seasons, Hajj markers) — "ميقات الصلاة" would cap it. Hajj-ambiguity caveat
  neutralized by the official tagline: **«مواقيت الصلاة والصيام»** (store listing +
  launcher subtitle at S8). Package `app.miqat` already applied — no further changes.
- **S5 design direction FINAL: قوس اليوم (Day Arc):** Material 3 + palette that follows
  the prayer of the moment (Fajr cool night blue → Duha bright → Asr amber → Maghrib
  sunset → Isha deep navy). Principles: ONE hero (next-prayer countdown), new features
  (S9) appear as contextual cards on the same home — never new screens; Cairo for
  Arabic display type (donor precedent), dark-first, deliberate color restraint
  (Pixel-stock benchmark).
- Process rule for S5: mock the three key screens (home / alarm / widget) as
  interactive HTML first (ZCode visualize), review with Hesham, THEN write Compose.

## 2026-11-07 — Product name: الميقات / Miqat (working brand)

- Hesham researched "ميقات" (Gemini pass, 2026-11-07): sharia-sound word for the fixed
  time of worship — Quranic basis {الصلاة كانت على المؤمنين كتاباً موقوتاً}; concern:
  bare "الميقات" may first evoke Hajj/Umrah ihram miqats.
- Applied: display name الميقات / Miqat, package + applicationId `app.miqat`
  (folder stays `Miqat` = donor working name; brand ≠ folder, precedent
  BigProjectBank/NexaBank). Options evaluated: ميقات الصلاة / مواقيت / موقوتاً /
  المُعلِم / حافظوا — superseded by the lock above.
