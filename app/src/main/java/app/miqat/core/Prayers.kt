package app.miqat.core

/**
 * THE PRAYER VOCABULARY — one source for the six names, their keys and the
 * conversion from a fractional hour to minutes of day.
 *
 * Before this file the names were written by hand in the widget, the alarm
 * scheduler, the countdown service, the UI and the settings, and `minutesOf`
 * had two COPIES THAT DISAGREED: the Times screen rounded half up (the donor's
 * rule) while the widget truncated — so the widget could show a minute that
 * the app itself did not. Both now use the donor's round-half-up.
 */
object Prayers {

    val NAMES: List<String> = listOf(
        "الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء",
    )

    val KEYS: List<String> = listOf(
        "fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha",
    )

    /** UTC-independent, round-half-up, wrapped into [0, 1440). */
    fun minutesOf(hours: Double): Int {
        if (hours.isNaN()) return 0
        val total = (((hours + 0.5 / 60.0) % 24.0) + 24.0) % 24.0
        return (total * 60.0).toInt() % (24 * 60)
    }
}
