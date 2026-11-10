package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** R1 dual clocks + R5 worship windows — pure-logic gates (decisions 2026-11-10 (4)). */
class DayClockTest {

    // a realistic Makkah-like day: fajr 5:12, sunrise 6:42, dhuhr 12:06,
    // asr 15:24, maghrib 18:00, isha 19:18
    private val day = listOf(
        Appointment("fajr", "الفجر", 5.2),
        Appointment("sunrise", "الشروق", 6.7),
        Appointment("dhuhr", "الظهر", 12.1),
        Appointment("asr", "العصر", 15.4),
        Appointment("maghrib", "المغرب", 18.0),
        Appointment("isha", "العشاء", 19.3),
    )

    @Test
    fun `mid-day segment - elapsed and remaining in seconds`() {
        val seg = DayClock.position(day, 13.0)!!
        assertEquals("dhuhr", seg.current.key)
        assertEquals("asr", seg.next.key)
        assertEquals(3240L, seg.elapsedSeconds)      // 13:00 − 12:06 = 0.9 h
        assertEquals(8640L, seg.remainingSeconds)    // 15:24 − 13:00 = 2.4 h
        assertFalse(seg.currentIsYesterday)
        assertFalse(seg.nextIsTomorrow)
    }

    @Test
    fun `before fajr - current is yesterday isha, next is today fajr`() {
        val seg = DayClock.position(day, 3.0)!!
        assertEquals("isha", seg.current.key)
        assertTrue(seg.currentIsYesterday)
        assertFalse(seg.nextIsTomorrow)
        assertEquals("fajr", seg.next.key)
        assertEquals(27_720L, seg.elapsedSeconds)  // 24 − 19.3 + 3 = 7.7 h (19.3 = 19:18)
        assertEquals(7920L, seg.remainingSeconds)  // 5.2 − 3
    }

    @Test
    fun `after isha - next is tomorrow fajr`() {
        val seg = DayClock.position(day, 20.0)!!
        assertEquals("isha", seg.current.key)
        assertFalse(seg.currentIsYesterday)
        assertTrue(seg.nextIsTomorrow)
        assertEquals(2520L, seg.elapsedSeconds)     // 20:00 − 19:18
        assertEquals(33_120L, seg.remainingSeconds) // 5.2 + 24 − 20 = 9.2 h
    }

    @Test
    fun `exact boundary - that appointment is current with zero elapsed`() {
        val seg = DayClock.position(day, 12.1)!!
        assertEquals("dhuhr", seg.current.key)
        assertEquals(0L, seg.elapsedSeconds)
        assertEquals("asr", seg.next.key)
    }

    @Test
    fun `NaN appointments are skipped - polar night timeline still works`() {
        val polar = listOf(
            Appointment("fajr", "الفجر", 5.2),
            Appointment("sunrise", "الشروق", Double.NaN),
            Appointment("dhuhr", "الظهر", 12.1),
            Appointment("asr", "العصر", 15.4),
            Appointment("maghrib", "المغرب", Double.NaN),
            Appointment("isha", "العشاء", 19.3),
        )
        val seg = DayClock.position(polar, 13.0)!!
        assertEquals("dhuhr", seg.current.key)
        assertEquals("asr", seg.next.key)
        assertNull(DayClock.position(listOf(Appointment("fajr", "الفجر", Double.NaN)), 3.0))
    }

    @Test
    fun `formatHms floors to the second`() {
        assertEquals("12:30:30", formatHms(12.0 + 30.0 / 60 + 30.0 / 3600))
        assertEquals("05:02:59", formatHms(5.0 + 2.0 / 60 + 59.999 / 3600))
        assertEquals("--:--:--", formatHms(Double.NaN))
    }
}

class WorshipWindowsTest {

    // Makkah-like: maghrib 18.0 today, fajr 5.0 tomorrow → night length 11 h
    @Test
    fun `night thirds over maghrib to next fajr`() {
        val n = WorshipWindows.night(maghrib = 18.0, fajrNext = 5.0)
        assertEquals(11.0, n.nightLengthHours, 1e-9)
        assertEquals(23.5, n.midnightHour, 1e-9)          // 18 + 5.5
        assertEquals(1.333, n.lastThirdStartHour, 1e-2)   // 18 + 7.333 → 01:20 (mod 24)
    }

    @Test
    fun `duha window is between sunrise and dhuhr`() {
        val d = WorshipWindows.duha(sunrise = 5.5, dhuhr = 12.0)
        assertEquals(5.5 + 20.0 / 60, d.startHour, 1e-9)
        assertEquals(12.0 - 20.0 / 60, d.endHour, 1e-9)
        assertTrue(d.startHour < d.endHour)
    }

    @Test
    fun `sahar window is the last sixth of the night ending at fajr`() {
        val s = WorshipWindows.sahar(maghrib = 18.0, fajrNext = 5.0)
        assertEquals(5.0, s.endHour, 1e-9)
        assertEquals(5.0 - 11.0 / 6.0, s.startHour, 1e-9) // 3.1667 → mod 24
        assertEquals(3.1667, s.startHour, 1e-3)
    }

    @Test
    fun `NaN propagates - polar day shows dashes`() {
        assertTrue(WorshipWindows.duha(Double.NaN, 12.0).startHour.isNaN())
        assertTrue(WorshipWindows.night(Double.NaN, 5.0).midnightHour.isNaN())
        assertTrue(WorshipWindows.sahar(18.0, Double.NaN).startHour.isNaN())
    }
}
