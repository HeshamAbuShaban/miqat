package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Times screen's «المواقيت المصاحبة» (council seat 2, 2026-11-18).
 * The old code gapped the FULL timeline, so each sunnah marker parked on a
 * prayer's minute produced «المدة ٠ د» under that prayer.
 */
class TimesGapsTest {

    private val labels = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
    private fun h(hour: Int, minute: Int): Double = hour + minute / 60.0
    private val day = LocalPrayerTimes(
        fajr = h(4, 48), sunrise = h(6, 12), dhuhr = h(12, 5),
        asr = h(15, 20), maghrib = h(18, 28), isha = h(19, 58),
    )
    private val minuteOf: (Double) -> Int = { (it * 60).toInt() }

    @Test fun `six prayer gaps and not one zero`() {
        val gaps = Moments.prayerGaps(day, h(4, 51), labels, minuteOf)
        assertEquals(6, gaps.size)
        assertTrue(gaps.all { it.second > 0 })
        assertEquals(listOf(Moments.FAJR, Moments.SUNRISE, Moments.DHUHR,
            Moments.ASR, Moments.MAGHRIB, Moments.ISHA), gaps.map { it.first })
    }

    @Test fun `the night gap reaches tomorrow's fajr, not a sunnah marker`() {
        val gaps = Moments.prayerGaps(day, h(4, 51), labels, minuteOf)
        val night = gaps.last().second
        val isha = minuteOf(day.isha)
        val fajrTomorrow = minuteOf(h(4, 51)) + 1440
        assertEquals(fajrTomorrow - isha, night)
        assertTrue(night > 8 * 60)
    }

    @Test fun `each gap matches the real prayer times`() {
        val g = Moments.prayerGaps(day, h(4, 51), labels, minuteOf).toMap()
        assertEquals(minuteOf(day.sunrise) - minuteOf(day.fajr), g[Moments.FAJR])
        assertEquals(minuteOf(day.dhuhr) - minuteOf(day.sunrise), g[Moments.SUNRISE])
        assertEquals(minuteOf(day.asr) - minuteOf(day.dhuhr), g[Moments.DHUHR])
        assertEquals(minuteOf(day.maghrib) - minuteOf(day.asr), g[Moments.ASR])
        assertEquals(minuteOf(day.isha) - minuteOf(day.maghrib), g[Moments.MAGHRIB])
    }

    @Test fun `the old full-timeline gapping really had sunnah markers on prayer minutes`() {
        // the regression we removed: five sunnah markers sit ON prayers
        val full = Moments.timeline(day, h(4, 51), labels, minuteOf)
        val prayerMinutes = full.filter { it.category == MomentCategory.PRAYER }
            .map { it.minute }.toSet()
        val collisions = full.filter {
            it.category == MomentCategory.SUNNAH && it.minute in prayerMinutes
        }
        assertTrue("the old defect must be real, not imagined", collisions.isNotEmpty())
        assertTrue(collisions.map { it.kind }.contains(Moments.SLEEP_START))
    }
}
