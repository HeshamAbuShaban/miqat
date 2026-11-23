## 2026-11-23 — Night ink: warmer dims, smoother page

User: “night mode feels dead”. Fixes on the dark ramp:

- `NightTextDim` → `#9AA0B8` (was `#7A7F95`); measured 6.68 on `surface`, 5.9 on `surfaceHigh` (old 4.37).
- `NightSurfaceHigh` → `#1D2537` (was `#1E2438`) so chips/dividers read smoother against `surface`.
- FastingScreen track uses standard night track colors instead of ad-hoc blues.
