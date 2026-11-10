# Design brief — الميقات / Miqat (v1 draft, 2026-11-07)

> Design is the priority after performance (Hesham). This brief maps the donor's
> own failures (teardown §8) to hard design requirements, locks the icon
> direction, and defines the Day-Arc system the S5 Compose build implements.

## 0. Brand mark — v2 state (updated 2026-11-10, recovered from harness memory)

History: the geometric candidates (أ القوس والنقطة / ب الدورة الميقاتية /
ج ميم هندسي / د لحظة الأفق) were **all rejected** by Hesham («كلهم سيء»).
His reference, self-rated 6/10: **gold «الميقات» wordmark on a deep-navy
squircle** (Reem-Kufi-like construction). Direction locked: premium
typographic gold-on-navy — NOT geometric symbol marks. (He later offered a
mihrab-arch-with-hanging-crescent variant himself, despite the earlier
no-crescent line.)

v2 refinement rendered (goal: refine his reference to ≥8.5/10 or defer):
`docs/design/miqat-design-session.html` (rescued 2026-11-10 from the ZCode
visualization store — the repo is now the canonical home):

| variant | mark |
|---|---|
| ١ — الكلمة، نقاء | pure Reem Kufi wordmark, 4-stop champagne→deep gold gradient, diagonal navy depth |
| **٢ — الكلمة + نقطة القوس (recommended)** | wordmark + thin day-arc beneath with one gold point — the «كل شيء له ميقات» signature, literally the UI's language |
| ٣ — المحراب بلا هلال | mihrab arch, the reference's crescent replaced by a hanging point + wordmark below |
| ٤ — الوضعان الوظيفيان | light mode (cream/deep-gold), monochrome for themed icons, Amiri naskh alternative |

**PICKED 2026-11-10: variant ٢ (wordmark + arc-point)** — Hesham's pick from the
rendered four. Next: convert the wordmark to vector paths (fonttools subset — a
store icon must not depend on a live font), then generate the adaptive icon set:
background `#0D1424`, foreground inside the 66/108 safe zone, monochrome layer
in white.

## 1. Donor gaps → hard design requirements (the "النقص" list)

| donor failure (teardown §8 / §4) | Miqat design requirement |
|---|---|
| Views-era UI, no theme system beyond colors | Material 3 + Day-Arc palette on top of dynamic color; dark-first, light/dynamic supported; one theme source of truth |
| no accessibility pass at all | TalkBack labels on every interactive element, 48dp targets, AA contrast in both modes, reduced-motion respected — a11y reviewed per screen, not a final pass |
| mono-locale logic hacked via MODE_MULTI_PROCESS prefs | state via proper Android storage; locale = resource qualifier only; RTL is the primary layout (LTR follows) |
| `allowBackup=false` — nothing survives device loss | backup/export is designed-in: settings export/import + on-device JSON dump (S6), surfaced in settings, not hidden |
| PersistentNotificationService forced on everyone | countdown notification is opt-in by default-off; home hero carries the countdown instead |
| battery-burning foreground service for scheduling | scheduler = AlarmManager exact + re-arm receivers (S3); the UI never promises "always-on" behavior it can't keep; battery-kind choice explained in plain language |
| ads surface unresolved (AdsActivity) | zero ads surfaces anywhere; no dark patterns in settings |
| settings sprawl (4 silent-mode screens etc.) | settings grouped by intent (الصلاة / التنبيهات / الصيام والذِكر / العام); per-prayer granularity kept — the donor's completeness is the checklist, its organization is not |
| no tests shipped | design QA: screenshot states (empty, Ramadan, offline-first) defined per screen for S7 |

## 2. Day-Arc system (locked direction, decisions.md 2026-11-07 (2))

- **One hero**: next-prayer countdown on the arc. Everything else = contextual
  cards below it (S9 features appear on the same home — never new screens).
- **Palette follows the prayer of the moment**: Fajr cool night blue → Duha
  bright → Asr amber → Maghrib sunset → Isha deep navy. Dynamic color is the
  base; accents restrained (Pixel-stock benchmark).
- Type: Cairo for Arabic display (donor bundles it), system font for Latin
  numerals alternative — decide at mockup review; numerals: Arabic-Indic by
  default in RTL, user-toggleable.
- Motion: one idea only — the arc sweep and the moment point. No looping
  animations; `prefers-reduced-motion` honored (a11y requirement).
- Widgets re-imagined on the same language (Glance): arc widget = parity bar
  vs donor's 3 widgets + analog clock.

## 3. Performance posture (Hesham: "بعد الأداء")

- Engine (S1) is pure math — no allocations in the compute path beyond arrays,
  no I/O, corpus-certified. UI never blocks on it.
- No network in the core path (design invariant, donor §6 — we keep it and go
  further: zero telemetry).
- Widgets/home update on time-tick, not a foreground service; alarms exact,
  batched where possible; the design shows battery-kind status honestly.

## 4. Next design steps (S5 ladder)

1. Hesham reviews icon candidates → pick → generate adaptive icon set (S8 assets).
2. Home mockup review (this session's HTML) → iterate → alarm + widget mockups.
3. Compose implementation begins only after mockups are signed.
