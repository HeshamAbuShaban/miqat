#!/usr/bin/env python3
"""
Anti-regression gates for the things this project has already broken once.

Each check is a fact about a PAST defect, so a failure means a known bug came
back — not a style opinion:

  1. dead settings      — a preference that is persisted but never consumed
  2. seconds            — a live clock that silently drops its seconds
  3. palette drift      — a new hardcoded colour outside the approved set
  4. uncited text       — a source line with no verifiable citation
  5. monolith regression— a source file that grows past its recorded size

Run with:  python3 -I tools/verify_regressions.py
The baselines are snapshots of the CURRENT tree; changing them is an explicit
act that belongs in the same commit as the change they permit.
"""
import glob
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.chdir(ROOT)

fails = []


def kt_files():
    return sorted(glob.glob("app/src/main/java/**/*.kt", recursive=True))


def read(path):
    return io.open(path, encoding="utf-8").read()


def note(ok, label, detail=""):
    print(("  ok   " if ok else "  FAIL ") + label + ((" — " + detail) if detail else ""))
    if not ok:
        fails.append(label)


# ---------------------------------------------------------------- 1. dead keys
print("1. settings keys and fields are consumed")
sources = {p: read(p) for p in kt_files()}
all_code = "\n".join(v for k, v in sources.items())

for path in (
    "app/src/main/java/app/miqat/core/UserSettings.kt",
    "app/src/main/java/app/miqat/core/NotificationPrefs.kt",
):
    text = sources[path]
    keys = re.findall(r"const val (K_[A-Z0-9_]+)", text)
    dead = [k for k in keys if len(re.findall(r"\b" + re.escape(k) + r"\b", text)) < 2]
    note(not dead, os.path.basename(path) + " preference keys", ", ".join(dead))

    # A persisted field must be read by a feature outside its own file.
    cls = "DisplayPrefs" if "UserSettings" in path else "NotificationPrefs"
    body = text[text.index("data class " + cls):]
    body = body[: body.index("\n)")]
    fields = re.findall(r"^\s+val ([a-zA-Z0-9_]+):", body, re.M)
    consumers = "\n".join(
        v for k, v in sources.items()
        if not k.endswith(("UserSettings.kt", "NotificationPrefs.kt"))
    )
    unused = [f for f in fields if not re.search(r"\." + re.escape(f) + r"\b", consumers)]
    note(not unused, cls + " fields", ", ".join(unused))

    # Inline string keys (NotificationPrefs) must be written as well as read.
    gets = set(re.findall(r'p\.get(?:Boolean|Int|String|StringSet)\(\s*"([^"$]+)"', text))
    puts = set(re.findall(r'e\.put(?:Boolean|Int|String|StringSet)\(\s*"([^"$]+)"', text))
    note(not gets - puts, cls + " inline keys", ", ".join(sorted(gets - puts)))

# ---------------------------------------------------------------- 2. seconds
print("2. live clocks keep their seconds")
# Fmt.tick / NumFmt.tick deliberately drop seconds; they must never be called
# from the UI. (The definitions themselves live in core and are allowed.)
call_sites = []
for path, text in sources.items():
    if path.endswith(("core/NumFmt.kt", "ui/Components.kt")):
        continue
    for m in re.finditer(r"\b(?:fmt|NumFmt)\.tick\s*\(", text):
        line = text.count("\n", 0, m.start()) + 1
        call_sites.append(f"{os.path.basename(path)}:{line}")
note(not call_sites, "no seconds-dropping tick in the UI", ", ".join(call_sites))

for path in (
    "app/src/main/java/app/miqat/ui/HeroCard.kt",
    "app/src/main/java/app/miqat/ui/TimesScreen.kt",
    "app/src/main/java/app/miqat/ui/FastingScreen.kt",
):
    note("hms(" in sources[path], os.path.basename(path) + " prints H:MM:SS")

