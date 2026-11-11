## 2026-11-11 — THE TIMERS: the home's clocks, one per feature

Hesham found the home's main timer mixing times: «there must be multiple
feature in its own line … right now im seeing something like "12:41:20" for
how much it has been since "someprayer" which is wrong».

**What was actually wrong** (three faults, one cause):

- `DayClock.position` knew ONE day. After the day's last appointment it
  took "the next" from the same list — so after the Isha it announced
  today's Fajr and counted from yesterday's Isha. That is where the
  twelve-hour elapsed came from.
- The home's strip mixed three clocks on one row (an elapsed, a countdown to
  a moment of any kind — even الإمساك — and the state chip).
- The elapsed was a headline, in seconds, for hours on end.

**What I built:**

- `core/Timers.kt` — ONE model on an ABSOLUTE timeline (yesterday's Isha →
  today → midnight → tomorrow's Fajr), pure and JVM-tested (11 cases).
- The Isha's window now ends at MIDNIGHT; the gap before the dawn belongs to
  no prayer.
- The home: the hero holds the countdown to the next PRAYER; «مرّ من وقت X»
  is its own line (first hour only); «إقامة X» is its own line with its own
  countdown. Each is a separate row with a hairline, each toggleable in
  «المؤشرات» (two new switches).
- A duration no longer wears a ص/م suffix (`hms` vs `clockHMS`) — «1:11:40 ص»
  was a lie about the clock.
- `DayClock.kt` + its test are deleted: an unused model with the old
  wrap-around logic is a trap for the next agent.
- The Times screen's rows use the same model; «الأقرب» no longer points at a
  fasting boundary.

**Verified on the emulator** (the clock was moved to the exact bug times):
09:11 → no elapsed at all (the pre-dawn gap belongs to no prayer); 11:45 →
«مرّ من وقت الظهر ٠:٠٦» + «إقامة الظهر بعد ٠:٠٨» + the hero's countdown to
the Asr; 23:30 → one number only (the countdown to tomorrow's Fajr) and the
chip «داخل وقت العشاء». 60/60 unit tests green; parity unchanged.

# Journal — newest first

> One dated entry per working session (see AGENTS.md). Next agent: read ROADMAP top
> unfinished step, then this.

## 2026-11-11 — THE LONG RUN: four council seats, five layers, the app rebuilt

Hesham: «take the wheel, let the council help you» — and named the councils:
**مجلس النماذج** (the free-model seats), **مجلس التصميم** (the design seats),
**المجلس الاستشاري** (the adversarial seat). Four seats ran in parallel and their
verdicts drove the build:

- **Layer 0 — the gates.** the «تقديري بحسب الاصطلاح» qualifier now rides
  EVERY window-bound payload; ONE clock for the whole app (two clocks is how
  the location bug happened); the manual-adjust ceiling raised to ±60; the
  missed-Fajr gate (battery-optimisation deep link, audio-focus yield to a
  call, the iqama second call, the always-on countdown service); an explicit
  confidence class per time (مؤكد / محسوب / تقديري).
- **Layer 1 — the calendar as a worship ledger.** one Hijri number per cell +
  a fixed three-slot sigil strip (fast / event / rite — shapes, never colour);
  the day card = a spine where the prayers are points and **the spacing IS the
  gap**, with the windows as bands behind it, the fast as brackets and the
  Hajj as a pin; the prayer strip under the grid; the month ledger as a
  pull-up sheet; the year as an overlay.
- **Layer 2 — the acts catalog.** `assets/acts-v1.json`: static, versioned,
  cited content (what it is / why it matters / how it is computed / the source)
  + the live fields from the engine. The index is a flat time-ordered list;
  the depth is on each act's own screen. The acts now carry MEANING, not just
  times — the gap the founder named.
- **Layer 3 — the craft.** Palette A «لازورد» (the violet family is BANNED:
  it composites to mud); the hero became a PROPORTIONAL clock — the travelled
  part of the arc in gold, the rest a hairline, so progress is read as
  LENGTH before any number; the home's seven lines, every one toggleable
  («المؤشرات»), including a daily verse with its reference and the night
  markers.
- **Layer 4/5 — the daily practice and the tooling.** the tasbih screen over
  the donor's 21 texts with the council's verified citations; the root
  `DESIGN.md` (the Stitch format, nine sections) so every future agent builds
  to the same tokens; `docs/ui-patterns.md` (the admission rule for any
  third-party UI library); the `anatomy` skill installed for OpenCode,
  Antigravity, Codex and MiniMax Code on this machine, and the first figure
  built with it (مِقياس الميقات).

**Also**: the crash of the location rule is fixed and guarded by tests; the
council's cuts were applied (the separate "nearest act" card, the separate
motivational channel, the duplicated events list, "which prayer?" as a
settings prerequisite). 40+ unit tests green, parity unchanged.

## 2026-11-10 (9) — BUGFIX: the location and its timezone were mixed

Hesham: «the app is talking about 20min left for fajir based on makkah, when I
set it to cairo, I got that its in daytime». Diagnosed on the device:

- The app had been using the DEVICE's GPS fix (the emulator's default fix:
  37.42, −122.08 — Mountain View, California) while computing the times with
  the PICKED CITY's UTC offset. The result belonged to neither place: Fajr at
  15:05, Dhuhr at 21:52, and a countdown that pointed at the wrong hour. The
  two "now" values even disagreed with each other (the strip said 22:52, the
  segment said "inside Dhuhr").
