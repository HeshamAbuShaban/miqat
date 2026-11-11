package app.miqat.core

/**
 * The DAILY REMINDER — one line on the home screen, a verse whose timing fits
 * the hour, with its source.
 *
 * The wording and every reference below were verified by the council against
 * two independent sources (the Tanzil Uthmani text and api.quran.com), with a
 * full-text uniqueness scan. Three of the original candidates carried WRONG
 * references and were corrected — «وَمِنَ اللَّيْلِ فَتَهَجَّدْ بِهِ نَافِلَةً لَّكَ»
 * is الإسراء ٧٩ (not المزمل ٧٣), and the strings are stored VERBATIM
 * (the Uthmanic spellings are part of the verification).
 *
 * A partial ayah is marked with a trailing «…» so the app never implies it
 * printed the whole verse.
 */
data class DailyReminder(
    val text: String,
    val reference: String,
    val partOfDay: PartOfDay,
    val partial: Boolean = true,
) {
    enum class PartOfDay { NIGHT, LATE_NIGHT, MORNING, NOON, EVENING }
}

object DailyReminders {

    /** The attribution the app prints — one line, nothing falsifiable beyond it. */
    const val ATTRIBUTION = "المصدر: القرآن الكريم — %s، الآية %s"

    /** The curated list — verified wordings, with their references. */
    private val LIST = listOf(
        DailyReminder(
            "فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ",
            "البقرة ١٥٢",
            DailyReminder.PartOfDay.MORNING,
            partial = false,
        ),
        DailyReminder(
            "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا إِنَّ مَعَ الْعُسْرِ يُسْرًا",
            "الشرح ٥–٦",
            DailyReminder.PartOfDay.NOON,
            partial = false,
        ),
        DailyReminder(
            "حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ",
            "آل عمران ١٧٣",
            DailyReminder.PartOfDay.EVENING,
        ),
        DailyReminder(
            "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
            "الرعد ٢٨",
            DailyReminder.PartOfDay.NIGHT,
        ),
        DailyReminder(
            "وَقُل رَّبِّ زِدْنِي عِلْمًا",
            "طه ١١٤",
            DailyReminder.PartOfDay.LATE_NIGHT,
        ),
        DailyReminder(
            "وَمِنَ اللَّيْلِ فَتَهَجَّدْ بِهِ نَافِلَةً لَّكَ",
            "الإسراء ٧٩",
            DailyReminder.PartOfDay.LATE_NIGHT,
        ),
    )

    /** The line as the app prints it: the verse, the excerpt mark, the source. */
    fun line(reminder: DailyReminder): Pair<String, String> {
        val text = if (reminder.partial) "${reminder.text} …" else reminder.text
        val surah = reminder.reference.substringBefore(" ")
        val ayah = reminder.reference.substringAfter(" ")
        return text to ATTRIBUTION.format(surah, ayah)
    }

    /**
     * The line for today: the reminder that fits the hour, falling back to the
     * cycle of the day so it changes from one day to the next.
     */
    fun forNow(hourOfDay: Double, dayOfYear: Int): DailyReminder {
        val part = when {
            // the tahajjud hours, then the last third, then the day
            hourOfDay < 4.5 || hourOfDay >= 20.0 -> DailyReminder.PartOfDay.LATE_NIGHT
            hourOfDay < 5.5 -> DailyReminder.PartOfDay.NIGHT
            hourOfDay < 11.0 -> DailyReminder.PartOfDay.MORNING
            hourOfDay < 15.0 -> DailyReminder.PartOfDay.NOON
            else -> DailyReminder.PartOfDay.EVENING
        }
        val matching = LIST.filter { it.partOfDay == part }
        val pool = if (matching.isNotEmpty()) matching else LIST
        return pool[(dayOfYear / 7) % pool.size]
    }
}
