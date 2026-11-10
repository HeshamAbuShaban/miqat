# Research Agenda — «النسخة الحالية سيئة وفقيرة على كافة الأصعدة»

Source: Hesham's brief, 2026-11-10. His words are classified below into research streams
(R1–R7) plus a cross-cutting fiqh-safety guardrail. Verbatim Arabic is quoted in
blockquotes; nothing is paraphrased away. Each stream states: the brief, what the
donor already answers (cite TEARDOWN sections, never re-derive), open research
questions, and the deliverable it feeds.

> «اعمل بحث ودراسة لأمور عليك التركيز عليها وتفاصيل مهمة في خواص و افكار وتطلعات التطبيق»
> «قيم ثابتة ومزالق اللغة الممزوجة والتواقيت وقيم المتبقي - كلها مصنوعة بشكل رخيص جامد لا يناسب الجميع»
> «يجب ان تضيف عناصر كثيرة وشاشات وتوزع التكدسات … يفصل تفصيلا ويوضح بشكل مريح امور كثيرة»
> «لا يعني هذا اني أرضى شكلا مكدسا في الواجهات والمعروضات ، وانما بذكاء يتم توزيعها وتنقيتها في طريقة عرضها»

The last two quotes are the constitution of this whole agenda: **expand depth, not
clutter**. Every addition must pass the distribution discipline (R7).

---

## R1 — The clocks: remaining primary, elapsed secondary, seconds everywhere

> «الوقت المتبقي بشكل رئيسي
> والوقت المنقضي منذ بداية الموعد بشكل ثانوي
> هذا يتم حسابه بالساعات الى الثواني حتى لكل المواعيد والطاعات»

Dual-clock contract for **every** prayer and every worship window (not just the
hero countdown):

- **Primary**: time remaining to the next appointed time — HH:MM:SS, ticking.
- **Secondary**: time elapsed since the *previous* appointed time began — e.g.
  "منذ الفجر ١:٢٧:٠٥" — so the user always knows *which* time he is in, not just
  what is coming.
- Both clocks apply to المواعيد (five prayers + sunrise) والطاعات (Duha, night
  portions, أسحار window — R5).

### On-demand computation («وضع المدار»)

> «ان كان هناك فكرة ذكية تجعل العمليات الحسابية تحصل عندما نحتاجها - فمثلا في التقويم لو أردت ان أعرف مواقيت ليوم بعيد لا يحصل استنزاف لموارد الجهاز في كل وقت وحين لكي ارى القيم في كل ايام السنة - وضع مدار قد يسهل التعامل في نطاق حسابي»

Research questions:

1. **Ticker cadence**: hero clock ticks seconds (Compose state driven by a single
   elapsed-time flow); all other HH:MM:SS rows tick per-minute; second display in
   non-hero rows only on user tap/expand. Avoid 6 parallel second tickers.
2. **Windowed computation**: compute times for a *window* (current day ± 1), not
   the year. Calendar screen computes a month lazily when opened and caches it;
   far-future days compute on scroll. The engine is ~µs/day — the real cost is
   recomposition and layout, so the discipline is *state* scoping, not math.
3. **«وضع المدار»** as a name for the windowed computation scope: a day-range
   orbit [today−1 … today+N] recomputed on date change. Cache keyed by
   (city, method, adjustments-fingerprint).

Deliverable: S5 clock spec + calendar computation policy (decisions entry when
names settle: candidate `DualClock`, `OrbitWindow`).

---

## R2 — Search & results: «معلنة ومدللة»

> «البحث ونتائجه رخيصة وغير معلنة او مدللة كفاية ، ناهيك ان تصميمها رديء»

Current state (v0): indexed SQL prefix-ranked rows "Cair — MK". Functional, poor
presentation. Research questions:

1. **Rich rows**: city + region/country + timezone offset + local time right now +
   method badge + distance from currently-selected city. Arabic name prominent
   (the corpus carries EN/AR).
2. **Ranking disclosure** («مدللة»): why these results — prefix match, alias
   match, common-name match — visibly grouped, not a flat mystery list.
3. **Arabic matching**: normalization (hamza forms, ta-marbuta, alef maqsura),
   prefix vs substring tradeoffs, and transliteration recall (مصر/Cairo/القاهرة).
   The donor's own search behaviour is the baseline to beat — see TEARDOWN §3.
4. Recency: remember last N picks as a "recent" rail above results.

Deliverable: search UI spec (S5) on top of the S2 `CityStore` index (already
shipped, commit `93bbe42`).

---

## R3 — Precision, offsets & transparency — «حتى لا يلبس على الناس»

