package app.miqat.core

/**
 * THE DAY-LINE'S FACTS — one pure reading of the six appointed moments.
 *
 * The visual rail (ui/DayRail.kt) draws these; it must not recompute time
 * itself. Every fact here is checkable against the same [LocalPrayerTimes]
 * the engine produces.
 *
 * The sunrise is a MARK on the sky, never a prayer: it gets a position and
 * a state, but it can never be the "next" appointment.
 */
enum class RailState { PASSED, CURRENT, NEXT, LATER }

data class RailEntry(
    val index: Int,
    val name: String,
    val minute: Int,
    /** Minutes from this moment to the next PRAYER (sunrise is skipped). */
    val gapToNext: Int,
    val state: RailState,
    val isPrayer: Boolean = index != 1,
    val isSunrise: Boolean = index == 1,
)

object DayRailInfo {

    /** Absolute minutes of the six moments (isha may be < fajr in raw hours). */
    fun minutesOf(times: LocalPrayerTimes): List<Int> = listOf(
        times.fajr, times.sunrise, times.dhuhr, times.asr, times.maghrib, times.isha,
    ).map { (((it % 24.0) + 24.0) % 24.0 * 60.0).toInt() }

    /**
     * @param nowMinute the CITY's minute of day (never the device's clock)
     */
    fun entries(
        times: LocalPrayerTimes,
        labels: List<String>,
        nowMinute: Int,
        tomorrowFajrMinute: Int? = null,
        elapsedCeilMinutes: Int = 60,
    ): List<RailEntry> {
        val minutes = minutesOf(times)
        // the next PRAYER, on the absolute timeline (tomorrow's Fajr closes it)
        val nextIndex = (0..5).firstOrNull { i ->
            i != 1 && minutes[i] > nowMinute
        } ?: 0 // before Fajr, the next appointment is today's Fajr
        return minutes.mapIndexed { i, minute ->
            val next = (i + 1..5).firstOrNull { j -> j != 1 }
            val nextMinute = if (next == null) (tomorrowFajrMinute ?: minutes[0]) + 1440
            else minutes[next]
            val gap = (nextMinute - minute).coerceAtLeast(0)
            val state = when {
                i == nextIndex && minute > nowMinute -> RailState.NEXT
                minute <= nowMinute && nowMinute - minute < elapsedCeilMinutes -> RailState.CURRENT
                minute <= nowMinute -> RailState.PASSED
                else -> RailState.LATER
            }
            RailEntry(i, labels[i], minute, gap, state)
        }
    }
}
