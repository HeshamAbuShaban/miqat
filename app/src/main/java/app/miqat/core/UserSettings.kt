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
    // --- urgency (the app must react as the appointed time approaches) ---
    val urgencyEnabled: Boolean = true,
    val urgencySoonMinutes: Int = 15,
    val urgencyImminentMinutes: Int = 5,
    // --- gaps (the time between one appointment and the next) ---
    val showGaps: Boolean = true,
    /** Which gap categories the user wants to see between the prayer rows. */
    val gapCategories: Set<String> = setOf("صلاة", "شمس", "عبادة", "ليل", "سنة"),
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
    private const val K_URGENCY_ON = "urgency_enabled"
    private const val K_URGENCY_SOON = "urgency_soon"
    private const val K_URGENCY_IMMINENT = "urgency_imminent"
    private const val K_SHOW_GAPS = "show_gaps"
    private const val K_GAP_CATEGORIES = "gap_categories"
    private const val K_USE_GPS = "use_gps"
    private const val K_GPS_LAT = "gps_lat"
    private const val K_GPS_LON = "gps_lon"

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
            urgencyEnabled = p.getBoolean(K_URGENCY_ON, true),
            urgencySoonMinutes = p.getInt(K_URGENCY_SOON, 15),
            urgencyImminentMinutes = p.getInt(K_URGENCY_IMMINENT, 5),
            showGaps = p.getBoolean(K_SHOW_GAPS, true),
            gapCategories = p.getStringSet(
                K_GAP_CATEGORIES, setOf("صلاة", "شمس", "عبادة", "ليل", "سنة"),
            ) ?: emptySet(),
        )
    }

    // --- GPS location override ---------------------------------------------

    /** The last confirmed device fix, or null when the user declined GPS. */
    fun gpsFix(context: Context): LocationFix? {
        val p = prefs(context)
        if (!p.getBoolean(K_USE_GPS, false)) return null
        val lat = p.getFloat(K_GPS_LAT, 0f).toDouble()
        val lon = p.getFloat(K_GPS_LON, 0f).toDouble()
        if (lat == 0.0 && lon == 0.0) return null
        val offset = java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis())
            .toDouble() / 3_600_000.0
        return LocationFix(lat, lon, offset, LocationSource.GPS)
    }

    fun saveGpsFix(context: Context, fix: LocationFix?) {
        val e = prefs(context).edit()
        if (fix == null) {
            e.putBoolean(K_USE_GPS, false)
        } else {
            e.putBoolean(K_USE_GPS, true)
                .putFloat(K_GPS_LAT, fix.latitude.toFloat())
                .putFloat(K_GPS_LON, fix.longitude.toFloat())
        }
        e.apply()
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
            .putBoolean(K_URGENCY_ON, d.urgencyEnabled)
            .putInt(K_URGENCY_SOON, d.urgencySoonMinutes)
            .putInt(K_URGENCY_IMMINENT, d.urgencyImminentMinutes)
            .putBoolean(K_SHOW_GAPS, d.showGaps)
            .putStringSet(K_GAP_CATEGORIES, d.gapCategories)
            .apply()
    }
}
