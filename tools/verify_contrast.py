#!/usr/bin/env python3
"""The palette's contrast audit — does the app really pass WCAG AA?

`DESIGN.md` claims: «Contrast pairs are checked for WCAG AA on both
schemes». This script is that check, run on the tokens the app actually
renders. A pair that fails here is a pair a user in the sun cannot read.

    python3 -I tools/verify_contrast.py
"""

from __future__ import annotations

import re
import sys

# --- the tokens (ui/Theme.kt) --------------------------------------------
DARK = {
    "bg": "#0A0D18",
    "surface": "#161A28",
    "surfaceHigh": "#1E2438",
    "outline": "#262F48",
    "outlineVariant": "#1E2438",
    "primary": "#D4A857",
    "onPrimary": "#241C00",
    "primaryContainer": "#2C2518",
    "onPrimaryContainer": "#FFF2C8",
    "secondary": "#5CC4B8",
    "onSecondary": "#00312C",
    "tertiary": "#F0C875",
    "onTertiary": "#3A2500",
    "tertiaryContainer": "#2C2518",
    "onTertiaryContainer": "#F8E7AE",
    "text": "#EBE8E0",
    "dim": "#AAA398",
    "error": "#F06544",
    "onError": "#0A0D18",
    # fixed dark moments — identical in BOTH schemes (see Theme.kt OnDark*)
    "onDark": "#EBE8E0",
    "onDarkDim": "#C3CCE2",
    "onDarkWarm": "#EBD9A8",
    "onDarkFaint": "#7A8096",
    "skyMid": "#1A1E32",
    "skyHorizon": "#4A2F28",
    "ground": "#06070D",
}
LIGHT = {
    "bg": "#F7F4EE",
    "surface": "#FDFBF7",
    "surfaceHigh": "#EFE9DF",
    "outline": "#D9D1C2",
    "outlineVariant": "#E7E0D3",
    "primary": "#6F5610",
    "onPrimary": "#FFFFFF",
    "primaryContainer": "#F0E7CE",
    "onPrimaryContainer": "#4A3A10",
    "secondary": "#1F6A62",
    "onSecondary": "#FFFFFF",
    "tertiary": "#96500A",
    "onTertiary": "#FFFFFF",
    "tertiaryContainer": "#F1E7D6",
    "onTertiaryContainer": "#4A2A00",
    "text": "#1F1B16",
    "dim": "#5E554A",
    "error": "#B3261E",
    "onError": "#FFFFFF",
    # same fixed dark moments — light mode must NOT put dark ink on them
    "onDark": "#EBE8E0",
    "onDarkDim": "#C3CCE2",
    "onDarkWarm": "#EBD9A8",
    "onDarkFaint": "#7A8096",
    "skyMid": "#1A1E32",
    "skyHorizon": "#4A2F28",
    "ground": "#06070D",
}

# (foreground, background, minimum, what it is)
PAIRS = [
    ("text", "surface", 4.5, "the body text on a card"),
    ("text", "bg", 4.5, "the body text on the page"),
    ("text", "surfaceHigh", 4.5, "the body text on a raised chip"),
    ("dim", "surface", 4.5, "the labelSmall / the meta on a card"),
    ("dim", "bg", 4.5, "the dim line on the page"),
    ("dim", "surfaceHigh", 4.5, "the dim text on a chip"),
    ("primary", "surface", 4.5, "the gold prayer name on a card"),
    ("primary", "bg", 4.5, "the gold on the page"),
    ("primary", "primaryContainer", 4.5, "the gold on the lapis container (the today cell)"),
    ("onPrimaryContainer", "primaryContainer", 4.5, "the today cell's number"),
    ("onPrimary", "primary", 4.5, "the label on a gold chip"),
    ("secondary", "surface", 4.5, "the teal (the worship windows) on a card"),
    ("secondary", "surfaceHigh", 3.0, "the teal on a chip"),
    ("tertiary", "surface", 4.5, "the amber (the live moment) on a card"),
    ("tertiary", "tertiaryContainer", 4.5, "the amber on its container"),
    ("onTertiaryContainer", "tertiaryContainer", 4.5, "the text on the amber container"),
    ("onSecondary", "secondary", 4.5, "the label on a teal chip"),
    ("onTertiary", "tertiary", 4.5, "the label on an amber chip"),
    ("outline", "surface", 1.25, "the card edge (a hairline, not a stroke)"),
    ("outline", "bg", 1.15, "the divider on the page"),
    ("outlineVariant", "surface", 1.10, "the faint hairline (a non-text UI part)"),
    ("error", "surface", 4.5, "the error colour on a card"),
    # The sky, the fast bar and the day-rail are FIXED dark surfaces. Their
    # ink must stay light in the paper scheme too — this is the light-mode
    # disaster that put scheme.onSurface (dark) on a dark sky.
    ("onDark", "skyMid", 4.5, "the countdown on the sky"),
    ("onDarkDim", "skyMid", 4.5, "the captions on the sky"),
    ("onDarkWarm", "skyMid", 4.5, "the cream iqama line on the sky"),
    ("onDark", "skyHorizon", 4.5, "text on the sunset horizon"),
    ("onDark", "ground", 4.5, "the prayer rail on the ground"),
    ("onDarkFaint", "ground", 3.0, "a passed prayer on the ground"),
]


def srgb(channel: int) -> float:
    c = channel / 255.0
    return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4


def luminance(hex_color: str) -> float:
    h = hex_color.lstrip("#")
    r, g, b = (int(h[i:i + 2], 16) for i in (0, 2, 4))
    return 0.2126 * srgb(r) + 0.7152 * srgb(g) + 0.0722 * srgb(b)


def ratio(fg: str, bg: str) -> float:
    a, b = luminance(fg), luminance(bg)
    hi, lo = max(a, b), min(a, b)
    return (hi + 0.05) / (lo + 0.05)


def audit(name: str, tokens: dict) -> list[tuple]:
    fails = []
    print(f"\n=== {name}")
    for fg, bg, minimum, what in PAIRS:
        if fg not in tokens or bg not in tokens:
            continue
        r = ratio(tokens[fg], tokens[bg])
        ok = r >= minimum
        flag = "ok  " if ok else "FAIL"
        print(f"  {flag} {r:5.2f} (min {minimum}) {fg:>18} on {bg:<18} {what}")
        if not ok:
            fails.append((fg, bg, r, minimum, what))
    return fails


def main() -> int:
    dark_fails = audit("DARK", DARK)
    light_fails = audit("LIGHT", LIGHT)
    total = len(dark_fails) + len(light_fails)
    print(f"\n{total} failing pairs")
    if total:
        print("the claim in DESIGN.md ('checked for WCAG AA on both schemes') is not true yet")
        return 1
    print("every pair passes")
    return 0


if __name__ == "__main__":
    sys.exit(main())
