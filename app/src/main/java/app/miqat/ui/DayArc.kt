package app.miqat.ui

import kotlin.math.abs

/**
 * THE DAY ARC — the app's signature shape, in ONE place.
 *
 * The hero (the day-sky) and the home-screen widget draw the SAME horizon:
 * the same quadratic arc, the same prayer ticks, the same gold for what the
 * day has travelled. One shape, everywhere — that is the whole point.
 *
 * The geometry is pure (no Android types) so both the Compose canvas and the
 * widget's bitmap renderer can use it.
 */
object DayArc {

    /** The horizon's ends, as a fraction of the width. */
    const val LEFT = 0.07f
    const val RIGHT = 0.93f

    /** The apex's height, as a fraction of the arc's height (from the top). */
    const val APEX = 0.07f

    /**
     * The prayer ticks along the arc — Fajr, sunrise, Dhuhr, Asr, Maghrib,
     * Isha. Fixed positions (the shape must not jump between days); the
     * gold one is the next prayer.
     */
    val TICKS = floatArrayOf(0f, 0.24f, 0.47f, 0.68f, 0.86f, 1f)

    /** The arc's parameter for a moment of the day (0 = Fajr, 1 = Isha). */
    fun parameterFor(minuteOfDay: Double, fajrMinute: Double, ishaMinute: Double): Float {
        var isha = ishaMinute
        if (isha < fajrMinute) isha += 1440.0
        var now = minuteOfDay
        if (now < fajrMinute) now += 1440.0
        val span = (isha - fajrMinute).coerceAtLeast(1.0)
        return (((now - fajrMinute) / span).coerceIn(0.0, 1.0)).toFloat()
    }

    /** The travelled part of the arc, in the same 0..1 space. */
    fun travelled(parameter: Float): Float = LEFT + (RIGHT - LEFT) * parameter.coerceIn(0f, 1f)

    /**
     * A point on the quadratic horizon: the ends sit on the base line, the
     * control point lifts the apex.
     */
    fun point(t: Float, width: Float, height: Float, baseY: Float, apexY: Float): Pair<Float, Float> {
        val mt = 1f - t
        // RTL: t = 0 (the Fajr) is the RIGHT end, t = 1 (the Isha) the left
        val x = mt * mt * (width * RIGHT) + 2 * mt * t * (width * 0.5f) + t * t * (width * LEFT)
        val y = mt * mt * baseY + 2 * mt * t * apexY + t * t * baseY
        return x to y
    }

    /**
     * The six prayer ticks at their REAL times (0 = the Fajr, 1 = the Isha) —
     * the hero and the widget both read these, so the two can never disagree
     * about where a prayer sits on the horizon (the widget council, 2026-11-11).
     */
    fun tickParams(
        minutesOfDay: List<Double>,
        fajrMinute: Double,
        ishaMinute: Double,
    ): List<Float> {
        var isha = ishaMinute
        if (isha < fajrMinute) isha += 1440.0
        val span = (isha - fajrMinute).coerceAtLeast(1.0)
        return minutesOfDay.map { m ->
            var mm = m
            if (mm < fajrMinute) mm += 1440.0
            (((mm - fajrMinute) / span).coerceIn(0.0, 1.0)).toFloat()
        }
    }

    /** The arc's parameter for the next prayer (by its index in the ticks). */
    fun tickFor(prayerIndex: Int): Float =
        if (prayerIndex in TICKS.indices) TICKS[prayerIndex] else TICKS.last()

    /** A safe index for a prayer name in [PRAYER_NAMES] (the sunrise excluded). */
    fun prayerIndex(name: String?): Int {
        if (name == null) return TICKS.lastIndex
        val i = PRAYER_NAMES.indexOf(name)
        return if (i in 0 until PRAYER_NAMES.size && i != 1) i else TICKS.lastIndex
    }

    /** Is a number close enough to be the same minute? */
    fun sameMinute(a: Double, b: Double): Boolean = abs(a - b) < 0.5
}
