package app.miqat.core

/**
 * R5 worship windows («زوائد غابت», research-agenda.md R5) — computed offline
 * from the same solar math as the prayers. The donor ships NONE of these
 * (verified by grep over sources + values-ar, journal 2026-11-10).
 *
 * GUARDRAIL (research-agenda §Guardrail): every bound below is a CONVENTION
 * (fiqh estimate from solar geometry), not a Sharia-exact moment — the UI must
 * keep the «تقديري» disclosure, and rulings belong to the local authority.
 * All bounds propagate NaN (polar day/night) — display as "--:--".
 */
data class DuhaWindow(val startHour: Double, val endHour: Double)
data class NightInfo(
    val maghribHour: Double,
    val fajrNextHour: Double,
    val midnightHour: Double,
    val lastThirdStartHour: Double,
    val nightLengthHours: Double,
)
data class SaharWindow(val startHour: Double, val endHour: Double)

object WorshipWindows {

    /** Start of Duha: the sun risen by ~a spear ≈ 15–20 min; we take 20. */
    const val DUHA_RISE_MINUTES = 20.0

    /** Duha ends shortly before Dhuhr; we take 20 min before, «تقديري». */
    const val DUHA_BEFORE_DHUHR_MINUTES = 20.0

    /** الأسحار window = the last sixth of the night (common suhur criterion). */
    const val SAHAR_NIGHT_FRACTION = 1.0 / 6.0

    /** الضحى — distinct from الشروق (Hesham's brief: its own window, own card). */
    fun duha(sunrise: Double, dhuhr: Double): DuhaWindow = DuhaWindow(
        startHour = sunrise + DUHA_RISE_MINUTES / 60.0,
        endHour = dhuhr - DUHA_BEFORE_DHUHR_MINUTES / 60.0,
    )

    /**
     * Night portions over Maghrib → next Fajr — the majority fiqh interval.
     * The Isha-based variant is a named alternative (research-agenda R5.2);
     * do not switch silently — cite, then decide in decisions.md.
     * [fajrNext] is TOMORROW's fajr as hour-of-day.
     */
    fun night(maghrib: Double, fajrNext: Double): NightInfo {
        val len = if (fajrNext.isNaN() || maghrib.isNaN()) Double.NaN
        else ((fajrNext - maghrib) % 24.0).let { if (it <= 0.0) it + 24.0 else it }
        val n = NightInfo(
            maghribHour = maghrib,
            fajrNextHour = fajrNext,
            nightLengthHours = len,
            midnightHour = mod24(maghrib + len / 2.0),
            lastThirdStartHour = mod24(maghrib + len * 2.0 / 3.0),
        )
        return n
    }

    /**
     * الأسحار — the last portion of the night before Fajr, time of
     * وَبِالْأَسْحَارِ هُمْ يَسْتَغْفِرُونَ [الذاريات ١٨] (the ONLY sharia text
     * allowed on this card until the Muslim-2702 hadith is verified — guardrail).
     */
    fun sahar(maghrib: Double, fajrNext: Double): SaharWindow {
        val n = night(maghrib, fajrNext)
        return SaharWindow(
            startHour = mod24(n.fajrNextHour - n.nightLengthHours * SAHAR_NIGHT_FRACTION),
            endHour = mod24(n.fajrNextHour),
        )
    }

    private fun mod24(h: Double): Double = ((h % 24.0) + 24.0) % 24.0
}
