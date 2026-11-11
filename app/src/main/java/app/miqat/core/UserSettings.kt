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

/**
 * How much the HOME shows (the design council, 2026-11-11).
 *
 * The granular per-line switches stay, but the front door is a DENSITY: one
 * choice that says how much of the day the screen carries. «Too much is not a
 * restriction — but you get to configure it» is better served by three
 * densities than by twelve checkboxes.
 */
enum class HomeDensity(val labelAr: String, val noteAr: String) {
    COMPACT("مختصر", "البطل والشريط فقط"),
    BALANCED("متوازن", "البطل والشريط وآية اليوم والقبلة"),
    FULL("كامل", "كل ما في التطبيق على شاشة واحدة"),
    ;

    companion object {
        fun next(d: HomeDensity): HomeDensity = entries[(d.ordinal + 1) % entries.size]
    }
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
    // --- the indicators (Hesham: "too much isn't a restriction when it gives
    //     value — and you get to config it in the settings") ---
    /** The awareness strip at the top (now / the time that entered / the next). */
    val showAwarenessStrip: Boolean = true,
    /** The daylight-saving notice when the clock is about to change. */
    val showDstNotice: Boolean = true,
    /** The nearest moment of any kind on the home. */
    val showNearestMoment: Boolean = true,
    /** The calendar's day cells carry the Hijri day. */
    val calendarShowHijri: Boolean = true,
    /** The calendar's day cells carry the Gregorian day. */
    val calendarShowGregorian: Boolean = true,
    /** The calendar's day cells carry the event dot. */
    val calendarShowEvents: Boolean = true,
    /** The widget names the city — ONLY when the user opted in and the
     *  times actually come from that city (never under a GPS fix). */
    val widgetShowCity: Boolean = false,
    /** Which prayer the calendar's month table shows (0 = Fajr). */
    val calendarPrayer: Int = 0,
    /** The calendar's month table is visible. */
    val calendarShowTable: Boolean = true,
    /** The daily reminder line (a verse with its source). */
    val showDailyReminder: Boolean = true,
    /** The night markers line (sunrise, sunset, the last third). */
    val showNightMarkers: Boolean = true,
    /** The strip of the six prayer times. */
    val showPrayerStrip: Boolean = true,
    /** The line: «مرّ من وقت X» — the elapsed since the prayer that just came. */
    val showTimerElapsed: Boolean = true,
    /** The line: «إقامة X» — the second call, with its own countdown. */
    val showTimerIqama: Boolean = true,
    /** How much the home carries (one choice, instead of a dozen). */
    val homeDensity: HomeDensity = HomeDensity.BALANCED,
    /** What the calendar is organized around (the Gregorian, the Hijri, or
     *  the acts of worship). */
    val calendarContext: String = "HIJRI",
    /** The identity the user chose (the accent). */
    val accent: String = "GOLD",
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
    private const val K_SHOW_AWARENESS = "show_strip"
    private const val K_SHOW_DST = "show_dst"
    private const val K_SHOW_NEAREST = "show_nearest"
    private const val K_CAL_HIJRI = "cal_hijri"
    private const val K_CAL_GREG = "cal_greg"
    private const val K_CAL_EVENTS = "cal_events"
    private const val K_WIDGET_CITY = "widget_show_city"
    private const val K_CAL_PRAYER = "cal_prayer"
    private const val K_CAL_TABLE = "cal_table"
    private const val K_SHOW_REMINDER = "show_reminder"
    private const val K_SHOW_NIGHT = "show_night"
    private const val K_SHOW_STRIP = "show_prayer_strip"
    private const val K_TIMER_ELAPSED = "timer_elapsed"
    private const val K_TIMER_IQAMA = "timer_iqama"
    private const val K_HOME_DENSITY = "home_density"
    private const val K_CAL_CONTEXT = "calendar_context"
    private const val K_ACCENT = "accent"
    private const val K_IMSAK_LEAD = "imsak_lead_minutes"
    private const val K_OFFICIAL = "official_adopted"
    private const val K_USE_GPS = "use_gps"
    private const val K_GPS_LAT = "gps_lat"
    private const val K_GPS_LON = "gps_lon"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // --- selected city -----------------------------------------------------

