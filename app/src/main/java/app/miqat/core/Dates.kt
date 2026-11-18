package app.miqat.core

import java.util.Calendar
import java.util.TimeZone

/**
 * Real date arithmetic — no `month + 1` with the day kept (Jan 31 → Feb 31,
 * then silently normalised, so the night's Fajr belonged to another day).
 *
 * Uses the UTC calendar purely as a date machine: the app's times still come
 * from the CITY's zone; only the (y, m, d) tuple is stepped here.
 */
object Dates {
    fun nextDay(year: Int, month: Int, day: Int): Triple<Int, Int, Int> {
        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, day)
            add(Calendar.DAY_OF_MONTH, 1)
        }
        return Triple(
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH),
        )
    }
}