- **The rule now**: the coordinates AND the UTC offset always travel together —
  a GPS fix brings its own zone, a city brings its own. `LocationRules` makes
  that a pure, tested function.
- **Choosing a city wins**: picking a city from the list turns the GPS mode
  OFF, because an explicit choice must beat a stale device fix.
- **The app no longer lies**: with the GPS in use the header says «موقعك الحالي
  — من نظام الموقع» instead of showing the city's name, and the settings show
  the fix's own coordinates.
- 40/40 unit tests green (3 new: the location rule).

## 2026-11-10 (8) — The compass, the distance tool, the day-sky (his 5th brief)

- **The qibla is a direction now, not a number**: `CompassSensor` fuses the
  accelerometer and the magnetometer, applies the local declination (true
  north), filters the heading, and `QiblaScreen` draws the rose — the same sky
  as the hero, the cardinal marks in Arabic, the Kaaba's needle in gold, the
  phone's heading marked at the top, and the ring closing at the three-degree
  lock. Without a magnetometer the app says so instead of pretending.
- **«كم بعدت عن هذا الوقت؟»** — the neovim measure, on demand, over EVERY
  appointed time (a prayer, a worship window, a sunnah time, a fasting
  boundary): the distance, the side, and the clock time.
- **«الطاعة» in the full sense** (his correction): the timeline now carries
  the fasting boundaries (الإمساك، الإفطار), the Eid prayer, the takbir of
  Arafah and the stoning of the pillars — on the days they belong to — beside
  the prayers, the worship windows, the night markers and the sunnah timings.
  The imsak lead is a setting.
- **The v3 design direction** (docs/design-direction.md): the sky over the
  Kaaba; the arc as the one signature (hero, compass, gap bridge, drift
  chart); a new palette; and the hero rebuilt as THE DAY-SKY — stars above a
  warm horizon, with the sun travelling the arc exactly where the day is.
- While rebuilding the hero, earlier patch damage in MiqatApp.kt surfaced
  (duplicate declarations, an unbalanced brace) — the file was reconstructed
  cleanly rather than patched further.
- 37/37 unit tests green; parity unchanged.

## 2026-11-10 (8) — The compass, the distance tool, the day-sky (his 5th brief)

- **The qibla is a direction now, not a number**: `CompassSensor` fuses the
  accelerometer and the magnetometer, applies the local declination (true
  north), filters the heading, and `QiblaScreen` draws the rose — the same sky
  as the hero, the cardinal marks in Arabic, the Kaaba's needle in gold, the
  phone's heading marked at the top, and the ring closing at the three-degree
  lock. Without a magnetometer the app says so instead of pretending.
- **«كم بعدت عن هذا الوقت؟»** — the neovim measure, on demand, over EVERY
  appointed time (a prayer, a worship window, a sunnah time, a fasting
  boundary): the distance, the side, and the clock time.
- **«الطاعة» in the full sense** (his correction): the timeline now carries
  the fasting boundaries (الإمساك، الإفطار), the Eid prayer, the takbir of
  Arafah and the stoning of the pillars — on the days they belong to — beside
  the prayers, the worship windows, the night markers and the sunnah timings.
  The imsak lead is a setting.
