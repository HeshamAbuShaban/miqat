#!/usr/bin/env python3
"""Build the Miqat launcher icon from the LOCKED design (design-brief.md §0):

    variant 2 — gold «الميقات» wordmark (Reem Kufi, wght 600) + thin day-arc
    beneath with one gold point, on a deep-navy squircle.

Outputs (app/src/main/res):
  drawable/ic_launcher_foreground.xml   gold wordmark+arc+point, viewport 108
  drawable/ic_launcher_monochrome.xml   white version for themed icons
  values/colors.xml                     ic_launcher_background = #0D1424
  mipmap-anydpi-v26/ic_launcher.xml     adaptive (background/foreground/monochrome)
  mipmap-anydpi-v26/ic_launcher_round.xml
  mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png      legacy (API 24/25)
  mipmap-*/ic_launcher_round.png
plus docs/design/icon-preview-512.png for human review.

Self-verifying: asserts every inked point of the foreground fits inside the
66dp adaptive safe zone (r=33 from center, with 2dp margin), so circular
masks can never clip the wordmark.

Dependencies: fonttools (pure python, vendored on first run into tools/_vendor,
gitignored) + system libharfbuzz via ctypes. Offline after first run.
"""

import math
import os
import struct
import subprocess
import sys
import zipfile
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
RES = REPO / "app" / "src" / "main" / "res"
FONTS = REPO / "tools" / "fonts"
VENDOR = REPO / "tools" / "_vendor"
FONT_VAR = FONTS / "ReemKufi-SemiBold.ttf"  # variable, wght 400..700
FONT_URL = ("https://files.pythonhosted.org/packages/py3/f/fonttools/"
            "fonttools-4.56.0-py3-none-any.whl")

WORD = "الميقات"
W600 = 600
NAVY = "#0D1424"
GOLD_STOPS = [(0.0, "#F8E7AE"), (0.45, "#EAC964"), (0.78, "#C9962E"), (1.0, "#A97B22")]
ARC_GOLD = "#EAC964"
POINT_CREAM = "#F8E7AE"


def ensure_fonttools():
    try:
        import fontTools  # noqa: F401
        return
    except ImportError:
        pass
    VENDOR.mkdir(parents=True, exist_ok=True)
    wheel = VENDOR / "fonttools.whl"
    if not wheel.exists():
        print(f"vendoring fonttools wheel -> {wheel}")
        subprocess.run(["curl", "-sL", "-o", str(wheel), FONT_URL], check=True)
    target = VENDOR / "fonttools"
    if not target.exists():
        with zipfile.ZipFile(wheel) as z:
            z.extractall(target)
    sys.path.insert(0, str(target))
    import fontTools  # noqa: F401
    print("fonttools vendored OK")


