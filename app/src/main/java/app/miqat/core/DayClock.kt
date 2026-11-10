package app.miqat.core

import kotlin.math.floor

/**
 * R1 dual-clock computation (decisions.md 2026-11-10 (4), research-agenda.md R1):
 * the appointed times of one day form a timeline; given "now" it returns the
 * segment in effect — [ClockSegment.current] with [ClockSegment.elapsedSeconds]
 * primary-secondary contract: REMAINING to the next miqat is the primary display,
 * ELAPSED since the current one began is the secondary (Hesham's brief).
 *
 * Seconds precision throughout (the engine computes to the second; display
 * rounds minutes only where [PrayerTimes.format] is used, floor here for the
 * ticking clock).
 */
data class Appointment(val key: String, val labelAr: String, val hour: Double)

data class ClockSegment(
    val current: Appointment,
    /** now is before today's Fajr → the segment in effect began yesterday. */
    val currentIsYesterday: Boolean,
    val elapsedSeconds: Long,
    val next: Appointment,
    /** now is after today's last appointment → next is tomorrow's first. */
    val nextIsTomorrow: Boolean,
    val remainingSeconds: Long,
)

object DayClock {

    /**
     * NaN appointments are skipped (polar day/night). Returns null when no
     * appointment survives. Boundary rule: now exactly at an appointment →
     * that appointment is current (elapsed 0).
     */
    fun position(appointments: List<Appointment>, nowHour: Double): ClockSegment? {
        val valid = appointments.filter { !it.hour.isNaN() }.sortedBy { it.hour }
        if (valid.isEmpty()) return null
        val now = ((nowHour % 24.0) + 24.0) % 24.0

        val last = valid.last()
        if (now >= last.hour) {
            // current = today's last; next = tomorrow's first
            return segment(current = last, currentIsYesterday = false,
                elapsedH = now - last.hour,
                next = valid.first(), nextIsTomorrow = true,
                remainingH = valid.first().hour + 24.0 - now)
        }
        val nextIdx = valid.indexOfFirst { it.hour > now }
        if (nextIdx == 0) {
            // before today's first → current = YESTERDAY's last
            return segment(current = last, currentIsYesterday = true,
                elapsedH = now + 24.0 - last.hour,
                next = valid.first(), nextIsTomorrow = false,
                remainingH = valid.first().hour - now)
        }
        val cur = valid[nextIdx - 1]
        val next = valid[nextIdx]
        return segment(current = cur, currentIsYesterday = false,
            elapsedH = now - cur.hour,
            next = next, nextIsTomorrow = false,
            remainingH = next.hour - now)
    }

    private fun segment(
        current: Appointment, currentIsYesterday: Boolean, elapsedH: Double,
        next: Appointment, nextIsTomorrow: Boolean, remainingH: Double,
    ) = ClockSegment(
        current = current, currentIsYesterday = currentIsYesterday,
        elapsedSeconds = Math.round(elapsedH * 3600.0),
        next = next, nextIsTomorrow = nextIsTomorrow,
        remainingSeconds = Math.round(remainingH * 3600.0),
    )
}

/**
 * Floor-to-second "HH:MM:SS" from fractional local hours — the ticking-clock
 * display (PrayerTimes.format is the round-half-up minute display, donor
 * t5.a.j parity; this one is for the R1 dual clocks). NaN → "--:--:--".
 */
fun formatHms(hours: Double): String {
    if (hours.isNaN()) return "--:--:--"
    val s = floor(((hours % 24.0) + 24.0) % 24.0 * 3600.0).toInt()
    return "%02d:%02d:%02d".format(s / 3600, s % 3600 / 60, s % 60)
}
