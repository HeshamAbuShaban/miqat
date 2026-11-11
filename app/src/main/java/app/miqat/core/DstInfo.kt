package app.miqat.core

import android.content.Context
import java.util.TimeZone

/**
 * Daylight-saving transitions and the drift of a prayer time across the year —
 * the two things the user asked to SEE (Hesham, 2026-11-10):
 *
 *  - «اظهار مؤشرات تغير التوقيت الصيفي والشتوي» — when the clock shifts, and
 *    by how much. The user wants to anticipate it («تأملا كيف كان الوقت قبل
 *    هذا»), because a prayer's wall-clock time moves with it.
 *  - «اظهار الفروق الزمنية على مدى الأشهر» — how a prayer time drifts month
 *    by month: Fajr earlier in summer, Maghrib later, and the TOTAL change
 *    over the year visible at a glance.
 *
 * WHERE THE RULES COME FROM (the advisory council, 2026-11-11): the platform's
 * own timezone database is CURRENT, while the bundled donor table's rules stop
 * in 2023 — so a city in a DST country was an hour wrong in summer, and the
 * notice could never fire (the table's seconds were compared with millis).
 * The platform's rules are now the source of truth; the bundled table stays as
 * the fallback for a zone the platform does not know.
 */
data class DstTransition(
    /** When the clock changes (epoch millis). */
    val atMillis: Long,
    val oldOffsetHours: Double,
    val newOffsetHours: Double,
    val isDstNow: Boolean,
) {
    /** The signed change in the wall clock (+1 = the clock goes forward). */
    val deltaHours: Double get() = newOffsetHours - oldOffsetHours

    /** «يتقدّم» / «يتراجع» — the plain-Arabic direction. */
    val directionAr: String
        get() = when {
            deltaHours > 0 -> "يتقدّم"
            deltaHours < 0 -> "يتراجع"
            else -> "لا يتغيّر"
        }

    val labelAr: String
        get() = "التوقيت $directionAr ${fmtHours(kotlin.math.abs(deltaHours))}"

    companion object {
        fun fmtHours(h: Double): String =
            if (h % 1.0 == 0.0) "${h.toInt()} ساعة" else "${"%.1f".format(h)} ساعة"
    }
}

object DstInfo {

    /** The platform's timezone for an IANA name — null when it is unknown. */
    fun zoneFor(zone: String): TimeZone? {
        if (zone.isBlank()) return null
        val tz = TimeZone.getTimeZone(zone)
        return if (tz.getID() == zone) tz else null
    }

