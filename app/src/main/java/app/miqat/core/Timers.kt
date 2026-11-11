package app.miqat.core

import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * ONE point on the day's ABSOLUTE timeline.
 *
 * [hour] is an absolute fractional hour measured from today's local midnight:
 * today's times live in [0, 24), tomorrow's Fajr is +24, yesterday's Isha is
 * −24. On one straight line the night (Isha → midnight → Fajr) stops being a
 * wrap-around trick — and that trick is exactly where the old clock broke:
 * after the last appointment of the day it called "today's Fajr" the next one
 * and counted twelve hours since Isha as if they were news.
 */
data class Point(val name: String, val hour: Double, val isPrayer: Boolean)

/**
 * THE TIMERS — every feature gets ITS OWN line and ITS OWN clock.
 *
 * Hesham (2026-11-11): «there must be multiple feature in its own line,
 * meaning if the iqama came it has a timer of its own, if the prayer has
 * started it has timer up until the other prayer to count through, and whats
 * left as well in its own … right now im seeing something like "12:41:20" for
 * how much it has been since "someprayer" which is wrong».
 *
 * The rules the model enforces:
 *  - [remainingSeconds] — the countdown to the NEXT PRAYER (never the sunrise:
 *    a prayer is what the user waits for; never a prayer that already came).
 *  - [sinceSeconds] — the elapsed since the current prayer's time entered.
 *    It only exists while we are INSIDE that prayer's window, and the window
 *    is bounded (the Isha's window ends at midnight — so the twelve-hour
 *    «منذ العشاء» can never be produced again).
 *  - [iqamaSeconds] — the iqama (the second call) has its own countdown,
 *    attached to the prayer it belongs to.
 */
