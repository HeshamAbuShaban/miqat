package app.miqat.core

/**
 * The DAILY REMINDER — one line on the home screen, a verse whose timing fits
 * the hour, with its source.
 *
 * The guardrail applies here too: every line carries its reference, and the
 * list is small and curated (the council verified each wording before it
 * shipped — docs/sourcing-table.md).
 */
data class DailyReminder(val text: String, val reference: String, val partOfDay: PartOfDay) {
    enum class PartOfDay { NIGHT, MORNING, NOON, EVENING, ANY }
}

object DailyReminders {

    /** The curated list — verified wordings, with their references. */
    private val LIST = listOf(
        DailyReminder(
            "وَمِنَ اللَّيْلِ فَتَهَجَّدْ بِهِ نَافِلَةً لَكَ",
            "سورة مزمل ٧٣",
            DailyReminder.PartOfDay.NIGHT,
        ),
        DailyReminder(
            "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
            "سورة الرعد ٢٨",
            DailyReminder.PartOfDay.ANY,
        ),
        DailyReminder(
            "وَقُل رَّبِّ زِدْنِي عِلْمًا",
            "سورة طه ١١٤",
            DailyReminder.PartOfDay.MORNING,
        ),
        DailyReminder(
            "إِنَّ مَعَ الْعُسْرِ يُسْرًا",
            "سورة الشرح ٦",
            DailyReminder.PartOfDay.NOON,
        ),
        DailyReminder(
            "حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ",
            "سورة آل عمران ١٧٣",
            DailyReminder.PartOfDay.EVENING,
        ),
    )

    /**
     * The line for today: the reminder that fits the hour, falling back to the
     * cycle of the day so it changes from one day to the next.
     */
    fun forNow(hourOfDay: Double, dayOfYear: Int): DailyReminder {
        val part = when {
            hourOfDay < 5.0 || hourOfDay >= 21.0 -> DailyReminder.PartOfDay.NIGHT
            hourOfDay < 11.0 -> DailyReminder.PartOfDay.MORNING
            hourOfDay < 15.0 -> DailyReminder.PartOfDay.NOON
            else -> DailyReminder.PartOfDay.EVENING
        }
        val matching = LIST.filter { it.partOfDay == part }
        val pool = if (matching.isNotEmpty()) matching else LIST
        return pool[(dayOfYear / 7) % pool.size]
    }
}
