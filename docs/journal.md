# Journal — newest first

> One dated entry per working session (see AGENTS.md). Next agent: read ROADMAP top
> unfinished step, then this.

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
  `app.miqat` (git mv of Kotlin trees, zero `salatukplus` residuals in code). Folder
  stays `SalatukPlus` (donor working name). Deliberation + options recorded in
  `docs/decisions.md`; final variant = one-string change.
- **Handed off:** his Islamic-calendar expansion vision recorded as **S9** in ROADMAP
  (Eid/Arafah/Ashura/white days engine, أثلاث الليل + Duha + imsak times,
  voluntary-fasting reminder layer Mon-Thu/white-days/Arafah/Ashura/Shawwal-six with
  one-tap ack, escalating salah reminders with user-tunable frequency). All must ride
  the existing offline engine + S2 data layer.
- **Nothing else changed:** engine, parity numbers, donor artifacts untouched today.
- **Next session:** S1 tail (other methods' parity runs + `qibla` column decode) or
  S2 data layer; S9 design sketch after S2 exists. Git remote still his call.
