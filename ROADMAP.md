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
- [x] Indexed storage — DONE 2026-11-10 as **prebuilt SQLite asset, no Room**
      (decisions 2026-11-10 (2)): `assets/cities-v2.db` + `CityStore.kt`,
      copy-on-first-open, async first frame, DB-backed ranked search
- [x] Own tz table — DONE 2026-11-10: the donor's `zone` (416) + `timezone`
      (28,522 DST rules) are bundled; every city carries its zone id, so the
      UTC offset is DST-correct for any date (`AppState.utcOffsetAt`)
- [x] Arabic-tolerant search — DONE 2026-11-10: the names are folded at BUILD
      time into an indexed column (`ar_lower`), so «الاهرام» finds «الأهرام»
- [ ] Seasonal corrections table (schema mirroring donor `qibla`, cleaner columns)
- [ ] `day_events` (Islamic calendar events AR/EN) + `prayer_events` (duas/azkar
      per prayer with priority) — donor corpus already ships the text (AR/EN/FR)
- [ ] Hijri: keep Umm al-Qura tables concept (donor `util/ummalqura`), add
      adjustable ±0..3 day offset + multiple regional calendars (donor has only
      Umm al-Qura + manual offset — donor did NOT solve sighting; research-agenda R4)

## S3 — Alarm & audio machinery (teardown §4)

