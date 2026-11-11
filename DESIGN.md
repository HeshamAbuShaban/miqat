# DESIGN.md — Miqat (الميقات)

> The design system of this app, in tokens. **Every AI agent reads this file
> before touching the UI.** It is derived from `docs/design-direction.md` and
> the council's review (2026-11-11); when they disagree, this file wins.
> Format: the Google Stitch DESIGN.md convention (9 sections).

## 1. Visual Theme & Atmosphere

- **World**: the sky over the Kaaba, one day at a time. **Character**: nocturnal,
  exact, unhurried. Banned adjectives: clean / modern / premium / sleek.
- **One rich moment per screen**: Home = the day-sky hero; Qibla = the rose over
  the same sky; Calendar = the ledger cell + the drift curve. Everything else is
  rows, hairlines and quiet text.
- **Signature shape**: the quadratic arc — the hero's horizon, the compass's
  horizon, the drift baseline, the gap bridge. One shape, everywhere.
- **Motion**: exactly two moving things — the sun and the needle. Anything else
  must explain a state change or it is deleted.
- **No emoji, ever.** Material icons only, one size (`20dp`), `textDim` unless
  the item is the active destination.
- Gradients are legal in exactly one place: the hero/compass sky. Never on a
  card, a chip or the navigation.

## 2. Colour Palette & Roles

| Token | Dark | Light | Role |
|---|---|---|---|
| `bg` | `#060B1C` | `#F2ECDC` | page (lapis night / dawn paper) |
| `surface` | `#0E1526` | `#FFFCF4` | cards, rows |
| `surfaceHigh` | `#1B2542` | `#E9DEC4` | raised: pressed, pill, sheet header |
| `outline` | `#33426B` | `#C0B392` | 1dp card edge, the hairline, the switch's border |
| `outlineVariant` | `#232C4C` | `#DFD5BC` | the faint hairline inside a card |
| `gold` (`primary`) | `#E8C46A` | `#6F5610` | **chosen / ours**: the brand, the active destination, the selection, today's cell, the focus ring, the app's own marks |
| `saffron` (`tertiary`) | `#FFB25C` | `#96500A` | **now / the clock**: the sun, the next prayer's tick, the countdown, the "now" dot |
| `teal` (`secondary`) | `#7FD0C8` | `#1F6A62` | **a window**: a span of time given to an act — never a number, never a marker |
| `cream` (`onTertiaryContainer`) | `#F8E7AE` | `#6E5A3C` | **an occasion, or a caution**: the Eids, the rites, the warnings, the citations' notes |
| `text` | `#EDEBE3` | `#16181F` | primary text |
| `dim` | `#98A2C4` | `#5A5F72` | secondary text, inactive nav, citations |

- The four meanings are strict: **gold = ours, saffron = now, teal = a window,
  cream = an occasion**. The sun is saffron (the clock), a worship window is
  teal (a span), the next prayer's tick is saffron (it is the clock), and the
  month's occasion is cream (it is an occasion, not a warning).
- **Every card draws its 1dp `outline` edge.** The page→card step is only
  1.07:1, so without the edge the cards have no perceptible boundary — the
  single cheapest-looking thing in the app.
- The contrast is not a claim: `tools/verify_contrast.py` computes every pair
  the app renders, in both schemes, and it must pass.
- The violet family (`#2A1B33`, `#1E1836`) is **banned** — it composites to mud
  between blue and magenta, and the interpolation must not cross it either
  (the sunset desaturates BEFORE the hue turns). **Gold and saffron are the
  only warm hues.**
- Hue means something, never decoration. There is no red token: doubt is
  `textDim` plus a chip, never an error colour.
- The sky's mid-band is lapis ink `#101B36`; the horizon glow is `#8A5533` at
  55% over it.

## 3. Typography Rules

Cairo (SIL OFL, bundled in `res/font` — no download, no network) for
everything. Changa (OFL) may be used for the **countdown numerals only**, and
only if it is bundled locally; if it is not available, Cairo stays alone.

- Every digit-bearing composable sets `fontFeatureSettings = "tnum"`.
- Every numeric run is wrapped in U+2066 … U+2069 (LTR isolate) so an
  Arabic-Indic clock never renders reversed.
- Arabic-Indic digits are the default; Latin is a setting.
- Hierarchy comes from **size and colour**, never from weight: 400 default,
  500 for rare emphasis, 700 only in the hero's title.

| Token | Size/LH sp | Weight | Colour | Use |
|---|---|---|---|---|
| `hero.title` | 34/40 | 700 | `text` | the screen title, once |
| `display` | 28/34 | 500 | `text` | the countdown, the big window |
| `headline` | 22/28 | 500 | `text` | a section opener |
| `title` | 18/24 | 400 | `text` | a row title |
| `body` | 15/22 | 400 | `text` | prose |
| `label` | 13/18 | 400 | `textDim` | row meta, nav label |
| `caption` | 11/16 | 400 | `textDim` | a citation, a source line |

