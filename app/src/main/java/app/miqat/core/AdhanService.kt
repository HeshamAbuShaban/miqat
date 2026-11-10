package app.miqat.core

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import app.miqat.ui.AlarmActivity

/**
 * Plays the adhan in the FOREGROUND so the call is heard even when the phone
 * is in a pocket: the service owns the ongoing notification (with the
 * full-screen intent that opens the lock-screen alarm) and stops itself when
 * the recitation ends.
 */
class AdhanService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val slot = intent?.getIntExtra("slot", 2) ?: 2
        val label = intent?.getStringExtra("label") ?: "الصلاة"
        val pack = intent?.getStringExtra("pack") ?: AdhanPlayer.PACK_STANDARD
        val version = intent?.getIntExtra("version", 1) ?: 1

        val notification = NotificationPoster.buildAdhanNotification(this, slot, label)
        startForeground(NotificationPoster.ADHAN_NOTIFICATION_ID, notification)

        // yield to a call: when the audio focus is lost (a call, an alarm), we
        // stop rather than talk over it (gate 0.4 — no phone permission needed)
        val focus = AudioFocusGuard(this)
        val gotFocus = focus.request {
            Log.i("MiqatAdhan", "audio focus lost — yielding")
            AdhanPlayer.stop()
            stopForeground(true)
            stopSelf()
        }
        if (!gotFocus) {
            Log.i("MiqatAdhan", "no audio focus")
        }

        AdhanPlayer.play(this, pack, slot, version) {
            Log.i("MiqatAdhan", "adhan finished")
            focus.abandon()
            stopForeground(true)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        AdhanPlayer.stop()
        super.onDestroy()
    }
}
