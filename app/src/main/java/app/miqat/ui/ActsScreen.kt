package app.miqat.ui

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
            hijriMonth = Hijri.at(y, m, d).hMonth, hijriDay = Hijri.at(y, m, d).hDay,
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

        live.forEach { item ->
            ActRow(item, fmt) { onOpenAct(item.act.id) }
            Divider()
        }
        Spacer(Modifier.height(Space.md))
        NavRow("التسبيح", "أذكار اليوم مع مراجعها وعدّاد", onOpenTasbih)
        Spacer(Modifier.height(Space.xxl))
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
                        running -> scheme.tertiary
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
        }
        Spacer(Modifier.width(Space.md))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                fmt.hhmm(item.startMinute),
                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TabularFigures),
                color = if (running) scheme.tertiary else scheme.onSurface,
            )
            Text(
                when (item.state) {
                    ActsCatalog.ActState.UPCOMING -> "بعد ${fmt.n(item.minutesAway)} د"
                    ActsCatalog.ActState.RUNNING -> "جارٍ · يبقى ${fmt.n(item.minutesAway)} د"
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
            hijriMonth = Hijri.at(y, m, d).hMonth, hijriDay = Hijri.at(y, m, d).hDay,
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
                        ActsCatalog.ActState.UPCOMING -> "قادم · بعد ${fmt.n(live.minutesAway)} دقيقة"
                        ActsCatalog.ActState.RUNNING -> "جارٍ الآن · يبقى ${fmt.n(live.minutesAway)} دقيقة"
                        ActsCatalog.ActState.PASSED -> "مضى اليوم"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                if (live.elapsedMinutes != null) {
                    Text(
                        "مضى ${fmt.n(live.elapsedMinutes)} دقيقة من هذا الوقت",
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
