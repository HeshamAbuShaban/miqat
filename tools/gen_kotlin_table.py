#!/usr/bin/env python3
"""Generate app/src/main/java/app/miqat/core/MethodTable.kt from the fitted
corpus tables (tools/fitted_methods.json) plus the donor's live method table
(y5/g.java, encoded in tools/verify_engine.py METHOD_PARAMS).

- methods present in fitted_methods.json → parity-certified (donor corpus)
- the rest of y5.g → donor-live parameters, marked not-corpus-verified

Regenerate after every fit:  python3 -I tools/gen_kotlin_table.py
"""
import json
import sys

sys.path.insert(0, "tools")
import verify_engine as ve

OUT = "app/src/main/java/app/miqat/core/MethodTable.kt"
RULE_MID = {"uoif", "moscow"}


def num(x):
    """Compact double literal for generated Kotlin."""
    if x == int(x):
        return f"{int(x)}.0"
    return repr(round(float(x), 4))


def margins_call(m, w):
    parts = []
    for k, attr in (("fajr", "fajr"), ("sunrise", "sunrise"), ("dhuhr", "dhuhr"),
                    ("asr", "asr"), ("maghrib", "maghrib"), ("isha", "isha")):
        v = round(m + w.get({"fajr": "fm", "sunrise": "sm", "dhuhr": "dm",
                             "asr": "am", "maghrib": "mm", "isha": "im"}[k], 0.0), 2)
        if v != 0.0:
            parts.append(f"{attr} = {num(v)}")
    return "Margins(" + ", ".join(parts) + ")"


def emit_fitted(mid, entry):
    s = entry["params"]
    windows = entry.get("windows", [])
    lines = [f'        "{mid}" to CalcMethod(']
    lines.append(f'            id = "{mid}",')
    lines.append(f"            fajrAngle = {num(s[0])},")
    lines.append(f"            maghribMinutesMode = {'true' if s[1] == 1.0 else 'false'},")
    lines.append(f"            maghribValue = {num(s[2])},")
    lines.append(f"            ishaMinutesMode = {'true' if s[3] == 1.0 else 'false'},")
    lines.append(f"            ishaValue = {num(s[4])},")
    base_m = {"fajr": s[5], "sunrise": s[6], "dhuhr": s[7], "asr": s[8],
              "maghrib": s[9], "isha": s[10]}
    lines.append(f"            margins = {margins_call(base_m, {})},")
    lines.append(f"            asrShadow = {entry.get('asr_shadow', 1)},")
    if mid in RULE_MID:
        lines.append("            highLatRule = HighLatitudeRule.MID_NIGHT,")
    if windows:
        lines.append("            seasonal = listOf(")
        for w in windows:
            cols = ", ".join(f"{k} = {num(w[k])}" for k in
                             ("fm", "sm", "dm", "am", "mm", "im") if w.get(k))
            lines.append(f"                SeasonalCorrections({w['b']}, {w['e']}, {cols}),")
        lines.append("            ),")
    lines.append("        ),")
    return lines


def emit_donor(mid):
    s = ve.METHOD_PARAMS[mid]
    lines = [f'        "{mid}" to CalcMethod(',
             f'            id = "{mid}",',
             f"            fajrAngle = {num(s[0])},",
             f"            maghribMinutesMode = {'true' if s[1] == 1.0 else 'false'},",
             f"            maghribValue = {num(s[2])},",
             f"            ishaMinutesMode = {'true' if s[3] == 1.0 else 'false'},",
             f"            ishaValue = {num(s[4])},"]
    m = {"fajr": s[5], "sunrise": s[6], "dhuhr": s[7], "asr": s[8],
         "maghrib": s[9], "isha": s[10]}
    lines.append(f"            margins = {margins_call(m, {})},")
    if mid in RULE_MID:
        lines.append("            highLatRule = HighLatitudeRule.MID_NIGHT,")
    lines.append("        ),  // donor-live y5.g, not corpus-verified")
    return lines


