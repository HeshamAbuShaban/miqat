package app.miqat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miqat.core.ActsCatalog
import app.miqat.core.ClosingList
import app.miqat.core.CloseRole
import app.miqat.core.DayLine
import app.miqat.core.Hijri
import app.miqat.core.Moments
import app.miqat.core.Moment
import app.miqat.core.Proximity
import app.miqat.core.TimerKind

/**
 * THE ACTS — «مواقيت السنة والعبادات»: a flat, time-ordered INDEX of every
 * act of worship today. One rich ribbon + one live panel + quiet rows.
 */
@Composable
fun ActsScreen(
    state: AppState,
    fmt: Fmt,
    nowState: androidx.compose.runtime.State<Long>,
    onOpenAct: (String) -> Unit,
    onOpenTasbih: () -> Unit = {},
    onOpenDistance: () -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val minuteBucket by androidx.compose.runtime.derivedStateOf {
        nowState.value / 60_000L
    }
    val now = minuteBucket * 60_000L
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
    val closeLine = DayLine.of(
        state.times(y, m, d), state.times(ty, tm, td).fajr,
        PRAYER_NAMES, ::minutesOf, state.imsakLeadMinutes,
    )
    val closings = ClosingList.of(closeLine, nowMinute)

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
            "الطاعات",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = Kufi, fontSize = 27.sp, fontWeight = FontWeight.Bold,
                lineHeight = 32.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            Hijri.label(Hijri.at(y, m, d, state.display.hijriOffsetDays), fmt.ar),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Space.md))

        // DAY RIBBON
        DayRibbon(state, y, m, d, timeline, live, nowMinute, fmt, scheme)

        Spacer(Modifier.height(Space.lg))

        // NOW / NEXT PANEL
        val running = live.firstOrNull { it.state == ActsCatalog.ActState.RUNNING }
        val next = live.firstOrNull { it.state == ActsCatalog.ActState.UPCOMING }
        val lastPassed = live.filter { it.state == ActsCatalog.ActState.PASSED }.maxByOrNull { it.startMinute }
        // The panel is a live act too: it must open the same detail (with the
        // citation and the computation) as any row. The old running row did.
        val panelAct = running ?: next ?: lastPassed

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.panel))
                .background(scheme.surface)
                .border(1.dp, scheme.outline, RoundedCornerShape(Radius.panel))
                .clickable { panelAct?.let { onOpenAct(it.act.id) } },
        ) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(scheme.secondary))
            Column(Modifier.weight(1f).padding(16.dp)) {
                if (running != null) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "جارٍ الآن",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                            color = scheme.secondary,
                        )
                        val pos = live.sortedBy { it.startMinute }.indexOfFirst { it == running } + 1
                        Text(
                            "${fmt.n(pos)} من ${fmt.n(live.size)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        running.act.name,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = Kufi, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        ),
                        color = scheme.onSurface,
                    )
                    Text(
                        fmt.range(running.startMinute, running.endMinute),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = Plex),
                        color = scheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Space.md))
                    val span = (running.endMinute - running.startMinute).coerceAtLeast(1)
                    val frac = ((nowMinute - running.startMinute).toFloat() / span).coerceIn(0f, 1f)
                    Box(
                        Modifier.fillMaxWidth().height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(scheme.outlineVariant),
                    ) {
                        Box(
                            Modifier.fillMaxWidth(frac).height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(scheme.secondary),
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = Space.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "مضى ${fmt.duration((nowMinute - running.startMinute).coerceAtLeast(0))}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                            color = scheme.onSurfaceVariant,
                        )
                        Text(
                            "بقي ${fmt.duration(running.minutesAway.coerceAtLeast(0))}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                            color = scheme.secondary,
                        )
                    }
                    if (state.display.showClosingEvents && closings.isNotEmpty()) {
                        Spacer(Modifier.height(Space.xs))
                        closings.take(2).forEach { c ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    c.slot.labelAr + " · " + closeRoleText(c.role),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = Plex),
                                    color = scheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    if (c.role == CloseRole.ANCHOR) "جارٍ"
                                    else fmt.duration(c.startIn.coerceAtLeast(0)),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = Kufi, fontFeatureSettings = TabularFigures,
                                    ),
                                    color = scheme.secondary,
                                )
                            }
                        }
                    } else if (running.act.source != null) {
                        Spacer(Modifier.height(Space.xs))
                        Text(
                            running.act.source,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else if (next != null) {
                    Text(
                        "التالي",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                        color = scheme.secondary,
                    )
                    Text(
                        next.act.name,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = Kufi, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        ),
                        color = scheme.onSurface,
                    )
                    Text(
                        if (next.act.isWindow || next.act.isBoundary)
                            fmt.range(next.startMinute, next.endMinute)
                        else fmt.hhmm(next.startMinute),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = Plex),
                        color = scheme.onSurfaceVariant,
                    )
                    // The closing rows already carry this countdown. Print
                    // it once, not twice under two different labels.
                    val showClosings = state.display.showClosingEvents && closings.isNotEmpty()
                    if (!showClosings) {
                        Text(
                            "بعد ${fmt.duration(next.minutesAway)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                            color = scheme.secondary,
                        )
                    }
                    if (showClosings) {
                        Spacer(Modifier.height(Space.xs))
                        closings.take(2).forEach { c ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    c.slot.labelAr + " · " + closeRoleText(c.role),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = Plex),
                                    color = scheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    if (c.role == CloseRole.ANCHOR) "جارٍ"
                                    else fmt.duration(c.startIn.coerceAtLeast(0)),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = Kufi, fontFeatureSettings = TabularFigures,
                                    ),
                                    color = scheme.secondary,
                                )
                            }
                        }
                    } else if (next.act.source != null) {
                        Spacer(Modifier.height(Space.xs))
                        Text(
                            next.act.source,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else if (lastPassed != null) {
                    Text(
                        "مضى اليوم",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                        color = scheme.onSurfaceVariant,
                    )
                    Text(
                        lastPassed.act.name,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = Kufi, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        ),
                        color = scheme.onSurface,
                    )
                    Text(
                        if (lastPassed.act.isWindow || lastPassed.act.isBoundary)
                            fmt.range(lastPassed.startMinute, lastPassed.endMinute)
                        else fmt.hhmm(lastPassed.startMinute),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = Plex),
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(Space.lg))

        // QUICK LOG
        if (state.display.showQuickLog) {
            val dayKey = "%04d-%02d-%02d".format(y, m, d)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                listOf("prayer" to "سجّل صلاة", "dhikr" to "سجّل ذكر", "fast" to "سجّل صيام")
                    .forEach { (kind, label) ->
                        val key = "$dayKey:$kind"
                        val on = key in state.display.dayActLog
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (on) scheme.secondary.copy(alpha = 0.12f) else scheme.surface)
                                .border(1.dp, scheme.outline, RoundedCornerShape(12.dp))
                                .clickable {
                                    state.updateDisplay(
                                        state.display.copy(
                                            dayActLog = if (on) state.display.dayActLog - key
                                            else state.display.dayActLog + key,
                                        ),
                                    )
                                }
                                .padding(horizontal = Space.md, vertical = Space.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (on) "✓" else "+",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Kufi),
                                color = if (on) scheme.secondary else scheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(Space.xs))
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                                color = scheme.onSurface,
                            )
                        }
                    }
            }
            Spacer(Modifier.height(Space.md))
        }

        // ROWS
        val upcoming = live.filter { it.state == ActsCatalog.ActState.UPCOMING }
        val passed = live.filter { it.state == ActsCatalog.ActState.PASSED }
        if (upcoming.isNotEmpty()) {
            SectionLabel("قادم · الأقرب أولاً")
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(scheme.surface),
            ) {
                upcoming.sortedBy { it.startMinute }.forEachIndexed { i, item ->
                    ActRow(item, fmt) { onOpenAct(item.act.id) }
                    if (i < upcoming.size - 1) Divider(Modifier.padding(horizontal = Space.md))
                }
            }
            Spacer(Modifier.height(Space.md))
        }
        if (state.display.showPassedActs && passed.isNotEmpty()) {
            SectionLabel("مضى اليوم")
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(scheme.surface),
            ) {
                passed.sortedByDescending { it.startMinute }.forEachIndexed { i, item ->
                    ActRow(item, fmt) { onOpenAct(item.act.id) }
                    if (i < passed.size - 1) Divider(Modifier.padding(horizontal = Space.md))
                }
            }
            Spacer(Modifier.height(Space.md))
        }

        Spacer(Modifier.height(Space.md))
        CtaTile("التسبيح", "أذكار اليوم مع مراجعها وعدّاد", Glyph.SAJDA, onOpenTasbih, scheme)
        Spacer(Modifier.height(Space.sm))
        CtaTile("كم بعدت عن هذا الوقت؟", "المسافة بين أي لحظتين", Glyph.TICKS, onOpenDistance, scheme)
        Spacer(Modifier.height(Space.xxl))
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
                            TimerKind.ELAPSED, live.elapsedMinutes * 60L, act.name.removePrefix("وقت "),
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