data class Timers(
    /** The prayer whose time has entered (null between the windows). */
    val currentPrayer: String?,
    /** Minutes since the current prayer's time entered. */
    val sinceCurrent: Int?,
    /** The elapsed since the current prayer's time entered (the ticking one). */
    val sinceSeconds: Long,
    /** The next prayer — never the sunrise. */
    val nextPrayer: String?,
    /** Minutes until the next prayer. */
    val nextIn: Int,
    /** The next prayer's absolute hour. */
    val nextAt: Double,
    /** Seconds until the next prayer (the ticking clock). */
    val remainingSeconds: Long,
    /** Is the next prayer tomorrow's? */
    val nextIsTomorrow: Boolean,
    /** Minutes until the pending iqama (null when there is none). */
    val iqamaIn: Int?,
    /** Seconds until the pending iqama. */
    val iqamaSeconds: Long,
    /** Which prayer the pending iqama belongs to. */
    val iqamaPrayer: String?,
) {
    /**
     * Is the elapsed worth a line of its own?
     *
     * Only in the FIRST hour of the prayer's window: that is the moment the
     * information is fresh («the adhan just came, six minutes ago»). After
     * that the elapsed is a second big number next to the hero's countdown —
     * the same confusion the user called out («putting time with another
     * ones»), only smaller. The STATE chip carries the rest («داخل وقت
     * العشاء»).
     */
    val sinceWorthShowing: Boolean get() = currentPrayer != null && sinceSeconds in 1..3600

    /** The state line: what the clock is inside right now (not a timer). */
    val stateAr: String?
        get() = when {
            currentPrayer != null -> "داخل وقت $currentPrayer"
            nextIn <= 60 -> "اقترب وقت $nextPrayer"
            else -> null
        }

    companion object {
        val EMPTY = Timers(null, null, 0L, null, 0, 0.0, 0L, false, null, 0L, null)

        /**
         * @param points the day's ABSOLUTE timeline (yesterday's Isha → today's
         *   points → midnight → tomorrow's Fajr), as built by [prayerPoints]
         * @param nowHour the city's fractional hour of the day right now
         * @param iqamaMinutes the iqama delay in minutes (0 = the iqama is off)
         */
        fun of(points: List<Point>, nowHour: Double, iqamaMinutes: Int = 0): Timers {
            if (points.isEmpty()) return EMPTY
            val line = points.sortedBy { it.hour }

            // the point we are standing on
            var idx = 0
            for (i in line.indices) {
                if (line[i].hour <= nowHour) idx = i else break
            }
            val current = line[idx]

            // the next PRAYER — the sunrise is a mark on the sky, not a prayer
            var next: Point? = null
            for (i in (idx + 1) until line.size) {
                val p = line[i]
                if (p.isPrayer && p.hour > nowHour) {
                    next = p
                    break
                }
            }
            if (next == null) {
                // tomorrow's line: the first prayer that is still ahead
                next = line.map { it.copy(hour = it.hour + 24.0) }
                    .firstOrNull { it.isPrayer && it.hour > nowHour }
            }
            if (next == null) return EMPTY

            val sinceSeconds = if (current.isPrayer) {
                ((nowHour - current.hour) * 3600.0).roundToLong().coerceAtLeast(0L)
            } else 0L

            // the iqama: the current prayer's if it has not come yet, else the
            // next prayer's — the one the user is waiting for
            var iqamaIn: Int? = null
            var iqamaSeconds = 0L
            var iqamaPrayer: String? = null
            if (iqamaMinutes > 0) {
                val candidates = ArrayList<Pair<Double, String>>(2)
                if (current.isPrayer) candidates.add(current.hour + iqamaMinutes / 60.0 to current.name)
                candidates.add(next.hour + iqamaMinutes / 60.0 to next.name)
                val pending = candidates
                    .map { (h, n) -> (h - nowHour) * 3600.0 to n }
                    .filter { it.first > 0 }
                    .minByOrNull { it.first }
                if (pending != null) {
                    iqamaSeconds = pending.first.roundToLong()
                    iqamaIn = (pending.first / 60.0).roundToInt()
                    iqamaPrayer = pending.second
                }
            }

            return Timers(
                currentPrayer = if (current.isPrayer) current.name else null,
                sinceCurrent = if (current.isPrayer) (sinceSeconds / 60).toInt() else null,
                sinceSeconds = sinceSeconds,
                nextPrayer = next.name,
                nextIn = ((next.hour - nowHour) * 60.0).roundToInt(),
                nextAt = next.hour,
                remainingSeconds = ((next.hour - nowHour) * 3600.0).roundToLong(),
                nextIsTomorrow = next.hour >= 24.0,
                iqamaIn = iqamaIn,
                iqamaSeconds = iqamaSeconds,
                iqamaPrayer = iqamaPrayer,
            )
        }
    }
}

/**
 * The day's ABSOLUTE timeline.
 *
 * Yesterday's Isha opens the line and tomorrow's Fajr closes it, so the
 * night is one straight span. The two midnights are the boundaries of the
 * Isha's window: the Isha's time ends at midnight (not at the next dawn —
 * that mistake is what produced «منذ العشاء ١٢:٤١:٢٠»), and the gap between
 * midnight and the Fajr belongs to no prayer at all.
 */
fun prayerPoints(
    times: LocalPrayerTimes,
    tomorrowFajr: Double,
    labels: List<String>,
): List<Point> = listOf(
    // yesterday's Isha — the night begins
    Point(labels[5], times.isha - 24.0, true),
    // midnight — the Isha's window ends
    Point("منتصف الليل", 0.0, false),
    // today
    Point(labels[0], times.fajr, true),
    Point(labels[1], times.sunrise, false),
    Point(labels[2], times.dhuhr, true),
    Point(labels[3], times.asr, true),
    Point(labels[4], times.maghrib, true),
    Point(labels[5], times.isha, true),
    // midnight — tonight's Isha window ends
    Point("منتصف الليل", 24.0, false),
    // tomorrow's Fajr
    Point(labels[0], tomorrowFajr + 24.0, true),
)
