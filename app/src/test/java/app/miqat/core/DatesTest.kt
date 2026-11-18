package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** The day sheet's night must not get Jan 31 → Feb 31 (the weird screen). */
class DatesTest {

    @Test fun `january 31 becomes february 1`() {
        assertEquals(Triple(2026, 2, 1), Dates.nextDay(2026, 1, 31))
    }

    @Test fun `december 31 becomes january 1 of the next year`() {
        assertEquals(Triple(2027, 1, 1), Dates.nextDay(2026, 12, 31))
    }

    @Test fun `a leap day is respected`() {
        assertEquals(Triple(2028, 2, 29), Dates.nextDay(2028, 2, 28))
        assertEquals(Triple(2028, 3, 1), Dates.nextDay(2028, 2, 29))
        assertEquals(Triple(2027, 3, 1), Dates.nextDay(2027, 2, 28))
    }

    @Test fun `an ordinary day just advances`() {
        assertEquals(Triple(2026, 11, 13), Dates.nextDay(2026, 11, 12))
    }
}
