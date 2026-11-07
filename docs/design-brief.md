# Design brief — الميقات / Miqat (v1 draft, 2026-11-07)

> Design is the priority after performance (Hesham). This brief maps the donor's
> own failures (teardown §8) to hard design requirements, locks the icon
> direction, and defines the Day-Arc system the S5 Compose build implements.

## 0. Brand mark — direction (Hesham's constraint: NO crescent/dome/doodles)

The icon must express **«ميقات» = the appointed time** — the umbrella concept
over prayers, fasting, night thirds, seasons, Hajj markers (S9). Candidates
explored (visuals in the session):

| candidate | mark | verdict |
|---|---|---|
| **أ — القوس والنقطة** (recommended) | the day's sun-path arc in the Day-Arc palette gradient, one white point ON the arc = the appointed moment; horizon tick below | brand language = UI language, zero religious iconography, scales down cleanly, monochrome-friendly (Android 13 themed icons) |
| ب — الدورة الميقاتية | open day/night cycle ring with the point at the opening | strong "umbrella" reading; reserve as alternate |
| ج — ميم هندسي | geometric kufic-style meem whose head is the point | letter-based, more conventional |
| د — لحظة الأفق | half-disc rising over a horizon line | most literal (fajr/maghrib moment) |

Adaptive icon spec (S8): background `#0E1326`; foreground = arc + point inside
the 66/108 safe zone; monochrome layer = arc + point in white. Point placement
follows the actual next prayer's position on the arc (dynamic icon on Android
13+ themed/monochrome is a stretch goal).

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
