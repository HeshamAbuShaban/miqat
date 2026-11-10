package app.miqat.core

/**
 * VERIFICATION (R3.3) — the honest offline answer to «المعيار الذهبي هي قيم
 * الإنترنت»: the user brings the official timetable of their city (from the
 * mosque, the authority, or any published table), and the app compares it with
 * what IT computed — showing every difference in minutes — and can adopt them
 * as the manual adjustments.
 *
 * Nothing is sent anywhere: the comparison happens on the device, and the
 * official table is the user's own data.
 */
data class OfficialTimes(
    val fajr: Int,
    val sunrise: Int,
    val dhuhr: Int,
    val asr: Int,
    val maghrib: Int,
    val isha: Int,
) {
    fun asList(): List<Int> = listOf(fajr, sunrise, dhuhr, asr, maghrib, isha)

    companion object {
        /** Parses "05:10" / "5:10" into minutes; returns null when invalid. */
        fun parse(text: String): Int? {
            val parts = text.trim().split(":", "،", ".", " ").filter { it.isNotBlank() }
            if (parts.size != 2) return null
            val h = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            if (h !in 0..23 || m !in 0..59) return null
            return h * 60 + m
        }
    }
}

/** The comparison: what the app computed vs what the city publishes. */
data class Reconciliation(
    val computed: List<Int>,
    val official: List<Int>,
) {
    /** The difference per slot, in minutes (official − computed). */
    val deltas: List<Int> get() = official.indices.map { official[it] - computed[it] }

    /** How many slots differ by more than a minute. */
    val mismatches: Int get() = deltas.count { kotlin.math.abs(it) > 1 }

    /** The differences as the app's manual adjustments (rounded to minutes). */
    fun asAdjusts(): PrayerAdjusts {
        val d = deltas
        return PrayerAdjusts(
            fajr = d.getOrElse(0) { 0 },
            sunrise = d.getOrElse(1) { 0 },
            dhuhr = d.getOrElse(2) { 0 },
            asr = d.getOrElse(3) { 0 },
            maghrib = d.getOrElse(4) { 0 },
            isha = d.getOrElse(5) { 0 },
        )
    }

    /** The largest absolute difference — a quick measure of trust. */
    val maxDelta: Int get() = deltas.maxOfOrNull { kotlin.math.abs(it) } ?: 0
}
