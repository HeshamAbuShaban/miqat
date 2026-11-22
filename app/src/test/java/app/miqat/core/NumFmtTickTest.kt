package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** Secondary timers never print seconds beyond the minute they can keep. */
class NumFmtTickTest {

    @Test fun `under an hour shows minutes and seconds`() {
        assertEquals("⁦١٢:٣٤⁩", NumFmt.tick(754L, true))
        assertEquals("⁦12:34⁩", NumFmt.tick(754L, false))
    }

    @Test fun `over an hour drops the seconds`() {
        assertEquals("⁦١:١٧⁩", NumFmt.tick(4625L, true))
    }

    @Test fun `a clock keeps its meridiem but a duration never does`() {
        val clock = NumFmt.hhmm(17 * 60 + 5, true, true)
        val duration = NumFmt.hms(17L * 3600 + 5 * 60, true)
        assert(clock.contains("م"))
        assert(!duration.contains("م"))
    }
}
