package app.miqat.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import app.miqat.MainActivity
import app.miqat.R
import app.miqat.core.AppStateHolder
import app.miqat.core.Timers
import app.miqat.core.prayerPoints
import app.miqat.ui.Fmt
import app.miqat.ui.PRAYER_NAMES

/**
 * THE COMPACT WIDGET — 2×1, the same facts as the full widget minus the sky.
 *
 * At 110dp a day arc would be a meaningless bow, so this tile says only
 * «which prayer, and how long» — the two facts that survive that size.
 * It is pushed by the SAME minute tick as the full widget, so the two can
 * never disagree.
 */
class MiqatCompactWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        MiqatWidgetProvider.armAndRefresh(context)
        ids.forEach { id -> push(context, manager, id) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            MiqatWidgetProvider.ACTION_STATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> refresh(context)
        }
    }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(
                ComponentName(context, MiqatCompactWidgetProvider::class.java),
            )
            ids.forEach { push(context, manager, it) }
        }

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
                iqamaFor = { state.iqamaMinutesFor(it) },
            )

            val views = android.widget.RemoteViews(context.packageName, R.layout.widget_compact)
            views.setTextViewText(R.id.compact_prayer, timers.nextPrayer ?: "—")
            val minutes = ((timers.remainingSeconds + 59) / 60).toInt()
            views.setTextViewText(R.id.compact_countdown, fmt.duration(minutes))
            views.setTextColor(
                R.id.compact_countdown,
                when {
                    minutes <= 5 -> 0xFFFF8566.toInt()
                    minutes <= 15 -> 0xFFF06544.toInt()
                    else -> 0xFFEBE8E0.toInt()
                },
            )
            val open = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_compact_root, open)
            manager.updateAppWidget(id, views)
        }
    }
}