> «وفي شق الحسابات ينبغي ان يفتح مجال لزيادة دقة الامر المحسوب ، وتبيان ما يشبه ال offset calculation
> # ***حتى لا يلبس على الناس***
> يمكن حتى ان يتاح مزامنة موثوقة او شيء مثل هذا لتستبين»

The engine already carries layered corrections (engine-parity §1: params →
seasonal windows → city offsets → city/year weekly windows). The failure of the
current UI is that **none of this is visible**, so users see one number with no
provenance and no way to reconcile it with their mosque.

1. **«Why this time» screen** — tap any time → full provenance: method + angles,
   seasonal window in effect (with dates), city offsets, manual adjustments —
   each layer shown with the minutes it contributed. The correction chain is
   already data-driven; this is a display of existing state.
2. **Manual-adjust discipline**. Donor evidence (`adjust_min_*` keys,
   `adjust_min_activation_summary`): donor lets the user add per-prayer minutes
   *silently*. Our contract per the brief:
   > «تنبيه لامور حسابية وان كان في مسلك المستخدم تعديلا لها أفضل من حساب التطبيق لان التطبيق ليس متأكد تماما فهذا نوع من التنبيه والتنبيه الأخر فيه توخي وحذر ما هو بالغ الاحتراز والقبول وفقا لحسابات التطبيق»

   Two distinct warning registers, in Arabic UI copy:
   - **Ignorance-avoiding warning** (حتى لا يلبس على الناس): when the user's
     manual offset diverges from the engine by more than a threshold, the row
     carries a persistent subtle badge, and the why-this-time screen explains
     both values.
   - **Caution warning** (توخي وحذر): for engine-side uncertainty (high
     latitude interpolation, DST edges) the UI states the confidence class.
3. **«مزامنة موثوقة»** — offline reconciliation surface (S6): the user opens his
   city's published timetable (photo, PDF text, or manual entry of 3–5 anchor
   dates), the app shows per-prayer deltas and proposes offsets that reproduce
   the published table. **No network**: reconciliation against imported human
   data, not a server. This directly answers «تستبين» without violating the
   offline-first invariant.
4. **Precision headroom**: per-prayer seconds display (donor rounds to minutes;
   our engine computes to the second internally — engine-parity fixtures are
   minute-gated but the raw values carry seconds). Research: show seconds on
   the detail screen, keep minutes on lists.

Deliverable: transparency screen spec + adjustment-warning copy deck (R6 uses
the same registers for notifications).

---

## R4 — Calendar & moon sighting: the problem the donor did not solve

> «هناك احتمال ان تجد رؤية الهلال تختلف فيختلف حكم الشهر في مختلف البلدان... لا أعرف كيف حل هذه المشكلة البالغة في الأهمية التطبيق المتبرع
> وهذا طبعا يطال التقويم (الذي كما توقعت ليس مرضي عنه حاليا)»

**Donor answer, verified from the decompile (not from memory)**: it did not solve
it. Donor ships Umm al-Qura tables (`util/ummalqura/a-c.java`) + a **manual
Hijri offset** only — strings `adjust_hijri` (اضبط التاريخ الهجري),
`hijri_cat` (التقويم الهجري), `hijri_offset_summary` (هنا يمكنك تعديل التقويم
الهجري). No regional calendars, no sighting criteria, no moon-visibility layer.
So this stream is greenfield.

Research questions:

1. **Regional presets**: per-calendar Hijri display (Umm al-Qura; MWL/global;
   local sighting conventions). Each preset = name + adjustment policy, stored
   as data (mirroring the donor `qibla` table pattern — decisions.md).
2. **Sighting-criteria layer** (offline): MABIMS/Odeh-style moon-visibility
   criteria are angle/date computations — feasible fully offline. Scope check
   with the council: is a *visibility forecast* («غدا ليلة الثلاثين،
   الاستهلال ممكن/غير ممكن») in scope for v1, or is preset + manual offset
   enough? Guardrail: forecasts are informational, rulings belong to the
   local authority — copy must say so.
3. **Calendar UI** (his dissatisfaction is with the current one): Hijri-first
   month view with Gregorian secondary, events (R5/S9) marked, day tap → prayer
   times for that day (computed on demand per R1), month swipe with lazy
   computation.
