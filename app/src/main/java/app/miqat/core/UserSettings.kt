package app.miqat.core

import android.content.Context
import android.content.SharedPreferences

/**
 * User-adjust minutes per slot (donor `adjust_min_*` parity) + persisted
 * selected city — decisions.md 2026-11-10 (5). Plain SharedPreferences
 * (F-Droid clean). Adjusts are a USER OVERRIDE applied in the UI layer after
 * `PrayerTimes.compute`; the engine and its ihtiyat margins stay untouched,
 * and the R3a warning register surfaces the divergence (research-agenda R3).
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

object UserSettings {

    private const val FILE = "user_settings"
    private const val K_CITY = "city_v1"
    private const val K_ADJ = "adjust_min_"

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
}