# The ongoing notification is a ticking place too — the count must carry
# seconds, not fall back to the minute-only formatter.
countdown = sources.get("app/src/main/java/app/miqat/core/CountdownService.kt", "")
note("%02d:%02d" in countdown, "ongoing notification prints H:MM:SS")

# ---------------------------------------------------------------- 3. palette
print("3. no new hardcoded colour")
baseline_path = "tools/palette-baseline.txt"
if not os.path.exists(baseline_path):
    io.open(baseline_path, "w", encoding="utf-8").write("\n".join(sorted(set(
        re.findall(r"Color\(0x([0-9A-Fa-f]{8})\)", all_code)
    ))) + "\n")
    print("  (baseline created — review it into git before the next run)")
approved = set(io.open(baseline_path, encoding="utf-8").read().split())
found = set(re.findall(r"Color\(0x([0-9A-Fa-f]{6,8})\)", all_code))
new = sorted(c for c in found if c not in approved)
note(not new, "colours stay within the baseline", ", ".join("0x" + c for c in new))

# ---------------------------------------------------------------- 4. citations
print("4. religious text carries its source")
table = read("docs/sourcing-table.md")
note(len(re.findall(r"^\|\s*\d+\s*\|", table, re.M)) >= 8, "sourcing table has >= 8 verified rows")
src_lines = []
for path, text in sources.items():
    if not path.startswith("app/src/main/java/app/miqat/ui/"):
        continue
    src_lines += [(path, l.strip()) for l in text.split("\n") if "source =" in l]
markers = ("البخاري", "مسلم", "الترمذي", "النسائي", "أبو داود", "ابن ماجه", "سورة")
uncited = [
    f"{os.path.basename(p)}: {l[:60]}"
    for p, l in src_lines if not any(m in l for m in markers)
]
note(not uncited, "every UI source line names a verified collection", "; ".join(uncited))

# ---------------------------------------------------------------- 5. file size
print("5. no monolith regression")
size_path = "tools/size-baseline.txt"
if not os.path.exists(size_path):
    io.open(size_path, "w", encoding="utf-8").write("\n".join(
        f"{len(read(p).splitlines())} {p}" for p in kt_files()
    ) + "\n")
    print("  (baseline created — review it into git before the next run)")
sizes = {}
for line in io.open(size_path, encoding="utf-8"):
    n, path = line.rstrip("\n").split(" ", 1)
    sizes[path] = int(n)
grown, unlisted = [], []
for path in kt_files():
    if path not in sizes:
        unlisted.append(os.path.basename(path))
        continue
    now = len(read(path).splitlines())
    if now > sizes[path] * 1.15 + 10:
        grown.append(f"{os.path.basename(path)} {sizes[path]}->{now}")
note(not grown, "no source file grew >15% past baseline", ", ".join(grown))
note(not unlisted, "new files are recorded in the baseline", ", ".join(unlisted))

# ---------------------------------------------------------------- 6. hijri offset
print("6. every Hijri date honours the user's sighting offset")
# A 3-argument Hijri.at() silently uses offset 0 — which made the fasting
# header and the fasting nudge disagree with Home when the user set an offset.
short = []
for path, text in sources.items():
    for m in re.finditer(r"Hijri\.at\s*\(", text):
        depth, i, commas = 1, m.end(), 0
        while i < len(text) and depth:
            c = text[i]
            if c == "(":
                depth += 1
            elif c == ")":
                depth -= 1
            elif c == "," and depth == 1:
                commas += 1
            i += 1
        if commas < 3:
            line = text.count("\n", 0, m.start()) + 1
            short.append(f"{os.path.basename(path)}:{line}")
note(not short, "all Hijri.at calls pass the offset", ", ".join(short))

# ---------------------------------------------------------------- verdict
print("")
if fails:
    print(f"REGRESSIONS: {len(fails)} gate(s) failed")
    sys.exit(1)
print("REGRESSIONS: all gates pass")
