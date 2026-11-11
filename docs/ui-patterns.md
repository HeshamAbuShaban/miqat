# UI patterns — the lookup and the admission rule

A short index for the agent: **before** writing any Android UI pattern or
pulling in a dependency, look here. The upstream index is
[`wasabeef/awesome-android-ui`](https://github.com/wasabeef/awesome-android-ui)
(415 entries, mostly View-era) — read it as a LOOKUP, not a dependency list.

## The admission rule (the agent applies this without asking)

A third-party library enters the project only if **all four** pass:

1. **Offline** — no network, no telemetry at runtime.
2. **F-Droid-clean** — an OSI licence, no GMS/Play/Facebook, no ads, no
   non-redistributable assets, builds from source.
3. **Weight** — Compose-native, or a small maintained View behind
   `AndroidView`; no dependency tree larger than the problem it removes.
4. **DESIGN.md-conformant** — themable to gold/navy without forking it.

If one fails, we write it ourselves. Record the verdict here.

## The entries worth knowing

| Pattern | Where in the index | Our verdict |
|---|---|---|
| Animated / expandable bottom bar | `ExpandableBottomBar`, `AnimatedBottomBar` | **Maybe** — our navigation is a fixed four-item bar; an expandable bar would add motion without a job. Revisit only if a fifth destination arrives. |
| An analog clock | `ComposeClock` | **Rejected (2026-11-11)** — the widget is the app's own horizon (the day arc), not an analog face; a third-party face would be a different shape in the same panel. The widget is drawn by us: `ui/DayArc.kt` (the geometry) + `ui/widget/ArcRenderer.kt` (the bitmap), with NO new dependency. |
| A compact month calendar | `CompactCalendarView` | **No (yet)** — our ledger grid is Compose-native and Hijri-aware; a View-based month view would fight the Hijri-first cells. Reconsider only if the ledger needs sticky headers. |
| Arc motifs | `ArcLayout`, `ArcView`, `ArcProgressBar` | **Rejected** — the arc IS the brand signature; we own its geometry (a quadratic path is ~20 lines of `Canvas`). |
| Date/time pickers | `MaterialDateTimePicker` | **Maybe** — the verification screen needs a date picker; Material's is the obvious candidate if it can be themed without a fork. |
| Sticky headers | `StickyListHeaders` | **Rejected** — Compose's `LazyColumn` + a `stickyHeader` (foundation 1.7+) covers it without a dependency. |
| Charts | `MPAndroidChart`, `WilliamChart`, `hellocharts` | **Rejected** — the drift curve is one line with two marked ends; a chart library is a whole dependency tree for a single path. We draw it in `Canvas`. |
| Compose cookbooks | `ComposeCookBook`, `SSComposeCookBook` | **Reference only** — read for a pattern, never import. |
| Image loading | `Landscapist` | **No** — the app ships no network images; every asset is local. |

## The record

- 2026-11-11 — the rule and the first verdicts written; no dependency has been
  added to the project (and the tree stays dependency-light on purpose:
  Compose + the Android framework only).
- 2026-11-11 — the WIDGET shipped with NO new dependency: the platform's
  `AppWidgetProvider` + `RemoteViews`, and the horizon drawn into a bitmap
  with the platform's own Canvas (`ArcRenderer`) from the same geometry the
  hero uses (`DayArc`). Glance was considered and dropped: it would have
  brought a dependency for one screen, and the preview/image pipeline it
  offers is not needed when the launcher inflates our own layout.
