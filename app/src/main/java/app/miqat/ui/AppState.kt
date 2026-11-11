package app.miqat.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.miqat.core.CalcMethod
import app.miqat.core.City
import app.miqat.core.AlarmScheduler
import app.miqat.core.CityStore
import app.miqat.core.DisplayPrefs
import app.miqat.core.LocalPrayerTimes
import app.miqat.core.Location
import app.miqat.core.MethodTable
import app.miqat.core.NotificationPrefs
import app.miqat.core.NotificationPrefsStore
import app.miqat.core.PrayerAdjusts
import app.miqat.core.PrayerTimes
import app.miqat.core.LocationFix
import app.miqat.core.MiqatData
import app.miqat.core.UserSettings
import java.util.Calendar
import java.util.TimeZone

/**
 * The app's single source of truth (S5 v2 architecture).
 *
 * Everything the user can change lives here and is persisted immediately, so a
 * screen is a pure function of state + route. No screen owns a preference, no
 * preference is duplicated in two places — that was the v0 slop.
 */
class AppState(
    context: Context,
    /**
     * Arm the prayer alarms on construction. The app wants this; the widget
     * (which ticks every minute) does NOT — it would re-arm the alarms on
     * every tick.
     */
    private val armAlarms: Boolean = true,
) {

    private val appContext = context.applicationContext

    var city by mutableStateOf(CityStore.MAKKAH); private set
    var gpsFix by mutableStateOf<LocationFix?>(null); private set
    var adjusts by mutableStateOf(PrayerAdjusts()); private set
    var display by mutableStateOf(DisplayPrefs()); private set
    var notifications by mutableStateOf(NotificationPrefs()); private set

    /** True while the persisted state is still being read (first frame renders
     *  the Makkah constant so the UI never blocks). */
    var loaded by mutableStateOf(false); private set

    init {
        val saved = UserSettings.city(appContext)
        if (saved != null) city = saved
        adjusts = UserSettings.adjusts(appContext)
        display = UserSettings.display(appContext)
        notifications = NotificationPrefsStore.load(appContext)
        gpsFix = UserSettings.gpsFix(appContext)
        loaded = true
        // arm the alarms for the appointed times (S3)
        if (armAlarms) AlarmScheduler.reschedule(appContext)
    }

    // --- mutations (persist immediately) -----------------------------------

    /**
     * Picking a city is an explicit choice: it TURNS THE GPS OFF, so the
     * picked place — not a stale device fix — is what the times are computed
     * from. (The bug: a device fix and the city's offset were mixed, giving
     * nonsense times.)
     */
    fun selectCity(c: City) {
        city = c
        gpsFix = null
        UserSettings.saveCity(appContext, c)
        UserSettings.saveGpsFix(appContext, null)
        AlarmScheduler.reschedule(appContext)
        notifyWidgets()
    }

    fun updateAdjusts(a: PrayerAdjusts) {
        adjusts = a
        UserSettings.saveAdjusts(appContext, a)
        AlarmScheduler.reschedule(appContext)
        notifyWidgets()
    }

    fun updateDisplay(d: DisplayPrefs) {
        display = d
        UserSettings.saveDisplay(appContext, d)
        AlarmScheduler.reschedule(appContext)
        notifyWidgets()
    }

    fun updateNotifications(n: NotificationPrefs) {
        notifications = n
        NotificationPrefsStore.save(appContext, n)
        AlarmScheduler.reschedule(appContext)
        notifyWidgets()
    }

    /** The home-screen widget is a view of THIS state — it must never lag. */
    private fun notifyWidgets() {
        try {
            app.miqat.ui.widget.MiqatWidgetProvider.refresh(appContext)
        } catch (e: Exception) {
            // a widget must never break the app
        }
    }

    // --- derived -----------------------------------------------------------

    /** Effective calculation method (user override wins over the city's). */
    val method: CalcMethod get() = MethodTable[display.methodOverride ?: city.calcMethod]

    /**
     * The UTC offset in force at [epochMillis] — DST included, from the
     * bundled timezone table (the S2 "own tz table"), unless the user set a
     * manual override.
     */
    /**
     * The donor's key for the city's zone — from the city row, or resolved by
     * the IANA name (the row's key is the fast path; the lookup is the truth).
     */
    val effectiveZoneId: Int?
        get() = city.zoneId ?: MiqatData.zoneIdFor(appContext, city.zone)

    /**
     * The UTC offset in force at [epochMillis] — taken from the SAME source as
     * the coordinates: the device's own zone when the GPS fix is in use, the
     * city's zone (with its DST rules) when the city is. Never mixed.
     */
    fun utcOffsetAt(epochMillis: Long): Double {
        display.manualUtcOffsetMinutes?.let { return it / 60.0 }
        if (gpsFix != null) return deviceOffsetAt(epochMillis)
        val zoneId = effectiveZoneId
        if (zoneId != null) {
            val fromTable = MiqatData.utcOffsetHours(appContext, zoneId, epochMillis)
            if (fromTable != null) return fromTable
        }
        return city.utcOffset
    }

    /** The device's own UTC offset at an instant (the zone of a GPS fix). */
    private fun deviceOffsetAt(epochMillis: Long): Double =
        java.util.TimeZone.getDefault().getOffset(epochMillis) / 3_600_000.0

    /** The offset right now (the common case). */
    val utcOffset: Double get() = utcOffsetAt(System.currentTimeMillis())

    /** How long before Fajr the fast begins (the imsak lead) — a setting. */
    var imsakLeadMinutes: Int = 15

    /**
     * How much the app may claim about the times it shows (gate 0.5): the
     * method, whether the user adopted an official timetable, and whether the
     * place needs the high-latitude approximation.
     */
    fun confidence(): app.miqat.core.TimeConfidence = app.miqat.core.Confidence.of(
        method.id,
        officialAdopted = officialAdopted,
        highLatitude = kotlin.math.abs(city.lat) >= 45.0 &&
            method.highLatRule != app.miqat.core.HighLatitudeRule.NONE,
    )

    /** True once the user has adopted their city's official timetable. */
    var officialAdopted: Boolean = false
        private set

    /** Mark the times as the city's official ones (the verification screen). */
    fun markOfficial(adopted: Boolean) {
        officialAdopted = adopted
    }

    /** Is daylight saving in force right now for the picked city? */
    val isDst: Boolean
        get() = effectiveZoneId?.let { MiqatData.isDst(appContext, it, System.currentTimeMillis()) } ?: false

    /**
     * The fix the times are computed from: the device GPS fix when the user
     * allowed it, otherwise the city they picked. The state of this decision is
     * never hidden — [confidence] says plainly how much to trust the result.
     */
    val locationFix: app.miqat.core.LocationFix
        get() = gpsFix
            ?: app.miqat.core.LocationFix(
                city.lat, city.lon, utcOffsetAt(System.currentTimeMillis()),
                app.miqat.core.LocationSource.MANUAL,
            )

    /** Is the device's own location the source (not the picked city)? */
    val usingGps: Boolean get() = gpsFix != null

    val confidence: app.miqat.core.TimeConfidence get() = locationFix.confidence

    fun location(epochMillis: Long = System.currentTimeMillis()): Location {
        // the coordinates AND the offset must come from the same place
        val effective = app.miqat.core.LocationRules.effective(
            city = city,
            cityOffsetHours = utcOffsetAt(epochMillis),
            gps = gpsFix,
            deviceOffsetHours = deviceOffsetAt(epochMillis),
        )
        return Location(effective.latitude, effective.longitude, effective.offsetHours)
    }

    /** Refresh the device fix (called after the permission is granted). */
    fun refreshGps() {
        val fix = app.miqat.core.LocationProvider.lastKnown(appContext)
        gpsFix = fix
        UserSettings.saveGpsFix(appContext, fix)
    }

    /** Drop the GPS fix and go back to the picked city (with the warning). */
    fun clearGps() {
        gpsFix = null
        UserSettings.saveGpsFix(appContext, null)
    }

    /** Engine times for a date, WITHOUT the user's manual adjusts. */
    fun rawTimes(year: Int, month: Int, day: Int): LocalPrayerTimes =
        PrayerTimes.compute(year, month, day, location(epochOfLocalNoon(year, month, day)), method)

    /** The epoch of local noon on a date — the reference point for the offset. */
    private fun epochOfLocalNoon(year: Int, month: Int, day: Int): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, day, 12, 0, 0)
        }
        return utc.timeInMillis
    }

    /** Engine times + the user's manual minute-adjusts (what the UI shows). */
    fun times(year: Int, month: Int, day: Int): LocalPrayerTimes {
        val t = rawTimes(year, month, day)
        return LocalPrayerTimes(
            fajr = t.fajr + adjusts.fajr / 60.0,
            sunrise = t.sunrise + adjusts.sunrise / 60.0,
            dhuhr = t.dhuhr + adjusts.dhuhr / 60.0,
            asr = t.asr + adjusts.asr / 60.0,
            maghrib = t.maghrib + adjusts.maghrib / 60.0,
            isha = t.isha + adjusts.isha / 60.0,
        )
    }

    // --- city wall clock (the device tz is never trusted for a picked city) --

    fun cityCalendar(epochMillis: Long): Calendar {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = epochMillis + (utcOffsetAt(epochMillis) * 3600_000.0).toLong()
        return cal
    }

    fun cityHourOfDay(epochMillis: Long): Double {
        val c = cityCalendar(epochMillis)
        return c.get(Calendar.HOUR_OF_DAY) + c.get(Calendar.MINUTE) / 60.0 +
            c.get(Calendar.SECOND) / 3600.0 + c.get(Calendar.MILLISECOND) / 3.6e6
    }

    fun cityDate(epochMillis: Long): Triple<Int, Int, Int> {
        val c = cityCalendar(epochMillis)
        return Triple(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    fun cityTomorrow(epochMillis: Long): Triple<Int, Int, Int> {
        val c = cityCalendar(epochMillis)
        c.add(Calendar.DAY_OF_MONTH, 1)
        return Triple(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }
}
