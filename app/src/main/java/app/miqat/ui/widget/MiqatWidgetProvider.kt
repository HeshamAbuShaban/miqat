package app.miqat.ui.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import app.miqat.MainActivity
import app.miqat.R
import app.miqat.core.AppStateHolder
import app.miqat.core.Timers
import app.miqat.core.prayerPoints
import app.miqat.ui.DayArc
import app.miqat.ui.Fmt
import app.miqat.ui.PRAYER_NAMES
import app.miqat.ui.Sky
import app.miqat.ui.list

/**
 * THE WIDGET — the app's hero at a smaller size.
 *
 * It says the one thing a widget must say — «which prayer is next, and how
 * long until it» — over the SAME horizon, the SAME sky and the SAME ticks
 * the app's hero draws ([ArcRenderer]).
 *
 * It is honest about being a widget:
 *  - the countdown refreshes on the widget's OWN tick (a repeating alarm the
 *    provider re-arms on every update, every resize, every enable and every
 *    boot — the old code armed it once, on `onEnabled` alone, so after a
 *    package replace the countdown stood still for minutes: a lie with a
 *    ticking costume);
 *  - the countdown is printed in MINUTES, never seconds — a widget that
 *    cannot keep seconds true must not print them;
 *  - the next prayer's clock time is tomorrow's Fajr when the next prayer
 *    is tomorrow's;
 *  - the city appears only when the user opted in AND the times actually
 *    come from that city (never under a GPS fix — the same rule the Home
 *    screen keeps).
 */
class MiqatWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        armAndRefresh(context)
        ids.forEach { id -> push(context, manager, id) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        id: Int,
        newOptions: Bundle?,
    ) {
        armAndRefresh(context)
        push(context, manager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_WIDGET_TICK,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            ACTION_STATE_CHANGED,
            -> armAndRefresh(context)
        }
    }

    override fun onEnabled(context: Context) {
        armAndRefresh(context)
    }

    override fun onDisabled(context: Context) {
        cancelTick(context)
    }

    companion object {
        /** The app tells the widget when its own state changed. */
        const val ACTION_STATE_CHANGED = "app.miqat.action.WIDGET_STATE_CHANGED"

        /** The widget's own minute tick (never the protected system one). */
        private const val ACTION_WIDGET_TICK = "app.miqat.action.WIDGET_TICK"

        private const val TICK_REQUEST = 4411

        /** Arm the minute tick, then refresh every instance. */
        fun armAndRefresh(context: Context) {
            scheduleTick(context)
            refresh(context)
        }

        /** Refresh every widget instance. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, MiqatWidgetProvider::class.java))
            ids.forEach { push(context, manager, it) }
        }

        /** A minute tick, so the countdown stays honest. */
        private fun scheduleTick(context: Context) {
            val am = context.getSystemService(AlarmManager::class.java) ?: return
            am.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 30_000L,
                60_000L,
                tickPending(context),
            )
        }

        private fun cancelTick(context: Context) {
            val am = context.getSystemService(AlarmManager::class.java) ?: return
            am.cancel(tickPending(context))
        }

        private fun tickPending(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context, TICK_REQUEST,
                Intent(context, MiqatWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_TICK
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

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

            // the sky — the hour's phase, from the real sunrise and sunset
            val sunriseHour = times.list.getOrNull(1) ?: 6.0
            val maghribHour = times.list.getOrNull(4) ?: 18.0
            val skyPhase = Sky.phaseOf(state.cityHourOfDay(now), sunriseHour, maghribHour)

            // the horizon at the day's real progress, with the prayers at
            // their REAL times — the same numbers the hero reads
            val fajrMinute = minutesOf(times.fajr).toDouble()
            val ishaMinute = minutesOf(times.isha).toDouble()
            val travelled = DayArc.parameterFor(
                state.cityHourOfDay(now) * 60.0, fajrMinute, ishaMinute,
            )
            val nextIndex = DayArc.prayerIndex(timers.nextPrayer)
            val minutesOfDay = times.list.map { minutesOf(it).toDouble() }

            // the panel's real width (the bitmap must fill the row, not
            // letterbox into a third of it)
            val opts = manager.getAppWidgetOptions(id)
            val widthDp = (opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                .takeIf { it > 0 } ?: 320)
            val density = context.resources.displayMetrics.density
            val widthPx = (widthDp * density).toInt().coerceIn(160, 1200)
            val heightPx = (96 * density).toInt()

            views.setImageViewBitmap(
                R.id.widget_arc,
                ArcRenderer.render(widthPx, heightPx, travelled, DayArc.tickParams(minutesOfDay, fajrMinute, ishaMinute), nextIndex, skyPhase),
            )

            // the next prayer — the one thing a widget must say
            val remainingMinutes = ((timers.remainingSeconds + 59) / 60).toInt()
            views.setTextViewText(R.id.widget_prayer, timers.nextPrayer ?: "—")
            views.setTextViewText(R.id.widget_countdown, fmt.duration(remainingMinutes))
            // the countdown reacts as the appointed time approaches — the
            // app's own urgency tokens (≤15 min saffron, ≤5 min sunset)
            views.setTextColor(
                R.id.widget_countdown,
                when {
                    remainingMinutes <= 5 -> 0xFFFF7A59.toInt()
                    remainingMinutes <= 15 -> 0xFFFFB25C.toInt()
                    else -> 0xFFEDEBE3.toInt()
                },
            )
            // the caption: the clock time of the prayer that is coming —
            // tomorrow's Fajr when it is tomorrow's (the old bug: it printed
            // today's clock time under a «غداً» label)
            val nextClock = if (timers.nextIsTomorrow) minutesOf(tomorrow.fajr)
            else minutesOf(times.list.getOrElse(nextIndex) { times.isha })
            var meta = fmt.hhmm(nextClock)
            if (timers.nextIsTomorrow) meta += " · غداً"
            // the city, only when the times actually come from it — never
            // under a GPS fix, and never without the user's opt-in
            if (state.gpsFix == null && state.display.widgetShowCity) {
                val city = state.city.ar.ifBlank { state.city.en }
                if (city.isNotBlank()) meta = "$city · $meta"
            }
            views.setTextViewText(R.id.widget_meta, meta)

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
