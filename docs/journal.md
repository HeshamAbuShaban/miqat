# Journal — newest first

> One dated entry per working session (see AGENTS.md). Next agent: read ROADMAP top
> unfinished step, then this.

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
