package app.miqat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
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
        val fmt = Fmt(state.display)
        // the always-on countdown follows the setting
        if (state.notifications.alwaysOnCountdown) {
            app.miqat.core.CountdownController.setEnabled(this, true)
        }
        setContent {
            MiqatTheme(dark = state.display.darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MiqatApp(state, fmt)
                }
            }
        }
    }
}
