#!/usr/bin/env python3
"""Generate app/src/main/java/app/miqat/core/MethodTable.kt from the donor's
LIVE method table (y5/g.java, encoded in tools/verify_engine.py METHOD_PARAMS)
plus the donor's live seasonal-correction rows (tools/qibla_adjusts.json == the
DB `qibla` table, applied only to the y5.f gate methods).

The corpus-fitted tables (tools/fitted_methods.json) stay a harness-side
artifact: they reproduce the donor's shipped 2014-2019 fallback corpus for the
parity measurement (docs/engine-parity.md) but several are degenerate for live
use (they encode per-calendar-source offsets of that corpus). Live computation
mirrors what the donor app computes today.

Regenerate:  python3 -I tools/gen_kotlin_table.py
"""
import json
import sys

sys.path.insert(0, "tools")
import verify_engine as ve

OUT = "app/src/main/java/app/miqat/core/MethodTable.kt"
RULE_MID = {"uoif", "moscow"}


def num(x):
    if x == int(x):
        return f"{int(x)}.0"
    return repr(round(float(x), 4))


def margins_call(m):
    parts = []
    for attr, key in (("fajr", 5), ("sunrise", 6), ("dhuhr", 7),
                      ("asr", 8), ("maghrib", 9), ("isha", 10)):
        v = round(m[key], 2)
        if v != 0.0:
            parts.append(f"{attr} = {num(v)}")
    return "Margins(" + ", ".join(parts) + ")"


def emit(mid):
    s = ve.METHOD_PARAMS[mid]
    rows = ([r for r in ve.QIBLA_ROWS if r["mn"] == mid]
            if mid in ve.SEASONAL_METHODS else [])
    lines = [f'        "{mid}" to CalcMethod(',
             f'            id = "{mid}",',
             f"            fajrAngle = {num(s[0])},",
             f"            maghribMinutesMode = {'true' if s[1] == 1.0 else 'false'},",
             f"            maghribValue = {num(s[2])},",
             f"            ishaMinutesMode = {'true' if s[3] == 1.0 else 'false'},",
             f"            ishaValue = {num(s[4])},",
             f"            margins = {margins_call(s)},"]
    if mid in RULE_MID:
        lines.append("            highLatRule = HighLatitudeRule.MID_NIGHT,")
    if rows:
        lines.append("            seasonal = listOf(")
        for r in rows:
            cols = []
            if r.get("fa"):
                cols.append(f"fajrAngle = {num(r['fa'])}")
            if r.get("iv"):
                cols.append(f"ishaAngle = {num(r['iv'])}")
            for k, attr in (("fm", "fajr"), ("sm", "sunrise"), ("dm", "dhuhr"),
                            ("am", "asr"), ("mm", "maghrib"), ("im", "isha")):
                if r.get(k):
                    cols.append(f"{attr} = {num(r[k])}")
            lines.append(f"                SeasonalCorrections({r['b']}, {r['e']}, "
                         + ", ".join(cols) + "),")
        lines.append("            ),")
    lines.append("        ),")
    return lines


