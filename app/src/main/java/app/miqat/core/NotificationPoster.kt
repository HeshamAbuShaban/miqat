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

    fun post(context: Context, kind: NotifyKind, label: String, slot: Int, quiet: Boolean = false) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        NotifyChannels.ensure(context)

        val prefs = NotificationPrefsStore.load(context)
        val motivational = motivationalText(context)

        val builder = NotificationCompat.Builder(context, channelFor(kind))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titleFor(kind, label))
            .setContentText(bodyFor(kind, label, motivational, prefs))
            .setPriority(priorityFor(kind))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)

        val contentIntent = PendingIntent.getActivity(
            context, slot + 1,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        builder.setContentIntent(contentIntent)

        if (kind == NotifyKind.DIRECT) {
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
        NotifyKind.DIRECT -> "حان الآن وقت $label"
        NotifyKind.REMINDER -> "فات وقت $label"
        NotifyKind.MOTIVATIONAL -> "ذكرى"
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

    /** The contextual motivational line for today (Islamic day). */
    private fun motivationalText(context: Context): String? {
        val hijri = Hijri.today(offsetDays = UserSettings.display(context).hijriOffsetDays)
        val card = IslamicDays.todayCard(hijri) ?: return null
        return "${card.first} — ${card.second}"
    }

    private fun notificationId(kind: NotifyKind, slot: Int) = 100 + kind.ordinal * 10 + slot
}
