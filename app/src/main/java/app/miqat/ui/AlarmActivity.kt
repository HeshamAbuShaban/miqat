package app.miqat.ui

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miqat.core.AdhanPlayer
import app.miqat.core.AdhanService
import app.miqat.core.Hijri
import app.miqat.core.NotificationPoster
import app.miqat.core.NumFmt
import app.miqat.core.UserSettings
import java.util.Calendar

/**
 * The full-screen alarm (S3): the screen that meets you when the adhan goes
 * off — the prayer's name, its time, and one obvious way to stop the call.
 * It turns the screen on over the lock screen (as an alarm clock does) and
 * never asks for anything else.
 */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        val slot = intent.getIntExtra("slot", 2)
        val label = intent.getStringExtra("label") ?: PRAYER_NAMES.getOrElse(slot) { "الصلاة" }
        val prefs = UserSettings.display(this)
        val fmt = Fmt(prefs)

        setContent {
            MiqatTheme(dark = prefs.darkTheme) {
                AlarmScreen(
                    label = label,
                    time = fmt.hhmm(nowMinutes()),
                    hijri = Hijri.label(Hijri.today(), fmt.ar),
                    arabicIndic = fmt.ar,
                    onStop = {
                        AdhanPlayer.stop()
                        stopService()
                        finish()
                    },
                )
            }
        }
    }

    private fun nowMinutes(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    private fun stopService() {
        try {
            stopService(Intent(this, AdhanService::class.java).setAction("app.miqat.action.STOP_ADHAN"))
        } catch (_: Exception) {
        }
        NotificationManager::class.java.let { nm ->
            (getSystemService(NotificationManager::class.java))
                ?.cancel(NotificationPoster.ADHAN_NOTIFICATION_ID)
        }
    }

    /** Wake the screen and show over the lock screen, like an alarm clock. */
    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            )
        }
    }
}

@Composable
private fun AlarmScreen(
    label: String,
    time: String,
    hijri: String,
    arabicIndic: Boolean,
    onStop: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxSize()
            .background(scheme.background)
            .padding(Space.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "حان الآن وقت",
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.sm))
            Text(
                label,
                style = MaterialTheme.typography.displayMedium,
                color = scheme.primary,
            )
            Spacer(Modifier.height(Space.md))
            Text(
                time,
                style = MaterialTheme.typography.displaySmall,
                color = scheme.onSurface,
            )
            Spacer(Modifier.height(Space.sm))
            Text(
                hijri,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.xxl))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.panel))
                    .background(scheme.primaryContainer.copy(alpha = .6f))
                    .padding(Space.lg),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "الأذان جارٍ",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        "اضغط للإيقاف",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(Space.lg))
            Text(
                "إيقاف",
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.control))
                    .background(scheme.primary)
                    .clickable { onStop() }
                    .padding(vertical = Space.md),
            )
        }
    }
}
