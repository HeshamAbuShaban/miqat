package app.miqat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.miqat.core.ActsCatalog
import app.miqat.core.Hijri
import app.miqat.core.Moments
import app.miqat.core.Proximity
import app.miqat.core.TimerKind

/**
 * THE ACTS — «مواقيت السنة والعبادات»: a flat, time-ordered INDEX of every
 * act of worship today (the council's routing: the list is the index, not the
 * teaching). Each row: the time, the name, and the hook — what it is, in
 * three words. Tapping a row opens the act's own screen, where the depth
 * lives (what it is, why it matters, the rule, the source, the state).
 */
@Composable
fun ActsScreen(
    state: AppState,
    fmt: Fmt,
    nowState: androidx.compose.runtime.State<Long>,
    onOpenAct: (String) -> Unit,
    onOpenTasbih: () -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val now = nowState.value
    val nowMinute = (state.cityHourOfDay(now) * 60).toInt()
    val (y, m, d) = state.cityDate(now)
    val (ty, tm, td) = state.cityTomorrow(now)
    val timeline = remember(y, m, d, state.city.calcMethod, state.display.methodOverride, state.adjusts) {
        Moments.timeline(
            state.times(y, m, d), state.times(ty, tm, td).fajr, PRAYER_NAMES, ::minutesOf,
            hijriMonth = Hijri.at(y, m, d, state.display.hijriOffsetDays).hMonth,
            hijriDay = Hijri.at(y, m, d, state.display.hijriOffsetDays).hDay,
            imsakLeadMinutes = state.imsakLeadMinutes,
        )
    }
    val acts = remember { ActsCatalog.acts(context) }
    val live = remember(acts, timeline, nowMinute) { ActsCatalog.live(acts, timeline, nowMinute) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.lg),
    ) {
        Spacer(Modifier.height(Space.md))
        Text(
            "مواقيت العبادات",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            "كل ما في التطبيق من أوقات الصلاة والعبادات والسنة — مرتّباً على وقت اليوم",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Space.md))

        // THE ACT RAIL — the day as a SHAPE: every act at its real geometry,
        // not a list of labels with a time stapled on (the founder: «الطاعات
        // توضح بغير هذا الشكل .. أصلا الشكل الحالي لا ينم عن أي طاعة»).
        ActRail(state, year = y, month = m, dayOfMonth = d, live, nowMinute)
        Spacer(Modifier.height(Space.lg))

        // grouped by STATE — «الأقرب فالأقرب»: what is open, then what comes,
        // then what passed (never a fake countdown for a passed act)
        val order = mapOf(
            ActsCatalog.ActState.RUNNING to 0,
            ActsCatalog.ActState.UPCOMING to 1,
            ActsCatalog.ActState.PASSED to 2,
        )
        live.sortedBy { order[it.state] }.forEach { item ->
            ActRow(item, fmt) { onOpenAct(item.act.id) }
            Divider()
        }
        Spacer(Modifier.height(Space.md))
        NavRow("التسبيح", "أذكار اليوم مع مراجعها وعدّاد", onOpenTasbih)
        Spacer(Modifier.height(Space.xxl))
    }
}


/**
 * The day as a rail: RTL, 00:00 at the right, 24:00 at the left. A window is
 * a teal bar, a point is a gold dot, the fast is a bracket between two gold
 * pins, and NOW is one saffron tick — the legend the app already teaches.
 */