## 3b. The Sky (the hero's phases)

The sky is a FUNCTION OF THE HOUR — never of the theme. One palette, both
schemes: a night sky does not become a pale blue because the phone is in light
mode. The bands (top → horizon) and the ground:

| Phase | b0 | b1 | b2 | b3 (the glow) | ground |
|---|---|---|---|---|---|
| ليل NIGHT | `#070E1F` | `#101E42` | `#1C3164` | `#2B4682` | `#010206` |
| فجر DAWN | `#070E20` | `#111E40` | `#28334B` | `#5D432C` | `#010206` |
| نهار DAY | `#070E1E` | `#121E3E` | `#21325A` | `#3A4966` | `#010206` |
| غروب SUNSET | `#080E20` | `#121E3E` | `#39322A` | `#6A3D27` | `#010206` |

**The marks on the sky** (one token each, shared with the widget and the
exported card): the rim `#93A3CC` (steel — the horizon line, 3.6:1 against
every phase), the prayer tick `#C3CCE2` (pale, 5.6:1), the travelled path
gold `#E8C46A` → saffron `#FFB25C` (5.4:1), the sun `#FFB25C` with a `#FFE0A8`
core. Every mark clears 3:1 against every band it touches.

The stars are the pale token at a fading alpha (1.0 at night → 0.45 at dawn →
0.2 at sunset → 0 in the day). The sky BLEEDS to the hero card's edge — it is
the card, not a rectangle inside it.

## 3c. The navigation (five destinations, our own glyphs)