- **The v3 design direction** (docs/design-direction.md): the sky over the
  Kaaba; the arc as the one signature (hero, compass, gap bridge, drift
  chart); a new palette; and the hero rebuilt as THE DAY-SKY — stars above a
  warm horizon, with the sun travelling the arc exactly where the day is.
- While rebuilding the hero, patch damage in MiqatApp.kt surfaced (duplicate
  declarations, an unbalanced brace) — the file was reconstructed cleanly.
- **A tooling accident**: a scripted edit truncated `docs/decisions.md` to
  zero bytes; it was restored from git and the two missing entries re-written
  (the journal and the roadmap kept their copies). A guard was added
  (`tools/check_docs.sh`) so a zero-length document can never ship again.
- 37/37 unit tests green; parity unchanged.

## 2026-11-10 (7) — The awareness layer + the calendar rebuilt (his 4th brief)

- **The awareness strip** (the neovim idea, in time): «الآن ٢٠:٠٤» · «داخل وقت
  العشاء» (the time that has ENTERED) · «منذ العشاء ٠:٥٣» · «قبل منتصف الليل
  ٣:٢١» — the lines around you, and which one you stand on. It replaced the
  two duplicated rows the v2 home carried (less stacking, more meaning).
- **The calendar, rebuilt**: three views — الشهر (Hijri-first cells, the
  Gregorian secondary and smaller), جدول الشهر (every day with the chosen
  prayer's time AND its day-to-day change), and الانحراف السنوي (the curve
  across the year with the TOTAL change in words: «يتأخّر حتى ٩٠ دقيقة بين
  أبعد نقطتين في السنة» + the general trend).
- **The DST indicators**: the next transition of the city's zone, with its
  direction («التوقيت يتقدّم ساعة يوم ٣٠/٣ — فتتغيّر مواعيد الصلوات في تلك
  الليلة»), marked in the calendar and announced on the home.
- **Everything configurable** (his rule): a new «المؤشرات» settings section —
  each indicator can be switched off, the calendar's elements (Hijri day,
  Gregorian day, event dots) too, and the prayer the timetable/drift shows is
  chosen there.
- **A real bug found and fixed**: the Makkah constant carried a guessed zone
  id, so the DST-correct offset applied another zone's rules (Fajr showed 4:10
  instead of 5:10). Zone ids are now resolved by NAME from the timezone table.
- 37/37 unit tests green (5 new for the drift model and the DST wording).

## 2026-11-10 (6) — Data layer v3 + verification screen + the donor's other tables

- **The donor's DB carried more than the cities** — the teardown had missed four
  tables that belong in the app: the Arabic country names (251), the Islamic
  days (59, with the donor's own wording), the reminder texts (21, with their
  time windows) and the TIMEZONE table (416 zones / 28,522 DST rules). All of it
  is now bundled as `miqat-data-v1.db` and used:
  - the search rows show «فلسطين» instead of "PS";
  - the calendar's events come from the donor's own `day_events`;
  - the تذكيرية notification carries the donor's dhikr;
  - and the city asset became `cities-v3.db` with zone ids, so **the UTC offset
    is DST-correct for any date** — the S2 "own tz table" item, closed.
- **Arabic search now tolerates spelling**: the names are folded at BUILD time
  into an indexed column, so «الاهرام» finds «الأهرام» and «القاهره» finds
  «القاهرة». The FIRST build of that column was silently broken (empty folds,
  every Arabic query matched everything) — `ArabicFoldTest` now guards the
  rules, and the build script folds identically.
- **«تحقّق من المواعيد»** (R3.3): the user types their city's official
  timetable; the app shows the difference prayer by prayer and can adopt it as
  the manual adjusts. This is the honest offline answer to «المعيار الذهبي هي
  قيم الإنترنت»: the comparison is against the user's own official data, on the
  device, with nothing sent anywhere.
- **The adhkar citations**: the council verified the donor's 21 reminder texts
  one by one — eight carry a verified number, the rest print «رواه: <المصدر> —
  رقم الحديث غير متحقق منه». The reminder notification prints the dhikr with
  its source.
- 31/31 unit tests green; parity unchanged.

## 2026-11-10 (5) — Overnight run: adhan audio, alarm screen, calendar, fasting nudges, numbers audit

Hesham went to sleep and gave a standing order: keep going, no questions, make
everything customizable, make the light mode good, the donor's audio is allowed,
and work on the calendar.

