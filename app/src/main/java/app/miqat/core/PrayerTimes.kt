package app.miqat.core

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * The prayer-time engine — a faithful port of the DONOR's own pipeline
 * (decompiled t5/a.java + y5/{b,c,f,g}.java + x5/d.java; the full decode lives
 * in docs/engine-parity.md). Kept in lockstep with tools/verify_engine.py,
 * which measures parity against the donor's shipped timetable corpus
 * (`staticprayertimes`, 57 cities, 2014–2019).
 *
 * Pipeline for one (date, location, CalcMethod):
 *  1. params = CalcMethod 11-slot config + first matching
 *     [SeasonalCorrections] window (donor `qibla` layer semantics)
 *  2. computeTimes — 7 slots, single pass at fixed day-fractions
 *     (fajr, sunrise, dhuhr, asr, sunset, maghrib-slot, isha-slot)
 *  3. adjustTimes — civil shift (utc − lng/15), per-prayer margins,
 *     maghrib/isha mode overrides, high-latitude rule
 *  4. format — round half up to the displayed minute (donor t5.a.j)
 *
 * DST is the caller's concern (S2 ships our own tz table, donor §3): pass the
 * effective UTC offset in [Location]. Pure dependency-free math, offline-first.
 */
data class LocalPrayerTimes(
    val fajr: Double,
    val sunrise: Double,
    val dhuhr: Double,
    val asr: Double,
    val maghrib: Double,
    val isha: Double,
)

data class Location(val latitude: Double, val longitude: Double, val utcOffsetHours: Double)

object PrayerTimes {

    /**
     * Full donor pipeline. Returns the six prayers as fractional local hours;
     * NaN where a time does not exist (polar day/night) — [format] renders
     * NaN as "--:--".
     */
    fun compute(
        year: Int, month: Int, day: Int,
        location: Location,
        method: CalcMethod,
    ): LocalPrayerTimes {
        // 1. params: copy of the 11 slots + seasonal window (y5.b.a)
        val p = method.withSeasonalCorrections(month * 100 + day)

        val lat = location.latitude
        val lon = location.longitude
        val jd = julian(year, month, day) - lon / 360.0

        // 2. computeTimes (t5.a.g): each slot evaluated at its own day-fraction
        val t = doubleArrayOf(5.0 / 24, 6.0 / 24, 12.0 / 24, 13.0 / 24, 18.0 / 24, 18.0 / 24, 18.0 / 24)
        val v = DoubleArray(7)
        v[0] = sunAngleTime(jd, 180.0 - p.fajrAngle, lat, t[0]) ?: Double.NaN
        v[1] = sunAngleTime(jd, 180.0 - SUNRISE_DEPRESSION, lat, t[1]) ?: Double.NaN
        v[2] = fix(12.0 - sunPosition(jd + t[2]).second, 24.0)
        v[3] = asrTime(jd, method.asrShadow.toDouble(), lat, t[3]) ?: Double.NaN
        v[4] = sunAngleTime(jd, SUNRISE_DEPRESSION, lat, t[4]) ?: Double.NaN
        v[5] = sunAngleTime(jd, p.maghribValue, lat, t[5]) ?: Double.NaN
        v[6] = sunAngleTime(jd, p.ishaValue, lat, t[6]) ?: Double.NaN

        // 3. adjustTimes (t5.a.a)
        val shift = location.utcOffsetHours - lon / 15.0
        for (i in v.indices) v[i] += shift
        v[0] += p.margins.fajr / 60.0
        v[1] += p.margins.sunrise / 60.0
        v[2] += p.margins.dhuhr / 60.0
        v[3] += p.margins.asr / 60.0

        // maghrib: minutes-after-sunset mode, or keep the angle slot
        val sunset = v[4] + p.margins.maghrib / 60.0
        v[5] = if (p.maghribMinutesMode) sunset + p.maghribValue / 60.0 else v[5] + p.margins.maghrib / 60.0

        // isha: minutes-after-maghrib mode, or keep the angle slot (+ margin)
        if (p.ishaMinutesMode) v[6] = v[5] + p.ishaValue / 60.0
        v[6] += p.margins.isha / 60.0

        // high-latitude rule (t5.a.a tail + x5.d.a). donor b.l(a, b) = fix(b − a):
        // night = fix(sunrise − sunset) crosses midnight; gaps likewise.
        if (method.highLatRule != HighLatitudeRule.NONE) {
            val night = fix(v[1] - v[4], 24.0)
            val fajrPortion = method.highLatRule.fraction(p.fajrAngle) * night
            if (v[0].isNaN() || fix(v[1] - v[0], 24.0) > fajrPortion) {
                v[0] = v[1] - fajrPortion + p.margins.fajr / 60.0
            }
            val ishaAngle = if (p.ishaMinutesMode) 18.0 else p.ishaValue
            val ishaPortion = method.highLatRule.fraction(ishaAngle) * night
            if (v[6].isNaN() || fix(v[6] - v[5], 24.0) > ishaPortion) {
                v[6] = v[5] + ishaPortion + p.margins.isha / 60.0
            }
        }

        return LocalPrayerTimes(
            fajr = v[0], sunrise = v[1], dhuhr = v[2],
            asr = v[3], maghrib = v[5], isha = v[6],
        )
    }

