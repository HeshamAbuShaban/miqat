# ROADMAP — the ladder

Statuses: todo / doing / done / dead. New idea → new entry, never hoard in chat.
Every step cites its donor lead (teardown §).

## S0 — Foundation — DONE 2026-11-06

- [x] Donor pulled read-only from POCO `35d5637e` → `donor/` (sha256 14e92319…)
- [x] jadx decompiles (`extracted-code/`, `extracted-res/`) + cities DB extracted
- [x] REA Evidence `ev_ea7cc79a…` (inspect-android-package, jadx-headless 0.7.1)
- [x] Teardown doc (teardown §0–§8)
- [x] Parity harness `tools/verify_engine.py` + first parity numbers (§S1)

## S1 — Prayer engine, ported and verified — DONE 2026-11-07 (engine fully decoded)

- [x] PrayTimes-style core port (julian day, declination, eqt, angle times, asr)
- [x] `emirates` method solved from donor data (superseded by the full decode below)
- [x] ihtiyat margins measured (superseded: real margins come from y5.g + qibla now)
- [x] Kotlin `PrayerTimes` + data-driven `MethodTable` (app module)
- [x] Donor engine FULLY decoded: y5.g 59-method table (11-slot format), y5.b
      pipeline, QiblaAdjust column mapping (v5.f proven), high-lat rules,
      round-half-up format — docs/engine-parity.md §1
- [x] `--method` runs for ALL corpus methods → corpus-fitted correction chain
      (params → seasonal windows → city offsets → city/year weekly windows):
      **87.78% @ ±1 min** over 20,862 rows (libya/toulouse 100%, emirates 98.9%);
      UK/DE mosque-calendar methods 52-84% = corpus data difference (§2-§3) —
      ≥99% there needs per-calendar DATA at S2, not engine work
- [x] `qibla` table fully decoded + corrections layer implemented
      (tools/qibla_adjusts.json; TEARDOWN §3 corrected)
- [x] Unit test parity: GENERATED fixtures (ParityFixtures.kt, 138 donor-mode
      rows) in `app/src/test` gating the Kotlin port at ±1 min — tests not yet
      executed (no local Gradle; first build = idle-time task candidate)

## S2 — Data layer (the moat, teardown §3)

- [x] Cities bundled offline: `assets/cities.tsv.gz` — 218,032 cities (EN/AR,
      lat/lon/tz, per-city default method, 4.1 MB) + `Cities.kt` search loader
      (v0 linear scan; Room/indexing decision below)
- [ ] Room/indexed storage migration (v0 loader is whole-file lazy + scan)
- [ ] Own tz table (donor `timezone`/`zone` design) instead of trusting ICU —
      keeps old-device correctness
- [ ] Seasonal corrections table (schema mirroring donor `qibla`, cleaner columns)
- [ ] `day_events` (Islamic calendar events AR/EN) + `prayer_events` (duas/azkar
      per prayer with priority) — donor corpus already ships the text (AR/EN/FR)
- [ ] Hijri: keep Umm al-Qura tables concept (donor `util/ummalqura`), add
      adjustable ±0..3 day offset + multiple regional calendars (donor has only
      Umm al-Qura)

## S3 — Alarm & audio machinery (teardown §4)

- [ ] Adhan scheduler: AlarmManager exact + boot/timezone/date/connectivity re-arm
      (donor `SalatiReceiver` action list is the checklist)