- **The adhan is real now**: the donor's own recordings ship as assets
  (standard / maghrebi / haramayn, each with two recordings), a FOREGROUND
  service plays the three segments as one call (`USAGE_ALARM`), and a
  lock-screen **AlarmActivity** meets the user with the prayer's name, its time
  and one obvious way to stop it. Verified live: `MediaPlayer state:started`.
  Settings gained: the pack, the recording (١/٢), a real preview, the sound,
  the vibration and the full-screen switch.
- **Numbers audit**: every visible number now goes through `NumFmt` — the
  Arabic-Indic ↔ Latin toggle and the 12/24-hour toggle apply everywhere
  (prayers, gaps, the calendar, the detail sheet, the moment picker). The
  12-hour mode follows the clock everywhere, including the ticking countdown.
- **Light mode**: verified by screenshot — warm paper, dark ink, the amber
  accent only on the live element.
- **Calendar (R4)**: the month grid now lists the month's EVENTS under it (the
  dots are explained), the day screen carries the prayers, the gaps and the
  worship windows, and there is a "back to this month" affordance. The grid is
  a plain seven-column block — no lazy viewport, so no dead band (unslop).
- **Fasting nudges** (the تحفيزية kind): the app can remind you about
  tomorrow's fasting — after Maghrib or before Fajr — for Monday/Thursday, the
  white days, Arafah, Ashura and the six of Shawwal. The days are pickable.
- **Quiet hours are adjustable** (two 30-minute steppers), the qibla line lost
  its Latin digits and its middle-dot, and the home's "nearest" line now names
  the next EVENT (a boundary like the end of a Duha window is no longer
  presented as something to wait for).
- 27/27 unit tests green; parity unchanged.

## 2026-11-10 (4) — Skills applied + R8–R12 (urgency, gaps, intervals, location, sunnah)

Hesham's second brief (and a nudge to actually USE the skills): the remaining
time must be unmistakable, the gaps between appointments must be visible, any
two moments must be measurable on demand, the golden criterion for the times is
the real location, and the sunnah's own timings (sleep, suhur, qaylula, Qur'an
reading) belong in the app.

- **Skills applied, not just read**: `unslop` (direction + 9 passes), the
  `frontend-design` two-pass method, `mobile-android-design`. What changed:
  the hero's hue gradient is gone (flat panel, the arc is the one rich element),
  the ⚙ emoji became a Material icon, radii tightened to a nested scale
  (`Radius`), tabular figures on every clock, and the app's direction is now
  written down (nocturnal / exact / unhurried; the arc as the signature).
- **`Moments`** — one timeline for every appointed time (prayers, sunrise,
  worship windows, night markers, sunnah windows) in absolute minutes. Because
  everything is a moment, the gap between two appointments is a subtraction and
  an interval between ANY two categories is computable on demand.
- **`Urgency`** — CALM / SOON / IMMINENT / NOW from the remaining seconds,
  shared by the hero, the prayer rows and the notifications; thresholds in
  settings. The countdown now CHANGES as the time approaches (amber «اقترب
  وقت…», sunset «استعدّ لصلاة…» / «حان الآن»).
- **`GapBridge`** — the signature detail: the time between two appointments
  drawn as a hairline with its duration (better than the donor, per his words),
  plus the «المدد بين المواقيت» screen with a proportional measure; which gaps
  show is a setting.
