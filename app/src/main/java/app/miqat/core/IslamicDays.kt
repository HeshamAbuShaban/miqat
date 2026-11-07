package app.miqat.core

/**
 * The S9 layer — everything that has a miqat in the Islamic day/year, computed
 * offline from a Hijri date. Pure logic: the caller bridges from the calendar
 * system (android.icu Umm al-Qura in [Hijri], minSdk 24) and passes plain ints,
 * so this stays JVM-testable.
 *
 * weekday: 1=Sunday … 7=Saturday (java.util.Calendar.DAY_OF_WEEK convention).
 */
data class IslamicDay(val hYear: Int, val hMonth: Int, val hDay: Int, val weekday: Int)

object IslamicDays {

    const val WHITE_DAYS_MIN = 13
    const val WHITE_DAYS_MAX = 15

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
     * @return (title, subtitle) in Arabic
     */
    fun todayCard(d: IslamicDay): Pair<String, String>? {
        // Eids first: they outrange the fasting nudges (fasting is forbidden).
        when {
            isEidAlFitr(d) -> return Pair("عيد الفطر المبارك", "تقبّل الله — لا صيام اليوم")
            isEidAlAdha(d) -> return Pair("عيد الأضحى المبارك", "أيام التشريق — لا صيام اليوم")
            isRamadan(d) -> return Pair("رمضان مبارك", "شهر الصيام — من صامه إيماناً واحتساباً غُفر له ما تقدم من ذنبه")
        }
        if (isDayOfArafah(d)) return Pair("يوم عرفة", "صيامه كفّارة سنتين — لغير الحاجّ")
        if (isAshura(d)) return Pair("يوم عاشوراء", "صيامه كفّارة سنة — سنّة نبوية")
        if (isTasuaa(d)) return Pair("يوم تاسوعاء", "استعدّ لعاشوراء — صيامه مستحبّ")
        if (isWhiteDays(d)) return Pair("أيام البيض", "١٣–١٥ — صيامها كصيام السنة كلها")
        if (isSixOfShawwal(d)) return Pair("ستّ من شوال", "من صام رمضان ثم أتبعه بستّ من شوال...")
        if (isMondayThursday(d)) return Pair("صيام الاثنين والخميس", "الأعمال تُعرض كل خميس واثنين")
        if (isEveOfRamadan(d)) return Pair("غداً رمضان", "هيّأ نفسك — ليلة نزول القرآن")
        return null
    }
}
