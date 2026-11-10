package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The drift model (R4, «الفروق الزمنية على مدى الأشهر») and the DST
 * transition wording — pure logic, so the shape of the curve is pinned.
 */
class DriftTest {

    /** A stand-in engine: Fajr is 5:37 in January and drifts 1 minute later
     *  each month (so the total change over the year is 11 minutes). */
    private fun fakeTimes(year: Int, month: Int, day: Int): List<Double> {
        val minutes = 337 + (month - 1)
        return listOf(minutes / 60.0, 6.5, 12.0, 15.0, 17.5, 19.0)
    }

    @Test
    fun `the drift samples two points a month`() {
        val points = Drift.acrossYear(2026, 0, ::fakeTimes)
        assertEquals(24, points.size)
        assertEquals(1, points.first().month)
        assertEquals(1, points.first().day)
        assertEquals(15, points[1].day)
        assertEquals(12, points.last().month)
    }

    @Test
    fun `the delta is relative to the first point`() {
        val points = Drift.acrossYear(2026, 0, ::fakeTimes)
        assertEquals(0, points.first().deltaMinutes)   // 1 January — the base
        assertEquals(0, points[1].deltaMinutes)       // 15 January — the same day
        assertEquals(1, points[2].deltaMinutes)       // 1 February — one minute later
        assertEquals(11, points.last().deltaMinutes)  // 15 December
    }

    @Test
    fun `total change and trend`() {
        val points = Drift.acrossYear(2026, 0, ::fakeTimes)
        assertEquals(11, Drift.totalChange(points))
        assertEquals(11, Drift.trend(points))
    }

    @Test
    fun `a flat year has no change`() {
        val flat = Drift.acrossYear(2026, 0) { _, _, _ -> listOf(5.5, 6.5, 12.0, 15.0, 17.5, 19.0) }
        assertEquals(0, Drift.totalChange(flat))
        assertEquals(0, Drift.trend(flat))
    }
}

class DstTransitionTest {

    @Test
    fun `the direction is stated plainly`() {
        val forward = DstTransition(0L, 2.0, 3.0, true)
        assertEquals("يتقدّم", forward.directionAr)
        assertEquals(1.0, forward.deltaHours, 1e-9)
        assertTrue(forward.labelAr.contains("يتقدّم"))

        val back = DstTransition(0L, 3.0, 2.0, false)
        assertEquals("يتراجع", back.directionAr)
        assertEquals(-1.0, back.deltaHours, 1e-9)
    }

    @Test
    fun `hours are formatted for people`() {
        assertEquals("1 ساعة", DstTransition.fmtHours(1.0))
        assertEquals("2 ساعة", DstTransition.fmtHours(2.0))
        assertEquals("0.5 ساعة", DstTransition.fmtHours(0.5))
    }
}
