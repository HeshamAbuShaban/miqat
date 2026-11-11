package app.miqat.core

import app.miqat.ui.AppState

/**
 * The live app state, reachable from the services (the countdown service runs
 * without an Activity). The Activity publishes it on start; the services only
 * READ it — the state itself stays owned by the UI layer.
 */
object AppStateHolder {
    @Volatile
    private var current: AppState? = null

    fun publish(state: AppState) {
        current = state
    }

    fun state(context: android.content.Context): AppState =
        current ?: AppState(context.applicationContext, armAlarms = false).also { current = it }
}
