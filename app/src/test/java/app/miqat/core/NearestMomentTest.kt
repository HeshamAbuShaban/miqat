package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** «أقرب موعد ميقات» — one classifier, signed time, honest day labels. */
class NearestMomentTest {

    private val labels = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
    private fun h(hour: Int, minute: Int): Double = hour + minute / 60.0
    private val day = LocalPrayerTimes(
        fajr = h(4, 48), sunrise = h(6, 12), dhuhr = h(12, 5),
        asr = h(15, 20), maghrib = h(18, 28), isha = h(19, 58),
    )
    private val line = DayLine.of(day, h(4, 51), labels, { (it * 60).toInt() }, 15)
    private fun find(now: Int, shift: Int = 0, past: Boolean = false) =
        NearestMoment.find(line, now, Surface.DAY, shift, past)

    @Test fun `just after midnight the nearest is fajr`() {
        val n = find(0)!!
        assertEquals(Moments.FAJR, n.slot.kind)
        assertEquals(288, n.signedMin)
        assertNull(n.dayLabelAr)
    }

    @Test fun `before fajr at 3 eleven the nearest is fajr`() {
        assertEquals(97, find(3 * 60 + 11)!!.signedMin)
    }

    @Test fun `after isha the nearest is tomorrow's fajr, labelled`() {
        val n = find(22 * 60)!!
        assertEquals(291, n.clockMin)
        assertEquals("غداً", n.dayLabelAr)
        assertEquals(411, n.signedMin)
    }

    @Test fun `inside a prayer the entered time is DUE but the next ahead wins`() {
        val n = find(12 * 60 + 10)!!
        assertEquals(Moments.ASR, n.slot.kind)
        assertEquals(190, n.signedMin)
        assertEquals(Verdict.UPCOMING, n.verdict)
    }

    @Test fun `allowPast can report the just-entered prayer without a wrap`() {
        val n = find(12 * 60 + 10, past = true)!!
        assertEquals(Moments.DHUHR, n.slot.kind)
        assertEquals(-5, n.signedMin)
    }

    @Test fun `a selected tomorrow resolves on its own clock`() {
        val n = find(21 * 60, shift = 1440)!!
        assertEquals(Moments.FAJR, n.slot.kind)
        assertEquals("غداً", n.dayLabelAr)
        assertEquals(468, n.signedMin) // 4:48 on the selected day, +1440
    }

    @Test fun `no eid slot exists unless its actual day was passed`() {
        assertNull(line.slots.firstOrNull { it.kind == Moments.EID_PRAYER })
    }
}
