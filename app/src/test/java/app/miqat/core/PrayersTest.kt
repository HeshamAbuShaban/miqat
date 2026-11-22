package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PrayersTest {

    @Test fun `names and keys stay aligned`() {
        assertEquals(6, Prayers.NAMES.size)
        assertEquals(Prayers.NAMES.size, Prayers.KEYS.size)
    }

    @Test fun `minutes wrap and round half up`() {
        assertEquals(288, Prayers.minutesOf(4.8))
        assertEquals(0, Prayers.minutesOf(24.0))
        assertEquals(30, Prayers.minutesOf(0.5 + 0.5 / 60.0 - 0.0001))
        assertEquals(0, Prayers.minutesOf(Double.NaN))
    }
}
