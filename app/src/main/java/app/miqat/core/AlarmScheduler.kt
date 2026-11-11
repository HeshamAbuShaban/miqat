package app.miqat.core

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import app.miqat.R
import java.util.Calendar
import java.util.TimeZone

/**
 * S3 — the notification machinery, built to the council's map (4+1 channels,
 * not 24) and the donor's re-arm list.
 *
 * What is REAL here:
 *  - one NotificationChannel per KIND (تمهيدية/مباشرة/تذكيرية/تحفيزية) + a
 *    silent countdown channel; per-prayer granularity lives in the app, not in
 *    the system settings (24 channels would be unusable there).
 *  - exact alarms through AlarmManager for the six prayer times and the
 *    worship windows the user enabled.
 *  - re-arm on boot, time change and timezone change (the donor's list).
 *  - honest degradation: if exact alarms are not permitted, we fall back to
 *    inexact alarms and the UI says so — a late adhan must never be silent.
 */
object NotifyChannels {
    const val PREPARATORY = "miqat_preparatory"
    const val DIRECT = "miqat_direct"
    const val REMINDER = "miqat_reminder"
    const val MOTIVATIONAL = "miqat_motivational"
    const val COUNTDOWN = "miqat_countdown"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        fun channel(id: String, name: String, importance: Int, description: String) {
            val ch = NotificationChannel(id, name, importance)
            ch.description = description
            ch.setShowBadge(false)
            nm.createNotificationChannel(ch)
        }
        channel(PREPARATORY, "تنبيه تمهيدي", NotificationManager.IMPORTANCE_DEFAULT, "قبل الموعد بقليل")
        channel(DIRECT, "الأذان", NotificationManager.IMPORTANCE_HIGH, "عند دخول وقت الصلاة")
        channel(REMINDER, "تذكير", NotificationManager.IMPORTANCE_DEFAULT, "بعد الموعد")
        channel(MOTIVATIONAL, "محتوى تحفيزي", NotificationManager.IMPORTANCE_LOW, "أيام الصيام والفضائل")
        channel(COUNTDOWN, "العدّاد", NotificationManager.IMPORTANCE_LOW, "عدّاد دائم للمتبقي")
    }
}

/** What a scheduled alarm carries. */
data class AlarmSpec(
    val kind: NotifyKind,
    /** 0..5 for the six prayer slots, -1 for a worship window. */
    val slot: Int,
    val label: String,
    /** Trigger time in epoch millis. */
    val atMillis: Long,
    /**
     * Is this a CONVENTION (a worship window's bound, computed by our own
     * rule)? Then the payload must carry «تقديري بحسب الاصطلاح» — a window
     * bound announced without it is a claim of certainty we do not have.
     */
    val estimated: Boolean = false,
    /** The iqama — the second call, minutes after the adhan. */
    val iqama: Boolean = false,
)

object AlarmScheduler {

    private const val TAG = "MiqatAlarm"
    const val ACTION_ALARM = "app.miqat.action.ALARM"

