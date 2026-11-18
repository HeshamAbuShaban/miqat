package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The home line's facts — computed once, never by the drawing code. */
class DayRailInfoTest {

    private val labels = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
    private fun h(hour: Int, minute: Int): Double = hour + minute / 60.0
    private val day = LocalPrayerTimes(
        fajr = h(4, 48), sunrise = h(6, 12), dhuhr = h(12, 5),
        asr = h(15, 20), maghrib = h(18, 28), isha = h(19, 58),
    )

    @Test fun `sunrise is never the next appointment`() {
        val e = DayRailInfo.entries(day, labels, 8 * 60, tomorrowFajrMinute = 4 * 60 + 51)
        assertEquals(RailState.NEXT, e[2].state) // Dhuhr, not sunrise
        assertTrue(e[1].isSunrise)
        assertFalse(e[1].isPrayer)
    }

    @Test fun `a just-entered prayer is CURRENT, then PASSED after an hour`() {
        assertEquals(RailState.CURRENT, DayRailInfo.entries(day, labels, 12 * 60 + 10)[2].state)
        assertEquals(RailState.PASSED, DayRailInfo.entries(day, labels, 13 * 60 + 30)[2].state)
    }

    @Test fun `before fajr the next appointment is today's fajr`() {
        val e = DayRailInfo.entries(day, labels, 3 * 60)
        assertEquals(RailState.NEXT, e[0].state)
    }

    @Test fun `gaps skip the sunrise and cross midnight at isha`() {
        val e = DayRailInfo.entries(day, labels, 8 * 60, tomorrowFajrMinute = 4 * 60 + 51)
        // Fajr 4:48 → Dhuhr 12:05 (sunrise skipped)
        assertEquals(12 * 60 + 5 - (4 * 60 + 48), e[0].gapToNext)
        // Isha 19:58 → tomorrow Fajr 4:51
        assertEquals((4 * 60 + 51) + 1440 - (19 * 60 + 58), e[5].gapToNext)
    }
}
