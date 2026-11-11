package app.miqat.core

/**
 * The S9 layer — everything that has a miqat in the Islamic day/year, computed
 * offline from a Hijri date. Pure logic: the caller bridges from the calendar
 * system (android.icu Umm al-Qura in [Hijri], minSdk 24) and passes plain ints,
 * so this stays JVM-testable.
 *
 * weekday: 1=Sunday … 7=Saturday (java.util.Calendar.DAY_OF_WEEK convention).
 *
 * THE DISCIPLINE (the models council, 2026-11-11): this file states TIMES and
 * quotes NARRATIONS with their numbers. It never issues a ruling — no
 * «مستحبّ», no «لا صيام اليوم», no «كفّارة سنتين». Every line that names a
 * fasting day carries the text it rests on, so the user can check it.
 */
data class IslamicDay(val hYear: Int, val hMonth: Int, val hDay: Int, val weekday: Int)

object IslamicDays {

    const val WHITE_DAYS_MIN = 13
    const val WHITE_DAYS_MAX = 15

    /**
     * The chip that must sit beside every Hijri date: the app's calendar is
     * TABULAR (Umm al-Qura), not a sighting — so it says so.
     */
    const val HIJRI_APPROX = "تاريخ هجري تقديري — يمكن ضبطه"

    fun isWhiteDays(d: IslamicDay): Boolean = d.hDay in WHITE_DAYS_MIN..WHITE_DAYS_MAX

    /** الاثنين والخميس — the Prophet's weekly fast. */
    fun isMondayThursday(d: IslamicDay): Boolean = d.weekday == 2 || d.weekday == 5

    fun isRamadan(d: IslamicDay): Boolean = d.hMonth == 9

    fun isDayOfArafah(d: IslamicDay): Boolean = d.hMonth == 12 && d.hDay == 9

    fun isTasuaa(d: IslamicDay): Boolean = d.hMonth == 1 && d.hDay == 9

    fun isAshura(d: IslamicDay): Boolean = d.hMonth == 1 && d.hDay == 10

    /** ست من شوال — 2..7 Shawwal (any six days; this window is the customary set). */
    fun isSixOfShawwal(d: IslamicDay): Boolean = d.hMonth == 10 && d.hDay in 2..7

    fun isEidAlFitr(d: IslamicDay): Boolean = d.hMonth == 10 && d.hDay in 1..3

    fun isEidAlAdha(d: IslamicDay): Boolean = d.hMonth == 12 && d.hDay in 10..13

    fun isEveOfRamadan(d: IslamicDay): Boolean = d.hMonth == 8 && d.hDay in 29..30

    /**
     * Today's contextual card, highest priority first. Returns null on a plain
     * day — the home stays quiet unless there is something worth acting on.
     *
     * Every subtitle is either a quoted narration WITH its number, or a
     * statement about the app's own list. Never a ruling.
     * @return (title, subtitle) in Arabic
     */
    fun todayCard(d: IslamicDay): Pair<String, String>? {
        when {
            // the Eids first: they outrank the fasting nudges
            isEidAlFitr(d) -> return Pair(
                "عيد الفطر المبارك",
                "نَهَى النبي ﷺ عن صوم يوم الفطر — رواه البخاري ١٩٩٢",
            )
            isEidAlAdha(d) -> return Pair(
                "عيد الأضحى المبارك",
                "أيام التشريق — أيام أكل وشرب — رواه مسلم ١١٤١",
            )
            isRamadan(d) -> return Pair(
                "رمضان مبارك",
                "﴿كُلُوا وَاشْرَبُوا حَتَّى يَتَبَيَّنَ لَكُمُ الْخَيْطُ الْأَبْيَضُ… مِنَ الْفَجْرِ﴾ — سورة البقرة ١٨٧",
            )
        }
        if (isDayOfArafah(d)) return Pair(
            "يوم عرفة",
            "صيام يوم عرفة — رواه مسلم ١١٦٢ · والحاجّ لا يصومه — رواه الترمذي ٧٥١",
        )
        if (isAshura(d)) return Pair(
            "يوم عاشوراء",
            "صام النبي ﷺ عاشوراء — رواه البخاري ٢٠٠٣",
        )
        if (isTasuaa(d)) return Pair(
            "يوم تاسوعاء",
            "من الأيام البيض ١٣–١٥ — رواه النسائي ٢٤٢٢",
        )
        if (isWhiteDays(d)) return Pair(
            "أيام البيض",
            "ثلاثة أيام من كل شهر — رواه النسائي ٢٤٢٢",
        )
        if (isSixOfShawwal(d)) return Pair(
            "ستّ من شوال",
            "من صام رمضان ثم أتبعه بستّ من شوال — رواه مسلم ١١٦٤",
        )
        if (isMondayThursday(d)) return Pair(
            "صيام الاثنين والخميس",
            "﴿كَانَ النَّبِيُّ ﷺ يَتَحَرَّى صَوْمَ الِاثْنَيْنِ وَالْخَمِيسِ﴾ — رواه الترمذي ٧٤٥",
        )
        if (isEveOfRamadan(d)) return Pair(
            "غداً رمضان",
            HIJRI_APPROX,
        )
        return null
    }
}