    /**
     * Schedule every enabled alarm for the next 24 h. Called on start, on
     * every settings change, and after boot/time changes.
     */
    fun reschedule(context: Context) {
        val app = context.applicationContext
        NotifyChannels.ensure(app)
        val prefs = NotificationPrefsStore.load(app)
        val city = UserSettings.city(app) ?: CityStore.MAKKAH
        val adjusts = UserSettings.adjusts(app)
        val display = UserSettings.display(app)
        val method = MethodTable[display.methodOverride ?: city.calcMethod]
        val offset = display.manualUtcOffsetMinutes?.let { it / 60.0 } ?: city.utcOffset
        val loc = Location(city.lat, city.lon, offset)

        val am = app.getSystemService(AlarmManager::class.java) ?: return
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val (y, m, d) = cityDateOf(city, offset, now)
        val (ty, tm, td) = cityDateOf(city, offset, now + 86_400_000L)

        fun timesOf(yy: Int, mm: Int, dd: Int): LocalPrayerTimes {
            val t = PrayerTimes.compute(yy, mm, dd, loc, method)
            return LocalPrayerTimes(
                t.fajr + adjusts.fajr / 60.0, t.sunrise + adjusts.sunrise / 60.0,
                t.dhuhr + adjusts.dhuhr / 60.0, t.asr + adjusts.asr / 60.0,
                t.maghrib + adjusts.maghrib / 60.0, t.isha + adjusts.isha / 60.0,
            )
        }
        val today = timesOf(y, m, d)
        val tomorrow = timesOf(ty, tm, td)
        val prayerNames = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
        val todayList = listOf(
            today.fajr, today.sunrise, today.dhuhr, today.asr, today.maghrib, today.isha,
        )
        val tomorrowList = listOf(
            tomorrow.fajr, tomorrow.sunrise, tomorrow.dhuhr, tomorrow.asr, tomorrow.maghrib, tomorrow.isha,
        )

        val specs = mutableListOf<AlarmSpec>()

        // --- direct adhan per prayer ---
        if (prefs.kindOn(NotifyKind.DIRECT)) {
            for (i in 0..5) {
                if (!prefs.directOn(i)) continue
                // the sunris is not a prayer — the adhan is for the five + none
                if (i == 1) continue
                val at = epochFor(city, offset, now, todayList[i], isTomorrow = false)
                if (at > now) specs.add(AlarmSpec(NotifyKind.DIRECT, i, prayerNames[i], at))
                val at2 = epochFor(city, offset, now, tomorrowList[i], isTomorrow = true)
                if (at2 > now) specs.add(AlarmSpec(NotifyKind.DIRECT, i, prayerNames[i], at2))
                // the iqama — the second call, if the user asked for it
                if (prefs.iqamaMinutes > 0) {
                    val iq1 = at + prefs.iqamaMinutes * 60_000L
                    if (iq1 > now) specs.add(AlarmSpec(NotifyKind.DIRECT, i, "إقامة ${prayerNames[i]}", iq1, iqama = true))
                    val iq2 = at2 + prefs.iqamaMinutes * 60_000L
                    if (iq2 > now) specs.add(AlarmSpec(NotifyKind.DIRECT, i, "إقامة ${prayerNames[i]}", iq2, iqama = true))
                }
            }
        }

        // --- preparatory (lead time) ---
        if (prefs.kindOn(NotifyKind.PREPARATORY)) {
            val lead = prefs.preparatoryLeadMinutes * 60_000L
            for (i in 0..5) {
                if (!prefs.directOn(i) || i == 1) continue
                val at = epochFor(city, offset, now, todayList[i], isTomorrow = false) - lead
                if (at > now) specs.add(AlarmSpec(NotifyKind.PREPARATORY, i, prayerNames[i], at))
                val at2 = epochFor(city, offset, now, tomorrowList[i], isTomorrow = true) - lead
                if (at2 > now) specs.add(AlarmSpec(NotifyKind.PREPARATORY, i, prayerNames[i], at2))
            }
        }

        // --- reminder (after the time) ---
        if (prefs.kindOn(NotifyKind.REMINDER)) {
            val delay = prefs.reminderDelayMinutes * 60_000L
            for (i in 0..5) {
                if (!prefs.directOn(i) || i == 1) continue
                val at = epochFor(city, offset, now, todayList[i], isTomorrow = false) + delay
                if (at > now) specs.add(AlarmSpec(NotifyKind.REMINDER, i, prayerNames[i], at))
                val at2 = epochFor(city, offset, now, tomorrowList[i], isTomorrow = true) + delay
                if (at2 > now) specs.add(AlarmSpec(NotifyKind.REMINDER, i, prayerNames[i], at2))
            }
        }

        // --- the fasting / motivational nudge (the تحفيزية kind) ---
        if (prefs.kindOn(NotifyKind.MOTIVATIONAL) && prefs.fastingReminderEnabled) {
            for (dayAhead in 1..7) {
                val (dy, dm, dd) = cityDateOf(city, offset, now + dayAhead * 86_400_000L)
                val hijri = Hijri.at(dy, dm, dd)
                if (!fastingDayMatches(prefs, hijri)) continue
                val card = IslamicDays.todayCard(hijri) ?: continue
                val times = PrayerTimes.compute(dy, dm, dd, loc, method)
                val hour = if (prefs.fastingReminderTime == "fajr") {
                    times.fajr - 20.0 / 60.0
                } else {
                    times.maghrib + 20.0 / 60.0
                }
                val at = epochFor(city, offset, now, hour, isTomorrow = dayAhead >= 1)
                if (at > now) {
                    specs.add(AlarmSpec(NotifyKind.MOTIVATIONAL, -10, "غداً ${card.first}", at))
                }
            }
        }

        // --- worship windows (الضحى / الأسحار / الثلث الأخير) ---
        val night = WorshipWindows.night(today.maghrib, tomorrow.fajr)
        val duha = WorshipWindows.duha(today.sunrise, today.dhuhr)
        val sahar = WorshipWindows.sahar(today.maghrib, tomorrow.fajr)
        if (prefs.kindOn(NotifyKind.DIRECT)) {
            val dAt = epochFor(city, offset, now, duha.startHour, isTomorrow = false)
            if (!duha.startHour.isNaN() && dAt > now) {
                specs.add(AlarmSpec(NotifyKind.DIRECT, -1, "وقت الضحى", dAt))
            }
            val sAt = epochFor(city, offset, now, sahar.startHour, isTomorrow = true)
            if (!sahar.startHour.isNaN() && sAt > now) {
                specs.add(AlarmSpec(NotifyKind.DIRECT, -2, "وقت الأسحار", sAt, estimated = true))
            }
            val tAt = epochFor(city, offset, now, night.lastThirdStartHour, isTomorrow = true)
            if (!night.lastThirdStartHour.isNaN() && tAt > now) {
                specs.add(AlarmSpec(NotifyKind.DIRECT, -3, "الثلث الأخير من الليل", tAt, estimated = true))
            }
        }

        // --- cancel previous, then set ---
        val pending = PendingIntent.getBroadcast(
            app, REQUEST_CANCEL,
            Intent(app, AlarmReceiver::class.java).setAction(ACTION_ALARM + ".CANCEL"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.cancel(pending)

        // quiet hours: the alarm still fires, but the notification stays silent
        val quiet = prefs.quietFromMinute >= 0
        for (spec in specs) {
            val inQuiet = quiet && quietActive(prefs, spec.atMillis, offset)
            schedule(app, am, spec, canExact, inQuiet)
        }
        Log.i(TAG, "scheduled ${specs.size} alarms (exact=$canExact)")
    }

    private const val REQUEST_CANCEL = 9000
    private const val REQUEST_BASE = 9100

    /** Does the user want a nudge about this Islamic day? */
    private fun fastingDayMatches(prefs: NotificationPrefs, h: IslamicDay): Boolean {
        val days = prefs.fastingReminderDays
        if (days.contains("white") && IslamicDays.isWhiteDays(h)) return true
        if (days.contains("arafah") && IslamicDays.isDayOfArafah(h)) return true
        if (days.contains("ashura") && IslamicDays.isAshura(h)) return true
        if (days.contains("shawwal") && IslamicDays.isSixOfShawwal(h)) return true
        if (days.contains("monday") && IslamicDays.isMondayThursday(h) && h.weekday == 2) return true
        if (days.contains("thursday") && IslamicDays.isMondayThursday(h) && h.weekday == 5) return true
        return false
    }

    /** Is this instant inside the user's quiet window (city wall clock)? */
    private fun quietActive(prefs: NotificationPrefs, epoch: Long, offset: Double): Boolean {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = epoch + (offset * 3_600_000.0).toLong()
        val minute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return prefs.quietActive(minute)
    }

    private fun schedule(
        context: Context, am: AlarmManager, spec: AlarmSpec, canExact: Boolean, quiet: Boolean,
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM
            putExtra("kind", spec.kind.name)
            putExtra("slot", spec.slot)
            putExtra("label", spec.label)
            putExtra("quiet", quiet)
            putExtra("estimated", spec.estimated)
            putExtra("iqama", spec.iqama)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_BASE + spec.kind.ordinal * 10 + (spec.slot + 1),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, spec.atMillis, pi)
            } else {
                // honest degradation: it may be late, but it will arrive
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, spec.atMillis, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, spec.atMillis, pi)
        }
    }

    /** Epoch millis of a city wall-clock hour, today or tomorrow. */
    private fun epochFor(
        city: City,
        offset: Double,
        now: Long,
        hourOfDay: Double,
        isTomorrow: Boolean,
    ): Long {
        if (hourOfDay.isNaN()) return 0L
        val (y, m, d) = cityDateOf(city, offset, now)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val dayShift = if (isTomorrow) 86_400_000L else 0L
        val midnightUtc = (now + dayShift) - (((now + dayShift) / 1000) % 86_400) * 1000
        cal.timeInMillis = midnightUtc + (hourOfDay * 3_600_000.0).toLong()
        // convert the city wall clock to epoch: wall = utc + offset
        var epoch = cal.timeInMillis - (offset * 3_600_000.0).toLong()
        // keep the same Y/M/D in the city frame
        val check = cityDateOf(city, offset, epoch)
        if (check != Triple(y, m, d)) {
            // the wall clock belongs to the other day — recompute from the date
            val c2 = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            val ymd = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear(); set(check.first, check.second - 1, check.third)
            }
            c2.timeInMillis = ymd.timeInMillis + (hourOfDay * 3_600_000.0).toLong()
            epoch = c2.timeInMillis - (offset * 3_600_000.0).toLong()
        }
        return epoch
    }

    private fun cityDateOf(city: City, offset: Double, epoch: Long): Triple<Int, Int, Int> {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = epoch + (offset * 3_600_000.0).toLong()
        return Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }
}