def hb_shape_word(text, wght):
    """Shape Arabic text with system libharfbuzz via ctypes.

    Returns list of (glyph_id, x_offset, y_offset) and total advance, in font
    units, with the wght variation applied.
    """
    import ctypes
    import ctypes.util
    soname = ctypes.util.find_library("harfbuzz")
    if not soname:
        sys.exit("libharfbuzz not found (pacman -S harfbuzz)")
    hb = ctypes.CDLL(soname)

    class HbVariation(ctypes.Structure):
        _fields_ = [("tag", ctypes.c_uint), ("value", ctypes.c_float)]

    class HbGlyphInfo(ctypes.Structure):
        _fields_ = [("codepoint", ctypes.c_uint), ("mask", ctypes.c_uint),
                    ("cluster", ctypes.c_uint), ("var1", ctypes.c_uint), ("var2", ctypes.c_uint)]

    class HbGlyphPosition(ctypes.Structure):
        # this libharfbuzz ABI: x_adv, y_adv, x_off, y_off, var (5 x int32)
        _fields_ = [("x_advance", ctypes.c_int), ("y_advance", ctypes.c_int),
                    ("x_offset", ctypes.c_int), ("y_offset", ctypes.c_int),
                    ("var", ctypes.c_int)]

    # FULL signatures — without these, ctypes truncates 64-bit pointers.
    hb.hb_blob_create_from_file.argtypes = [ctypes.c_char_p]
    hb.hb_blob_create_from_file.restype = ctypes.c_void_p
    hb.hb_face_create.argtypes = [ctypes.c_void_p, ctypes.c_uint]
    hb.hb_face_create.restype = ctypes.c_void_p
    hb.hb_face_get_upem.argtypes = [ctypes.c_void_p]
    hb.hb_face_get_upem.restype = ctypes.c_uint
    hb.hb_font_create.argtypes = [ctypes.c_void_p]
    hb.hb_font_create.restype = ctypes.c_void_p
    hb.hb_font_set_variations.argtypes = [ctypes.c_void_p, ctypes.POINTER(HbVariation), ctypes.c_uint]
    hb.hb_buffer_create.argtypes = []
    hb.hb_buffer_create.restype = ctypes.c_void_p
    hb.hb_buffer_add_utf8.argtypes = [ctypes.c_void_p, ctypes.c_char_p, ctypes.c_int, ctypes.c_int, ctypes.c_int]
    hb.hb_buffer_set_direction.argtypes = [ctypes.c_void_p, ctypes.c_int]
    hb.hb_buffer_set_script.argtypes = [ctypes.c_void_p, ctypes.c_uint]
    hb.hb_buffer_set_language.argtypes = [ctypes.c_void_p, ctypes.c_void_p]
    hb.hb_shape.argtypes = [ctypes.c_void_p, ctypes.c_void_p, ctypes.c_void_p, ctypes.c_uint]
    hb.hb_buffer_get_length.argtypes = [ctypes.c_void_p]
    hb.hb_buffer_get_length.restype = ctypes.c_uint
    hb.hb_buffer_get_glyph_infos.argtypes = [ctypes.c_void_p, ctypes.POINTER(ctypes.c_uint)]
    hb.hb_buffer_get_glyph_infos.restype = ctypes.POINTER(HbGlyphInfo)
    hb.hb_buffer_get_glyph_positions.argtypes = [ctypes.c_void_p, ctypes.POINTER(ctypes.c_uint)]
    hb.hb_buffer_get_glyph_positions.restype = ctypes.POINTER(HbGlyphPosition)

    def tag(s):
        return struct.unpack(">I", s.encode())[0]

    blob = hb.hb_blob_create_from_file(str(FONT_VAR).encode())
    assert blob, f"harfbuzz could not open {FONT_VAR}"
    face = hb.hb_face_create(blob, 0)
    font = hb.hb_font_create(face)
    upem = hb.hb_face_get_upem(face)

    v = HbVariation(tag("wght"), float(wght))
    hb.hb_font_set_variations(font, ctypes.byref(v), 1)

    buf = hb.hb_buffer_create()
    hb.hb_buffer_add_utf8(buf, text.encode("utf-8"), -1, 0, -1)
    hb.hb_buffer_set_direction(buf, 5)  # HB_DIRECTION_RTL
    hb.hb_buffer_set_script(buf, tag("Arab"))
    hb.hb_buffer_set_language(buf, None)
    hb.hb_shape(font, buf, None, 0)

    length = hb.hb_buffer_get_length(buf)
    assert length > 0, "shaping produced no glyphs"
    infos = hb.hb_buffer_get_glyph_infos(buf, ctypes.byref(ctypes.c_uint(length)))
    poss = hb.hb_buffer_get_glyph_positions(buf, ctypes.byref(ctypes.c_uint(length)))

    glyphs = []
    total = 0
    for i in range(length):
        gi, gp = infos[i], poss[i]
        # raw y_offset: positive = up in font space (y-up)
        glyphs.append((gi.codepoint, total + gp.x_offset, gp.y_offset))
        total += gp.x_advance
    return glyphs, total, upem


def glyph_paths_svg(glyphs, upem, scale, word_width_units):
    """Extract shaped glyph outlines as one SVG path (y already flipped).

    Returns (svg_path, ink_bbox) with ink in *icon units after scale*,
    origin at the shaped word's LEFT edge, baseline at y=0.
    """
    from fontTools.ttLib import TTFont
    from fontTools.pens.svgPathPen import SVGPathPen
    from fontTools.pens.boundsPen import BoundsPen
    from fontTools.pens.transformPen import TransformPen
    from fontTools.misc.transform import Transform

    font = TTFont(str(FONT_VAR))
    from fontTools.varLib.instancer import instantiateVariableFont
    instantiateVariableFont(font, {"wght": W600}, inplace=True)
    glyphset = font.getGlyphSet()
    order = font.getGlyphOrder()

    parts = []
    minx = miny = float("inf")
    maxx = maxy = float("-inf")
    for gid, dx, dy in glyphs:
        gname = order[gid]
        glyph = glyphset[gname]
        t = Transform(scale, 0, 0, -scale, dx * scale, -dy * scale)  # flip y (font y-up -> svg y-down)
        pen = SVGPathPen(glyphset, ntos=lambda v: f"{v:.2f}")
        tpen = TransformPen(pen, t)
        glyph.draw(tpen)
        d = pen.getCommands()
        if d:
            parts.append(d)
        bpen = BoundsPen(glyphset)
        glyph.draw(bpen)
        if bpen.bounds:
            x0, y0, x1, y1 = bpen.bounds
            # transform: x' = x*s + dx*s ; y' = -y*s - dy*s
            for (gx, gy) in [(x0, y0), (x1, y1)]:
                px = (gx * scale) + dx * scale
                py = (-gy * scale) - dy * scale
                minx, miny = min(minx, px), min(miny, py)
                maxx, maxy = max(maxx, px), max(maxy, py)
    ink = (minx, miny, maxx, maxy)
    return " ".join(parts), ink