def main():
    fitted = json.load(open("tools/fitted_methods.json"))
    donor_ids = [k for k in ve.METHOD_PARAMS if k not in fitted]
    out = []
    out.append("""package app.miqat.core

/**
 * Prayer calculation methods — GENERATED file, do not hand-edit.
 * Regenerate with: python3 -I tools/gen_kotlin_table.py
 *
 * Two provenance tiers:
 *  - corpus-fitted: parameters solved from the donor's shipped timetable corpus
 *    (`staticprayertimes`, 57 cities, 2014-2019) — parity-certified at
 *    docs/engine-parity.md (≥99% @ ±1 min target, S1 ladder)
 *  - donor-live: the donor app's own method table (decompiled y5/g.java) for
 *    methods the corpus does not cover — same pipeline, not corpus-verified
 *
 * Slot semantics (donor y5.g 11-element array, decoded in engine-parity.md):
 *  fajrAngle            — sun depression degrees for fajr
 *  maghribMinutesMode   — false: maghribValue = depression degrees (angle mode)
 *                         true:  maghribValue = minutes after sunset
 *  ishaMinutesMode      — false: ishaValue = depression degrees
 *                         true:  ishaValue = minutes after maghrib (makkah = 90)
 *  Margins              — ihtiyat minutes per prayer (fajr..isha)
 *  SeasonalCorrections  — per-window minute corrections (donor `qibla` layer,
 *                         columns fm/sm/dm/am/mm/im → fajr/sunrise/dhuhr/asr/
 *                         maghrib/isha), first window with fromMMdd <= MMDD <=
 *                         toMMdd wins
 */
enum class HighLatitudeRule(val fraction: (Double) -> Double) {
    NONE({ _ -> 0.0 }),
    MID_NIGHT({ _ -> 0.5 }),
    ONE_SEVENTH_NIGHT({ _ -> 0.14286 }),
    ANGLE_NIGHT({ angle -> angle / 60.0 }),
}

/**
 * Per-window seasonal minute corrections — the decoded donor `qibla` table
 * (v5.f "QiblaAdjust" / tools/qibla_adjusts.json).
 */
data class SeasonalCorrections(
    val fromMMdd: Int,
    val toMMdd: Int,
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
    /** y5.b.a(): add the first matching seasonal window to the margins. */
    fun withSeasonalCorrections(mmdd: Int): CalcMethod {
        val w = seasonal.firstOrNull { it.fromMMdd <= mmdd && mmdd <= it.toMMdd }
            ?: return this
        return copy(margins = Margins(
            fajr = margins.fajr + w.fajr,
            sunrise = margins.sunrise + w.sunrise,
            dhuhr = margins.dhuhr + w.dhuhr,
            asr = margins.asr + w.asr,
            maghrib = margins.maghrib + w.maghrib,
            isha = margins.isha + w.isha,
        ))
    }
}

object MethodTable {
""")
    out.append("    // --- parity-certified: fitted from the donor corpus ---\n")
    out.append("    private val fitted = mapOf(\n")
    for mid in sorted(fitted):
        out.extend(emit_fitted(mid, fitted[mid]))
    out.append("    )\n\n")
    out.append("    // --- donor-live parameters (y5/g.java), no corpus coverage ---\n")
    out.append("    private val donorLive = mapOf(\n")
    for mid in sorted(donor_ids):
        out.extend(emit_donor(mid))
    out.append("    )\n\n")
    out.append("""    private val table: Map<String, CalcMethod> = fitted + donorLive

    operator fun get(id: String): CalcMethod =
        table[id] ?: table.getValue("makkah")

    fun ids(): Set<String> = table.keys
}
""")
    open(OUT, "w").write("\n".join(out))
    print(f"wrote {OUT}: {len(fitted)} fitted + {len(donor_ids)} donor-live methods")


if __name__ == "__main__":
    main()
