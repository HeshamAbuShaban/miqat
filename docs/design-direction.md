# The Miqat design direction — v3 (2026-11-10)

Hesham's verdict on the v2 look: «التصميم يحتاج جهود ابداعية … لا تقف على
الحالي ولونه وامور رخيصة في الذكاء العرضي». So this is not a palette tweak; the
app's visual world is re-decided here, using the frontend-design two-pass
method (plan → critique → build) and unslop's rules (one rich moment per
screen, quiet chrome, real material, no decorative filler).

## The direction (five lines)

- **Product.** An offline companion that tells a Muslim when the next appointed
  time arrives, how far it is, and why that time is what it is.
- **Character.** Nocturnal, exact, unhurried. (Not "clean/modern/premium".)
- **World.** The sky over the Kaaba through one day: indigo before dawn, the
  gold hour at sunrise, the long amber afternoon, the plum of the last third of
  the night, and the first light again. The prayer arc is the horizon.
- **Moments.** (1) **The day-sky in the hero** — the arc is a sky with the sun
  travelling it, the horizon glowing under it, and the stars still out above;
  (2) **The compass** — a rose drawn over the same sky, with the Kaaba's
  needle; (3) the drift curve in the calendar, one line, one accent.
- **Signature.** The arc: it is the hero's horizon, the compass's horizon, the
  gap-bridge's line and the drift chart's baseline. One shape, everywhere.

## Colour (the new palette — not the old navy-and-gold flatness)

| Role | Dark | Light | Where |
|------|------|-------|-------|
| Night (background) | `#0A0D1C` | `#F6F1E4` (dawn paper) | surfaces |
| Surface | `#12172B` | `#FFFCF4` | cards, rows |
| Surface high | `#1A2138` | `#EFE7D4` | the raised moments |
| Gold (accent) | `#E8C46A` | `#8A6A16` | the brand, the needle, the marks |
| Saffron (the live moment) | `#FFB25C` | `#B4600C` | the sun, the urgency |
| Plum (the night) | `#2A1B33` | `#EDE3F0` | the night portion, the sky's depth |
| Teal (the direction) | `#6FCFC4` | `#1F6A62` | the compass, the worship windows |
| Ink (text) | `#EDEBE3` | `#16181F` | primary text |
| Ink dim | `#9AA0B8` | `#5A5F72` | secondary text |

The chrome stays quiet (one accent, three text roles); the richness lives in
the drawn moments, where several hues may appear as long as each colour MEANS
something (gold = the brand/needle, saffron = now, plum = night, teal =
direction).

## Type

- Cairo (SIL OFL) for everything — one family, weights 400/500/700 used with
  restraint; hierarchy comes from SIZE and COLOUR, not weight.
- **Tabular figures everywhere** (`tnum`) — a ticking clock must not reflow.
- The countdown is displayMedium at a fixed size so the digit width never
  changes the layout.

## Layout

- Four destinations, one per core; a fifth thing (the compass, the distance
  tool) opens from where it is used, never as a fifth tab.
- Home is ONE hero + the awareness strip + one compact schedule strip. No
  stacks of cards.
- The calendar has three VIEWS (month / month table / year drift), one at a
  time.

## The principles that keep it from being slop

1. **One rich moment per screen.** Everything else is rows, hairlines and
   quiet text.
2. **No decorative motion.** A transition must explain something; the only
   moving thing is the clock and the needle.
3. **Numbers are one format.** Arabic-Indic by default, LTR-isolated, tabular.
4. **Honest about what it does not know** — the R3 registers: the location
   confidence, the estimated windows, the unverified citations.
5. **Everything that shows can be switched off** in «المؤشرات».
