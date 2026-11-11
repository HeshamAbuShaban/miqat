# Decisions log — Miqat

> One line per decision; newest first. `applicationId` `app.miqat` freezes at release.

## 2026-11-11 — The three councils, and the layers they set

- **مجلس النماذج (Council of Models)** — the free-model seats (deepseek /
  minimax / longcat / flint / mimo): research, citation verification, wide
  critique.
- **مجلس التصميم (Design Council)** — palette, type, layout, the day-sky, the
  compass, the home composition.
- **المجلس الاستشاري (Advisory Council)** — the adversarial seat: what a user
  would call dishonest, noisy or missing, and what must NOT be built yet.

**Layers (from the council's verdicts):**

- **Layer 0 — the gates (DONE).** the «تقديري بحسب الاصطلاح» qualifier in
  every window-bound payload; ONE clock for the whole app (two clocks is how
  the location bug happened); the manual-adjust ceiling raised to ±60; the
  missed-Fajr gate (battery-optimisation deep link, audio-focus yield to a
  call, the iqama second call, the always-on countdown); an explicit
  confidence class per time (مؤكد / محسوب / تقديري).
- **Layer 1 — the calendar as a worship ledger (DONE).** one Hijri number per
  cell + a fixed three-slot sigil strip (fast / event / rite — shapes, never
  colour); the day card = a spine (the prayers as points, the spacing IS the
  gap) with the windows as bands behind it, the fast as brackets and the Hajj
  as a pin; the prayer strip under the grid (pick a prayer, see its drift);
  the month ledger as a pull-up sheet; the year as an overlay.
- **Layer 2 — the acts catalog (DONE).** `assets/acts-v1.json` — STATIC,
  versioned, cited content (what it is / why it matters / how it is computed /
  the source) + the live fields from the engine (the window, the state, the
  neighbours). The index screen is a flat time-ordered list; the depth lives on
  each act's own screen.
- **Layer 3 — the craft (DONE).** Palette A «لازورد» (the violet family is
  banned — it composites to mud); the hero as a PROPORTIONAL clock (the gold
  travelled part vs the hairline remainder — progress read as length); the
  home's seven lines, each toggleable.
- **Layer 4 — the sighting-informed calendar + the daily practice.** The
  sighting presets need a signed data drop (no server, no keys) — the
  hijri offset stays the honest lever meanwhile. The daily practice: the
  tasbih screen over the donor's 21 texts, each with its verified citation.
- **Layer 5 — the tooling.** the root `DESIGN.md` (the Stitch format, nine
  sections) and `docs/ui-patterns.md` (the admission rule for any third-party
  UI library). The `anatomy` skill is installed for OpenCode, Antigravity,
  Codex and MiniMax Code on this machine.
- **Layer 6 — the widgets and the export.**

**Cuts:** the separate "nearest act" card (merged into the strip), the separate
motivational channel (merged into the reminder), the duplicated events list
under the grid, "which prayer?" as a settings prerequisite, gradients on cards,
and shadows in the dark theme.

## 2026-11-11 — The three councils, and the Layer-0 mandate

Hesham: «take the wheel, let the council help you — Council of Models → مجلس
النماذج · Design Council → مجلس التصميم · Advisory Council → المجلس
الاستشاري». Named and used from here on:

- **مجلس النماذج (Council of Models)** — the free-model seats (deepseek /
  minimax / longcat / flint / mimo): research, verification, and wide critique.
- **مجلس التصميم (Design Council)** — palette, type, layout, the day-sky, the
  compass, the home composition.
- **المجلس الاستشاري (Advisory Council)** — the adversarial seat: what a user
  would call dishonest, noisy or missing, and what must NOT be built yet.

**The mandate it produced (2026-11-10):** the app is right at its spine
(«متى الميقات، كم بقي، ولماذا — بصدق، بلا إنترنت») but it drifted into a
general Islamic utility, and two gates were open. So:

- **Layer 0 (gates) FIRST** — no feature is added before:
  0.1 the «تقديري بحسب الاصطلاح» qualifier in EVERY window-bound payload;
  0.2 ONE source of "now" for the whole app (the two-clock split caused the
      location bug — the same split could lie again);
  0.3 the manual-adjust ceiling raised (a ±30 min cap refuses an official
      timetable that is further off);
  0.4 the missed-Fajr gate: battery-optimisation deep link, call/audio-focus
      pause, the iqama (second call), the always-on countdown;
  0.5 an explicit confidence class per time (مؤكد / محسوب / تقديري).
- **Then** Layer 1 the calendar as a worship ledger, Layer 2 the acts catalog,
  Layer 3 the craft (palette A, the proportional-clock hero, the seven-line
  home), Layer 4 the sighting-informed calendar + athkar, Layer 5 the tooling
  (root `DESIGN.md`, `docs/ui-patterns.md`, the anatomy skill, the figures),
  Layer 6 the widgets and the export.
- **Cuts**: the separate "nearest act" card (merged into the strip), the
  separate motivational channel (merged into the reminder), the duplicated
  events list under the grid, "which prayer?" as a settings prerequisite, the
  gradients on cards, and the shadows in the dark theme.

## 2026-11-10 (11) — The location rule (bugfix, 2026-11-10)

- `LocationRules.effective(...)` — the coordinates and the UTC offset ALWAYS
  come from the same source: a GPS fix brings its own coordinates AND the
  device's zone; a city brings its own coordinates AND its DST-correct offset.
  Mixing them (which the app did for a while: the emulator's Mountain View fix
  with the city's +2) produced times that belonged to neither place — Fajr at
  15:05, a "20 minutes to Fajr" that meant nothing.
- Picking a city from the list turns the GPS mode OFF: an explicit choice
  beats a stale device fix.
- With the GPS in use the home header says «موقعك الحالي — من نظام الموقع»
  instead of the city's name. The app never shows a place it is not computing
  from (the same «حتى لا يلبس» rule as the time provenance).

## 2026-11-10 (10) — The compass, the distance tool, «الطاعة» in full, and the v3 design

Hesham's fifth brief, and the correction that mattered most: «طاعة» means an act
of worship — it embraces fasting and Hajj, not only the prayers.

- `CompassSensor` — a REAL heading: accelerometer + magnetometer fused,
  `GeomagneticField` for the local declination (true north), a low-pass filter,
  and a three-degree alignment lock. On a device without a magnetometer the app
  says so plainly and still gives the exact bearing and distance.
- `QiblaScreen` — the qibla as a DIRECTION, not a number: a drawn rose over the
  same sky the hero uses, the cardinal marks in Arabic, the Kaaba's needle in
  gold, the phone's heading marked at the top, and the ring closing when the
  needle finds the Kaaba.
- `DistanceScreen` — «كم بعدت عن هذا الوقت؟»: the neovim idea on demand, over
  EVERY appointed time (a prayer, a worship window, a sunnah time, a fasting
  boundary) — with the distance, the side, and the clock time.
- **«الطاعة» widened** (`MomentCategory`): FASTING (الإمساك والإفطار), HAJJ
  (تكبير عرفة، رمي الجمار — on the days they belong to), EID (صلاة العيد), on
  top of PRAYER / WORSHIP / NIGHT / SUNNAH. The rites that have a TIME are
  computed; the day-level ones live in the calendar.
- **The v3 design direction** (docs/design-direction.md) — the sky over the
  Kaaba as the world, the arc as the signature (the hero's horizon, the
  compass's horizon, the gap-bridge's line, the drift chart's baseline), a new
  palette (night indigo → plum → saffron, with gold as the brand and teal as
  the direction), and the hero rebuilt as THE DAY-SKY: stars, a warm horizon,
  and the sun travelling the arc where the day actually is.

## 2026-11-10 (9) — The awareness layer, the DST indicators, the calendar rebuilt

- «اظهار مؤشرات تغير التوقيت الصيفي والشتوي» → `DstInfo` (the next transition of
  the city's zone, with its direction), a notice on the home and a marker in
  the calendar.
- «اظهار الفروق الزمنية على مدى الأشهر» → `Drift` + the drift chart + the
  month table (every day with its time AND its day-to-day change).
- «في neovim … كم تبعد عن الأسطر بوقوفك الحالي» → `AwarenessStrip`: the now,
  the time that has ENTERED («داخل وقت العشاء»), and the distances to the lines
  above and below. It replaced the duplicated rows on the home.
- «أبرز الهجري ودلّل بالميلادي» → the calendar's cells lead with the HIJRI day.
- «التحكم في الموعد المراد عرضه» → three calendar views and the chosen prayer
  as a setting; every indicator toggleable in «المؤشرات».
- Fixed while doing it: the Makkah constant carried a GUESSED zone id, so the
  DST-correct offset applied another zone's rules — zone ids are now resolved by
  NAME from the timezone table.

## 2026-11-10 (8) — Data layer v3: the donor's other tables, used properly

The donor DB carried more than the city list, and the teardown had missed it:

- `MiqatData` — the companion asset `miqat-data-v1.db`: the Arabic COUNTRY
  names (251), the Islamic DAYS worth knowing (59, with the donor's own
  wording), the donor's REMINDER TEXTS with their time windows (21), and the
  TIMEZONE table (416 zones, 28,522 DST rules).
- The city asset became `cities-v3.db`: every city now carries its IANA zone
  id and the donor's zone key, so the app computes the DST-correct UTC offset
  for ANY date (the S2 "own tz table" item) instead of trusting a fixed offset.
- `ArabicFold` — the Arabic search folding (hamza, ta-marbuta, alef maqsura,
  diacritics, tatweel). The BUILD script folds the same way, so the comparison
  runs on an INDEXED column: «الاهرام» finds «الأهرام», «القاهره» finds
  «القاهرة». Pinned by `ArabicFoldTest` — the first build of this column was
  silently broken (empty folds) and the test now guards it.
- `Reconciliation` / `OfficialTimes` — the VERIFICATION screen (R3.3): the
  user enters their city's official timetable and the app shows the difference
  per prayer and can adopt it as the manual adjusts. This is the honest
  offline answer to «المعيار الذهبي هي قيم الإنترنت» — the comparison happens
  against the user's own official data, on the device, with nothing sent
  anywhere.
- `AdhkarCitations` — the council verified the donor's 21 reminder texts one by
  one; eight carry a verified number, the rest print the collection-only
  attribution. The reminder notification now carries the dhikr WITH its source.

## 2026-11-10 (7) — Moment registry, urgency model, location source

- `Moments` / `Moment` / `MomentCategory` — ONE timeline for every appointed
  time (prayers, sunrise, worship windows, night markers, sunnah windows), in
  absolute minutes from local midnight. Because everything is a moment: the gap
  between two appointments is a subtraction, and an interval between ANY two
  categories is computable on demand (his brief: «احسب الوقت بين موعدين اثنين
  ايا كان الصنف»).
- `Urgency` / `UrgencyRules` — CALM / SOON / IMMINENT / NOW from the remaining
  seconds, thresholds configurable (15 / 5 minutes by default). ONE model, used
  by the hero, the prayer rows and the notifications, so the app says the same
  thing at the same urgency («اذا أوشك الوقت على النفاذ فتجعله مختلف»).
- `LocationSource` (GPS / MANUAL) + `TimeConfidence` (CONFIRMED / ESTIMATED) —
  the golden criterion is the user's real location; when it is absent or
  refused, the app says so plainly («موقعك غير مُتحقَّق منه») and computes from
  the picked city. GPS via Android's own LocationManager (no Play Services, so
  the tree stays F-Droid clean).
- `GapBridge` — the app's signature detail: the time between two appointments
  drawn as a hairline connector with its duration, in the prayer list, in the
  gaps table, and in the interval tool.
- The sunnah texts are gated by `docs/sourcing-table.md` — the council verified
  them corpus-wide; three well-known phrasings turned out NOT to exist and were
  dropped before shipping.

## 2026-11-10 (6) — S5 v2 ARCHITECTURE (supersedes the "cards on home" rule)

Hesham's verdict on the v1 UI: «النسخة الحالية سيئة وفقيرة على كافة الأصعدة … its
missing tons» and «كلها وزيادات اخرى سيئة … بذكاء يتم توزيعها وتنقيتها في طريقة
عرضها». The v1 home had become a STACK of unrelated cards, and "settings" was one
bottom sheet with six steppers. Both are now rebuilt as an APP:

- **Four top-level destinations, one per core** (`TopLevel`): الرئيسية / المواقيت /
  التقويم / الإعدادات. This SUPERSEDES decisions 2026-11-07 (2) ("never new
  screens"): his new instruction is explicit — every feature and core gets its
  OWN view. Home keeps ONE hero and nothing else.
- **Every worship window is its own SCREEN** (`WorshipKind`: DUHA / SAHAR /
  MIDNIGHT / LAST_THIRD) with window, definition, source and the
  «تقديري بحسب الاصطلاح» chip — not a row in a list.
- **Settings is a TREE** (`SettingsRoute`): الموقع / الحساب / الإشعارات /
  التقويم / المظهر / عن التطبيق — each opens a real screen whose controls are
  effective (no decorative switches: the dead "continuous notification" switch
  was removed rather than shipped).
- **Design system** (`ui/Theme.kt`): Cairo (SIL OFL, from the donor's own
  assets), one spacing scale, one palette, light+dark.
- **RTL is forced** — the layout direction is a property of the PRODUCT, not of
  the device locale; an en-US device previously rendered the Arabic UI mirrored
  (prayer order reversed). This was the «اللغة الممزوجة» complaint, fixed at
  the root.
- **All numbers are Arabic-Indic by default** and isolated LTR (U+2066/2069) so
  a clock never renders reversed — `NumFmt` is the single entry point.
- `AppState` is the single source of truth; every mutation persists immediately
  and re-arms the alarms.

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
