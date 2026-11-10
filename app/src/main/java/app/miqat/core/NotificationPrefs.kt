package app.miqat.core

import android.content.Context
import android.content.SharedPreferences

/**
 * Notification taxonomy + per-prayer control (R6, donor `AdhanPreferences` +
 * `RemindersSilentPreferences` + `VibrationPreferences` parity).
 *
 * The four kinds are Hesham's own taxonomy (research-agenda R6):
 *  - تمهيدية  PREPARATORY — lead time before the appointed time
 *  - مباشرة   DIRECT      — at the appointed time (adhan)
 *  - تذكيرية  REMINDER    — after it (missed-prayer dhikr / iqama gap)
 *  - تحفيزية  MOTIVATIONAL — separate channel; virtues + fasting encouragement
 * The motivational kind may ride ALONG appended to another kind's payload
 * («محتوى من ما تأتي به يكن مرفوقا مع اخواتها في اخر المحتوى المقدم»).
 */
enum class NotifyKind { PREPARATORY, DIRECT, REMINDER, MOTIVATIONAL }

data class NotificationPrefs(
    /** Which kinds are enabled at all. */
    val kindEnabled: Map<NotifyKind, Boolean> = mapOf(
        NotifyKind.PREPARATORY to false,
        NotifyKind.DIRECT to true,
        NotifyKind.REMINDER to false,
        NotifyKind.MOTIVATIONAL to false,
    ),
    /** Per-slot enable for the direct adhan (index 0..5 = fajr..isha). */
    val directPerPrayer: List<Boolean> = List(6) { true },
    /** Minutes before the appointed time for the preparatory nudge. */
    val preparatoryLeadMinutes: Int = 10,
    /** Minutes after the appointed time for the reminder. */
    val reminderDelayMinutes: Int = 20,
    /** Append the motivational text to other kinds' payloads (vs. standalone). */
    val motivationalAppended: Boolean = true,
    /** Standalone motivational notifications (fasting virtues, Islamic days). */
    val motivationalStandalone: Boolean = false,
    /** Play the adhan audio (vs. a plain notification). */
    val adhanSound: Boolean = true,
    /** Vibration on the direct adhan. */
    val vibrate: Boolean = true,
    /** Full-screen lockscreen intent (the alarm experience). */
    val fullScreenAlarm: Boolean = true,
    /** Keep a persistent countdown notification. */
    val continuousNotification: Boolean = false,
    /** Quiet hours — notifications suppressed between these minutes (0..1439). */
    val quietFromMinute: Int = -1,
    val quietToMinute: Int = -1,
) {
    fun kindOn(kind: NotifyKind): Boolean = kindEnabled[kind] ?: false

    fun directOn(index: Int): Boolean = directPerPrayer.getOrElse(index) { true }

    fun quietActive(nowMinuteOfDay: Int): Boolean {
        if (quietFromMinute < 0 || quietToMinute < 0) return false
        return if (quietFromMinute <= quietToMinute)
            nowMinuteOfDay in quietFromMinute..quietToMinute
        else nowMinuteOfDay >= quietFromMinute || nowMinuteOfDay <= quietToMinute
    }
}

object NotificationPrefsStore {

    private const val FILE = "notification_prefs"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(context: Context): NotificationPrefs {
        val p = prefs(context)
        val defaults = NotificationPrefs()
        return NotificationPrefs(
            kindEnabled = NotifyKind.entries.associateWith { kind ->
                p.getBoolean("kind_${kind.name}", defaults.kindOn(kind))
            },
            directPerPrayer = (0..5).map { p.getBoolean("direct_$it", true) },
            preparatoryLeadMinutes = p.getInt("prep_lead", defaults.preparatoryLeadMinutes),
            reminderDelayMinutes = p.getInt("reminder_delay", defaults.reminderDelayMinutes),
            motivationalAppended = p.getBoolean("mot_appended", defaults.motivationalAppended),
            motivationalStandalone = p.getBoolean("mot_standalone", defaults.motivationalStandalone),
            adhanSound = p.getBoolean("adhan_sound", defaults.adhanSound),
            vibrate = p.getBoolean("vibrate", defaults.vibrate),
            fullScreenAlarm = p.getBoolean("fsi", defaults.fullScreenAlarm),
            continuousNotification = p.getBoolean("continuous", defaults.continuousNotification),
            quietFromMinute = p.getInt("quiet_from", -1),
            quietToMinute = p.getInt("quiet_to", -1),
        )
    }

    fun save(context: Context, n: NotificationPrefs) {
        val e = prefs(context).edit()
        for ((k, v) in n.kindEnabled) e.putBoolean("kind_${k.name}", v)
        for (i in 0..5) e.putBoolean("direct_$i", n.directPerPrayer.getOrElse(i) { true })
        e.putInt("prep_lead", n.preparatoryLeadMinutes)
        e.putInt("reminder_delay", n.reminderDelayMinutes)
        e.putBoolean("mot_appended", n.motivationalAppended)
        e.putBoolean("mot_standalone", n.motivationalStandalone)
        e.putBoolean("adhan_sound", n.adhanSound)
        e.putBoolean("vibrate", n.vibrate)
        e.putBoolean("fsi", n.fullScreenAlarm)
        e.putBoolean("continuous", n.continuousNotification)
        e.putInt("quiet_from", n.quietFromMinute)
        e.putInt("quiet_to", n.quietToMinute)
        e.apply()
    }
}