def fmt(v):
    s = f"{v:.2f}".rstrip("0").rstrip(".")
    return s if s else "0"


def build_foreground_svg_paths():
    """Shape the word, return (word_path_108, arc_path_108, point_cx_cy_r)."""
    ensure_fonttools()
    glyphs, total_adv, upem = hb_shape_word(WORD, W600)
    # measure ink at unit scale, then fit mask-safe (see verify_safe_zone):
    # measured ink at scale 1: width W_u, height H_u -> aspect ~2.79
    _, ink_probe = glyph_paths_svg(glyphs, upem, 1.0, total_adv)
    ink_w_u = ink_probe[2] - ink_probe[0]
    aspect = ink_w_u / (ink_probe[3] - ink_probe[1])
    # fit inside r=31 circle with the arc below: word w=48 -> h=48/aspect
    TARGET_WORD_W = 48.0
    scale = TARGET_WORD_W / ink_w_u
    word_path, ink = glyph_paths_svg(glyphs, upem, scale, total_adv)
    w = ink[2] - ink[0]
    h = ink[3] - ink[1]
    print(f"word ink: {w:.1f} x {h:.1f} dp (aspect {aspect:.2f}) at scale {scale:.5f} ({len(glyphs)} glyphs)")

    # Layout inside 108x108 (center 54,54), everything within r<=31:
    # word center-y at 44, arc beneath (mock rhythm: arc at 73% height),
    # point hanging on the arc's upper reach.
    word_cx, word_cy = 54.0, 44.0
    ox = word_cx - (ink[0] + w / 2)
    oy = word_cy - (ink[1] + h / 2)
    word_path_t = translate_path(word_path, ox, oy)

    arc_w = 36.0
    arc_y = 79.0           # endpoints baseline (0.73 of box, like the mock)
    arc_drop = 8.0         # apex rise above endpoints
    x0, x1 = 54 - arc_w / 2, 54 + arc_w / 2
    arc_path = (f"M{fmt(x0)},{fmt(arc_y)} Q54,{fmt(arc_y - 2 * arc_drop)} {fmt(x1)},{fmt(arc_y)}")
    point = (54.0, arc_y - arc_drop * 0.62, 2.8)  # sits on the arc's upper reach
    return word_path_t, arc_path, point, (ox, oy, ink, scale)


def translate_path(d, dx, dy):
    """Translate an absolute-command SVG path by (dx, dy)."""
    import re
    out = []
    i = 0
    tokens = re.findall(r"([MLQCZHV])|(-?\d*\.?\d+)", d)
    cur_cmd = None
    nums = []
    seq = []
    for cmd, num in tokens:
        if cmd:
            if cur_cmd:
                seq.append((cur_cmd, nums))
            cur_cmd, nums = cmd, []
        elif num != "":
            nums.append(float(num))
    if cur_cmd:
        seq.append((cur_cmd, nums))
    for cmd, nums in seq:
        if cmd in ("M", "L"):
            out.append(cmd + fmt(nums[0] + dx) + "," + fmt(nums[1] + dy))
            rest = nums[2:]
            while rest:
                out.append(cmd + fmt(rest[0] + dx) + "," + fmt(rest[1] + dy))
                rest = rest[2:]
        elif cmd == "Q":
            for k in range(0, len(nums), 4):
                out.append(cmd + fmt(nums[k] + dx) + "," + fmt(nums[k + 1] + dy)
                           + " " + fmt(nums[k + 2] + dx) + "," + fmt(nums[k + 3] + dy))
        elif cmd in ("H",):
            out.append(cmd + fmt(nums[0] + dx))
        elif cmd in ("V",):
            out.append(cmd + fmt(nums[0] + dy))
        elif cmd == "Z":
            out.append("Z")
    return "".join(out)


