# AGENTS.md — SalatukPlus agent contract

Whatever model/agent you are: read `ROADMAP.md`, work the top unfinished step, leave the
ladder + journal better than you found it. Donor facts live in `docs/TEARDOWN.md` — never
re-derive them from the decompile when the doc already has the answer (cite the doc
section instead).

## Session protocol

1. Read `ROADMAP.md` (top unfinished) + last entries in `docs/journal.md` if present.
2. Do not re-pull the APK from the phone; `donor/` is the frozen donor. Read-only
   artifact — never modified, never re-decompiled without need.
3. Engine changes MUST pass `python3 -I tools/verify_engine.py` parity state or improve
   it; record the new number in `docs/engine-parity.md`.
4. No network calls in the app's core path — offline-first is a design invariant
   (donor shipped zero telemetry for its core job; we keep that and go further).
5. No embedded third-party API keys/FB SDK/ad SDKs from the donor — the donor's keys in
   the teardown are evidence, not assets.
6. End every session: append dated entry to `docs/journal.md` (newest first), update
   `ROADMAP.md` statuses.
7. Git: short imperative subjects; `donor/` is gitignored (33 MB APK + decompiles stay
   local).

## Non-goals

- No ads, no Facebook/GMS dependency, no account/cloud anything.
- minSdk 24 floor (donor was 21; Lollipop is dead) — revisit only with a real reason.
- Don't clone the donor's UI 1:1 — the UI is the part we deliberately replace
  (Compose, Material 3, Pixel-stock benchmark per Hesham).
