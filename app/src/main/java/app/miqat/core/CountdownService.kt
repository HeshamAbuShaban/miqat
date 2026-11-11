package app.miqat.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import app.miqat.R
import app.miqat.MainActivity
import java.util.concurrent.TimeUnit

/**
 * The ALWAYS-ON COUNTDOWN (gate 0.4) — the promise the product makes:
 * «متى الميقات، وكم بقي، ولماذا — بصدق، بلا إنترنت، على شاشة قفلك».
 *
 * A foreground service owns an ongoing notification that counts down to the
 * next appointed time, refreshed every minute. It is deliberately quiet
 * (LOW channel, no sound) and can be switched off in the settings.
 */
class CountdownService : android.app.Service() {

    companion object {
        const val CHANNEL = "miqat_countdown"
        const val NOTIFICATION_ID = 998
        const val ACTION_START = "app.miqat.action.COUNTDOWN_START"
        const val ACTION_STOP = "app.miqat.action.COUNTDOWN_STOP"
    }

    private var wakeLock: android.os.PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): android.os.IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL, "العدّاد الدائم", NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "عدّاد مستمر للمتبقي إلى الموعد التالي"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            nm.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        scheduleNext()
        return START_STICKY
    }

    /** The live countdown, recomputed every minute. */
    private fun scheduleNext() {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(NOTIFICATION_ID, buildNotification())
                scheduleNext()
            }
        }, TimeUnit.MINUTES.toMillis(1))
    }

    private fun buildNotification(): Notification {
        val state = AppStateHolder.state(this)
        val now = System.currentTimeMillis()
        val (y, m, d) = state.cityDate(now)
        val times = state.times(y, m, d)
        val (ty, tm, td) = state.cityTomorrow(now)
        val tomorrow = state.times(ty, tm, td)
        val appointments = listOf(
            app.miqat.core.Appointment("fajr", "الفجر", times.fajr),
            app.miqat.core.Appointment("sunrise", "الشروق", times.sunrise),
            app.miqat.core.Appointment("dhuhr", "الظهر", times.dhuhr),
            app.miqat.core.Appointment("asr", "العصر", times.asr),
            app.miqat.core.Appointment("maghrib", "المغرب", times.maghrib),
            app.miqat.core.Appointment("isha", "العشاء", times.isha),
            app.miqat.core.Appointment("fajr2", "الفجر", tomorrow.fajr),
        )
        val seg = app.miqat.core.DayClock.position(appointments, state.cityHourOfDay(now))
        val title = if (seg != null) "المتبقي لميقات ${seg.next.labelAr}" else "المواقيت"
        val body = if (seg != null) {
            val h = seg.remainingSeconds / 3600
            val m = (seg.remainingSeconds % 3600) / 60
            val s = seg.remainingSeconds % 60
            "${app.miqat.core.NumFmt.digits("%d:%02d:%02d".format(h, m, s), state.display.arabicIndicDigits)}"
        } else ""

        val open = PendingIntent.getActivity(
            this, 1,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(open)
            .build()
    }

    override fun onDestroy() {
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }
}

/**
 * AUDIO FOCUS — the call pause (gate 0.4), without asking for the phone
 * permission: when the adhan asks for audio focus and LOSES it (a call, an
 * alarm, another app), we yield instead of talking over the call.
 */
class AudioFocusGuard(private val context: Context) {

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var request: AudioFocusRequest? = null

    /** Request alarm-grade focus; [onLoss] fires when something else takes over. */
    fun request(onLoss: () -> Unit): Boolean {
        val am = audioManager ?: return false
        val listener = AudioManager.OnAudioFocusChangeListener { change ->
            if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                onLoss()
            }
        }
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setOnAudioFocusChangeListener(listener)
                .build()
            request = req
            am.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                listener, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            )
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun abandon() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            request?.let { am.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(null)
        }
    }
}
