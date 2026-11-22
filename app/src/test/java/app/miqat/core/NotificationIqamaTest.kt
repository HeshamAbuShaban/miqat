package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** The alarm must fire at the SAME iqama minute the Home screen shows. */
class NotificationIqamaTest {

    @Test fun `per-prayer delay wins over the global default`() {
        val prefs = NotificationPrefs(
            iqamaMinutes = 10,
            iqamaByPrayer = mapOf("asr" to 25, "isha" to 0),
        )
        assertEquals(10, prefs.iqamaForIndex(0))   // fajr — default
        assertEquals(25, prefs.iqamaForIndex(3))   // asr — override
        assertEquals(0, prefs.iqamaForIndex(5))    // isha — disabled
    }

    @Test fun `an unset override falls back to the default`() {
        val prefs = NotificationPrefs(iqamaMinutes = 15)
        assertEquals(15, prefs.iqamaForIndex(2))
    }
}