    /**
     * The next transition for a zone after [nowMillis], or null when the zone
     * has no further rules (a fixed-offset zone).
     *
     * @param zone the IANA zone name (the platform's rules)
     * @param zoneId the bundled table's key (the fallback for old zones)
     */
    fun nextTransition(
        context: Context,
        zone: String,
        zoneId: Int?,
        nowMillis: Long,
    ): DstTransition? {
        zoneFor(zone)?.let { tz ->
            val at = nextChange(tz, nowMillis) ?: return null
            val before = tz.getOffset(at - 1) / 3600000.0
            val after = tz.getOffset(at) / 3600000.0
            return DstTransition(at, before, after, tz.inDaylightTime(java.util.Date(at)))
        }
        // the bundled table (its transitions are stored in SECONDS)
        if (zoneId == null) return null
        return try {
            val db = MiqatData.open(context)
            val nowSeconds = nowMillis / 1000L
            val next = db.rawQuery(
                "SELECT start_epoch, gmt_offset, dst FROM tz_rules WHERE zone_id = ?"
                    + " AND start_epoch > ? ORDER BY start_epoch ASC LIMIT 1",
                arrayOf(zoneId.toString(), nowSeconds.toString()),
            ).use { c -> if (c.moveToFirst()) Triple(c.getLong(0), c.getInt(1), c.getInt(2)) else null }
            val prev = db.rawQuery(
                "SELECT gmt_offset FROM tz_rules WHERE zone_id = ? AND start_epoch <= ?"
                    + " ORDER BY start_epoch DESC LIMIT 1",
                arrayOf(zoneId.toString(), nowSeconds.toString()),
            ).use { c -> if (c.moveToFirst()) c.getInt(0) / 3600.0 else null }
            if (next != null && prev != null) {
                DstTransition(
                    atMillis = next.first * 1000L,
                    oldOffsetHours = prev,
                    newOffsetHours = next.second / 3600.0,
                    isDstNow = next.third == 1,
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /** Every transition inside a calendar year (for the calendar's markers). */
    fun transitionsInYear(
        context: Context,
        zone: String,
        zoneId: Int?,
        year: Int,
    ): List<DstTransition> {
        zoneFor(zone)?.let { tz ->
            val out = ArrayList<DstTransition>()
            val start = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear(); set(year, 0, 1)
            }.timeInMillis
            val end = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear(); set(year + 1, 0, 1)
            }.timeInMillis
            var at = nextChange(tz, start - 1) ?: return out
            while (at < end) {
                val before = tz.getOffset(at - 1) / 3600000.0
                val after = tz.getOffset(at) / 3600000.0
                if (before != after) {
                    out.add(DstTransition(at, before, after, tz.inDaylightTime(java.util.Date(at))))
                }
                at = nextChange(tz, at) ?: break
            }
            return out
        }
        if (zoneId == null) return emptyList()
        return try {
            val db = MiqatData.open(context)
            val rows = db.rawQuery(
                "SELECT start_epoch, gmt_offset, dst FROM tz_rules WHERE zone_id = ?"
                    + " ORDER BY start_epoch ASC",
                arrayOf(zoneId.toString()),
            ).use { c ->
                val out = ArrayList<Triple<Long, Int, Int>>()
                while (c.moveToNext()) out.add(Triple(c.getLong(0), c.getInt(1), c.getInt(2)))
                out
            }
            val out = ArrayList<DstTransition>()
            var prevOffset: Double? = null
            for ((start, offset, dst) in rows) {
                val cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                cal.timeInMillis = start * 1000L // the table stores seconds
                if (cal.get(java.util.Calendar.YEAR) == year && prevOffset != null) {
                    out.add(
                        DstTransition(start * 1000L, prevOffset, offset / 3600.0, dst == 1),
                    )
                }
                prevOffset = offset / 3600.0
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * The first instant after [from] where the zone's offset changes — found
     * by a coarse scan and then a bisection (the platform does not expose the
     * transition dates, but it does answer «what is the offset at T»).
     */
    private fun nextChange(tz: TimeZone, from: Long): Long? {
        val start = tz.getOffset(from)
        val year = 400L * 24 * 3600_0000
        var lo = from
        var hi = from + 6 * 3600_000L
        while (hi - from < year) {
            if (tz.getOffset(hi) != start) break
            lo = hi
            hi += 6 * 3600_000L
        }
        if (hi - from >= year) return null
        // bisect to the second
        while (hi - lo > 1000L) {
            val mid = lo + (hi - lo) / 2
            if (tz.getOffset(mid) == start) lo = mid else hi = mid
        }
        return hi
    }
}

/**
 * The drift of one prayer time across the months — the second thing the user
 * wants to see: Fajr creeping earlier into summer, Maghrib creeping later,
 * and the TOTAL change over the year.
 */
data class DriftPoint(
    val month: Int,
    val day: Int,
    /** The wall-clock time, in minutes of the day. */
    val minuteOfDay: Int,
    /** The difference from the first point, in minutes (negative = earlier). */
    val deltaMinutes: Int,
)

object Drift {

    /**
     * Samples one prayer across a year (the 1st and the 15th of each month —
     * two points per month show the shape of the curve, not just its end).
     *
     * @param timesOf the day's times for a date — the caller owns the engine.
     */
    fun acrossYear(
        year: Int,
        prayerIndex: Int,
        timesOf: (year: Int, month: Int, day: Int) -> List<Double>,
    ): List<DriftPoint> {
        val raw = ArrayList<Pair<Int, Int>>()   // month, minuteOfDay
        for (m in 1..12) {
            for (d in intArrayOf(1, 15)) {
                val list = timesOf(year, m, d)
                val minutes = ((list[prayerIndex] % 24.0 + 24.0) % 24.0 * 60).toInt()
                raw.add(m to minutes)
            }
        }
        val base = raw.first().second
        return raw.mapIndexed { i, (m, minutes) ->
            val (month, day) = if (i % 2 == 0) m to 1 else m to 15
            DriftPoint(month, day, minutes, minutes - base)
        }
    }

    /** The total change over the year, in minutes (negative = earlier). */
    fun totalChange(points: List<DriftPoint>): Int =
        (points.maxOfOrNull { it.minuteOfDay } ?: 0) - (points.minOfOrNull { it.minuteOfDay } ?: 0)

    /** The change from the first point to the last (the trend). */
    fun trend(points: List<DriftPoint>): Int =
        (points.lastOrNull()?.minuteOfDay ?: 0) - (points.firstOrNull()?.minuteOfDay ?: 0)
}
