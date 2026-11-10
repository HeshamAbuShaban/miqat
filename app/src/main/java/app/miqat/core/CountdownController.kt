package app.miqat.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * The always-on countdown and the battery-optimisation door (gate 0.4).
 *
 * The advisory council's line: «users forgive a plain UI, never a missed
 * Fajr». So the app asks for the battery exemption as a LINK (the user decides
 * in the system screen) — never a prompt, never a nagging dialog.
 */
object CountdownController {

    fun setEnabled(context: Context, enabled: Boolean) {
        val am = context.getSystemService(android.app.AlarmManager::class.java)
        val intent = Intent(context, CountdownService::class.java).apply {
            action = if (enabled) CountdownService.ACTION_START else CountdownService.ACTION_STOP
        }
        try {
            if (enabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            // the service refuses to start (e.g. background restrictions) — the
            // countdown simply stays off; the app never lies about it
        }
    }

    /** Is the app exempt from battery optimisation? */
    fun isExempt(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Open the system's battery-optimisation screen for this app. */
    fun openBatterySettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
    }
}
