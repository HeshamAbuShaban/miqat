package app.miqat.core

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/**
 * PrayTimes-style prayer time engine — a faithful port of `tools/verify_engine.py`
 * (same formulas, same time convention), the port that reached 87% @ ±2 min parity
 * against the donor's own official-timetable corpus on 2026-11-06
 * (docs/engine-parity.md). Keep this file in lockstep with the python harness.
 *
 * All math is dependency-free; inputs are plain numbers so the core stays testable
 * on the JVM without Android.
 */
data class PrayerTimesUtc(
    val fajr: Double,
    val sunrise: Double,
    val dhuhr: Double,
    val asr: Double,
    val maghrib: Double,
    val isha: Double,
)

data class Location(val latitude: Double, val longitude: Double, val utcOffsetHours: Double)

object PrayerTimes {

    /** Asr shadow factor: 1.0 = Standard (Shafi'i/majority), 2.0 = Hanafi. */
    fun compute(
        year: Int, month: Int, day: Int,
        location: Location,
        method: Method,
        asrFactor: Double = 1.0,
    ): PrayerTimesUtc {
        val lat = location.latitude
        val lon = location.longitude
        val jd = julian(year, month, day) - lon / (15.0 * 24.0)

        val sunriseBase = sunAngleTime(jd, 0.833, lat, 6.0 / 24.0, ccw = true)
        val maghribBase = sunAngleTime(jd, 0.833, lat, 18.0 / 24.0, ccw = false)
        val decl = sunPosition(jd + 13.0 / 24.0).first
        val asrBase = asrTime(jd, asrFactor, lat, decl, 13.0 / 24.0)
        val (_, eqt) = sunPosition(jd)
        val dhuhrBase = fix(12.0 - eqt, 24.0)
        val ishaBase = if (method.ishaAngle != null)
            sunAngleTime(jd, method.ishaAngle, lat, 18.0 / 24.0, ccw = false)
        else maghribBase + method.ishaMinutes!! / 60.0
        val fajrBase = sunAngleTime(jd, method.fajrAngle, lat, 5.0 / 24.0, ccw = true)

        val m = method.margins
        // PrayTimes convention: jd frame shift + final civil correction tz − lng/15.
        fun local(ut: Double?, extraMin: Int): Double {
            val v = ut ?: return Double.NaN
            return ((v + location.utcOffsetHours - lon / 15.0 + extraMin / 60.0) % 24.0)
        }
        return PrayerTimesUtc(
            fajr = local(fajrBase, 0),
            sunrise = local(sunriseBase, m.sunrise),
            dhuhr = local(dhuhrBase, m.dhuhr),
            asr = local(asrBase, m.asr),
            maghrib = local(maghribBase, method.maghribMinutes ?: m.maghrib),
            isha = local(ishaBase, m.isha),
        )
    }

    /** Solar noon at the location, local civil hours. */
    fun midDay(year: Int, month: Int, day: Int, location: Location): Double {
        val jd = julian(year, month, day) - location.longitude / (15.0 * 24.0)
        val (_, eqt) = sunPosition(jd)
        return (fix(12.0 - eqt, 24.0) + location.utcOffsetHours - location.longitude / 15.0) % 24.0
    }

    /** "HH:MM" from fractional local hours; rounds to the nearest minute. */
    fun format(hours: Double): String {
        if (hours.isNaN()) return "--:--"
        val totalMin = (hours * 60).roundToInt()
        val h = ((totalMin / 60) % 24 + 24) % 24
        val m = (totalMin % 60 + 60) % 60
        return "%02d:%02d".format(h, m)
    }

    // --- solar math (keep in lockstep with verify_engine.py) ---

    private fun fix(a: Double, b: Double): Double = a - b * floor(a / b)

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

    /** @return Pair(sun declination degrees, equation of time hours) */
    fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fix(357.529 + 0.98560028 * d, 360.0)
        val q = fix(280.459 + 0.98564736 * d, 360.0)
        val l = fix(q + 1.915 * sin(Math.toRadians(g)) + 0.020 * sin(Math.toRadians(2 * g)), 360.0)
        val e = 23.439 - 0.00000036 * d
        val ra = fix(
            Math.toDegrees(
                atan2(
                    cos(Math.toRadians(e)) * sin(Math.toRadians(l)),
                    cos(Math.toRadians(l))
                )
            ) / 15.0, 24.0)
        val decl = Math.toDegrees(asin(sin(Math.toRadians(e)) * sin(Math.toRadians(l))))
        val eqt = q / 15.0 - ra
        return Pair(decl, eqt)
    }

    private fun sunAngleTime(jd: Double, angle: Double, lat: Double, t: Double, ccw: Boolean): Double? {
        val decl = sunPosition(jd + t).first
        val noon = fix(12.0 - sunPosition(jd + t).second, 24.0)
        val num = -sin(Math.toRadians(angle)) - sin(Math.toRadians(decl)) * sin(Math.toRadians(lat))
        val den = cos(Math.toRadians(decl)) * cos(Math.toRadians(lat))
        if (den == 0.0 || abs(num / den) > 1.0) return null
        val dt = Math.toDegrees(acos(num / den)) / 15.0
        return if (ccw) noon - dt else noon + dt
    }

    private fun asrTime(jd: Double, factor: Double, lat: Double, decl: Double, t: Double): Double? {
        val angle = -Math.toDegrees(atan(1.0 / (factor + tan(Math.toRadians(abs(lat - decl))))))
        return sunAngleTime(jd, angle, lat, t, ccw = false)
    }
}
