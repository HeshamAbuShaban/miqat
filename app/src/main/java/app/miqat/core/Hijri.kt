package app.miqat.core

import java.util.Calendar
import java.util.GregorianCalendar

/**
 * Hijri bridge — android.icu's Umm al-Qura Islamic calendar (available since
 * minSdk 24, dependency-free). Kept in its own object so the pure
 * [IslamicDays] logic stays JVM-testable without ICU.
 */
object Hijri {

    /** Today's IslamicDay (Umm al-Qura) for the device default timezone. */
    fun today(nowMillis: Long = System.currentTimeMillis()): IslamicDay {
        // Umm al-Qura selected via the ICU locale keyword (stable across API levels;
        // Android's bundled IslamicCalendar exposes no CalculationType API)
        val cal = android.icu.util.IslamicCalendar(
            android.icu.util.ULocale("en@calendar=islamic-umalqura"),
        )
        cal.setTimeInMillis(nowMillis)
        val g = GregorianCalendar().apply { timeInMillis = nowMillis }
        return IslamicDay(
            hYear = cal.get(android.icu.util.IslamicCalendar.YEAR),
            hMonth = cal.get(android.icu.util.IslamicCalendar.MONTH) + 1,
            hDay = cal.get(android.icu.util.IslamicCalendar.DAY_OF_MONTH),
            weekday = g.get(Calendar.DAY_OF_WEEK),
        )
    }

    /** Short "١٥ رمضان ١٤٤٧" style label (western digits kept for tabular clarity). */
    fun label(d: IslamicDay): String {
        val month = MONTHS_AR.getOrElse(d.hMonth - 1) { "" }
        return "${d.hDay} $month ${d.hYear}هـ"
    }

    val MONTHS_AR = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
    )
}