def verify_safe_zone(word_path, arc_path, point):
    """Parse all coordinates and assert they fit r<=31 from (54,54)."""
    import re
    coords = [float(x) for x in re.findall(r"-?\d*\.?\d+", word_path + arc_path)]
    worst = 0.0
    for i in range(0, len(coords) - 1, 2):
        dx, dy = coords[i] - 54.0, coords[i + 1] - 54.0
        worst = max(worst, math.hypot(dx, dy))
    worst = max(worst, math.hypot(0, 0) if False else math.hypot(point[0] - 54 + point[2], point[1] - 54 + point[2]))
    print(f"safe-zone worst radius: {worst:.1f} (limit 31, circle mask 33)")
    assert worst <= 31.0, f"icon ink exceeds safe zone: {worst:.1f} > 31"
    return worst


def vector_xml(path_data, w=108, h=108):
    return (
        f'<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    xmlns:aapt="http://schemas.android.com/aapt"\n'
        f'    android:width="{w}dp" android:height="{h}dp"\n'
        f'    android:viewportWidth="{w}" android:viewportHeight="{h}">\n'
        f'{path_data}\n'
        f'</vector>\n'
    )


def gold_path_xml(path_data):
    """A word path filled with the 4-stop vertical gold gradient (design §0)."""
    gstops = "".join(
        f'\n              <item android:offset="{o}" android:color="{c}"/>' for o, c in GOLD_STOPS
    )
    grad = (
        f'''        <aapt:attr name="android:fillColor">
          <gradient
              android:type="linear"
              android:startX="0" android:startY="30"
              android:endX="0" android:endY="56">{gstops}
          </gradient>
        </aapt:attr>'''
    )
    return (
        f'    <path\n        android:pathData="{path_data}">\n{grad}\n    </path>'
    )


def main():
    ensure_fonttools()
    word_path, arc_path, point, meta = build_foreground_svg_paths()
    verify_safe_zone(word_path, arc_path, point)

    pcx, pcy, pr = point
    word_xml = gold_path_xml(word_path)
    arc_xml = (
        f'    <path\n        android:pathData="{arc_path}"\n'
        f'        android:strokeColor="{ARC_GOLD}" android:strokeWidth="1.6"\n'
        f'        android:strokeLineCap="round"/>\n'
        f'    <path\n        android:pathData="M{fmt(pcx - pr)},{fmt(pcy)}a{fmt(pr)},{fmt(pr)} 0 1,1 {fmt(2 * pr)},0a{fmt(pr)},{fmt(pr)} 0 1,1 {fmt(-2 * pr)},0"\n'
        f'        android:fillColor="{POINT_CREAM}"/>'
    )

    fg = vector_xml(word_xml + "\n" + arc_xml)
    mono_word = f'    <path android:pathData="{word_path}" android:fillColor="#FFFFFF"/>'
    mono = vector_xml(
        mono_word + "\n"
        + f'    <path android:pathData="{arc_path}" android:strokeColor="#FFFFFF" android:strokeWidth="1.6" android:strokeLineCap="round"/>\n'
        + f'    <path android:pathData="M{fmt(pcx - pr)},{fmt(pcy)}a{fmt(pr)},{fmt(pr)} 0 1,1 {fmt(2 * pr)},0a{fmt(pr)},{fmt(pr)} 0 1,1 {fmt(-2 * pr)},0" android:fillColor="#FFFFFF"/>'
    )

    (RES / "drawable").mkdir(parents=True, exist_ok=True)
    (RES / "drawable" / "ic_launcher_foreground.xml").write_text(fg)
    (RES / "drawable" / "ic_launcher_monochrome.xml").write_text(mono)

    colors_path = RES / "values" / "colors.xml"
    entry = '    <color name="ic_launcher_background">#0D1424</color>\n'
    if colors_path.exists():
        txt = colors_path.read_text()
        if "ic_launcher_background" not in txt:
            txt = txt.replace("</resources>", entry + "</resources>")
            colors_path.write_text(txt)
    else:
        colors_path.write_text('<resources>\n' + entry + '</resources>\n')

    anydpi = RES / "mipmap-anydpi-v26"
    anydpi.mkdir(parents=True, exist_ok=True)
    (anydpi / "ic_launcher.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        '    <background android:drawable="@color/ic_launcher_background"/>\n'
        '    <foreground android:drawable="@drawable/ic_launcher_foreground"/>\n'
        '    <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>\n'
        '</adaptive-icon>\n')
    (anydpi / "ic_launcher_round.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        '    <background android:drawable="@color/ic_launcher_background"/>\n'
        '    <foreground android:drawable="@drawable/ic_launcher_foreground"/>\n'
        '    <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>\n'
        '</adaptive-icon>\n')

    build_legacy_pngs(word_path, arc_path, point)
    print("icon assets written.")


