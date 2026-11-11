package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * THE TIMERS — the home's clocks.
 *
 * Hesham's bug report (2026-11-11): the home showed «منذ الفجر ١٢:٤١:٢٠» — a
 * twelve-hour elapsed counter beside a countdown, mixed on one line, and the
 * "next prayer" pointing at the wrong one after the day's last appointment.
 *
 * These tests pin the fix: one model, each feature with its own line, and no
 * elapsed noise.
 */
class TimersTest {

    private val labels = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")

    /** A typical day: Fajr 4:48, sunrise 6:12, Dhuhr 12:05, Asr 15:20, Maghrib 18:28, Isha 19:58. */
    private fun h(hour: Int, minute: Int): Double = hour + minute / 60.0

    private val day = LocalPrayerTimes(
        fajr = h(4, 48), sunrise = h(6, 12), dhuhr = h(12, 5),
        asr = h(15, 20), maghrib = h(18, 28), isha = h(19, 58),
    )
    private val tomorrowFajr = h(4, 51)

    private fun at(hour: Double, iqama: Int = 0): Timers =
        Timers.of(prayerPoints(day, tomorrowFajr, labels), hour, iqama)

    @Test
    fun `the next prayer is never the sunrise`() {
        // 5:00 — the Fajr has entered, the sunrise is still ahead
        val t = at(5.0)
        assertEquals("الفجر", t.currentPrayer)
        assertEquals("الظهر", t.nextPrayer)
        assertFalse(t.nextIsTomorrow)
    }

    @Test
    fun `inside a window the elapsed is shown and the next prayer is the following one`() {
        // 12:10 — five minutes into the Dhuhr
        val t = at(h(12, 10))
        assertEquals("الظهر", t.currentPrayer)
        assertEquals(5, t.sinceCurrent)
        assertEquals(300L, t.sinceSeconds)
        assertTrue(t.sinceWorthShowing)
        assertEquals("العصر", t.nextPrayer)
        assertEquals(190, t.nextIn)
        assertEquals("داخل وقت الظهر", t.stateAr)
    }

    @Test
    fun `the twelve-hour elapsed never surfaces`() {
        // 9:11 in the morning — twelve hours and forty-one minutes since the
        // Isha of the night before. The old home showed it as a headline.
        val t = at(h(9, 11))
        assertFalse("a twelve-hour elapsed must not be shown", t.sinceWorthShowing)
        assertNull("we are in no prayer's window at 9:11", t.currentPrayer)
        // and the next prayer is today's Dhuhr, not a prayer that already came
        assertEquals("الظهر", t.nextPrayer)
        assertFalse(t.nextIsTomorrow)
        assertTrue(t.remainingSeconds > 0)
    }

    @Test
    fun `the Isha's window ends at midnight, not at the next dawn`() {
        // 23:30 — still inside tonight's Isha
        val late = at(h(23, 30))
        assertEquals("العشاء", late.currentPrayer)
        assertEquals(212, late.sinceCurrent) // 19:58 → 23:30
        // the elapsed is fresh news only in the first hour
        assertFalse(late.sinceWorthShowing)
        assertEquals("الفجر", late.nextPrayer)
        assertTrue(late.nextIsTomorrow)

        // 1:30 the next morning — the Isha's window is closed, and the gap
        // before the Fajr belongs to no prayer
        val after = at(h(1, 30))
        assertNull(after.currentPrayer)
        assertFalse(after.sinceWorthShowing)
        assertEquals("الفجر", after.nextPrayer)
        assertFalse(after.nextIsTomorrow)
    }

    @Test
    fun `before the Fajr the next prayer is today's Fajr`() {
        // 3:11 — the night belongs to no prayer; the next prayer is today's
        // Fajr, in about an hour and a half
        val t = at(h(3, 11))
        assertEquals("الفجر", t.nextPrayer)
        assertFalse(t.nextIsTomorrow)
        assertEquals(97, t.nextIn) // 4:48 today − 3:11
        assertTrue(t.remainingSeconds in 5_800..6_000)
        assertNull(t.currentPrayer)
        assertFalse(t.sinceWorthShowing)
    }

    @Test
    fun `after the last prayer the night wraps to tomorrow`() {
        // 22:00 — the Isha has come; the next prayer is tomorrow's Fajr
        val t = at(22.0)
        assertEquals("الفجر", t.nextPrayer)
        assertTrue(t.nextIsTomorrow)
        assertEquals(411, t.nextIn) // 4:51 tomorrow − 22:00
        assertEquals("العشاء", t.currentPrayer)
    }

    @Test
    fun `the elapsed is shown only in the first hour`() {
        // 12:10 — six minutes into the Dhuhr: fresh, so it is shown
        assertTrue(at(h(12, 10)).sinceWorthShowing)
        // 13:30 — an hour and a half in: the chip carries the state instead
        val later = at(h(13, 30))
        assertEquals("الظهر", later.currentPrayer)
        assertFalse(later.sinceWorthShowing)
    }

    @Test
    fun `the iqama has its own countdown`() {
        // 12:10 with a 15-minute iqama: the Dhuhr's iqama is ten minutes away
        val t = at(h(12, 10), iqama = 15)
        assertEquals(10, t.iqamaIn)
        assertEquals("الظهر", t.iqamaPrayer)
        assertTrue(t.iqamaSeconds in 590..610)
        // the iqama never replaces the next prayer — both have their own line
        assertEquals("العصر", t.nextPrayer)
    }

    @Test
    fun `once the iqama has come the next prayer's iqama is the pending one`() {
        // 13:00 — the Dhuhr's iqama is long gone; the Asr's is ahead
        val t = at(13.0, iqama = 15)
        assertEquals("العصر", t.iqamaPrayer)
        assertEquals(155, t.iqamaIn) // the Asr's iqama: 15:20 + 15 − 13:00
    }

    @Test
    fun `no iqama means no iqama line`() {
        val t = at(h(12, 10))
        assertNull(t.iqamaIn)
        assertNull(t.iqamaPrayer)
    }

    @Test
    fun `the state chip appears when a prayer is close`() {
        // 12:00 — the Dhuhr is a minute away, no window has opened yet
        val t = at(12.0)
        assertEquals("اقترب وقت الظهر", t.stateAr)
    }

    @Test
    fun `an empty day is empty, not a crash`() {
        val t = Timers.of(emptyList(), 12.0)
        assertNull(t.nextPrayer)
        assertEquals(0, t.nextIn)
        assertFalse(t.sinceWorthShowing)
    }

    @Test
    fun `the timeline covers the whole night without a gap`() {
        val points = prayerPoints(day, tomorrowFajr, labels)
        assertNotNull(points.firstOrNull { it.hour < 0 })
        assertTrue(points.any { it.hour == 0.0 })
        assertTrue(points.any { it.hour == 24.0 })
        assertTrue(points.any { it.hour > 24.0 })
        // the line is monotonic — one straight night
        val sorted = points.sortedBy { it.hour }
        assertEquals(sorted.size, points.size)
    }
}
