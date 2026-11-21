package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** «الأحداث المتقاربة حاليًا» — bounded by the window that is open now. */
class ClosingListTest {

    private val labels = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
    private fun h(hour: Int, minute: Int): Double = hour + minute / 60.0
    private val day = LocalPrayerTimes(
        fajr = h(4, 48), sunrise = h(6, 12), dhuhr = h(12, 5),
        asr = h(15, 20), maghrib = h(18, 28), isha = h(19, 58),
    )
    private val line = DayLine.of(day, h(4, 51), labels, { (it * 60).toInt() }, 15)

    @Test fun `a passed act never gets a forward label`() {
        val rows = ClosingList.of(line, 21 * 60)
        assertTrue(rows.none { it.slot.atMin < 21 * 60 && it.labelAr.startsWith("بعد") })
    }

    @Test fun `no row reaches past the anchor horizon`() {
        val rows = ClosingList.of(line, 12 * 60)
        assertTrue(rows.all { it.fracOfAnchor in 0f..1f })
    }

    @Test fun `no row survives its horizon as now advances`() {
        listOf(11 * 60, 12 * 60, 12 * 60 + 30, 13 * 60).forEach { now ->
            val rows = ClosingList.of(line, now)
            assertTrue("a row outlived its window at $now",
                rows.all { it.fracOfAnchor in 0f..1f })
        }
    }

    @Test fun `fraction inside the anchor rises monotonically`() {
        val a = ClosingList.of(line, 11 * 60)
        val b = ClosingList.of(line, 12 * 60)
        val daySlot = Moments.DHUHR
        val fa = a.firstOrNull { it.slot.kind == daySlot }?.fracOfAnchor ?: return
        val fb = b.firstOrNull { it.slot.kind == daySlot }?.fracOfAnchor ?: return
        assertTrue(fb >= fa)
    }

    @Test fun `no eid row before its day`() {
        assertTrue(ClosingList.of(line, 8 * 60).none { it.slot.category == MomentCategory.EID })
    }

    @Test fun `the result is stable across calls`() {
        assertEquals(
            ClosingList.of(line, 15 * 60).map { it.slot.kind },
            ClosingList.of(line, 15 * 60).map { it.slot.kind },
        )
    }
}
