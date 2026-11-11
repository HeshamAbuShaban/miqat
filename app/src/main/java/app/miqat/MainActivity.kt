package app.miqat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import app.miqat.ui.AppState
import app.miqat.ui.Fmt
import app.miqat.ui.MiqatApp
import app.miqat.ui.MiqatTheme

/**
 * The shell only: build the state, apply the theme, host the navigation.
 * Every screen is a pure function of state + route (ui/ package).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = AppState(this)
        app.miqat.core.AppStateHolder.publish(state)
        // the always-on countdown follows the setting
        if (state.notifications.alwaysOnCountdown) {
            app.miqat.core.CountdownController.setEnabled(this, true)
        }
        setContent {
            // the number format FOLLOWS the display prefs: a build of the Fmt
            // once froze the digits and the 12/24h switch until the next
            // launch (the advisory council, 2026-11-11)
            val fmt = remember(state.display) { Fmt(state.display) }
            MiqatTheme(
                dark = state.display.darkTheme,
                accent = app.miqat.ui.Accent.of(state.display.accent),
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MiqatApp(state, fmt)
                }
            }
        }
    }
}
