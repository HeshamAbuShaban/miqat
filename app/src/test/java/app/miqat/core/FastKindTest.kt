package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** One truth for the calendar badge, the day sheet and the fast catalogue. */
class FastKindTest {

    private fun d(month: Int, day: Int, weekday: Int = 1) = IslamicDay(1448, month, day, weekday)

    @Test fun `every day of Ramadan is a fast day`() {
        (1..30).forEach {
            assertEquals(IslamicDays.FastDay.RAMADAN, IslamicDays.fastKind(d(9, it)))
        }
    }

    @Test fun `tashreeq vetoes the white-day rule on 13 dhul-hijjah`() {
        val k = IslamicDays.fastKind(d(12, 13))
        assertEquals(IslamicDays.FastDay.TASHREEQ, k)
        assertFalse(k.fasts)
    }

    @Test fun `arafah and ashura are distinct fast days`() {
        assertEquals(IslamicDays.FastDay.ARAFAH, IslamicDays.fastKind(d(12, 9)))
        assertEquals(IslamicDays.FastDay.ASHURA, IslamicDays.fastKind(d(1, 10)))
        assertTrue(IslamicDays.fastKind(d(12, 9)).fasts)
    }

    @Test fun `eid outranks every other fast atom`() {
        assertEquals(IslamicDays.FastDay.EID, IslamicDays.fastKind(d(10, 1)))
        assertFalse(IslamicDays.fastKind(d(12, 10)).fasts)
    }

    @Test fun `an ordinary day has no badge`() {
        assertEquals(IslamicDays.FastDay.NONE, IslamicDays.fastKind(d(5, 4, weekday = 3)))
    }
}