- [ ] Full-screen lockscreen alarm (`USE_FULL_SCREEN_INTENT` + slide-to-stop)
- [ ] Adhan audio packs: ship per-muezzin packs (donor has haramayn/maghrebi/
      standard + iqama + shahada + hayya-'ala-salah Fajr variant + bird sounds)
      as downloadable/offline-selectable sets
- [ ] Pause on incoming call (TelephonyCallback), BT headset routing awareness,
      shake-to-stop (donor proves all three worth shipping)
- [ ] Auto-DND during prayer with restore (donor SilentService), per-prayer granular
- [ ] Persistent next-prayer countdown notification (donor
      PersistentNotificationService — but optional, off by default; donor forces it)

## S4 — Qibla (teardown §5)

- [x] Great-circle bearing/distance math (`Qibla.kt`, JVM-testable) + home
      mini-section showing bearing & distance per city

- [ ] Compass: accel+magnet fusion, low-pass, `GeomagneticField` declination,
      ±3° alignment lock (donor's exact recipe)
- [ ] Improvements: true-north toggle, sun-shadow qibla method (no magnetometer
      needed), haptic tick at alignment

## S5 — UI — direction LOCKED 2026-11-07: «قوس اليوم» (decisions.md)

- [ ] **Day-Arc color system** over Material 3: palette follows the prayer of the
      moment — Fajr cool night blue → Duha bright → Asr amber → Maghrib sunset →
      Isha deep navy; dark-first, dynamic color as base, restrained accents
- [ ] ONE hero: next-prayer countdown; all S9 features land as contextual cards on
      the same home (fasting today / Arafah / last-third) — never new screens
- [ ] Compose + Material 3, dark/light/dynamic; Pixel-stock benchmark
- [ ] RTL-first (Arabic typography: Cairo display type — donor bundles it), full
      a11y pass (donor has none)
- [x] **Mock first**: home as interactive HTML, reviewed with Hesham (icons v2
      gold wordmark direction pending his pick; alarm/widget next)
- [x] Compose home v0: Day-Arc hero + countdown + contextual cards + city picker
      (MainActivity; palette hardcoded until the theme system lands)
- [ ] Widgets re-imagined (Glance; donor's 3 widgets + analog clock = parity bar)
- [ ] Calendar: Gregorian + Hijri month views (Compose-native)
- [ ] Settings tree mirroring donor completeness (per-prayer adjusts, DST,
      silent modes, formats) — donor settings coverage is the checklist

## S6 — Beyond the donor (the "tons of additions")

- [ ] Athkar/tasbih module (donor DB already carries azkar text)
- [ ] Monthly timetable export: PDF/image share (donor's SharingActivity minimal)
- [ ] Battery-kind scheduler (WorkManager + exact-alarm hybrid; donor burns a
      foreground service for persistence — do better)
- [ ] Backup/export settings (donor has `allowBackup=false` = nothing survives)
- [ ] Per-city method auto-pick from DB default + manual override with badge
- [ ] Optional community/telemetry-free update channel for corrections table
      (signed file drop, no server-side accounts)
- [ ] Wear OS complication / tile (donor has nothing)
- [ ] Locale packs beyond AR/EN/FR (donor ships ~ dozens via strings — keep)

## S7 — Quality

- [ ] Parity CI: donor-corpus fixtures as unit tests, threshold gates
- [ ] R8 + baseline profiles (donor ships baseline.prof — keep the idea)
- [ ] Screenshot tests for home/alarm/widgets

## S8 — Release

- [ ] Store listing: name **الميقات / Miqat** + official tagline
      **«مواقيت الصلاة والصيام»** (decisions.md 2026-11-07 (2))
- [ ] F-Droid-friendly build (no nonfree deps at all), signing, changelog
- [ ] Store-free install path (ADB/sideload — sideload sovereignty, see lab L15)

## S9 — Islamic-calendar expansion (Hesham's vision, 2026-11-07 — handed to next session)

His words: the starter is core on the RE, then expand to **other timings really needed
in the Islamic calendar — computed smartly, no internet, like Salatuk does** — plus
reminders that **spike the user into action**. Concretely:

- [x] Islamic-day engine v0: `IslamicDays.kt` (white days, Mon/Thu, Arafah,
      Ashura, six-of-Shawwal, Ramadan, eids — pure logic) + `Hijri.kt`
      (android.icu Umm al-Qura bridge, minSdk 24, dependency-free) + unit gates
- [ ] **Islamic-day engine** (build on donor `day_events` corpus, teardown §3):
      Eid al-Fitr / Eid al-Adha, day of Arafah, Ashura, white days (أيام البيض
      13/14/15), Ramadan boundaries, Hajj season markers — all precomputed offline
      per year + location, no server.
- [ ] **Night-based times** beyond the 6 prayers: أثلاث الليل (last third of the
      night — Isha-to-Fajr interval math is already in the engine), middle of the
      night, Duha/الضحى time (sunrise-based, standard fiqh offsets), sunrise/sunset
      fast boundaries (imsak vs fajr toggle).
- [ ] **Voluntary-fasting reminder layer** (the engagement spike he wants): Mon/Thu
      (الاثنين والخميس), white days, Arafah (non-pilgrim), Ashura, six of Shawwal,
      Muharram — each push phrased as encouragement with the virtue, one-tap
      "I'm fasting today" acknowledgment that mutes further nudges that day.
- [ ] **Salah reminders with teeth**: the donor's reminder system (AddReminder
      + per-prayer messages in `prayer_events`) as the floor; add escalating
      "gateway" nudges (e.g. wudu prompt, azkar-after-prayer follow-up) — but no
      guilt-trip spam; user-tunable frequency is a hard requirement.
- [ ] Design note: all of this rides on the SAME offline engine + data layer (S2) —
      no new permissions, no network. Keep the donor's zero-telemetry stance.
