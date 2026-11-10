package app.miqat.core

/**
 * How much the app may claim about a time (gate 0.5).
 *
 * The advisory council's charge was blunt: the app «fakes certainty in the one
 * place users act on». So every time carries an honest class, and the UI shows
 * it where it matters (the prayer's detail sheet) instead of hiding it.
 */
enum class TimeConfidence(val labelAr: String, val noteAr: String) {
    /** The user adopted their city's official timetable — this IS their time. */
    CONFIRMED("مؤكد", "معتمد من جدول مدينتك الرسمي"),

    /** A published, established calculation method (Umm al-Qura, MWL, …). */
    COMPUTED("محسوب", "محسوب بطريقة معتمدة"),

    /** A local estimate, or a high-latitude approximation. */
    ESTIMATED("تقديري", "تقدير — قد يختلف عن جدول بلدك بدقائق"),
}

object Confidence {

    /** The methods that are published, established tables (not local estimates). */
    private val PUBLISHED = setOf(
        "makkah", "ummalqura", "mwl", "egypt", "isna", "karachi", "tehran", "jafari",
        "kuwait", "qatar", "oman", "omanmuscat", "singapore", "malaysia", "indonesia",
        "southkorea", "morocco", "algeria", "tunisia", "libya", "turkey", "pakistan",
        "jakarta", "korea", "taiwan", "russia", "kazakhstan", "tajikistan", "syria",
        "iraq", "jordan", "palestine", "sudan", "yemen", "france", "belgium",
    )

    /**
     * The confidence of one computed time.
     *
     * @param methodId the calculation method
     * @param officialAdopted the user adopted their city's official timetable
     * @param highLatitude the place needs the high-latitude approximation
     */
    fun of(methodId: String, officialAdopted: Boolean, highLatitude: Boolean): TimeConfidence =
        when {
            officialAdopted -> TimeConfidence.CONFIRMED
            highLatitude -> TimeConfidence.ESTIMATED
            methodId.lowercase() in PUBLISHED -> TimeConfidence.COMPUTED
            else -> TimeConfidence.ESTIMATED
        }
}
