# ROADMAP — the ladder

Statuses: todo / doing / done / dead. New idea → new entry, never hoard in chat.
Every step cites its donor lead (teardown §).

## S0 — Foundation — DONE 2026-11-06

- [x] Donor pulled read-only from POCO `35d5637e` → `donor/` (sha256 14e92319…)
- [x] jadx decompiles (`extracted-code/`, `extracted-res/`) + cities DB extracted
- [x] REA Evidence `ev_ea7cc79a…` (inspect-android-package, jadx-headless 0.7.1)
- [x] Teardown doc (teardown §0–§8)
- [x] Parity harness `tools/verify_engine.py` + first parity numbers (§S1)

## S1 — Prayer engine, ported and verified — DONE 2026-11-06 (87% @ ±2 min)

- [x] PrayTimes-style core port (julian day, declination, eqt, angle times, asr)
- [x] `emirates` method solved from donor data: **fajr 18.2° / isha 18.2°**
- [x] ihtiyat margins measured: dhuhr +2 (matches donor `qibla.dm`), asr +2,
      maghrib +3, sunrise −2 (fractional residuals documented)
- [x] Kotlin `PrayerTimes` + data-driven `MethodTable` (app module)
- [ ] `--method` runs for `libya`, `austria`, `syria`, `iraq`, `aachen`, `paris`…
      → complete the method table from donor data, mirror into Kotlin
- [ ] Decode `qibla` table fully: `fa` fractional curve + `fm/sm/dm/am/mm/im` sign
      conventions + `ms/mv/is/iv` columns → apply as the corrections layer → target
      **≥99% @ ±1 min**
- [ ] Unit test parity in CI: sample donor rows as fixtures in `app/src/test`

## S2 — Data layer (the moat, teardown §3)

- [ ] City database design: Room + on-disk `cities` (218k EN/AR names, lat/lon,
      tz, per-city default method) — decide: bundle subset by country, full DB on
      first-run download (offline-first: bundle full, compressed)
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

- [ ] Compass: accel+magnet fusion, low-pass, `GeomagneticField` declination,
      ±3° alignment lock (donor's exact recipe)
- [ ] Improvements: true-north toggle, sun-shadow qibla method (no magnetometer
      needed), haptic tick at alignment

## S5 — UI (the part we replace, teardown §8 weak spots)

- [ ] Compose + Material 3, dark/light/dynamic color; Pixel-stock benchmark
- [ ] RTL-first like donor (Arabic typography: donor bundles Cairo font — keep idea),
      full a11y pass (donor has none)
- [ ] Home: next-prayer hero + countdown + day strip; widgets re-imagined
      (Glance; donor's 3 widgets + analog clock = feature parity bar)
- [ ] Calendar: Gregorian + Hijri month views (donor vendored MaterialCalendarView;
      we do Compose-native)
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

- [ ] F-Droid-friendly build (no nonfree deps at all), signing, changelog
- [ ] Store-free install path (ADB/sideload — sideload sovereignty, see lab L15)
