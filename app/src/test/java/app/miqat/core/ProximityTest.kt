package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The proximity gate — what the app MAY say, where, and when (founder,
 * 2026-11-18). Every line here is a case the old app got wrong.
 */
class ProximityTest {

    private val labels = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")

    private fun h(hour: Int, minute: Int): Double = hour + minute / 60.0

    private val day = LocalPrayerTimes(
        fajr = h(4, 48), sunrise = h(6, 12), dhuhr = h(12, 5),
        asr = h(15, 20), maghrib = h(18, 28), isha = h(19, 58),
    )
    private val tomorrowFajr = h(4, 51)
    private val minuteOf: (Double) -> Int = { (it * 60).toInt() }

    private val line = DayLine.of(day, tomorrowFajr, labels, minuteOf, imsakLeadMinutes = 15)

    private fun verdict(kind: String, nowHour: Int, nowMin: Int, surface: Surface): Verdict {
        // tomorrow's Fajr shares its kind with today's — select the +1 day slot
        val slot = line.slots.firstOrNull { it.kind == kind && it.dayOffset > 0 }
            ?: line.at(kind)
            ?: return Verdict.NOT_RELEVANT
        return Proximity.judge(slot, nowHour * 60 + nowMin, Proximity.rules(surface), line).verdict
    }

    @Test fun `imsak long passed is silent on the fasting screen`() {
        assertEquals(Verdict.NOT_RELEVANT, verdict(Moments.IMSAK, 8, 0, Surface.FASTING))
    }

    @Test fun `today's iftar is upcoming on the fasting screen`() {
        assertEquals(Verdict.UPCOMING, verdict(Moments.IFTAR, 8, 0, Surface.FASTING))
    }

    @Test fun `tomorrow's fajr is NEVER shown on the fasting screen, even after iftar`() {
        assertEquals(Verdict.NOT_RELEVANT, verdict(Moments.FAJR, 8, 0, Surface.FASTING))
        assertEquals(Verdict.NOT_RELEVANT, verdict(Moments.FAJR, 18, 40, Surface.FASTING))
        assertFalse(
            Proximity.of(line, 18 * 60 + 40, Surface.FASTING)
                .any { it.slot.dayOffset > 0 }
        )
    }

    @Test fun `dhuhr five minutes past is DUE with an elapsed`() {
        val slot = line.at(Moments.DHUHR)!!
        val j = Proximity.judge(slot, 12 * 60 + 10, Proximity.rules(Surface.HOME), line)
        assertEquals(Verdict.DUE, j.verdict)
        assertEquals(5, j.elapsedMin)
    }

    @Test fun `dhuhr 85 minutes past is silent - no twelve-hour lie`() {
        assertEquals(Verdict.NOT_RELEVANT, verdict(Moments.DHUHR, 13, 30, Surface.HOME))
    }

    @Test fun `asr twenty minutes ahead is NEAR`() {
        assertEquals(Verdict.NEAR, verdict(Moments.ASR, 15, 0, Surface.HOME))
    }

    @Test fun `tomorrow's fajr is the night's primary fact after isha`() {
        assertEquals(Verdict.UPCOMING, verdict(Moments.FAJR, 21, 0, Surface.HOME))
        assertEquals(Moments.FAJR, Proximity.primary(line, 21 * 60, Surface.HOME)?.slot?.kind)
    }

    @Test fun `tomorrow's fajr is locked before isha`() {
        assertEquals(Verdict.NOT_RELEVANT, verdict(Moments.FAJR, 18, 40, Surface.HOME))
    }

    @Test fun `sunrise is not a home fact - it is a mark on the sky`() {
        assertEquals(Verdict.NOT_RELEVANT, verdict(Moments.SUNRISE, 9, 0, Surface.HOME))
    }

    @Test fun `eid exists only on its own day, never on tashreeq`() {
        assertNull(line.at(Moments.EID_PRAYER))
        val eidLine = DayLine.of(day, tomorrowFajr, labels, minuteOf, eidDay = true)
        assertNotNull(eidLine.at(Moments.EID_PRAYER))
        val tashreeq = Moments.timeline(
            day, tomorrowFajr, labels, minuteOf,
            hijriMonth = 12, hijriDay = 11,
        )
        assertFalse(tashreeq.any { it.kind == Moments.EID_PRAYER })
    }

    @Test fun `no slot is ever judged with a wrapped forward distance`() {
        val now = 21 * 60
        Proximity.of(line, now, Surface.HOME).forEach { j ->
            assertTrue("startIn must be signed", j.startIn in -1440..1440)
            assertFalse("a passed slot must not wrap", j.verdict == Verdict.DUE && j.startIn < -60)
        }
    }

    @Test fun `the timer vocabulary has one wording per kind`() {
        val remaining = Proximity.timerLine(TimerKind.PRAYER_REMAINING, 720L, "الفجر")
        assertEquals("بقي لـالفجر", remaining.prefixAr)
        val elapsed = Proximity.timerLine(TimerKind.ELAPSED, 300L, "العصر")
        assertEquals("منذ أذان العصر", elapsed.prefixAr)
        val between = Proximity.timerLine(TimerKind.BETWEEN_ACTS, 5400L, "المغرب", "العشاء")
        assertEquals("بين المغرب والعشاء", between.prefixAr)
        val all = TimerKind.entries.map {
            Proximity.timerLine(it, 60L, "الفجر", "العشاء").prefixAr
        }.joinToString(" ")
        assertFalse(all.contains("بعد"))
        assertFalse(all.contains("يبقى"))
        assertFalse(all.contains("مرّ من"))
    }
}
