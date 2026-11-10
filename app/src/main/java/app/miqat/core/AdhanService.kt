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

        AdhanPlayer.play(this, pack, slot, version) {
            Log.i("MiqatAdhan", "adhan finished")
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
