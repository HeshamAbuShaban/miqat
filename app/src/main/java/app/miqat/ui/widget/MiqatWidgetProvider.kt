package app.miqat.ui.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import app.miqat.MainActivity
import app.miqat.R
import app.miqat.core.AppStateHolder
import app.miqat.core.Timers
import app.miqat.core.prayerPoints
import app.miqat.ui.DayArc
import app.miqat.ui.Fmt
import app.miqat.ui.PRAYER_NAMES
import app.miqat.ui.list

/**
 * THE WIDGET — the day's next prayer on the home screen.
 *
 * The donor shipped three widgets; this is ours: ONE widget that says the
 * one thing a widget should say — «which prayer is next, and how long until
 * it» — over the SAME horizon the app's hero uses.
 *
 * It is honest about being a widget:
 *  - the countdown is refreshed on every minute tick (the same trick the
 *    donor used with TIME_TICK) and at every prayer alarm;
 *  - the times come from the SAME engine and the SAME state as the app, so
 *    the widget can never disagree with the app;
 *  - no network, no location, no account — the widget is a view of the
 *    device's own data.
 */
class MiqatWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { id -> push(context, appWidgetManager, id) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_TIME_TICK,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            ACTION_STATE_CHANGED,
            -> refresh(context)
        }
    }

    override fun onEnabled(context: Context) {
        scheduleTick(context)
        refresh(context)
    }

    override fun onDisabled(context: Context) {
        cancelTick(context)
    }

    companion object {
        /** The app tells the widget when its own state changed. */
        const val ACTION_STATE_CHANGED = "app.miqat.action.WIDGET_STATE_CHANGED"

        private const val TICK_REQUEST = 4411

        /** Refresh every widget instance. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, MiqatWidgetProvider::class.java))
            ids.forEach { push(context, manager, it) }
        }

        /** A minute tick, so the countdown stays honest. */
        private fun scheduleTick(context: Context) {
            val am = context.getSystemService(AlarmManager::class.java) ?: return
            val intent = Intent(context, MiqatWidgetProvider::class.java).apply {
                action = Intent.ACTION_TIME_TICK
            }
            val pending = PendingIntent.getBroadcast(
                context, TICK_REQUEST, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            // inexact on purpose: a widget does not deserve an exact alarm
            am.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 30_000L,
                60_000L,
                pending,
            )
        }

        private fun cancelTick(context: Context) {
            val am = context.getSystemService(AlarmManager::class.java) ?: return
            val intent = Intent(context, MiqatWidgetProvider::class.java).apply {
                action = Intent.ACTION_TIME_TICK
            }
            val pending = PendingIntent.getBroadcast(
                context, TICK_REQUEST, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            am.cancel(pending)
        }

        /** Build and push one widget. */
        private fun push(context: Context, manager: AppWidgetManager, id: Int) {
            val state = AppStateHolder.state(context)
            val now = System.currentTimeMillis()
            val fmt = Fmt(state.display)
            val (y, m, d) = state.cityDate(now)
            val times = state.times(y, m, d)
            val (ty, tm, td) = state.cityTomorrow(now)
            val tomorrow = state.times(ty, tm, td)
            val timers = Timers.of(
                prayerPoints(times, tomorrow.fajr, PRAYER_NAMES),
                state.cityHourOfDay(now),
                state.notifications.iqamaMinutes,
            )

            val views = android.widget.RemoteViews(context.packageName, R.layout.widget_miqat)

            // the horizon: the same shape as the hero, at the day's real progress
            val fajrMinute = minutesOf(times.fajr)
            val ishaMinute = minutesOf(times.isha)
            val travelled = DayArc.parameterFor(
                state.cityHourOfDay(now) * 60.0, fajrMinute.toDouble(), ishaMinute.toDouble(),
            )
            val nextIndex = DayArc.prayerIndex(timers.nextPrayer)
            views.setImageViewBitmap(
                R.id.widget_arc,
                ArcRenderer.render(520, 150, travelled, DayArc.tickFor(nextIndex)),
            )

            // the next prayer — the one thing a widget must say
            views.setTextViewText(R.id.widget_prayer, timers.nextPrayer ?: "—")
            views.setTextViewText(R.id.widget_countdown, fmt.hms(timers.remainingSeconds))
            views.setTextViewText(
                R.id.widget_meta,
                (state.city.ar.ifBlank { state.city.en }) + " · " +
                    fmt.hhmm(minutesOf(times.list.getOrElse(nextIndex) { times.isha })) +
                    if (timers.nextIsTomorrow) " · غداً" else "",
            )

            // tapping it opens the app
            val open = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)

            manager.updateAppWidget(id, views)
        }

        private fun minutesOf(hours: Double): Int =
            (((hours % 24.0) + 24.0) % 24.0 * 60.0).toInt()
    }
}