    /** Solar noon at the location, local civil hours (t5.a.e). */
    fun midDay(year: Int, month: Int, day: Int, location: Location): Double {
        val jd = julian(year, month, day) - location.longitude / 360.0
        val (_, eqt) = sunPosition(jd + 0.5)
        return (fix(12.0 - eqt, 24.0) + location.utcOffsetHours - location.longitude / 15.0) % 24.0
    }

    /**
     * "HH:MM" from fractional local hours — donor t5.a.j: round half up to the
     * displayed minute (floor(t + 0.5 min)), NaN → "--:--".
     */
    fun format(hours: Double): String {
        if (hours.isNaN()) return "--:--"
        val dj = fix(hours + 0.5 / 60.0, 24.0)
        val h = floor(dj).toInt()
        val m = floor((dj - h) * 60.0).toInt()
        return "%02d:%02d".format(h, m)
    }

    // --- solar math (lockstep with tools/verify_engine.py / donor t5.a.q) ---

    /** Sun horizon depression for sunrise/sunset: 0.833° = refraction + radius. */
    private const val SUNRISE_DEPRESSION = 0.833

    private fun fix(a: Double, b: Double): Double = a - b * floor(a / b)

    /** Donor t5.a.p — Julian day at 00:00 UT. */
    fun julian(y: Int, m: Int, d: Int): Double {
        var yy = y
        var mm = m
        if (mm <= 2) {
            yy -= 1
            mm += 12
        }
        val a = floor(yy / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (yy + 4716)) + floor(30.6001 * (mm + 1)) + d + b - 1524.5
    }

    /** @return Pair(sun declination degrees, equation of time hours) — donor t5.a.q */
    fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fix(357.529 + 0.98560028 * d, 360.0)
        val q = fix(280.459 + 0.98564736 * d, 360.0)
        val l = fix(q + 1.915 * sin(Math.toRadians(g)) + 0.020 * sin(Math.toRadians(2 * g)), 360.0)
        val e = 23.439 - 0.00000036 * d
        val ra = fix(
            Math.toDegrees(
                atan2(cos(Math.toRadians(e)) * sin(Math.toRadians(l)), cos(Math.toRadians(l)))
            ) / 15.0, 24.0)
        val decl = Math.toDegrees(asin(sin(Math.toRadians(e)) * sin(Math.toRadians(l))))
        val eqt = q / 15.0 - ra
        return Pair(decl, eqt)
    }

    /**
     * UT hour of the sun crossing depression [angle]; angle in (90, 180] means
     * the morning crossing (donor t5.a.f). null when the sun never gets there.
     */
    private fun sunAngleTime(jd: Double, angle: Double, lat: Double, t: Double): Double? {
        val (decl, eqt) = sunPosition(jd + t)
        val noon = fix(12.0 - eqt, 24.0)
        val num = -sin(Math.toRadians(angle)) - sin(Math.toRadians(decl)) * sin(Math.toRadians(lat))
        val den = cos(Math.toRadians(decl)) * cos(Math.toRadians(lat))
        if (den == 0.0) return null
        val x = num / den
        if (abs(x) > 1.0) return null
        val dt = Math.toDegrees(acos(x)) / 15.0
        return if (angle > 90.0) noon - dt else noon + dt
    }

    /** Asr: shadow length 1 (Shafi'i) or 2 (Hanafi) — donor t5.a.d. */
    private fun asrTime(jd: Double, shadow: Double, lat: Double, t: Double): Double? {
        val (decl, _) = sunPosition(jd + t)
        val angle = -Math.toDegrees(atan2(1.0, shadow + tan(Math.toRadians(abs(lat - decl)))))
        return sunAngleTime(jd, angle, lat, t)
    }
}
