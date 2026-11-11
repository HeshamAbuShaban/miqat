package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The unified moment registry (gaps, intervals) and the urgency model. */
class MomentsTest {

    // a Makkah-like day (local hours) — the same fixture as TimersTest
    private val today = LocalPrayerTimes(5.2, 6.7, 12.1, 15.4, 18.0, 19.3)
    private val tomorrowFajr = 5.2
    private val labels = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")

    private fun minuteOf(h: Double) = (((h + 0.5 / 60.0) % 24.0) * 60).toInt()

    private val timeline = Moments.timeline(today, tomorrowFajr, labels, ::minuteOf)

    @Test
    fun `timeline is sorted and carries every category`() {
        assertEquals(timeline.sortedBy { it.minute }, timeline)
        val kinds = timeline.map { it.kind }.toSet()
        assertTrue(kinds.containsAll(listOf(
            Moments.FAJR, Moments.SUNRISE, Moments.DHUHR, Moments.ASR, Moments.MAGHRIB,
            Moments.ISHA, Moments.DUHA_START, Moments.MIDNIGHT, Moments.LAST_THIRD,
            Moments.SAHAR_START, Moments.QAYLULA_START,
        )))
    }

    @Test
    fun `night markers land inside the maghrib-to-fajr span`() {
        val maghrib = timeline.first { it.kind == Moments.MAGHRIB }.minute
        val midnight = timeline.first { it.kind == Moments.MIDNIGHT }.minute
        val lastThird = timeline.first { it.kind == Moments.LAST_THIRD }.minute
        val fajrTomorrow = timeline.first { it.kind == Moments.SLEEP_END }.minute
        assertTrue(maghrib < midnight)
        assertTrue(midnight < lastThird)
        assertTrue(lastThird < fajrTomorrow)
        // night length = 18:00 → 05:12 = 672 min → midnight at +336, last third at +448
        assertEquals(maghrib + 336, midnight)
        assertEquals(maghrib + 448, lastThird)
    }

    @Test
    fun `gaps cover the whole day without holes`() {
        val gaps = Moments.gaps(timeline)
        assertEquals(timeline.size - 1, gaps.size)
        gaps.forEach { assertEquals(it.to.minute - it.from.minute, it.minutes) }
        // the fajr → sunrise gap on this fixture
        val fajrToSunrise = gaps.first { it.from.kind == Moments.FAJR }
        assertEquals(minuteOf(6.7) - minuteOf(5.2), fajrToSunrise.minutes)
    }

    @Test
    fun `interval works across categories and across midnight`() {
        val fajr = timeline.first { it.kind == Moments.FAJR }
        val duha = timeline.first { it.kind == Moments.DUHA_START }
        val lastThird = timeline.first { it.kind == Moments.LAST_THIRD }
        // fajr → duha start (a cross-category interval: prayer → worship)
        assertEquals(duha.minute - fajr.minute, Moments.interval(fajr, duha))
        // last third → fajr tomorrow crosses midnight
        val fajrTomorrow = timeline.first { it.kind == Moments.SLEEP_END }
        assertEquals(
            fajrTomorrow.minute - lastThird.minute,
            Moments.interval(lastThird, fajrTomorrow),
        )
        // reversed order wraps around the day
        assertEquals(1440 - (duha.minute - fajr.minute), Moments.interval(duha, fajr))
    }

    @Test
    fun `until counts forward to the next occurrence`() {
        val isha = timeline.first { it.kind == Moments.ISHA }
        val now = minuteOf(18.0)
        assertEquals(78, Moments.until(isha, now))
    }
}

class UrgencyTest {

    @Test
    fun `urgency escalates as the appointed time approaches`() {
        assertEquals(Urgency.CALM, UrgencyRules.of(3600))
        assertEquals(Urgency.SOON, UrgencyRules.of(15 * 60))
        assertEquals(Urgency.IMMINENT, UrgencyRules.of(5 * 60))
        assertEquals(Urgency.NOW, UrgencyRules.of(0))
        assertEquals(Urgency.NOW, UrgencyRules.of(-10))
    }

    @Test
    fun `thresholds are configurable`() {
        assertEquals(Urgency.SOON, UrgencyRules.of(30 * 60, soonMinutes = 30, imminentMinutes = 10))
        assertEquals(Urgency.CALM, UrgencyRules.of(30 * 60, soonMinutes = 15, imminentMinutes = 5))
    }
}