- [x] Adhan scheduler — **v0 DONE 2026-11-10**: exact alarms via AlarmManager for
      the prayers + worship windows, boot/time/timezone/date re-arm, 4+1 channels
      (تمهيدية/مباشرة/تذكيرية/تحفيزية + عدّاد), quiet hours, in-context
      POST_NOTIFICATIONS request, notification previews. REMAINING: adhan audio
      packs (donor's ogg sets — licensing review before shipping), ongoing
      countdown notification, full-screen alarm ACTIVITY, iqama/second-call,
      call-pause and DND integration
- [x] Full-screen lockscreen alarm — DONE 2026-11-10 (`ui/AlarmActivity`:
      wakes the screen, shows the prayer and its time, stops the call with one
      tap). REMAINING: slide-to-stop instead of a button, and the second-call
      (iqama) mode
- [x] Adhan audio packs — DONE 2026-11-10: the donor's own recordings ship as
      assets (standard / maghrebi / haramayn, two recordings each), played by a
      foreground service as one three-part call; the pack and the recording are
      settings, with a live preview. REMAINING: the iqama / shahada / Fajr
      variant packs, and the bird sounds
- [ ] Pause on incoming call (TelephonyCallback), BT headset routing awareness,
      shake-to-stop (donor proves all three worth shipping)
- [ ] Auto-DND during prayer with restore (donor SilentService), per-prayer granular
- [ ] Persistent next-prayer countdown notification (donor
      PersistentNotificationService — but optional, off by default; donor forces it)
- [ ] Notification-kind matrix: تمهيدية/مباشرة/تذكيرية/تحفيزية, channel per kind,
      per-case sound/vibration, scoped overrides (research-agenda R6)

## S4 — Qibla (teardown §5)

- [x] Great-circle bearing/distance math (`Qibla.kt`, JVM-testable) + home
      mini-section showing bearing & distance per city

- [x] Compass — DONE 2026-11-10: accel+magnetometer fusion, low-pass filter,
      `GeomagneticField` declination, ±3° alignment lock, and a DRAWN rose (the
      qibla is a direction, not a number). The declination is computed at
      runtime (the donor's `qibla` table turned out to be the seasonal layer,
      not a declination table)
- [ ] Improvements: true-north toggle, sun-shadow qibla method (no magnetometer
      needed), haptic tick at alignment

## S5 — UI — direction LOCKED 2026-11-07: «قوس اليوم» (decisions.md)

- [x] **Day-Arc color system** over Material 3: palette follows the prayer of the
      moment — Fajr cool night blue → Duha bright → Asr amber → Maghrib sunset →
      Isha deep navy; dark-first, dynamic color as base, restrained accents
      (v0 hardcoded palette live)
- [x] ONE hero on home — **v2 (2026-11-10)**: home = hero + today's strip +
      nearest worship window + qibla, NOTHING else; every other feature moved
      to its OWN screen (4 top-level destinations + sub-routes). This SUPERSEDES
      the old "cards on home, never new screens" rule — his new instruction was
      explicit: each feature and core in its proper view, no stacked home
      (decisions 2026-11-10 (6))
- [x] Design system: Cairo (OFL), one palette + spacing scale, light/dark,
      RTL forced at the app level, Arabic-Indic digits isolated LTR
      (`ui/Theme.kt`, `core/NumFmt.kt`)
- [x] Settings TREE: الموقع / الحساب / الإشعارات / التقويم / المظهر / عن التطبيق
      — six real screens, every control effective; method picker is a search
      dialog over the donor's 59 methods
- [ ] Compose + Material 3, dark/light/dynamic; Pixel-stock benchmark
- [x] **R1 dual clocks** — DONE 2026-11-10, REBUILT 2026-11-11: remaining
      PRIMARY + elapsed SECONDARY, HH:MM:SS ticking (1 Hz aligned ticker,
      leaf-scoped recomposition); city-wall-clock correctness (device tz never
      trusted for a picked city). The model is now `core/Timers.kt` (ONE clock
      for the app): every feature has its OWN line and its OWN timer — the
      next prayer in the hero, the elapsed since the prayer that just entered
      (first hour only), the iqama on its own line. `DayClock.kt` was DELETED
      (its wrap-around logic is what produced «منذ العشاء ١٢:٤١:٢٠»).
      REMAINING: the worship windows' own secondary clocks (R1 wants them too)
- [x] **R2 rich search rows** — local-time-now + distance per row, ranked
      prefix-first (v0; Arabic normalization still open below)
- [x] **R3 «why this time» sheet v1** — displayed-vs-computed-seconds,
      method/angles/modes/margins/seasonal-window/high-lat provenance,
      guardrail line «حتى لا يلبس على الناس»; R3b register copy shipped for
      high-latitude (R3a manual-adjust register lands with the settings pass)
- [x] **R5 worship-window cards v0** — الضحى (own window, distinct from
      الشروق), الأسحار (cited Uthmanic ayah only, guardrail), منتصف الليل +
      الثلث الأخير, all «تقديري بحسب الاصطلاح» (council chip);
      `WorshipWindows.kt` + tests
- [ ] RTL-first (Arabic typography: Cairo display type — donor bundles it), full
      a11y pass (donor has none)
- [x] **Mock first**: home as interactive HTML, reviewed with Hesham (icon DONE
      2026-11-10: variant ٢ gold wordmark + arc-point, commit `de919cd`;
      alarm/widget mocks next)
- [x] Compose home v0: Day-Arc hero + countdown + contextual cards + city picker
      (MainActivity; palette hardcoded until the theme system lands)
- [x] Widgets re-imagined — DONE 2026-11-11, TRUE 2026-11-12: ONE widget
      (the donor shipped three), no new dependency, and the SAME hero the
      app draws: the sky follows the hour, the horizon fills the panel's
      real width, the ticks sit at the prayers' real times (`DayArc.tickParams`),
      Cairo + tnum, type 22/30/13sp. The design council measured the old
      widget: the countdown stood 30 min stale (the tick was armed on
      `onEnabled` alone and used the protected system `TIME_TICK` — the alarm
      was simply never there), the horizon letterboxed into 33% of the panel,
      ink covered 3.1%. Now the countdown ticks on the widget's OWN alarm
      re-armed on update/resize/enable/boot (verified live and across a
      reboot), prints minutes never seconds, and the preview is the SAME
      layout on DayArc's own quadratic. The clock-face variant is KILLED
      (the horizon IS the clock face; the roadmap idea is stale). REMAINING:
      the 2×1 compact size (the council's rail design), `previewImage` for
      API 24–30, the keyguard layout.
- [x] Dual clocks everywhere — DONE 2026-11-11 (home, the times screen, the
      acts' rows and each act's own screen): the remaining is primary, the
      elapsed is secondary, and every feature keeps its own line and its own
      clock (`core/Timers.kt`).
- [ ] windowed on-demand computation («وضع المدار») for calendar (research-agenda R1)
- [ ] Rich search rows + ranking disclosure («معلنة ومدللة») over the S2 index
      (research-agenda R2)
- [ ] «Why this time» transparency screen + two warning registers (حتى لا يلبس
      على الناس / توخي وحذر) + offline timetable reconciliation (research-agenda R3)
- [x] Calendar — DONE 2026-11-10, REBUILT 2026-11-11 as a worship ledger: the
      Hijri leads, the Gregorian is secondary, a fixed three-slot sigil strip
      (fast/event/rite) marks each day, the day card is a spine (the prayers as
      points, the spacing IS the gap) with the windows as bands, the fast as
      brackets and the Hajj as a pin; the prayer strip under the grid, the
      month ledger as a pull-up sheet, the year as an overlay. REMAINING: the
      sighting-informed presets (needs a signed data drop), the monthly export.
      (The earlier three-view version: three views —
      الشهر (Hijri-first cells, Gregorian secondary), جدول الشهر (every day with
      the chosen prayer's time and its day-to-day change) and الانحراف السنوي
      (the curve with the total change in words); the month's events listed
      under the grid; the day screen carries the prayers, the gaps and the
      worship windows; DST transition days are marked.)
- [x] The awareness layer — DONE 2026-11-10: the strip (now / the time that
      entered / the distances to the neighbouring lines — the neovim idea),
      the DST notice, and every indicator toggleable in «المؤشرات»
- [ ] Settings tree mirroring donor completeness (per-prayer adjusts, DST,
      silent modes, formats) — donor's 13 preference screens are the checklist;
      add «التوابع» discipline: previews, computation warnings, location
      verification, scoped notification control (research-agenda R6).
      **v0 DONE 2026-11-10**: `SettingsSheet` (location block with coords/tz/
      method + per-prayer ± minute steppers, ±30 cap) + `UserSettings.kt`
      (SharedPreferences; saved city fixes the reset-to-Makkah gap) + R3a
      register live in the detail sheet («تعديل يدوي على هذا الوقت», both
      values shown); DST/silent/previews/notification-scoping remain

## S6 — Beyond the donor (the "tons of additions")

- [x] **R8 urgency model** — DONE 2026-11-10: the countdown changes as the
      appointed time approaches (CALM/SOON/IMMINENT/NOW), shared by the hero,
      the rows and the notifications; thresholds configurable
- [x] **R9 the gaps between appointments** — DONE 2026-11-10: hairline bridges
      with the duration in the prayer list + a «المدد بين المواقيت» screen with
      a proportional measure; which gaps show is a setting
- [x] **R10 interval between any two moments** — DONE 2026-11-10: «احسب بين
      ميقوتين» (any categories — e.g. Fajr → Duha for i'tikaf)
- [x] **R11 location source + confidence** — DONE 2026-11-10: GPS when the user
      allows it (Android LocationManager, F-Droid clean), the picked city
      otherwise, with the warning shown in the settings AND on the prayer-detail
      page. REMAINING: verification against an official timetable (R3.3)
- [x] **R12 مواقيت السنة** — DONE 2026-11-10: النوم، السحور، القيلولة، أوقات
      قراءة القرآن — computed from the day; texts gated by docs/sourcing-table.md
- [x] Tasbih / the daily practice — DONE 2026-11-11: the donor's 21 reminder
      texts (the DB carries no separate athkar table — that claim was wrong)
      with the council's verified citations and a tap-to-count. REMAINING: the
      traditional per-dhikr counts (not yet verified)
- [x] Monthly timetable export: PDF/image share — DONE 2026-11-11: the month
      as a designed card (`ui/export/MonthCard.kt`), shared through the
      platform's FileProvider (no new dependency), with the confidence and
      the method printed at its foot.
- [ ] Battery-kind scheduler (WorkManager + exact-alarm hybrid; donor burns a
      foreground service for persistence — do better)
- [ ] Backup/export settings (donor has `allowBackup=false` = nothing survives)
- [ ] Per-city method auto-pick from DB default + manual override with badge
- [x] Verification against an official timetable — DONE 2026-11-10: the user
      enters the official times of their city and the app shows the difference
      per prayer and can adopt it as the manual adjusts (R3.3, offline)
- [ ] Optional community/telemetry-free update channel for corrections table
      (signed file drop, no server-side accounts)
- [ ] Wear OS complication / tile (donor has nothing)
- [ ] Locale packs beyond AR/EN/FR (donor ships ~ dozens via strings — keep)

## S7 — Quality

- [ ] Parity CI: donor-corpus fixtures as unit tests, threshold gates
- [ ] R8 + baseline profiles (donor ships baseline.prof — keep the idea)
- [ ] Screenshot tests for home/alarm/widgets

## S8 — Release

- [x] Store listing: name **الميقات / Miqat** + official tagline — DONE
      2026-11-11: `docs/store-listing.md` (the AR + EN copy, the privacy
      statement, the screenshots to take, the release checklist) and
      `CHANGELOG.md`. The claims are verifiable: the built APK holds **no
      INTERNET permission** (`aapt2 dump permissions`).
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
      night, Duha/الضحى time (solar-altitude thresholds, distinct from Shurūq),
      sunrise/sunset fast boundaries (imsak vs fajr toggle), الأسحار/istighfar
      window — all first-class with own notification profile + settings
      (research-agenda R5; donor has NONE of these — «زوائد غابت» verified).
      **v0 DONE 2026-11-10**: `WorshipWindows.kt` (duha/night/sahar, NaN-safe)
      + home cards with the council chip; REMAINING: notification profiles +
      settings subtrees + imsak toggle + solar-altitude Duha refinement
- [x] **Voluntary-fasting reminder layer** — v0 DONE 2026-11-10: the nudge
      (تحفيزية channel) names tomorrow's fasting after Maghrib or before
      Fajr, for the pickable days (Mon/Thu, white days, Arafah, Ashura, six of
      Shawwal). REMAINING: the one-tap "I'm fasting today" acknowledgment that
      mutes further nudges that day, and Muharram
- [ ] **Salah reminders with teeth**: the donor's reminder system (AddReminder
      + per-prayer messages in `prayer_events`) as the floor; add escalating
      "gateway" nudges (e.g. wudu prompt, azkar-after-prayer follow-up) — but no
      guilt-trip spam; user-tunable frequency is a hard requirement.
- [ ] Design note: all of this rides on the SAME offline engine + data layer (S2) —
      no new permissions, no network. Keep the donor's zero-telemetry stance.
