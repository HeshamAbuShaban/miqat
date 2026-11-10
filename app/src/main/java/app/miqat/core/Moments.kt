package app.miqat.core

/**
 * The unified MOMENT registry (decisions 2026-11-10 (7)).
 *
 * Every appointed time in the app — prayers, sunrise, worship windows, night
 * markers, sunnah windows — is a [Moment] on ONE timeline (absolute minutes
 * from today's local midnight). Because everything is a moment:
 *  - the gap between any two appointments is a subtraction (Hesham's brief:
 *    «الوقت بين كل صلاة واخرى بائن وواضح»),
 *  - an interval between ANY two categories is computable on demand
 *    («احسب الوقت بين موعدين اثنين ايا كان الصنف»).
 *
 * Pure logic — no Android types — so it stays JVM-testable.
 */
enum class MomentCategory(val labelAr: String) {
    PRAYER("صلاة"),
    SUN("شمس"),
    WORSHIP("عبادة"),
    NIGHT("ليل"),
    SUNNAH("سنة"),
}

/** One appointed moment on the day's timeline. */
data class Moment(
    val kind: String,
    val labelAr: String,
    val category: MomentCategory,
    /** Absolute minutes from today's local midnight (may exceed 1440 = tomorrow). */
    val minute: Int,
) {
    /** Clock time of day (0..1439) for display. */
    val minuteOfDay: Int get() = ((minute % 1440) + 1440) % 1440
}

object Moments {

    const val FAJR = "fajr"
    const val SUNRISE = "sunrise"
    const val DHUHR = "dhuhr"
    const val ASR = "asr"
    const val MAGHRIB = "maghrib"
    const val ISHA = "isha"
    const val DUHA_START = "duha_start"
    const val DUHA_END = "duha_end"
    const val MIDNIGHT = "midnight"
    const val LAST_THIRD = "last_third"
    const val SAHAR_START = "sahar_start"
    const val SLEEP_START = "sleep_start"
    const val SLEEP_END = "sleep_end"
    const val SUHUR_START = "suhur_start"
    const val QAYLULA_START = "qaylula_start"
    const val QAYLULA_END = "qaylula_end"
    const val MORNING_READ = "morning_read"
    const val EVENING_READ = "evening_read"

    /** The prayers + sunrise of one day, as moments. */
    fun prayers(
        times: LocalPrayerTimes,
        labels: List<String>,
        minuteOf: (Double) -> Int,
    ): List<Moment> = listOf(
        Moment(FAJR, labels[0], MomentCategory.PRAYER, minuteOf(times.fajr)),
        Moment(SUNRISE, labels[1], MomentCategory.SUN, minuteOf(times.sunrise)),
        Moment(DHUHR, labels[2], MomentCategory.PRAYER, minuteOf(times.dhuhr)),
        Moment(ASR, labels[3], MomentCategory.PRAYER, minuteOf(times.asr)),
        Moment(MAGHRIB, labels[4], MomentCategory.PRAYER, minuteOf(times.maghrib)),
        Moment(ISHA, labels[5], MomentCategory.PRAYER, minuteOf(times.isha)),
    )

    /**
     * The FULL timeline for a day: prayers, worship windows, night markers and
     * the sunnah windows. [tomorrowFajr] is tomorrow's Fajr (as local minutes
     * of TODAY + 1440) so the night spans midnight correctly.
     */
    fun timeline(
        times: LocalPrayerTimes,
        tomorrowFajr: Double,
        labels: List<String>,
        minuteOf: (Double) -> Int,
    ): List<Moment> {
        val prayers = prayers(times, labels, minuteOf)
        val fajrTomorrow = minuteOf(tomorrowFajr) + 1440
        val maghrib = minuteOf(times.maghrib)
        val nightLength = fajrTomorrow - maghrib
        val duhaStart = minuteOf(times.sunrise) + 20
        val duhaEnd = minuteOf(times.dhuhr) - 20

        return (prayers + listOf(
            Moment(DUHA_START, "وقت الضحى", MomentCategory.WORSHIP, duhaStart),
            Moment(DUHA_END, "نهاية الضحى", MomentCategory.WORSHIP, duhaEnd),
            Moment(MIDNIGHT, "منتصف الليل", MomentCategory.NIGHT, maghrib + nightLength / 2),
            Moment(LAST_THIRD, "الثلث الأخير", MomentCategory.NIGHT, maghrib + nightLength * 2 / 3),
            Moment(SAHAR_START, "وقت الأسحار", MomentCategory.NIGHT, fajrTomorrow - nightLength / 6),
            // sunnah windows (times only — the texts are sourced separately)
            Moment(SLEEP_START, "وقت النوم", MomentCategory.SUNNAH, minuteOf(times.isha)),
            Moment(SLEEP_END, "الاستيقاظ", MomentCategory.SUNNAH, fajrTomorrow),
            Moment(SUHUR_START, "وقت السحور", MomentCategory.SUNNAH, fajrTomorrow - nightLength / 6),
            Moment(QAYLULA_START, "وقت القيلولة", MomentCategory.SUNNAH, minuteOf(times.dhuhr)),
            Moment(QAYLULA_END, "نهاية القيلولة", MomentCategory.SUNNAH, minuteOf(times.asr)),
            Moment(MORNING_READ, "قراءة الفجر", MomentCategory.SUNNAH, minuteOf(times.sunrise)),
            Moment(EVENING_READ, "قراءة المغرب", MomentCategory.SUNNAH, minuteOf(times.maghrib)),
        )).sortedBy { it.minute }
    }

    /** The gaps between consecutive moments (the «المدد بين المواقيت»). */
    fun gaps(moments: List<Moment>): List<Gap> {
        val sorted = moments.sortedBy { it.minute }
        return sorted.zipWithNext { a, b -> Gap(a, b, b.minute - a.minute) }
    }

    /** The duration between any two moments — the on-demand interval tool. */
    fun interval(from: Moment, to: Moment): Int {
        var d = to.minute - from.minute
        if (d < 0) d += 1440
        return d
    }

    /** How long until a moment (from now, in minutes). */
    fun until(moment: Moment, nowMinute: Int): Int {
        var d = moment.minute - nowMinute
        if (d < 0) d += 1440
        return d
    }
}

/** A span between two consecutive moments. */
data class Gap(val from: Moment, val to: Moment, val minutes: Int)

/**
 * URGENCY (decisions 2026-11-10 (7)) — the app must react as the appointed
 * time approaches (Hesham: «اذا أوشك الوقت على النفاذ فتجعله مختلف عن حاله
 * الطبيعي … التفكير بمنطق وذكاء ينفع كثيرا»).
 *
 * One shared model, applied everywhere the remaining time appears, so the
 * whole app says the same thing at the same urgency.
 */
enum class Urgency(val labelAr: String) {
    CALM(""),
    SOON("اقترب الموعد"),
    IMMINENT("حان وقت الصلاة"),
    NOW("حان الآن"),
}

object UrgencyRules {
    /** Default thresholds (minutes). */
    const val DEFAULT_SOON = 15
    const val DEFAULT_IMMINENT = 5

    fun of(
        remainingSeconds: Long,
        soonMinutes: Int = DEFAULT_SOON,
        imminentMinutes: Int = DEFAULT_IMMINENT,
    ): Urgency = when {
        remainingSeconds <= 0L -> Urgency.NOW
        remainingSeconds <= imminentMinutes * 60L -> Urgency.IMMINENT
        remainingSeconds <= soonMinutes * 60L -> Urgency.SOON
        else -> Urgency.CALM
    }
}
