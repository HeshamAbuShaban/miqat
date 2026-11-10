package app.miqat.core

import android.content.Context
import android.content.SharedPreferences

/**
 * User-adjust minutes per slot (donor `adjust_min_*` parity) + persisted
 * user preferences — decisions.md 2026-11-10 (5). Plain SharedPreferences
 * (F-Droid clean, no DataStore). Adjusts are a USER OVERRIDE applied in the UI
 * layer after `PrayerTimes.compute`; the engine and its ihtiyat margins stay
 * untouched, and the R3a warning register surfaces the divergence.
 */
data class PrayerAdjusts(
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0,
) {
    fun forIndex(i: Int): Int = when (i) {
        0 -> fajr; 1 -> sunrise; 2 -> dhuhr; 3 -> asr; 4 -> maghrib; else -> isha
    }

    fun withIndex(i: Int, value: Int): PrayerAdjusts = when (i) {
        0 -> copy(fajr = value); 1 -> copy(sunrise = value); 2 -> copy(dhuhr = value)
        3 -> copy(asr = value); 4 -> copy(maghrib = value); else -> copy(isha = value)
    }

    val hasAny: Boolean get() = listOf(fajr, sunrise, dhuhr, asr, maghrib, isha).any { it != 0 }
}

/** Display preferences (R6/R7): theme, digit system, clock, Hijri offset. */
data class DisplayPrefs(
    val darkTheme: Boolean = true,
    val arabicIndicDigits: Boolean = true,
    val use12h: Boolean = false,
    val hijriOffsetDays: Int = 0,
    /** Manual UTC-offset override in minutes (donor `time_utc_offset_mode`). */
    val manualUtcOffsetMinutes: Int? = null,
    /** Global calc-method override; null = the city's own default method. */
    val methodOverride: String? = null,
)

object UserSettings {

    private const val FILE = "user_settings"
    private const val K_CITY = "city_v1"
    private const val K_ADJ = "adjust_min_"
    private const val K_DARK = "display_dark"
    private const val K_AR_DIGITS = "display_arabic_digits"
    private const val K_USE_12H = "display_use_12h"
    private const val K_HIJRI_OFFSET = "hijri_offset"
    private const val K_UTC_OFFSET_MODE = "utc_offset_mode"     // "auto" | "manual"
    private const val K_UTC_OFFSET_MIN = "utc_offset_minutes"
    private const val K_METHOD_OVERRIDE = "method_override"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // --- selected city -----------------------------------------------------

    fun saveCity(context: Context, city: City) {
        prefs(context).edit().putString(
            K_CITY, listOf(city.en, city.ar, city.countryCode, city.lat, city.lon, city.utcOffset, city.calcMethod)
                .joinToString("\u0001")
        ).apply()
    }

    fun city(context: Context): City? {
        val s = prefs(context).getString(K_CITY, null) ?: return null
        val p = s.split("\u0001")
        if (p.size != 7) return null
        return City(
            en = p[0], ar = p[1], countryCode = p[2],
            lat = p[3].toDoubleOrNull() ?: return null,
            lon = p[4].toDoubleOrNull() ?: return null,
            utcOffset = p[5].toDoubleOrNull() ?: return null,
            calcMethod = p[6],
        )
    }

    // --- per-prayer manual adjusts (minutes) --------------------------------

    fun adjusts(context: Context): PrayerAdjusts {
        val p = prefs(context)
        fun g(k: String) = p.getInt(K_ADJ + k, 0)
        return PrayerAdjusts(
            fajr = g("fajr"), sunrise = g("shorook"), dhuhr = g("duhr"),
            asr = g("asr"), maghrib = g("maghreb"), isha = g("ichaa"),
        )
    }

    fun saveAdjusts(context: Context, a: PrayerAdjusts) {
        prefs(context).edit()
            .putInt(K_ADJ + "fajr", a.fajr).putInt(K_ADJ + "shorook", a.sunrise)
            .putInt(K_ADJ + "duhr", a.dhuhr).putInt(K_ADJ + "asr", a.asr)
            .putInt(K_ADJ + "maghreb", a.maghrib).putInt(K_ADJ + "ichaa", a.isha)
            .apply()
    }

    // --- display preferences ------------------------------------------------

    fun display(context: Context): DisplayPrefs {
        val p = prefs(context)
        val mode = p.getString(K_UTC_OFFSET_MODE, "auto")
        return DisplayPrefs(
            darkTheme = p.getBoolean(K_DARK, true),
            arabicIndicDigits = p.getBoolean(K_AR_DIGITS, true),
            use12h = p.getBoolean(K_USE_12H, false),
            hijriOffsetDays = p.getInt(K_HIJRI_OFFSET, 0),
            manualUtcOffsetMinutes = if (mode == "manual") p.getInt(K_UTC_OFFSET_MIN, 0) else null,
            methodOverride = p.getString(K_METHOD_OVERRIDE, null),
        )
    }

    fun saveDisplay(context: Context, d: DisplayPrefs) {
        prefs(context).edit()
            .putBoolean(K_DARK, d.darkTheme)
            .putBoolean(K_AR_DIGITS, d.arabicIndicDigits)
            .putBoolean(K_USE_12H, d.use12h)
            .putInt(K_HIJRI_OFFSET, d.hijriOffsetDays)
            .putString(K_UTC_OFFSET_MODE, if (d.manualUtcOffsetMinutes == null) "auto" else "manual")
            .putInt(K_UTC_OFFSET_MIN, d.manualUtcOffsetMinutes ?: 0)
            .putString(K_METHOD_OVERRIDE, d.methodOverride)
            .apply()
    }
}