- **«احسب بين ميقوتين»** — any two moments, any categories (Fajr → Duha for
  i'tikaf), with the duration and «من الآن حتى…».
- **`LocationSource` + `TimeConfidence`** — GPS (Android LocationManager, no
  Play Services → F-Droid clean) when the user allows it, the picked city
  otherwise, and the app SAYS SO: «موقعك غير مُتحقَّق منه» in the settings and
  on the prayer-detail page, with «ثقة الحساب: تقديري». No online-times mode —
  the app computes locally and never phones home; the honest replacement is
  verification against an official timetable (still open, R3.3).
- **«مواقيت السنة»** — النوم، السحور، القيلولة، أوقات قراءة القرآن, computed
  from the day.
- **The sourcing council did its job**: it verified every narration against the
  canonical corpus and found that THREE well-known phrasings DO NOT EXIST —
  «ينام أحدكم حتى لا يسهر»، «إن في القيلولة شفاء»، «الإخلاص والمفلقتين في يوم
  كأنما قرأ ثلث القرآن» — they never reached a screen. Six texts are verified
  and ship with their numbers (Bukhari 1145/Muslim 758, Bukhari 1923/Muslim
  1095, Bukhari 6281/6279, Muslim 804, Bukhari 5015). The gate lives in
  `docs/sourcing-table.md`.
- 27/27 unit tests green (7 new: moment registry + urgency), parity unchanged.
- Commit `20ab399` (R8–R12), docs updated.

## 2026-11-10 (3) — S5 v2 REBUILD: his «سيئة وفقيرة» verdict acted on

He came back and rejected the UI outright: «do i really have to tell you that
this design is slop! … i didnt prevent you from expanding and putting each
feature and core in its proper view, no bloated thing, this is absolutely
stacked badly presented, you really call that a settings! where the F is
everything... its missing tons!!!». Diagnosis was correct on my side: the home
had become a STACK of unrelated cards and "settings" was one sheet with six
steppers. Rebuilt as an app, not a screen:

- **Architecture**: 4 top-level destinations (الرئيسية / المواقيت / التقويم /
  الإعدادات) + sub-routes; each worship window, each prayer detail, each day
  and each settings section is its OWN screen. Home keeps one hero and stops.
  This supersedes decisions 2026-11-07 (2) («never new screens») — his newer
  instruction wins and is recorded (decisions 2026-11-10 (6)).
- **Settings tree**: 6 sections (الموقع / الحساب / الإشعارات / التقويم /
  المظهر / عن التطبيق), each a real screen with EFFECTIVE controls; the donor's
  59 calc methods are pickable through a searchable dialog; per-prayer minute
  adjusts; Hijri offset; digits/clock/theme. The dead "continuous notification"
  switch was DELETED rather than shipped — no decorative UI.
- **Design system**: Cairo (SIL OFL, from the donor's assets) + one palette +
  one spacing scale + light/dark, in `ui/Theme.kt`.
- **Two root-cause bugs found by looking at real screenshots**:
  (1) the device locale decided the layout direction, so an en-US device
  rendered the Arabic UI mirrored (prayer order reversed, wrong nav order) —
  RTL is now FORCED at the app level; (2) Arabic-Indic digits are bidi class
  AN, so clock strings rendered REVERSED — every number now goes through
  `NumFmt`, which isolates runs LTR (U+2066/2069).
- **S3 v0 (real, not a facade)**: exact alarms for the prayers + worship
  windows, boot/time/tz re-arm, 4+1 notification channels with Arabic names
  (the council's map), quiet hours, in-context POST_NOTIFICATIONS request with
  an honest rationale, and notification PREVIEWS (the «معاينة» he asked for).
  Verified live: 11 alarms scheduled (exact=true), channels created, a posted
  notification record (`channel=miqat_direct importance=4 category=alarm`).
- Fixed along the way: section content rendering under the app bar (the
  Scaffold padding was being dropped), Android BACK exiting the app instead of
  navigating (BackHandler now walks our own stack), the calendar's first
  weekday (Saturday-first offset was off by one), and the Arabic launcher label.
- 20/20 unit tests green; parity unchanged (87.78% @ ±1m). Commits:
  `7ed2526` (design system + navigation + calendar), `3ceb5f4` (S3 delivery),
  `9928811` + `09d698c` (docs + polish) — **and the whole stack pushed**:
  `410bce9..09d698c main -> main` (the credential store still had his token, so
  no new PAT was needed).

## 2026-11-10 (2) — LONG-RUN BUILD: R1/R2/R3/R5 v0 shipped + verified on emulator

His direction: free models only for the council, then «start the long run of
building and completing everything you have». Done in one run (3 commits):

- **Names locked first** (decisions 2026-11-10 (4)): `DayClock`, `Appointment`,
  `ClockSegment`, `WorshipWindows`, `DuhaWindow`/`NightInfo`/`SaharWindow`,
  `TimeDetailSheet`. Quran text gate: only the cited Uthmanic fragment ships.
- **R1 `DayClock.kt`** — dual clocks: remaining PRIMARY / elapsed SECONDARY,
  seconds precision, wraparound + NaN-safe; 1 Hz aligned ticker at Home scope,
  leaves read `State.value` (council: recomposition scoping is the real cost).
  Hero + the two live rows tick; city wall clock derived from UTC+offset
  (device tz never trusted — this fixed a latent correctness bug in "now"/date).
- **R5 `WorshipWindows.kt`** — Duha (sunrise+20 → dhuhr−20), night portions
  (Maghrib→Fajr, midnight + last third), الأسحار (last sixth → Fajr); all
  «تقديري بحسب الاصطلاح» chip (council wording — bare «تقديري» is the wrong
  register). Cards live on home; الأسحار carries ﴿…وَبِٱلْأَسْحَارِ
  هُمْ يَسْتَغْفِرُونَ﴾ [الذاريات ١٨] only — Muslim 2702 out until verified.
- **R3 `TimeDetailSheet`** — tap any prayer row: المعروض (round-half-up) vs
  المحسوب (floor seconds) + method/angles/modes/margins/seasonal window/
  high-lat rule/tz; R3b register («حساب تقريبي في هذه المنطقة») for |lat|≥45;
  guardrail line for «حتى لا يلبس على الناس».
- **R2 rich picker rows** — local-time-now + distance-from-selected per result.
- **Qibla «0 km» nit fixed** («أقل من ١ كم»); `Qibla.distanceKm` gained a
  4-arg haversine overload (additive; parity re-run: 87.78% @ ±1m unchanged).
- **Council (free models, 2 of 3 seats ran)**: engineering
  (deepseek-v4.1-flash-free) + fiqh-copy (minimax-m3.1-flash-free) — verdicts
  adopted and recorded in research-agenda.md appendix; IA seat failed on
  subagent routing (account funds) — rerun later. The engineering seat's
  «biggest risk» line is now the S3 gate: users forgive a plain UI, never a
  missed Fajr → alarm delivery + honest degradation is the reliability work.
- **20/20 unit tests green** (10 new: DayClock 6, WorshipWindows 4 — including
  two arithmetic bugs caught in my own expected values: 19.3 h = 19:18, 9.2 h =
  33120 s). Parity: 87.78% @ ±1m unchanged. Verified live: ticking hero
  (0:16:05 → 0:15:47 across screenshots), detail sheet, rich picker.
- Commits: `d41f06b` (core), `d0e64ea` (UI). **Still awaiting push** (needs his
  PAT): de919cd, 93bbe42, 8b5d2d9, d41f06b, d0e64ea + this docs commit.
- **Settings v0 follow-up (same run, `3536fd7`)**: gear in the header →
  `SettingsSheet` (location block: coords/tz/method + «تغيير المدينة»; 6
  stepper rows for per-prayer ± minutes, ±30 cap) + `UserSettings.kt`
  (SharedPreferences — saved city now survives restarts, fixing the
  reset-to-Makkah gap). Adjusts apply AFTER the engine in the UI layer only —
  engine + parity untouched — and the detail sheet grows the R3a register
  («تعديل يدوي على هذا الوقت» + الحساب الأصلي معروضاً معاً). Verified live:
  +1 min on Fajr propagated consistently to row/hero/sahar window.

## 2026-11-10 — S2 shipped + icon shipped + research agenda (his «سيئة وفقيرة» brief)

- **Icon DONE, committed (`de919cd`)**: his pick = variant ٢ (gold wordmark + arc
  point). Pipeline `tools/build_icon.py` (fonttools vendored; Reem Kufi SemiBold;
  libharfbuzz via ctypes — gotcha: 5-word `hb_glyph_position_t` stride, a 6-word
  assumption produced garbage). Adaptive + legacy PNGs + monochrome; verified live
  in the emulator launcher.
- **S2 cities DONE, committed (`93bbe42`)**: prebuilt indexed SQLite asset
  `cities-v2.db` (218,032 rows), framework `SQLiteDatabase`, **no Room** (F-Droid
  clean). `CityStore.kt` wrapper, copy-on-first-open, async first frame via
  hardcoded Makkah refined from DB. DB-backed search verified on emulator
  ("cai" → Cair first, ranked). Status-bar insets fixed on the way.
- **His product brief arrived** («النسخة الحالية سيئة وفقيرة على كافة الأصعدة») —
  classified into **`docs/research-agenda.md`**: R1 dual clocks + on-demand
  computation (وضع المدار), R2 rich search, R3 transparency «حتى لا يلبس على
  الناس» + offline reconciliation, R4 calendar/moon-sighting, R5 «زوائد غابت»
  (Duha, night portions, الأسحار), R6 notification taxonomy + settings discipline,
  R7 IA/skills. Guardrail section for «احذر من البدع والضلالات» — sourcing table
  before any religious text ships.
- **Donor RE answers fed the agenda** (grep, not memory): moon = manual offset
  only, donor did NOT solve regional sighting; zero Duha / night-thirds /
  istighfar strings anywhere — «زوائد غابت» confirmed; 13 preference screens =
  completeness checklist.
- Council protocol drafted into the agenda (models checked via
  `tools.opencode.models`; Claude Sonnet 5.5 / GPT-5.5 Pro / Gemini 3.1 Pro /
  DeepSeek V4 Pro / Kimi K3 seats, free-tier alternates for batch).
- ROADMAP: R-stream pointers wired into S2/S3/S5/S9.
- Commits awaiting push: `de919cd`, `93bbe42`, + this docs commit (needs his PAT
  in-chat per protocol).

## 2026-11-07 (5) — FIRST BUILD GREEN + app runs on the emulator

- Gradle 8.10.2 installed locally; platform-35 added via sdkmanager; deps from
  google()/mavenCentral. First build failed only in Hijri.kt: android.jar has
  no IslamicCalendar(Date, ULocale) ctor and no CalculationType API — fixed
  with IslamicCalendar(ULocale("en@calendar=islamic-umalqura")) + setTimeInMillis.
- **10/10 unit tests pass** — including the 138-row donor parity fixtures
  (worst deviation 0 min — the Kotlin port reproduces the reference engine
  exactly) and the S9/S4 logic gates.
- Fixed on the way: Qibla bearing x-term (missing sin of Kaaba latitude —
  caught by the Cairo/NY anchors), 3 test-authoring bugs (outside-window day,
  Thursday default in the plain-day test, parity gate asserted worst==1).
- Asset lesson: aapt strips the .gz suffix from assets — cities ship as plain
  `assets/cities.tsv` (APK deflate handles it; loader simplified).
- **The app runs**: emulator install + launch — Day-Arc hero with amber sweep
  and moment dot, live countdown, Hijri date (Umm al-Qura ✓), next-prayer and
  night-third cards, qibla mini-card, 6-prayer list with makkah method
  end-to-end (isha = maghrib+90 ✓). Known v0 nits: title overlaps the status
  bar (insets), qibla bearing noise at ~0 km distance, first-load pause while
  parsing the 218k-city TSV (~1 s) — all queued for the S5 polish pass.
- Note: a physical device (LMQ710MS) was attached to adb — NOT touched, per
  the explicit-go rule; emulator only.

- **Zero-trace rename (his explicit ask):** every trace of the donor working
  name removed from code (Theme.Miqat, rootProject.name = Miqat, MainActivity
  title الميقات) and docs; repo folder renamed `~/WorkingOn/Miqat`; git history
  carries zero references (checked). Donor evidence inside `donor/` untouched
  (its own names are evidence, not ours).
- **Icon direction (his explicit ask):** NO crescent/dome graphics — the mark
  must express «ميقات» (the appointed time) as the umbrella brand for the whole
  feature set (S9), not a prayer-only app. Design exploration runs in the
  visualize skill; candidates follow the locked Day-Arc language.
- **GitHub:** repo created and pushed (see remote origin) with his token — public,
  bilingual README, description & topics per his wording; reworked after his review.
- **Build v0 overnight (his approval):** cities asset built (218,032 cities,
  4.1 MB gz — tools/build_miqat_assets.py) + Cities.kt loader; IslamicDays (S9
  pure logic: white days, Mon/Thu, Arafah, Ashura, six-of-Shawwal, Ramadan,
  eids — offline from a Hijri date) + Hijri.kt bridge (android.icu Umm al-Qura,
  minSdk 24, dependency-free); MainActivity rewritten as Day-Arc home v0: arc
  hero with countdown, contextual fasting card, night-third strip, 218k-city
  RTL search picker, per-city default method from the asset. Not compiled yet
  (no local Gradle) — first build = idle-time task.
- **Design focus next (his words):** design is the priority after performance;
  cover the donor's own gaps (a11y, theming, backup, battery-kind scheduling,
  no forced notification) as design requirements, not add-ons.

## 2026-11-07 (3) — S1 tail DONE: full engine decode + corrections layer + corpus parity

**Goal (Hesham):** S1 tail — parity for the remaining methods, decode the `qibla`
table fully, mirror into Kotlin. Naming discipline up front ("التسمية مهمة").

- **The donor engine is fully decoded** (engine-parity.md §1): y5.g method table
  (59 methods, 11-slot format — makkah isha=maghrib+90 ✓, tehran maghrib angle
  4.5° ✓), y5.b city overrides + seasonal QiblaAdjust layer (byte-identical to
  DB `qibla`), v5.f column mapping proven (fa→fajr angle, iv→isha value,
  fm/sm/dm/am/mm/im→minutes), t5.a.g/a 7-slot pipeline, high-lat rules
  (`b.l(a,b)=fix24(b−a)` — swapped args, the decode's key gotcha), round-half-up
  formatting. Solar math identical to our port (t5.a.q ≡ our sun_position).
- **Corpus reality established:** `staticprayertimes` is hand-authored Excel
  (`#VALEUR!` shipped in lyon row; typos), per-city/per-year calendar sources,
  NOT engine output — so parity has two tiers: port parity (donor-live, exact)
  and corpus reproduction (fitted correction chain).
- **Fitted correction chain** (`tools/fit_methods.py` → fitted_methods.json):
  method params → seasonal windows → per-city offsets (`c` column semantics) →
  per-(city,year) weekly windows. 87.78% @ ±1 min across all 21 corpus methods
  (baseline was 87% @ ±2 min, emirates only). libya/toulouse 100%, emirates
  98.9%, syria 96.8%, iraq 93.9%; UK/DE mosque-calendar methods 52-84% —
  structural data difference, documented as S2 data work, not engine math.
- **Kotlin mirror** (naming per decisions.md 2026-11-07 (3)): `PrayerTimes.kt`
  rewritten = exact donor pipeline; `MethodTable.kt` GENERATED (58 donor-live
  methods + live seasonal rows); `ParityFixtures.kt` GENERATED (138 rows,
  donor-mode expected values) gating the port at ±1 min. Not yet executed —
  no Gradle toolchain locally (idle-time build candidate).
- Extracted donor tables: `tools/qibla_adjusts.json` (112 rows),
  `tools/city_tweaks.json` (109 city tweaks), `tools/extract_donor_tables.py`.
- Corpus data errors documented (engine-parity.md §4) incl. rasalkhaimah dhuhr
  6-h typo. TEARDOWN §3 qibla row corrected (was column mis-mapping).
- **Next session:** S2 data layer (cities DB + tz table + which times to ship
  for the 57 fallback cities) or S5 mockups — his pick. Git remote still his
  call; folder rename question answered in the session (defer to boundary).

## 2026-11-07 (2) — Name + design direction LOCKED (counsel session)

- **Name FINAL: الميقات / Miqat** — Hesham decided after the naming counsel (Gemini's
  Hajj-ambiguity caveat neutralized by the tagline «مواقيت الصلاة والصيام», store
  listing at S8). Package `app.miqat` already applied — nothing to change.
- **S5 direction FINAL: قوس اليوم (Day Arc)** — palette follows the prayer of the
  moment; one hero countdown; S9 features as contextual cards on the same home;
  Cairo display type; dark-first; mock-first rule (HTML mockups → review → Compose).
- New: `docs/zcode-playbook.md` (environment capability → project stage map) +
  AGENTS.md pointer. `docs/decisions.md` upgraded from "working brand" to locked.
- No code/engine/parity changes. Next session: S1 tail (other methods' parity +
  `qibla` decode) or S2 data layer — his pick.

## 2026-11-07 — Name applied (الميقات/Miqat) + vision handed off (S9)

**Goal (Hesham):** he researched "ميقات" as the app name (Gemini pass: sharia-sound,
minor Hajj-ambiguity caveat), wants to defer the final variant pick, apply what's
decided now, and hand the rest to the next session.

- **Applied:** display name الميقات/Miqat (values + values-ar), package/appId
  `app.miqat` (git mv of Kotlin trees, zero residuals in code). Deliberation +
  options recorded in
  `docs/decisions.md`; final variant = one-string change.
- **Handed off:** his Islamic-calendar expansion vision recorded as **S9** in ROADMAP
  (Eid/Arafah/Ashura/white days engine, أثلاث الليل + Duha + imsak times,
  voluntary-fasting reminder layer Mon-Thu/white-days/Arafah/Ashura/Shawwal-six with
  one-tap ack, escalating salah reminders with user-tunable frequency). All must ride
  the existing offline engine + S2 data layer.
- **Nothing else changed:** engine, parity numbers, donor artifacts untouched today.
- **Next session:** S1 tail (other methods' parity runs + `qibla` column decode) or
  S2 data layer; S9 design sketch after S2 exists. Git remote still his call.