`الرئيسية` (the horizon) · `المواقيت` (the ticked horizon) · `الصيام` (the
fast's open bracket) · `التقويم` (the ledger cell) · `المزيد` (the grid).
The glyphs are drawn in `ui/BrandGlyph.kt` — 24dp, 2dp stroke, gold when the
destination is current. No Material icons in the bar.

## 4. Component Stylings

- **Button/primary**: filled `gold`, label `bg`, radius 10, height 48 — **one per
  screen**.
- **Button/tonal**: `surfaceHigh` + `gold` label. **Ghost**: text only.
  **Icon**: a 48 hit target, a 20 glyph.
- **States**: pressed = `surfaceHigh`; focus = a 2dp `gold` ring; disabled =
  `textDim` at 40%; loading never (the app is offline).
- **Card**: radius 14, fill `surface`, 1dp `outline`, padding 16–24, gap 12, no
  nesting, no shadow.
- **Row**: radius 12, height 56, leading icon 20, trailing chevron 20, a hairline
  `outline` divider.
- **Chip**: radius 6, height 28, `caption` text. «تقديري بحسب الاصطلاح» and
  «رواه: <المصدر> — رقم الحديث غير متحقق منه» are **always text chips**, never
  icons, never hidden.
- **Input**: radius 10, 48 minimum, 1dp `outline` → `gold` on focus, label 12sp.
- **Navigation** (the densest surface): four destinations, bar 56 + the inset,
  labels 11–12sp, the active item `gold` on a `surfaceHigh` pill, the inactive
  `textDim`, 8dp between items.

## 5. Layout Principles

- A 4dp scale: 4 / 8 / 12 / 16 / 24 / 32. Page edge 16. Card padding 16–24.
  Section gap 24–32. Chip gap 8.
- One column, no dashboard grid. The hero is full-width minus the page edge.
  Content is capped at 600dp and centred above it.
- **At least 40% of every screen stays empty** at the default density. A screen
  that needs a second accent has too many moments.
- The radius ladder: chip 6 → control 10 → row 12 → card 14 → panel 18 → hero
  20. No 24, and no circle except the sun and the needle.

## 6. Depth & Elevation

Depth is **luminance, not shadow**: `bg` → `surface` → `surfaceHigh`, each a
legible step, with a 1dp `outline` where an edge would otherwise dissolve. The
dark theme uses **zero** elevation shadows. One scrim only: `bg` at 40% behind
sheets. Nothing floats; nothing is "elevated" by blur.

## 7. Do's and Don'ts

- **Do** put the citation chip on every religious line; **don't** render a
  narration without its source slot.
- **Do** keep the hero the only gradient; **don't** add a gradient to a card, a
  chip or the navigation.
- **Do** let the numbers breathe in `tnum`; **don't** animate the digit width
  or spell times out in words.
- **Do** force RTL regardless of the device locale; **don't** mirror the
  numerals into RTL order — isolate them LTR.
- **Do** use one icon size; **don't** scale icons to fill space.
- **Don't** bring the violet back "for the night depth" — there is no such
  token.
- **Don't** issue rulings or "recommended" practice: the app states times,
  cites, and stops. «تقديري بحسب الاصطلاح» is not optional on a computed
  window.

## 8. Responsive Behaviour

| Window | Behaviour |
|---|---|
| < 600dp | one column, page edge 16, hero arc 150 |
| 600–839 | hero 190, the schedule rows may pair up |
| ≥ 840 | content capped at 600 and centred, the nav becomes a rail, hero 220 |

Touch targets: 48 minimum, 56 for the navigation and primary actions, 8dp
minimum gaps. Hover exists only on pointer devices and never carries meaning.
A font scale of 2.0 must never clip: the countdown scales down, it never wraps.
Landscape: the hero compresses, the chrome does not reorder.

## 8b. The Widget (the hero, miniaturised)

The widget is the app's hero at a smaller size — same sky, same horizon,
same ticks, same type scale, same words. Not a donor widget in our colours.

- **Always night.** The widget owns no theme of its own: the night palette
  (`#060B1C` panel, the sky's night bands, gold/saffron marks) even in light
  mode — a dark object ON the home screen, never a hole in it.
- **The horizon fills the row.** The bitmap is rendered at the panel's REAL
  width (from `OPTION_APPWIDGET_MIN_WIDTH`), never a fixed size letterboxed
  into a third of the panel. The sky band grows with the panel (`weight=1`).
- **The ticks are the prayers' REAL times** — `DayArc.tickParams`, the same
  numbers the hero reads. One loud tick (the next prayer, saffron), the
  sunrise saffron, the rest pale. The sun hides at night; the stars take over.
- **The countdown is minutes, never seconds** — a widget that cannot keep
  seconds true must not print them. Type: name 22sp, countdown 30sp,
  caption 13sp, Cairo + `tnum`, centred, as the hero centres its type.
- **The tick must survive everything.** The widget's OWN minute alarm
  (`app.miqat.action.WIDGET_TICK` — never the protected system `TIME_TICK`),
  re-armed on update, resize, enable and boot. The urgency tokens apply:
  ≤15 min saffron, ≤5 min sunset.
- **The city is opt-in and honest** («أظهر المدينة في الأداة», default off;
  shown only when the times actually come from that city — never under a
  GPS fix). No other private fact (coordinates, qibla, fasting state) ever
  reaches the home screen.
- **The preview is the widget.** `previewLayout` is the SAME layout file;
  its static sky sits on DayArc's own quadratic, so the picture cannot
  drift from the widget it advertises.
- **Tokens live in `res/values/colors.xml`** (`miqat_*`) — no hex literals
  in the layout, no drift between files.

## 9. Agent Prompt Guide

**Quick colours**: bg `#060B1C`/`#F2ECDC` · surface `#0E1526`/`#FFFCF4` ·
surfaceHigh `#1B2542`/`#E9DEC4` · outline `#33426B`/`#C0B392` · gold
`#E8C46A`/`#6F5610` · saffron `#FFB25C`/`#96500A` · teal `#7FD0C8`/`#1F6A62` ·
cream `#F8E7AE`/`#6E5A3C` · text `#EDEBE3`/`#16181F` · dim `#98A2C4`/`#5A5F72`.

**The four meanings**: gold = ours (chosen), saffron = now (the clock), teal =
a window (a span), cream = an occasion or a caution.

1. "Build the home hero: a 150dp quadratic arc as the horizon; the sky above it
   a vertical gradient `bg` → `#101B36` → `#24304A` → `#8A5533`@55%; stars; a dark
   ground band below; the rim drawn TWICE — the travelled part as a 5dp
   gold→saffron gradient stroke, the rest as a 2dp `#2C3A5C` hairline; the sun
   disc (saffron, with a soft halo) at the junction; the prayer ticks as 3.5dp
   dots, the next one in gold. No shadow, no emoji, radius 20."
2. "Build a schedule row: radius 12, height 56, the prayer name in `title`, the
   time in `tnum` Arabic-Indic inside an LTR isolate, the meta in `label`, a 1dp
   `outline` hairline between rows. No card, no icon unless it is the next
   prayer."
3. "Add the estimate chip under any computed window: `caption`, radius 6,
   `surfaceHigh`, the text «تقديري بحسب الاصطلاح». Never an info icon."
4. "Build a ledger cell: the Hijri day at `bodyLarge`, the Gregorian at
   `labelSmall`, and a fixed three-slot sigil strip underneath — a 8×2dp bar
   (fast, `teal`), a 5dp lozenge (event, `saffron`), a 4dp pin (rite, `gold`).
   The slots are always reserved so the grid never jumps."
