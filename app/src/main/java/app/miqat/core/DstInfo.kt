package app.miqat.core

import android.content.Context

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
 * The transitions come from the bundled timezone table (28,522 rules), so this
 * works offline and matches the offsets the app actually uses.
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

    /**
     * The next transition for a zone after [nowMillis], or null when the zone
     * has no further rules (a fixed-offset zone).
     */
    fun nextTransition(
        context: Context,
        zoneId: Int,
        nowMillis: Long,
    ): DstTransition? = try {
        val db = MiqatData.open(context)
        val next = db.rawQuery(
            "SELECT start_epoch, gmt_offset, dst FROM tz_rules WHERE zone_id = ?"
                + " AND start_epoch > ? ORDER BY start_epoch ASC LIMIT 1",
            arrayOf(zoneId.toString(), nowMillis.toString()),
        ).use { c -> if (c.moveToFirst()) Triple(c.getLong(0), c.getInt(1), c.getInt(2)) else null }
        val prev = db.rawQuery(
            "SELECT gmt_offset FROM tz_rules WHERE zone_id = ? AND start_epoch <= ?"
                + " ORDER BY start_epoch DESC LIMIT 1",
            arrayOf(zoneId.toString(), nowMillis.toString()),
        ).use { c -> if (c.moveToFirst()) c.getInt(0) / 3600.0 else null }
        if (next != null && prev != null) {
            DstTransition(
                atMillis = next.first,
                oldOffsetHours = prev,
                newOffsetHours = next.second / 3600.0,
                isDstNow = next.third == 1,
            )
        } else null
    } catch (e: Exception) {
        null
    }

    /** Every transition inside a calendar year (for the calendar's markers). */
    fun transitionsInYear(
        context: Context,
        zoneId: Int,
        year: Int,
    ): List<DstTransition> = try {
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
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            cal.timeInMillis = start
            if (cal.get(java.util.Calendar.YEAR) == year && prevOffset != null) {
                out.add(
                    DstTransition(start, prevOffset, offset / 3600.0, dst == 1),
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
