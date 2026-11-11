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
    "bg": "#060B1C",
    "surface": "#0E1526",
    "surfaceHigh": "#1B2542",
    "outline": "#33426B",
    "outlineVariant": "#232C4C",
    "primary": "#E8C46A",
    "onPrimary": "#241C00",
    "primaryContainer": "#16213F",
    "onPrimaryContainer": "#E8C46A",
    "secondary": "#7FD0C8",
    "onSecondary": "#00312C",
    "tertiary": "#FFB25C",
    "onTertiary": "#3A2500",
    "tertiaryContainer": "#16213F",
    "onTertiaryContainer": "#F8E7AE",
    "text": "#EDEBE3",
    "dim": "#98A2C4",
    "error": "#E5484D",
    "onError": "#2B0608",
}
LIGHT = {
    "bg": "#F2ECDC",
    "surface": "#FFFCF4",
    "surfaceHigh": "#E9DEC4",
    "outline": "#C0B392",
    "outlineVariant": "#DFD5BC",
    "primary": "#6F5610",
    "onPrimary": "#FFFFFF",
    "primaryContainer": "#DEE6F4",
    "onPrimaryContainer": "#2A3A5C",
    "secondary": "#1F6A62",
    "onSecondary": "#FFFFFF",
    "tertiary": "#96500A",
    "onTertiary": "#FFFFFF",
    "tertiaryContainer": "#DEE6F4",
    "onTertiaryContainer": "#2B1800",
    "text": "#16181F",
    "dim": "#5A5F72",
    "error": "#B3261E",
    "onError": "#FFFFFF",
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
    ("outline", "surface", 1.8, "the card edge (a visible boundary)"),
    ("outline", "bg", 1.4, "the divider on the page"),
    ("outlineVariant", "surface", 1.3, "the faint hairline (a non-text UI part)"),
    ("error", "surface", 4.5, "the error colour on a card"),
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
