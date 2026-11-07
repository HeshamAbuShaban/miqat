<div dir="rtl">

# الميقات / Miqat

<p align="center"><b>مواقيت الصلاة والصيام — تقويم إسلامي ومؤقّت لكل شيء له ميقات</b></p>

<p align="center">
  <img alt="status" src="https://img.shields.io/badge/%D8%A7%D9%84%D9%85%D8%B1%D8%AD%D9%84%D8%A9-%D8%A7%D9%84%D9%85%D8%AD%D8%B1%D9%83%20S1%20%E2%9C%93-2ea043">
  <img alt="platform" src="https://img.shields.io/badge/Android-minSdk%2024-3ddc84">
  <img alt="offline" src="https://img.shields.io/badge/offline--first-100%25-blue">
  <img alt="privacy" src="https://img.shields.io/badge/telemetry-zero-success">
</p>

</div>

**الميقات / Miqat** — prayer, fasting, the special days (أيام البيض، عرفة، عاشوراء…),
Hajj season markers, night thirds — a sort of **Islamic calendar & timer for lots
of things**: everything in a Muslim's day and year that has an appointed time
(*miqāt*). Offline-first: no ads, no accounts, no telemetry — the network is
never needed for its core job.

<div dir="rtl">

> «الصلاة كانت على المؤمنين كتاباً موقوتاً» — والاسم متعمّد الاتساع: كل شيء موقوت
> في يوم المسلم وسنته، ليس الصلاة وحدها.

</div>

## Why this one (the differentiators)

| | |
|---|---|
| **Ministry-grade accuracy** | the time engine is a faithful, fully-decoded rebuild of a proven production prayer app's engine, then verified against its own official-timetable corpus — **87.8% @ ±1 min across 21 calculation methods** (100% on some), with the donor's seasonal-correction layer decoded and re-implemented ([engine-parity.md](docs/engine-parity.md)) |
| **Own time-zone database** | historical tz table shipped in-app — correct times on old devices, no ICU surprises |
| **Beyond prayer times** | Islamic-day engine: Eid, Arafah, Ashura, white days, Ramadan boundaries, night thirds (أثلاث الليل), voluntary-fasting reminders — all computed offline |
| **Modern, kind UI** | Jetpack Compose + Material 3, dark-first "Day-Arc" design, RTL-first (Arabic typography first-class), full accessibility — where the donor app stopped short |
| **Battery-kind scheduling** | exact alarms + re-arm receivers, no always-on foreground service, opt-in countdown notification |

## Status & roadmap

| stage | scope | status |
|---|---|---|
| **S0** | donor teardown & evidence | ✅ done |
| **S1** | prayer engine: decoded, ported, corpus-certified | ✅ done |
| **S2** | data layer: cities DB (218k), own tz table, corrections | next |
| **S3** | adhan/alarm machinery | — |
| **S4** | qibla compass | — |
| **S5** | Compose UI — Day-Arc design | mockups in review |
| **S6-S9** | athkar, exports, Wear, Islamic-calendar expansion | — |

The full ladder lives in [ROADMAP.md](ROADMAP.md); decisions in
[docs/decisions.md](docs/decisions.md); the reverse-engineering teardown in
[docs/TEARDOWN.md](docs/TEARDOWN.md); the engine parity report in
[docs/engine-parity.md](docs/engine-parity.md); the design brief in
[docs/design-brief.md](docs/design-brief.md).

## Build

```bash
git clone https://github.com/HeshamAbuShaban/miqat.git
cd miqat
./gradlew :app:assembleDebug      # JDK 17, Android SDK 35
./gradlew :app:testDebugUnitTest  # parity fixtures (138 donor-corpus rows)
```

## Principles

1. **Offline-first, zero telemetry** — a proven prayer app did it; we keep it and go further.
2. **The data is the moat** — an algorithm alone does not match ministries; algorithm + seasonal corrections + per-city defaults does.
3. **Everything has a miqat** — one brand, one home, every feature as a contextual card.

---

*License: to be decided before the first public release. The research donor
artifacts referenced in `docs/` are local-only and never distributed.*