    fun saveCity(context: Context, city: City) {
        prefs(context).edit().putString(
            K_CITY, listOf(
                city.en, city.ar, city.countryCode, city.lat, city.lon, city.utcOffset,
                city.calcMethod, city.zone, city.zoneId?.toString() ?: "",
            ).joinToString("\u0001")
        ).apply()
    }

    fun city(context: Context): City? {
        val s = prefs(context).getString(K_CITY, null) ?: return null
        val p = s.split("\u0001")
        if (p.size < 7) return null
        return City(
            en = p[0], ar = p[1], countryCode = p[2],
            lat = p[3].toDoubleOrNull() ?: return null,
            lon = p[4].toDoubleOrNull() ?: return null,
            utcOffset = p[5].toDoubleOrNull() ?: return null,
            calcMethod = p[6],
            zone = p.getOrElse(7) { "" },
            zoneId = p.getOrElse(8) { "" }.toIntOrNull(),
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

    /** The imsak lead (minutes before the Fajr) — OUR convention, persisted. */
    fun imsakLeadMinutes(context: Context): Int =
        prefs(context).getInt(K_IMSAK_LEAD, 15)

    fun saveImsakLeadMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(K_IMSAK_LEAD, minutes).apply()
    }

    /** Has the user adopted their city's official timetable? */
    fun officialAdopted(context: Context): Boolean =
        prefs(context).getBoolean(K_OFFICIAL, false)

    fun saveOfficialAdopted(context: Context, adopted: Boolean) {
        prefs(context).edit().putBoolean(K_OFFICIAL, adopted).apply()
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
            showAwarenessStrip = p.getBoolean(K_SHOW_AWARENESS, true),
            showDstNotice = p.getBoolean(K_SHOW_DST, true),
            showNearestMoment = p.getBoolean(K_SHOW_NEAREST, true),
            calendarShowHijri = p.getBoolean(K_CAL_HIJRI, true),
            calendarShowGregorian = p.getBoolean(K_CAL_GREG, true),
            calendarShowEvents = p.getBoolean(K_CAL_EVENTS, true),
            widgetShowCity = p.getBoolean(K_WIDGET_CITY, false),
            calendarPrayer = p.getInt(K_CAL_PRAYER, 0),
            calendarShowTable = p.getBoolean(K_CAL_TABLE, true),
            showDailyReminder = p.getBoolean(K_SHOW_REMINDER, true),
            showNightMarkers = p.getBoolean(K_SHOW_NIGHT, true),
            showPrayerStrip = p.getBoolean(K_SHOW_STRIP, true),
            showTimerElapsed = p.getBoolean(K_TIMER_ELAPSED, true),
            showTimerIqama = p.getBoolean(K_TIMER_IQAMA, true),
            calendarContext = p.getString(K_CAL_CONTEXT, "HIJRI") ?: "HIJRI",
            accent = p.getString(K_ACCENT, "GOLD") ?: "GOLD",
            homeDensity = runCatching {
                HomeDensity.valueOf(p.getString(K_HOME_DENSITY, HomeDensity.BALANCED.name) ?: "")
            }.getOrDefault(HomeDensity.BALANCED),
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
            .putBoolean(K_SHOW_AWARENESS, d.showAwarenessStrip)
            .putBoolean(K_SHOW_DST, d.showDstNotice)
            .putBoolean(K_SHOW_NEAREST, d.showNearestMoment)
            .putBoolean(K_CAL_HIJRI, d.calendarShowHijri)
            .putBoolean(K_CAL_GREG, d.calendarShowGregorian)
            .putBoolean(K_CAL_EVENTS, d.calendarShowEvents)
            .putBoolean(K_WIDGET_CITY, d.widgetShowCity)
            .putInt(K_CAL_PRAYER, d.calendarPrayer)
            .putBoolean(K_CAL_TABLE, d.calendarShowTable)
            .putBoolean(K_SHOW_REMINDER, d.showDailyReminder)
            .putBoolean(K_SHOW_NIGHT, d.showNightMarkers)
            .putBoolean(K_SHOW_STRIP, d.showPrayerStrip)
            .putBoolean(K_TIMER_ELAPSED, d.showTimerElapsed)
            .putBoolean(K_TIMER_IQAMA, d.showTimerIqama)
            .putString(K_HOME_DENSITY, d.homeDensity.name)
            .putString(K_CAL_CONTEXT, d.calendarContext)
            .putString(K_ACCENT, d.accent)
            .apply()
    }
}