4. Manual offset stays (donor's one good idea here) but **with provenance** (R3):
   "التقويم معروض على تقويم أم القرى +٣ أيام — عدّله".

Deliverable: S2 calendar schema + S5 calendar screen spec; council reviews the
fiqh copy before any sighting text ships.

---

## R5 — Timed worship expansion — «زوائد غابت»

> «وقت الضحى يختلف عن وقت الشروق (ويخصص في الاشعار وكل تبعية خاصة لا فقر في بيان ولا قفل للتخصيص في الإعدادت)
> وكذا أوقات الليل ليس فقط الثلث الأخير
> عندما أقول تفصيلا وتوسعا *أعنيه*»
>
> «طاعة (الاستغفار) ومنها اشتقاقات كثر مثلا "المستغفرين بالاسحار" و "انه ليران على قلبي واني لاستغفر الله..."
> مثل هكذا عبادات موقوتة هي كثيرة في الاسلام»

**Donor evidence (verified this session, grep over sources + values-ar)**: zero
Duha strings, zero night-third strings, no istighfar/أسحار content. «زوائد غابت»
confirmed — this is pure addition, donor gives no baseline.

1. **Duha (الضحى)** — distinct from Shurūq. Research: window definition
   (starts when the sun has risen by ~a spear, ≈ 15–20 min after sunrise;
   ends shortly before Dhuhr) implemented as solar-altitude thresholds, not
   magic minutes. First-class citizen: own row, own notification profile, own
   settings — «لا فقر في بيان ولا قفل للتخصيص».
2. **Night portions** — beyond الثلث الأخير: منتصف الليل، الثلث الأول/الأوسط/الأخير
   over Maghrib→Fajr. Open fiqh question for the council: interval endpoints
   (Maghrib→Fajr is the majority; Isha→Fajr variants exist) — cite, don't pick
   silently. Engine already has the interval math (S9 night-times entry).
3. **الأسحار / istighfar window** — last portion of the night before Fajr,
   presented as the time of وَبِالْأَسْحَارِ هُمْ يَسْتَغْفِرُونَ (Quran 51:18;
   also 3:17). His second quote is likely the Prophet's ﷺ saying «إني لأوتى على
   قلبي وإنني لأستغفر الله في اليوم مائة مرة» (Ṣaḥīḥ Muslim 2702) — exact
   wording and reference **must be verified by the council before it ships**
   (guardrail below). If verified, the أسحار card carries it.
4. **Generalized `TimedWorship` model** (proposed name, pending decisions.md):
   {window(s), display card, notification profile, settings subtree, sourced
   text}. Fasting layers (S9 voluntary-fasting reminders) ride the same model.

Deliverable: worship-window engine spec + content sourcing table (text, source,
verification status). Nothing ships without the sourcing table filled.

---

## R6 — Notification taxonomy & settings discipline

> «يستخدم اشعارات مختلفة
> تمهيدية ومباشرة وتذكيرية وتحفيزية ... اما التحفيزية فممكن ان تكون منفصلة و ايضا محتوى من ما تأتي به يكن مرفوقا مع اخواتها في اخر المحتوى المقدم»
> «والأصوات والاهتزازت وفق حالات مختلفة
> وفي التحكم بالإشعارات والتنبيهات المعينة في وقتا او تاريخ او مناسبة او نحوه يتاح له المجال في الاعدادات كذلك»

1. **Four kinds** (working taxonomy to validate with the council):
   - **تمهيدية Preparatory** — before the appointed time (وudu/خروج lead time,
     per-prayer offset).
   - **مباشرة Direct** — the appointed time itself (adhan).
   - **تذكيرية Reminder** — after (missed-prayer dhikr prompts, iqama gaps).
   - **تحفيزية Motivational** — separate channel; virtues/azkar; content may ride
     along *appended* to other kinds' payloads («في اخر المحتوى المقدم») or
     stand alone — both delivery modes researched.
2. **Android mapping**: one NotificationChannel per kind (per-prayer where
   meaningful). minSdk 24 → pre-O (API 26) fallback strategy must be designed,
   not assumed. Donor evidence: `AdhanPreferences`, `AdhanAlarmModePreferences`,
   `RemindersSilentPreferences`, `VibrationPreferences` — donor separates
   adhan/reminder/silent/vibration but has **no** taxonomy by intent.
3. **Per-case sound & vibration**: «وفق حالات مختلفة» — sound/vibration chosen
   per kind (and per prayer), with live preview inside settings.
4. **Settings discipline — «التوابع»**: every setting shows its consequences:
   - **Visual preview**: hear the adhan/vibration pattern before choosing.
   - **Computation warnings**: R3's two registers reused.
   - **Location verification** («في تبيان الموقع وتأكيده والصلاحيات»): the
     settings entry shows current coordinates, source (GPS/manual/DB city),
     and links to system permission state — so a wrong city is *visible*.
   - **Scoped control**: per-notification overrides by time / date / occasion
     (mute Fajr adhan on travel dates; extra تمهيدية in Ramadan).
5. **Donor settings checklist** (completeness bar, 13 screens in
   `sources/com/masarat/salati/preferences/`): Preferences, AdhanPreferences,
   AdhanAlarmModePreferences, AdhanSilentPreferences, RemindersSilentPreferences,
   SilentPreferences, VibrationPreferences, AdjustPrayerMinPreferences,
   PrayertimesPreferences, CalendarPreferences, DSTPreferences,
   AndroidAppsPreferences, MoreAppsPreferences. Our tree must cover the
   *function* of each (MoreApps = dead, skip) and add the discipline above.

Deliverable: notification-kind matrix (kind × channel × sound × vibration ×
override surfaces) + settings tree diagram (S3/S5).

---

## R7 — Layout: more screens, zero clutter — the distribution discipline

> «يجب ان تضيف عناصر كثيرة وشاشات وتوزع التكدسات
> يفصل تفصيلا ويوضح بشكل مريح امور كثيرة منها»
> «لا يعني هذا اني أرضى شكلا مكدسا في الواجهات والمعروضات ، وانما بذكاء يتم توزيعها وتنقيتها في طريقة عرضها
> اتبع الskills والمسلك المنطقي العملي في التوزيعات»

The locked home direction (decisions.md: ONE hero, contextual cards, never new
screens *for features*) + this brief = an information-architecture problem, not a
smaller feature set. Research questions:

1. **Screen inventory** (proposed): Home (hero + cards) · Prayer-detail (the
   dual clocks + why-this-time) · Calendar · Worship windows list · Search ·
   Settings tree. Each card on home opens its detail — depth, not stacking.
2. **Progressive disclosure rules**: home card shows 1 number + 1 line of
   context; the detail screen carries the rest. A card never grows past two
   lines — new info goes to its detail screen.
3. **Skills to apply** (verified on this machine):
   - `mobile-android-design` (Eilaji) — M3/Compose patterns for the inventory.
   - `frontend-design` (Eilaji/BrainGallery) — anti-template discipline: no
     decorative structure, motion only where it answers action.
   - `unslop` (github.com/heydamir/unslop) — audit pass over v0 UI before the
     v1 redesign; targets exactly his «رخيص» verdict.
   - Candidate discovery: `npx skills find` for notification-design and
     islamic-app patterns; keep results in this file, not chat.
4. Arabic typography remains the differentiator (Cairo display — donor bundles
   it; RTL-first). Numbers in Arabic-Indic vs Latin digits: a real research
   item for the dual clocks (test both on the design session HTML).

---

## Council protocol — «ضع مجلس من الموديلات»

Models available via `tools.opencode.models` (checked 2026-11-10). Per Hesham's
direction (2026-11-10): **free models only, council kept light** — one focused
pass per seat, verdicts appended, no ceremony:

| Seat | Free model (opencode) | Asked for |
|---|---|---|
| Engineering | `deepseek-v4.1-flash-free` | R1 computation windows, R6 Android mapping |
| Product/fiqh-copy | `minimax-m3.1-flash-free` | R3 copy registers, R5 sourcing check |
| Fresh eyes | `qwen3.8-flash` | R7 IA critique of the screen inventory |

(Paid models listed originally — Claude Sonnet 5.5 / GPT-5.5 Pro / Gemini 3.1 Pro
/ DeepSeek V4 Pro / Kimi K3 — remain available if a seat needs escalation.)

Rules:

1. Council sees **this document**, not the chat. Prompts are per-stream with the
   verbatim brief attached.
2. Every council output lands as a dated appendix here (or its own doc if long),
   with a verdict: adopt / adapt / reject — never merged silently.
3. Fiqh-adjacent outputs (any Quran/hadith text, any ruling phrasing) go through
   the guardrail regardless of model confidence.

## Guardrail — «احذر من البدع والضلالات»

Cross-cutting, applies to R3/R4/R5/R6:

- **No invented worship.** We display and remind; we do not prescribe new
  rituals. Every timed-worship text cites its source (surah/ayah, hadith with
  collection & number) in the sourcing table; unverified text does not ship.
- **No ruling claims.** Moon/sighting and night-portion diffs between madhhabs
  are presented as named positions with sources; copy never declares one
  position as *the* ruling. Local-authority deference for calendar rulings.
- **No fabricated precision.** The engine's confidence classes (R3) are honest;
  we never present computed seconds as Sharia-exact moments.
- The council seat assignments exist so at least one non-author model reviews
  every piece of religious content before it ships.

## Priorities

1. **R5 sourcing table + R4 preset schema** — the «زوائد غابت» gap is the
   feature moat; nothing else in the brief is new to the market.
2. **R1 dual clocks** — the first thing any user sees; cheap (engine already
   computes all needed quantities) and it answers «رخيص جامد» directly.
3. **R3 why-this-time + adjustment warnings** — «حتى لا يلبس على الناس» is his
   starred demand.
4. R6 matrix → then R7 redesign pass with unslop audit. R2 rides the S5 pass.
