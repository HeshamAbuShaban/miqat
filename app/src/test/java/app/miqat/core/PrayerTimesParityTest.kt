package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parity gate against the donor's shipped timetable corpus (staticprayertimes,
 * SalatukDB.db — see docs/engine-parity.md). Fixtures are sampled donor rows
 * that the reference python engine (tools/verify_engine.py, fitted mode)
 * reproduces within ±1 min; the Kotlin engine must do the same.
 *
 * Full-corpus measurement stays in the python harness; this suite keeps the
 * engine honest at CI speed (S7 will grow this into per-method threshold gates).
 */
class PrayerTimesParityTest {

    private fun minutesOf(hhmm: String): Int {
        val (h, m) = hhmm.split(":").map { it.toInt() }
        return h * 60 + m
    }

    private fun diffMin(actualHours: Double, donorHhmm: String): Int {
        // donor format (t5.a.j): round half up to the displayed minute
        val dj = ((actualHours + 0.5 / 60.0) % 24.0 + 24.0) % 24.0
        val totalMin = kotlin.math.floor(dj * 60.0).toInt() % (24 * 60)
        return Math.abs(totalMin - minutesOf(donorHhmm))
    }

    @Test
    fun `every parity fixture matches donor timetable within one minute`() {
        var worst = 0
        var worstAt = ""
        var checked = 0
        for (row in ParityFixtures.rows) {
            val times = PrayerTimes.compute(
                row.year, row.month, row.day,
                Location(row.lat, row.lon, row.utcOffset),
                MethodTable[row.method],
            )
            val cases = listOf(
                "fajr" to (times.fajr to row.fajr),
                "sunrise" to (times.sunrise to row.sunrise),
                "dhuhr" to (times.dhuhr to row.dhuhr),
                "asr" to (times.asr to row.asr),
                "maghrib" to (times.maghrib to row.maghrib),
                "isha" to (times.isha to row.isha),
            )
            for ((prayer, pair) in cases) {
                val (hours, want) = pair
                val msg = "${row.method}/${row.city}/${row.year}${row.month}/${row.day} $prayer: " +
                    "want $want, got " + PrayerTimes.format(hours)
                assertTrue(msg, !hours.isNaN())
                val d = diffMin(hours, want)
                if (d > worst) {
                    worst = d
                    worstAt = msg
                }
                assertTrue("$msg (diff=$d)", d <= 1)
                checked++
            }
        }
        assertEquals(true, checked > 0)
        assertTrue("worst deviation $worst at $worstAt", worst <= 1)
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

    @Test
    fun `seasonal corrections fold into angles and margins inside their window only`() {
        val method = MethodTable["emirates"]
        val win = method.seasonal.first { it.fromMMdd <= 101 && 101 <= it.toMMdd }
        val inWindow = method.withSeasonalCorrections(101)
        val outside = method.withSeasonalCorrections(431) // 431-500 = no window
        assertEquals(method.fajrAngle + win.fajrAngle, inWindow.fajrAngle, 1e-9)
        assertEquals(method.margins.fajr + win.fajr, inWindow.margins.fajr, 1e-9)
        assertEquals(method.margins.asr + win.asr, inWindow.margins.asr, 1e-9)
        assertEquals(method.fajrAngle, outside.fajrAngle, 1e-9)
        assertEquals(method.margins.fajr, outside.margins.fajr, 1e-9)
    }

    @Test
    fun `unknown method id falls back to the donor default makkah`() {
        assertEquals("makkah", MethodTable["does-not-exist"].id)
        assertEquals("makkah", MethodTable["makkah"].id)
    }
}
