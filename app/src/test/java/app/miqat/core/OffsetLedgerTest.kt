package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** One ledger, no double counting, no dead offsets. */
class OffsetLedgerTest {

    @Test fun `hijri days move dates only, never a prayer clock`() {
        val l = OffsetLedger.from(3, null, emptyMap(), 15)
        assertEquals(3, l.hijriDays)
        assertEquals(0, l.prayerMinutes("fajr"))
        assertEquals(0, l.utcClockMinutes)
    }

    @Test fun `a manual UTC shift is applied once and never summed into prayers`() {
        val l = OffsetLedger.from(0, -60, mapOf("fajr" to 5), 15)
        assertEquals(-60, l.utcClockMinutes)
        assertEquals(5, l.prayerMinutes("fajr"))
        assertEquals(0, l.prayerMinutes("dhuhr"))
    }

    @Test fun `the imsak convention is read alone`() {
        val l = OffsetLedger.from(0, null, mapOf("fajr" to 10), 20)
        assertEquals(20, l.conventionLeadMinutes)
        assertEquals(10, l.prayerMinutes("fajr"))
        // convention is NOT added to the prayer adjust
        assertEquals(10, l.prayerMinutes("fajr"))
    }

    @Test fun `DST is reported, never stored as a minute shift`() {
        val l = OffsetLedger.from(0, null, emptyMap(), 15, "Africa/Cairo")
        assertTrue(l.dstTargets.contains("Africa/Cairo"))
        assertEquals(0, l.utcClockMinutes)
        assertEquals(0, l.conventionLeadMinutes - 15)
    }
}
