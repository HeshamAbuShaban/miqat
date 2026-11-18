package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The remaining window between two timed acts — number AND shape. */
class SpanTest {

    @Test fun `an open window has a remaining tail and a consumed head`() {
        // Asr 15:20 → Maghrib 18:28, now 16:00
        val s = Span(920, 1108, 960)
        assertTrue(s.open)
        assertEquals(188, s.totalMin)
        assertEquals(148, s.remainingMin)
        assertEquals(40f / 188f, s.consumedFrac, 0.001f)
        assertEquals(148f / 188f, s.frac, 0.001f)
    }

    @Test fun `a window that has not begun has no remaining time`() {
        val s = Span(920, 1108, 600)
        assertFalse(s.open)
        assertEquals(188, s.remainingMin) // clamped to the TOTAL, never wrapped
        assertEquals(0f, s.consumedFrac, 0.001f)
    }

    @Test fun `a closed window has no remaining time and no tail`() {
        val s = Span(920, 1108, 1300)
        assertFalse(s.open)
        assertEquals(0, s.remainingMin)
        assertEquals(0f, s.frac, 0.001f)
        assertEquals(1f, s.consumedFrac, 0.001f)
    }

    @Test fun `the night span crosses midnight as an absolute window without a wrap`() {
        // Isha 19:58 → tomorrow Fajr 4:51 (+1440), now 23:00
        val s = Span(1198, 1731, 1380)
        assertTrue(s.open)
        assertEquals(533, s.totalMin)
        assertEquals(351, s.remainingMin)
    }

    @Test fun `the timer line names both ends of the span`() {
        val s = Span(920, 1108, 960)
        val line = Proximity.timerLine(
            TimerKind.BETWEEN_ACTS, s.remainingMin * 60L, "العصر", "المغرب",
        )
        assertEquals("بين العصر والمغرب", line.prefixAr)
    }
}