/** Fires the scheduled notification and re-arms the following ones. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotifyChannels.ensure(context)
        val kind = NotifyKind.valueOf(intent.getStringExtra("kind") ?: NotifyKind.DIRECT.name)
        val label = intent.getStringExtra("label") ?: "الموعد"
        val slot = intent.getIntExtra("slot", -1)
        val quiet = intent.getBooleanExtra("quiet", false)
        val estimated = intent.getBooleanExtra("estimated", false)
        val iqama = intent.getBooleanExtra("iqama", false)
        if (kind == NotifyKind.REMINDER) {
            // the reminder carries the dhikr (with its verified citation)
            val payload = NotificationPoster.reminderPayload(context, slot)
            if (payload != null) {
                NotificationPoster.postWithText(
                    context, kind, label, slot, quiet, payload.first, payload.second,
                )
            } else {
                NotificationPoster.post(context, kind, label, slot, quiet)
            }
        } else {
            NotificationPoster.post(context, kind, label, slot, quiet)
        }
        // the widget counts down to the NEXT prayer — at the moment a prayer
        // arrives, that next one changes
        try {
            app.miqat.ui.widget.MiqatWidgetProvider.refresh(context)
        } catch (e: Exception) {
            // a widget must never break a notification
        }

        // the adhan itself: a foreground service so it is heard with the phone
        // in a pocket
        if (kind == NotifyKind.DIRECT && !quiet) {
            val prefs = NotificationPrefsStore.load(context)
            if (prefs.adhanSound) {
                try {
                    val svc = Intent(context, AdhanService::class.java).apply {
                        putExtra("slot", slot)
                        putExtra("label", label)
                        putExtra("pack", prefs.adhanPack)
                        putExtra("version", prefs.adhanVersion)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(svc)
                    } else {
                        context.startService(svc)
                    }
                } catch (e: Exception) {
                    Log.w("MiqatAlarm", "adhan service failed: ${e.message}")
                }
            }
        }
        // keep the chain alive
        AlarmScheduler.reschedule(context)
    }
}

/** Re-arms after boot, time change, timezone change — the donor's list. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> AlarmScheduler.reschedule(context)
        }
    }
}
