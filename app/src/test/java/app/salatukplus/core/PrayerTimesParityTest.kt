package app.salatukplus.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Parity fixture from the donor's own official timetable corpus
 * (staticprayertimes: emirates/abudhabi/20140101 — SalatukDB.db).
 * Corpus-level result: 87% of emirates rows within ±2 min (docs/engine-parity.md);
 * this single-fixture gate uses ±3 min to absorb coordinate source differences.
 */
class PrayerTimesParityTest {

    private fun minutesOf(hhmm: String): Int {
        val (h, m) = hhmm.split(":").map { it.toInt() }
        return h * 60 + m
    }

    private fun assertParity(actualHours: Double, donorHhmm: String, toleranceMin: Int) {
        val actual = (actualHours * 60).toInt()
        val diff = Math.abs(actual - minutesOf(donorHhmm))
        val msg = "expected ${donorHhmm}±${toleranceMin}m, got " + PrayerTimes.format(actualHours)
        assertEquals(msg, true, diff <= toleranceMin)
    }

    @Test
    fun `abu dhabi 2014-01-01 emirates matches donor timetable`() {
        val times = PrayerTimes.compute(
            2014, 1, 1,
            Location(24.46667, 54.36667, 4.0),
            MethodTable["emirates"],
        )
        assertParity(times.fajr, "05:44", 3)
        assertParity(times.sunrise, "07:03", 3)
        assertParity(times.dhuhr, "12:28", 3)
        assertParity(times.asr, "15:28", 3)
        assertParity(times.maghrib, "17:49", 3)
        assertParity(times.isha, "19:07", 3)
    }

    @Test
    fun `makkah method uses 90 minutes after maghrib for isha`() {
        val times = PrayerTimes.compute(
            2026, 11, 6,
            Location(21.4225, 39.8262, 3.0),
            MethodTable["makkah"],
        )
        val gapMin = ((times.isha - times.maghrib) * 60).toInt()
        assertEquals(true, Math.abs(gapMin - 90) <= 2)
    }
}