def main():
    out = []
    out.append("""package app.miqat.core

/**
 * Prayer calculation methods — GENERATED file, do not hand-edit.
 * Regenerate with: python3 -I tools/gen_kotlin_table.py
 *
 * Source: the donor app's LIVE method table (decompiled y5/g.java, 59 methods,
 * slot semantics decoded in docs/engine-parity.md) + its live seasonal
 * correction rows (tools/qibla_adjusts.json == the DB `qibla` table; the donor
 * applies them only to methods in its y5.f gate list — emirates/birmingham get
 * rows, dubai rows exist but are gated out at runtime and are not shipped).
 *
 * Slot semantics (the donor's 11-element array):
 *  fajrAngle            — sun depression degrees for fajr
 *  maghribMinutesMode   — false: maghribValue = depression degrees (angle mode)
 *                         true:  maghribValue = minutes after sunset
 *  ishaMinutesMode      — false: ishaValue = depression degrees
 *                         true:  ishaValue = minutes after maghrib (makkah = 90)
 *  Margins              — ihtiyat minutes per prayer (fajr..isha)
 *  SeasonalCorrections  — per-window corrections (donor `qibla` layer):
 *                         fa → fajr ANGLE delta, iv → isha ANGLE delta,
 *                         fm/sm/dm/am/mm/im → per-prayer minute margins;
 *                         first window with fromMMdd <= MMDD <= toMMdd wins.
 *
 * The donor's shipped 2014-2019 timetable corpus does not match these live
 * parameters exactly (it was authored per calendar source); the measured drift
 * and the corpus-fitted tables live in tools/fitted_methods.json and
 * docs/engine-parity.md.
 */
enum class HighLatitudeRule(val fraction: (Double) -> Double) {
    NONE({ _ -> 0.0 }),
    MID_NIGHT({ _ -> 0.5 }),
    ONE_SEVENTH_NIGHT({ _ -> 0.14286 }),
    ANGLE_NIGHT({ angle -> angle / 60.0 }),
}

/**
 * Per-window seasonal corrections — the decoded donor `qibla` table
 * (v5.f "QiblaAdjust" / tools/qibla_adjusts.json). Angle deltas are degrees.
 */
data class SeasonalCorrections(
    val fromMMdd: Int,
    val toMMdd: Int,
    val fajrAngle: Double = 0.0,
    val ishaAngle: Double = 0.0,
    val fajr: Double = 0.0,
    val sunrise: Double = 0.0,
    val dhuhr: Double = 0.0,
    val asr: Double = 0.0,
    val maghrib: Double = 0.0,
    val isha: Double = 0.0,
)

data class Margins(
    val fajr: Double = 0.0,
    val sunrise: Double = 0.0,
    val dhuhr: Double = 0.0,
    val asr: Double = 0.0,
    val maghrib: Double = 0.0,
    val isha: Double = 0.0,
)

data class CalcMethod(
    val id: String,
    val fajrAngle: Double,
    val maghribMinutesMode: Boolean,
    val maghribValue: Double,
    val ishaMinutesMode: Boolean,
    val ishaValue: Double,
    val margins: Margins = Margins(),
    /** Asr shadow length: 1 = Shafi'i (default), 2 = Hanafi. */
    val asrShadow: Int = 1,
    val highLatRule: HighLatitudeRule = HighLatitudeRule.ANGLE_NIGHT,
    val seasonal: List<SeasonalCorrections> = emptyList(),
) {
    /** y5.b.a(): fold the first matching seasonal window into the parameters. */
    fun withSeasonalCorrections(mmdd: Int): CalcMethod {
        val w = seasonal.firstOrNull { it.fromMMdd <= mmdd && mmdd <= it.toMMdd }
            ?: return this
        return copy(
            fajrAngle = fajrAngle + w.fajrAngle,
            ishaValue = if (ishaMinutesMode) ishaValue else ishaValue + w.ishaAngle,
            margins = Margins(
                fajr = margins.fajr + w.fajr,
                sunrise = margins.sunrise + w.sunrise,
                dhuhr = margins.dhuhr + w.dhuhr,
                asr = margins.asr + w.asr,
                maghrib = margins.maghrib + w.maghrib,
                isha = margins.isha + w.isha,
            ),
        )
    }
}

object MethodTable {
    private val table = mapOf(
""")
    for mid in sorted(ve.METHOD_PARAMS):
        out.extend(emit(mid))
    out.append("""    )

    /** Unknown id falls back to the donor default (`makkah`, per t5.a). */
    operator fun get(id: String): CalcMethod =
        table[id] ?: table.getValue("makkah")

    fun ids(): Set<String> = table.keys
}
""")
    open(OUT, "w").write("\n".join(out))
    n_seasonal = sum(1 for m in ve.SEASONAL_METHODS
                     if any(r["mn"] == m for r in ve.QIBLA_ROWS))
    print(f"wrote {OUT}: {len(ve.METHOD_PARAMS)} methods, "
          f"{n_seasonal} with live seasonal rows")


if __name__ == "__main__":
    main()
