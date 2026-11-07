# SalatukPlus → **الميقات / Miqat**

Rebuild of **Salatuk** (`com.masarat.salati` v3.7.17, by Masarat) — takes every lead
from the donor teardown and turns it into a modern, offline-first prayer app.
Built by/for Hesham. The donor APK + full decompile live here in `donor/` (kept out of
the android-re lab on purpose — this project stands alone).

**Brand (2026-11-07):** الميقات / Miqat — package `app.miqat`. Folder keeps the donor
working name `SalatukPlus`; final name variant is Hesham's pending pick — options +
change-cost note in `docs/decisions.md`.

- **Teardown of record:** `docs/TEARDOWN.md` (mirror of android-re
  `docs/salatuk-teardown.md`, REA Evidence-cited)
- **Engine parity state:** `docs/engine-parity.md` — 87% of the donor's own
  official-timetable corpus reproduced within ±2 min on the first night
- **Work ladder:** `ROADMAP.md` (S0 → S8; every donor lead + the improvement backlog)
- **Agent contract:** `AGENTS.md` (read first, same discipline as the lab)

```
donor/                  donor APK, jadx decompiles, cities DB, REA Evidence
  ├─ com.masarat.salati-3.7.17.apk        sha256 14e92319…
  ├─ extracted-code/  extracted-res/      jadx 1.5.6 output
  ├─ cities-db/SalatukDB.db               218k cities / tz / corrections corpus
  └─ rea-evidence/inspect-evidence.json   REA Evidence ev_ea7cc79a…
app/                    the rebuild (Kotlin, Compose, offline-first)
tools/verify_engine.py  parity harness vs donor corpus
docs/                   teardown, parity, decisions
```

Build (first build downloads Gradle + deps — normal Android toolchain):
```bash
cd app && ./gradlew :app:assembleDebug
```