def build_legacy_pngs(word_path, arc_path, point):
    """API 24/25 fallback PNGs + review preview, via PIL (Raqm shapes Arabic)."""
    from PIL import Image, ImageDraw, ImageFont
    densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for d, px in densities.items():
        for shape in ("ic_launcher", "ic_launcher_round"):
            img = Image.new("RGBA", (px, px), (0, 0, 0, 0))
            draw = ImageDraw.Draw(img)
            if shape == "ic_launcher":
                _squircle(draw, px)
            else:
                draw.ellipse([0, 0, px - 1, px - 1], fill=NAVY)
            _paint_mark(draw, img, word_path, arc_path, point, px)
            out = RES / f"mipmap-{d}"
            out.mkdir(parents=True, exist_ok=True)
            img.save(out / f"{shape}.png")
    # 512 review preview (squircle)
    px = 512
    img = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    _squircle(draw, px, radius_ratio=0.229)
    _paint_mark(draw, img, word_path, arc_path, point, px)
    out = REPO / "docs" / "design" / "icon-preview-512.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print(f"preview -> {out}")


def _squircle(draw, px, radius_ratio=0.229):
    r = int(px * radius_ratio)
    draw.rounded_rectangle([0, 0, px - 1, px - 1], radius=r, fill=NAVY)


def _paint_mark(draw, img, word_path, arc_path, point, px):
    """Rasterize the mark: arc + point from geometry, word via PIL+Raqm."""
    from PIL import ImageFont
    s = px / 108.0
    # arc (quadratic)
    arc_pts = []
    for i in range(33):
        t = i / 32
        # reconstruct from arc_path M/Q endpoints
        import re
        nums = [float(x) for x in re.findall(r"-?\d*\.?\d+", arc_path)]
        x0, y0, cx, cy, x1, y1 = nums[:6]
        mt = 1 - t
        xx = mt * mt * x0 + 2 * mt * t * cx + t * t * x1
        yy = mt * mt * y0 + 2 * mt * t * cy + t * t * y1
        arc_pts.append((xx * s, yy * s))
    draw.line(arc_pts, fill=ARC_GOLD, width=max(2, int(1.6 * s)), joint="curve")
    pcx, pcy, pr = point
    r = pr * s
    draw.ellipse([pcx * s - r, pcy * s - r, pcx * s + r, pcy * s + r], fill=POINT_CREAM)
    # word: rasterize the same vector path via aggdraw-free method — use PIL text
    # with the variable font at wght 600 (Raqm shapes RTL correctly).
    # The vector path bounds are known: recompute from path coords.
    import re
    coords = [float(x) for x in re.findall(r"-?\d*\.?\d+", word_path)]
    xs, ys = coords[0::2], coords[1::2]
    wpx = (max(xs) - min(xs)) * s
    hpx = (max(ys) - min(ys)) * s
    fs = int(hpx * 1.35)
    fpath = str(FONT_VAR)
    f = ImageFont.truetype(fpath, fs)
    try:
        f.set_variation_by_axes([W600])
    except Exception:
        pass
    # draw text so its ink box matches the vector's word box
    target_cx = (min(xs) + max(xs)) / 2 * s
    target_cy = (min(ys) + max(ys)) / 2 * s
    bbox = draw.textbbox((0, 0), WORD, font=f, direction="rtl", language="ar")
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    # scale font so ink width matches
    if tw > 0:
        fs = int(fs * wpx / tw)
        f = ImageFont.truetype(fpath, fs)
        try:
            f.set_variation_by_axes([W600])
        except Exception:
            pass
        bbox = draw.textbbox((0, 0), WORD, font=f, direction="rtl", language="ar")
        tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    draw.text((target_cx - tw / 2 - bbox[0], target_cy - th / 2 - bbox[1]), WORD,
              font=f, fill="#EAC964", direction="rtl", language="ar")


if __name__ == "__main__":
    main()
