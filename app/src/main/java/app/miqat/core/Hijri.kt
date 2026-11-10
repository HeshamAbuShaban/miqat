package app.miqat.core

import java.util.Calendar
import java.util.GregorianCalendar

/**
 * Hijri bridge — android.icu's Umm al-Qura Islamic calendar (available since
 * minSdk 24, dependency-free). Kept in its own object so the pure
 * [IslamicDays] logic stays JVM-testable without ICU.
 */
object Hijri {

    /** Today's IslamicDay (Umm al-Qura) for the device default timezone.
     *  [offsetDays] = the user's manual sighting adjustment (donor
     *  `hijri_offset` / `adjust_hijri` parity, ±3 days). */
    fun today(nowMillis: Long = System.currentTimeMillis(), offsetDays: Int = 0): IslamicDay {
        // Umm al-Qura selected via the ICU locale keyword (stable across API levels;
        // Android's bundled IslamicCalendar exposes no CalculationType API)
        val cal = android.icu.util.IslamicCalendar(
            android.icu.util.ULocale("en@calendar=islamic-umalqura"),
        )
        val shifted = nowMillis + offsetDays * 86_400_000L
        cal.setTimeInMillis(shifted)
        val g = GregorianCalendar().apply { timeInMillis = shifted }
        return IslamicDay(
            hYear = cal.get(android.icu.util.IslamicCalendar.YEAR),
            hMonth = cal.get(android.icu.util.IslamicCalendar.MONTH) + 1,
            hDay = cal.get(android.icu.util.IslamicCalendar.DAY_OF_MONTH),
            weekday = g.get(Calendar.DAY_OF_WEEK),
        )
    }

    /** The IslamicDay for an arbitrary Gregorian date (calendar grid). */
    fun at(y: Int, m: Int, d: Int): IslamicDay {
        val g = GregorianCalendar(y, m - 1, d)
        return today(g.timeInMillis)
    }

    /** Short "١٥ رمضان ١٤٤٧" style label (western digits kept for tabular clarity). */
    fun label(d: IslamicDay, arabicIndic: Boolean = false): String {
        val month = MONTHS_AR.getOrElse(d.hMonth - 1) { "" }
        val day = if (arabicIndic) NumFmt.digits("${d.hDay}", true) else "${d.hDay}"
        val year = if (arabicIndic) NumFmt.digits("${d.hYear}", true) else "${d.hYear}"
        return "$day $month ${year}هـ"
    }

    val MONTHS_AR = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
    )
}
