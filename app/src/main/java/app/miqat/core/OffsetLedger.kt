package app.miqat.core

/**
 * ONE ledger for every adjustment the app makes — the founder's team asked
 * for the offset ideas to be worked on. Today they live in five places
 * (UserSettings Hijri days, manual UTC minutes, per-prayer adjusts, imsak
 * lead, DST) and are easy to double-count.
 *
 * Pure: no settings, no Context. The UI/state layers fill the entries;
 * the engine reads the RESULT through one doorway.
 */
enum class OffsetKind {
    /** Moves the printed Hijri DATE only — never a prayer's clock. */
    HIJRI_DAY,
    /** A manual shift of the civil clock; applied ONCE in utcOffsetAt. */
    UTC_CLOCK,
    /** Per-prayer minutes; applied ONCE in AppState.times. */
    PRAYER_MINUTE,
    /** Our own prudence margin before Fajr — derived, never summed. */
    CONVENTION_LEAD,
    /** A real clock change from the platform TZ database — never stored. */
    DST,
}

data class OffsetEntry(
    val kind: OffsetKind,
    /** "fajr" … for a prayer, the zone id for DST, "" for global shifts. */
    val target: String,
    val minutes: Int,
    val source: String,
)

data class OffsetLedger(val entries: List<OffsetEntry>) {

    /** Hijri days, as a signed day count (not minutes). */
    val hijriDays: Int get() = entries.filter { it.kind == OffsetKind.HIJRI_DAY }
        .sumOf { it.minutes }

    /** Minutes a prayer's time is shifted — DST and convention are NOT here. */
    fun prayerMinutes(target: String): Int =
        entries.firstOrNull {
            it.kind == OffsetKind.PRAYER_MINUTE && it.target == target
        }?.minutes ?: 0

    /** The one manual clock shift; DST is added by the TZ table, never here. */
    val utcClockMinutes: Int get() = entries.firstOrNull {
        it.kind == OffsetKind.UTC_CLOCK
    }?.minutes ?: 0

    /** The imsak lead is a convention, read on its own — never summed. */
    val conventionLeadMinutes: Int get() = entries.firstOrNull {
        it.kind == OffsetKind.CONVENTION_LEAD
    }?.minutes ?: 0

    /** DST is reported, not stored: its minutes come from the zone rules. */
    val dstTargets: List<String> get() = entries.filter { it.kind == OffsetKind.DST }
        .map { it.target }

    companion object {
        fun from(
            hijriOffsetDays: Int,
            manualUtcMinutes: Int?,
            prayerAdjusts: Map<String, Int>,
            imsakLeadMinutes: Int,
            dstZone: String? = null,
        ): OffsetLedger {
            val entries = ArrayList<OffsetEntry>()
            if (hijriOffsetDays != 0) {
                entries += OffsetEntry(OffsetKind.HIJRI_DAY, "", hijriOffsetDays, "sighting")
            }
            if (manualUtcMinutes != null) {
                entries += OffsetEntry(OffsetKind.UTC_CLOCK, "", manualUtcMinutes, "user")
            }
            prayerAdjusts.filter { it.value != 0 }.forEach { (name, minutes) ->
                entries += OffsetEntry(OffsetKind.PRAYER_MINUTE, name, minutes, "user")
            }
            entries += OffsetEntry(OffsetKind.CONVENTION_LEAD, "fajr", imsakLeadMinutes, "app")
            if (dstZone != null) {
                entries += OffsetEntry(OffsetKind.DST, dstZone, 0, "platform-tz")
            }
            return OffsetLedger(entries)
        }
    }
}
