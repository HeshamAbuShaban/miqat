package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-logic gates for the S9 Islamic-day layer and the S4 qibla math
 * (both dependency-free; ICU stays behind the Hijri bridge and is untested here).
 */
class IslamicDaysTest {

    private fun d(hM: Int, hD: Int, wd: Int = 5) = IslamicDay(1447, hM, hD, wd)

    @Test
    fun `white days are 13-15 of any month`() {
        assertTrue(IslamicDays.isWhiteDays(d(9, 13)))
        assertTrue(IslamicDays.isWhiteDays(d(3, 15)))
        assertFalse(IslamicDays.isWhiteDays(d(9, 12)))
    }

    @Test
    fun `monday thursday follows the weekday`() {
        assertTrue(IslamicDays.isMondayThursday(d(9, 20, wd = 2))) // Monday
        assertTrue(IslamicDays.isMondayThursday(d(9, 20, wd = 5))) // Thursday
        assertFalse(IslamicDays.isMondayThursday(d(9, 20, wd = 6))) // Friday
    }

    @Test
    fun `priority days`() {
        assertTrue(IslamicDays.isDayOfArafah(d(12, 9)))
        assertTrue(IslamicDays.isAshura(d(1, 10)))
        assertTrue(IslamicDays.isTasuaa(d(1, 9)))
        assertTrue(IslamicDays.isSixOfShawwal(d(10, 7)))
        assertFalse(IslamicDays.isSixOfShawwal(d(10, 1)))
        assertTrue(IslamicDays.isRamadan(d(9, 5)))
    }

    @Test
    fun `eid days forbid the fasting nudge`() {
        assertTrue(IslamicDays.isEidAlFitr(d(10, 1)))
        assertTrue(IslamicDays.isEidAlAdha(d(12, 11)))
        assertEquals("عيد الفطر المبارك", IslamicDays.todayCard(d(10, 2))?.first)
        assertNull(IslamicDays.todayCard(d(7, 3, wd = 3))) // plain Tuesday → quiet home
    }

    @Test
    fun `plain monday gives the weekly fast card`() {
        assertEquals("صيام الاثنين والخميس", IslamicDays.todayCard(d(7, 3, wd = 2))?.first)
    }

    @Test
    fun `qibla bearing known anchors`() {
        assertEquals(0.0, Qibla.bearing(Qibla.KAABA_LAT, Qibla.KAABA_LON), 0.5)
        assertEquals(136.1, Qibla.bearing(30.0444, 31.2357), 1.0)   // Cairo
        assertEquals(58.5, Qibla.bearing(40.7128, -74.0060), 1.5)   // New York (ENE)
        assertTrue(Qibla.distanceKm(Qibla.KAABA_LAT, Qibla.KAABA_LON) < 1.0)
    }
}
