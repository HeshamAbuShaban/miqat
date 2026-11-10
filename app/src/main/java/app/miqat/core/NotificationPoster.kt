package app.miqat.core

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import app.miqat.MainActivity
import app.miqat.R

/**
 * Builds the notifications themselves — one builder per KIND, so the system
 * settings show four meaningful channels instead of twenty-four.
 *
 * The motivational content rides ALONG at the end of another kind's payload
 * when the user asked for that («محتوى من ما تأتي به يكن مرفوقا مع اخواتها في
 * اخر المحتوى المقدم»), and stands alone when they did not.
 */
object NotificationPoster {

    private const val GROUP_DIRECT = "miqat_direct_group"

    /** The ongoing notification the adhan service owns. */
    const val ADHAN_NOTIFICATION_ID = 999

    /**
     * The adhan notification: the lock-screen full-screen intent opens the
     * alarm screen, and a stop action silences the call.
     */
    fun buildAdhanNotification(
        context: Context,
        slot: Int,
        label: String,
    ): android.app.Notification {
        NotifyChannels.ensure(context)
        val prefs = NotificationPrefsStore.load(context)
        val full = PendingIntent.getActivity(
            context, slot + 1,
            Intent(context, app.miqat.ui.AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("slot", slot)
                putExtra("label", label)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            context, slot + 50,
            Intent(context, AdhanService::class.java).apply { action = "app.miqat.action.STOP_ADHAN" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, NotifyChannels.DIRECT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("حان الآن وقت $label")
            .setContentText("اضغط لعرض التفاصيل")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(full)
            .setFullScreenIntent(full, true)
            .addAction(0, "إيقاف", stop)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    /** Posts a notification whose body carries a text and its citation. */
    fun postWithText(
        context: Context,
        kind: NotifyKind,
        label: String,
        slot: Int,
        quiet: Boolean,
        text: String,
        citation: String?,
    ) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        NotifyChannels.ensure(context)
        val prefs = NotificationPrefsStore.load(context)
        val body = if (citation != null) "$text\n$citation" else text
        val builder = NotificationCompat.Builder(context, channelFor(kind))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titleFor(kind, label))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priorityFor(kind))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
        val contentIntent = PendingIntent.getActivity(
            context, slot + 1,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        builder.setContentIntent(contentIntent)
        if (!quiet) builder.setDefaults(NotificationCompat.DEFAULT_SOUND)
        try {
            nm.notify(notificationId(kind, slot), builder.build())
        } catch (_: SecurityException) {
        }
    }

    /** The chip that says: this bound is OUR convention, not a ruling. */
    const val ESTIMATED = "تقديري بحسب الاصطلاح"

    fun post(
        context: Context,
        kind: NotifyKind,
        label: String,
        slot: Int,
        quiet: Boolean = false,
        estimated: Boolean = false,
        iqama: Boolean = false,
    ) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        NotifyChannels.ensure(context)

        val prefs = NotificationPrefsStore.load(context)
        val motivational = motivationalText(context)

        val body = bodyFor(kind, label, motivational, prefs) +
            (if (estimated) "\n" + ESTIMATED else "") +
            (if (iqama) "\nإقامة الصلاة" else "")
        val builder = NotificationCompat.Builder(context, channelFor(kind))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titleFor(kind, label))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priorityFor(kind))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)

        val contentIntent = PendingIntent.getActivity(
            context, slot + 1,
            if (kind == NotifyKind.DIRECT) {
                Intent(context, app.miqat.ui.AlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("slot", slot)
                    putExtra("label", label)
                }
            } else {
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        builder.setContentIntent(contentIntent)

        if (kind == NotifyKind.DIRECT && iqama) {
            // the iqama is a call, not the adhan: a plain sound, no recitation
            if (!quiet) builder.setDefaults(NotificationCompat.DEFAULT_SOUND)
        } else if (kind == NotifyKind.DIRECT) {
            if (!quiet && prefs.adhanSound) builder.setDefaults(NotificationCompat.DEFAULT_SOUND)
            if (!quiet && prefs.vibrate) builder.setVibrate(longArrayOf(0, 400, 200, 400))
            builder.setGroup(GROUP_DIRECT)
            if (prefs.fullScreenAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setFullScreenIntent(contentIntent, true)
                builder.setCategory(NotificationCompat.CATEGORY_ALARM)
            }
        } else if (kind == NotifyKind.MOTIVATIONAL) {
            builder.setPriority(NotificationCompat.PRIORITY_LOW)
        } else if (!quiet) {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND)
        }

        // the motivational line rides along at the END of the other payloads
        if (kind != NotifyKind.MOTIVATIONAL && prefs.motivationalAppended && motivational != null) {
            builder.setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    bodyFor(kind, label, null, prefs) + "\n" + motivational,
                ),
            )
        }

        try {
            nm.notify(notificationId(kind, slot), builder.build())
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted (Android 13+): nothing to do but
            // stay silent — the alarm itself still fired.
        }
    }

    /** A user-triggered preview — the same payload the real alarm posts. */
    fun preview(context: Context, kind: NotifyKind, label: String) {
        post(context, kind, if (label.isBlank()) "ذكرى" else label, 9)
    }

    /** The standalone motivational notification (Islamic days / fasting). */
    fun postMotivational(context: Context, text: String) {
        val builder = NotificationCompat.Builder(context, NotifyChannels.MOTIVATIONAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("ذكرى اليوم")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
        try {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm?.notify(8801, builder.build())
        } catch (_: SecurityException) {
        }
    }

    private fun channelFor(kind: NotifyKind) = when (kind) {
        NotifyKind.PREPARATORY -> NotifyChannels.PREPARATORY
        NotifyKind.DIRECT -> NotifyChannels.DIRECT
        NotifyKind.REMINDER -> NotifyChannels.REMINDER
        NotifyKind.MOTIVATIONAL -> NotifyChannels.MOTIVATIONAL
    }

    private fun priorityFor(kind: NotifyKind) = when (kind) {
        NotifyKind.DIRECT -> NotificationCompat.PRIORITY_HIGH
        NotifyKind.MOTIVATIONAL -> NotificationCompat.PRIORITY_LOW
        else -> NotificationCompat.PRIORITY_DEFAULT
    }

    private fun titleFor(kind: NotifyKind, label: String) = when (kind) {
        NotifyKind.PREPARATORY -> "اقترب وقت $label"
        NotifyKind.DIRECT -> if (label.startsWith("إقامة")) label else "حان الآن وقت $label"
        NotifyKind.REMINDER -> "فات وقت $label"
        NotifyKind.MOTIVATIONAL -> if (label.startsWith("غداً")) label else "ذكرى"
    }

    private fun bodyFor(
        kind: NotifyKind,
        label: String,
        motivational: String?,
        prefs: NotificationPrefs,
    ): String {
        val base = when (kind) {
            NotifyKind.PREPARATORY -> "استعدّ للصلاة — تهيّئ مكانك وقلبك"
            NotifyKind.DIRECT -> "حان الآن وقت $label"
            NotifyKind.REMINDER -> "هل أدّيت $label؟"
            NotifyKind.MOTIVATIONAL -> motivational ?: ""
        }
        return base
    }

    /**
     * The reminder payload: the donor's own dhikr for that prayer, with the
     * verified citation printed under it (nothing ships uncited).
     */
    fun reminderPayload(context: Context, slot: Int): Pair<String, String?>? {
        val events = MiqatData.prayerEvents(context, slot)
        val event = events.firstOrNull { AdhkarCitations.citationFor(it.id) != null } ?: return null
        val citation = AdhkarCitations.lineFor(event.id)
        return event.messageAr to citation
    }

    /** The contextual motivational line for today (Islamic day). */
    private fun motivationalText(context: Context): String? {
        val hijri = Hijri.today(offsetDays = UserSettings.display(context).hijriOffsetDays)
        val card = IslamicDays.todayCard(hijri) ?: return null
        return "${card.first} — ${card.second}"
    }

    private fun notificationId(kind: NotifyKind, slot: Int) = 100 + kind.ordinal * 10 + slot
}