@Composable
private fun ActRail(
    state: AppState, year: Int, month: Int, dayOfMonth: Int,
    items: List<ActsCatalog.Live>, nowMinute: Int,
) {
    val scheme = MaterialTheme.colorScheme
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        val w = size.width
        val h = size.height
        val y = h * 0.55f
        val xOf: (Int) -> Float = { m -> w * (0.92f - 0.84f * (m.coerceIn(0, 1440) / 1440f)) }
        // the day's REAL light: night before Fajr / after Maghrib, daylight
        // between sunrise and Maghrib — the rail finally has a scale
        val times = state.times(year, month, dayOfMonth)
        val sunrise = minutesOf(times.sunrise)
        val maghrib = minutesOf(times.maghrib)
        drawLine(
            scheme.surfaceVariant,
            Offset(xOf(0), y), Offset(xOf(sunrise), y),
            strokeWidth = 7f, cap = StrokeCap.Round,
        )
        drawLine(
            Sky.sunMark.copy(alpha = 0.12f),
            Offset(xOf(sunrise), y), Offset(xOf(maghrib), y),
            strokeWidth = 7f,
        )
        drawLine(
            scheme.surfaceVariant,
            Offset(xOf(maghrib), y), Offset(xOf(1440), y),
            strokeWidth = 7f, cap = StrokeCap.Round,
        )
        // the spine
        drawLine(
            scheme.outlineVariant,
            Offset(xOf(0), y), Offset(xOf(1440), y),
            strokeWidth = 1f,
        )
        items.forEach { item ->
            when {
                item.act.isWindow -> drawLine(
                    scheme.secondary.copy(alpha = if (item.state == ActsCatalog.ActState.PASSED) 0.3f else 0.9f),
                    Offset(xOf(item.startMinute), y), Offset(xOf(item.endMinute), y),
                    strokeWidth = 5f, cap = StrokeCap.Round,
                )
                item.act.isBoundary -> {
                    drawLine(
                        scheme.secondary.copy(alpha = 0.7f),
                        Offset(xOf(item.startMinute), y - 6f), Offset(xOf(item.endMinute), y - 6f),
                        strokeWidth = 2f,
                    )
                    listOf(item.startMinute, item.endMinute).forEach { minute ->
                        drawLine(
                            scheme.secondary.copy(alpha = 0.7f),
                            Offset(xOf(minute), y - 6f), Offset(xOf(minute), y),
                            strokeWidth = 2f,
                        )
                        drawCircle(scheme.primary, 3f, Offset(xOf(minute), y))
                    }
                }
                else -> drawCircle(
                    scheme.primary, 3.5f, Offset(xOf(item.startMinute), y),
                )
            }
        }
        // now — the only saffron mark on the rail
        drawLine(
            app.miqat.ui.Sky.sunMark,
            Offset(xOf(nowMinute), y - 12f), Offset(xOf(nowMinute), y + 12f),
            strokeWidth = 2f,
        )
    }
}
@Composable
private fun ActRow(item: ActsCatalog.Live, fmt: Fmt, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val running = item.state == ActsCatalog.ActState.RUNNING
    val passed = item.state == ActsCatalog.ActState.PASSED
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // the state: a dot, the moment, and what it is
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    when {
                        running -> scheme.secondary
                        passed -> scheme.outline
                        else -> scheme.primary
                    },
                ),
        )
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(
                item.act.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (passed) scheme.onSurfaceVariant else scheme.onSurface,
            )
            Text(
                item.act.what,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            Text(
                item.act.source ?: "تقديري بحسب الاصطلاح",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(Space.md))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (item.act.isWindow || item.act.isBoundary)
                    "${fmt.hhmm(item.startMinute)} — ${fmt.hhmm(item.endMinute)}"
                else fmt.hhmm(item.startMinute),
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TabularFigures),
                color = if (running) scheme.secondary else scheme.onSurface,
            )
            Text(
                when (item.state) {
                    ActsCatalog.ActState.UPCOMING -> Proximity.timerLine(
                        TimerKind.ACT_REMAINING, item.minutesAway * 60L, item.act.name,
                    ).prefixAr + " " + fmt.duration(item.minutesAway)
                    ActsCatalog.ActState.RUNNING -> Proximity.timerLine(
                        TimerKind.AVAILABLE, item.minutesAway * 60L, item.act.name,
                    ).prefixAr + " " + fmt.duration(item.minutesAway)
                    ActsCatalog.ActState.PASSED -> "مضى"
                },
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The act's own screen: what it is, why it matters, how it is computed, where
 * its reference is, and what the app can do about it.
 */
@Composable
fun ActDetailScreen(
    state: AppState,
    fmt: Fmt,
    nowState: androidx.compose.runtime.State<Long>,
    actId: String,
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val now = nowState.value
    val nowMinute = (state.cityHourOfDay(now) * 60).toInt()
    val (y, m, d) = state.cityDate(now)
    val (ty, tm, td) = state.cityTomorrow(now)
    val timeline = remember(y, m, d, state.city.calcMethod, state.display.methodOverride, state.adjusts) {
        Moments.timeline(
            state.times(y, m, d), state.times(ty, tm, td).fajr, PRAYER_NAMES, ::minutesOf,
            hijriMonth = Hijri.at(y, m, d, state.display.hijriOffsetDays).hMonth,
            hijriDay = Hijri.at(y, m, d, state.display.hijriOffsetDays).hDay,
            imsakLeadMinutes = state.imsakLeadMinutes,
        )
    }
    val acts = remember { ActsCatalog.acts(context) }
    val act = acts.firstOrNull { it.id == actId }
    val live = remember(acts, timeline, nowMinute) { ActsCatalog.live(acts, timeline, nowMinute) }
        .firstOrNull { it.act.id == actId }

    MiqatScaffold(act?.name ?: "العبادة", onBack = onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.lg),
        ) {
            Spacer(Modifier.height(Space.sm))
            if (act != null && live != null) {
                // the window, big
                Text(
                    if (act.isBoundary) {
                        "${fmt.hhmm(live.startMinute)} ← ${fmt.hhmm(live.endMinute)}"
                    } else {
                        fmt.hhmm(live.startMinute)
                    },
                    style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = TabularFigures),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    when (live.state) {
                        ActsCatalog.ActState.UPCOMING -> Proximity.timerLine(
                            TimerKind.ACT_REMAINING, live.minutesAway * 60L, act.name,
                        ).prefixAr + " " + fmt.duration(live.minutesAway)
                        ActsCatalog.ActState.RUNNING -> Proximity.timerLine(
                            TimerKind.AVAILABLE, live.minutesAway * 60L, act.name,
                        ).prefixAr + " " + fmt.duration(live.minutesAway)
                        ActsCatalog.ActState.PASSED -> "مضى اليوم"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                if (live.elapsedMinutes != null) {
                    Text(
                        Proximity.timerLine(
                            TimerKind.ELAPSED, live.elapsedMinutes * 60L, act.name,
                        ).prefixAr + " " + fmt.duration(live.elapsedMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(Space.md))

                Text("ما هو", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text(act.what, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Space.md))
                Text("لماذا يهم", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text(act.why, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Space.md))
                Text("كيف يُحسب", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text(act.rule, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Space.sm))
                // the honest disclosure of the convention
                Text(
                    "تقديري بحسب الاصطلاح",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.chip))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = Space.sm, vertical = 3.dp),
                )
                if (act.source != null) {
                    Spacer(Modifier.height(Space.md))
                    Text("المصدر", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text(act.source, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(Space.md))
                Text("التصنيف", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text(act.category, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(Space.xxl))
        }
    }
}
