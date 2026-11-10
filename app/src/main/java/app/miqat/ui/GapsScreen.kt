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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.miqat.core.Gap
import app.miqat.core.Moment
import app.miqat.core.MomentCategory
import app.miqat.core.Moments

/**
 * GAPS — «الوقت بين كل صلاة واخرى بائن وواضح» (Hesham's brief; the donor
 * never showed this). Every consecutive pair of moments gets a row: the two
 * moments, the duration between them, and a bar whose width is the gap's
 * share of the longest one — the arc signature, drawn as a measure.
 */
@Composable
fun GapsScreen(state: AppState, fmt: Fmt, onBack: () -> Unit) {
    val nowState = rememberNowState()
    val now = nowState.value
    val timeline = rememberTimeline(state, now)
    val gaps = Moments.gaps(timeline)
    val longest = gaps.maxOfOrNull { it.minutes } ?: 1
    val visible = gaps.filter { state.display.gapCategories.contains(it.to.category.labelAr) }

    MiqatScaffold("المدد بين المواقيت", onBack = onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg),
        ) {
            Spacer(Modifier.height(Space.sm))
            Text(
                "كم يبقى بين كل موعد والذي يليه — حسب جدول اليوم",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.md))

            visible.forEach { gap ->
                GapRow(gap, fmt, longest)
                Divider()
            }

            Spacer(Modifier.height(Space.md))
            Text(
                "تظهر هذه المدد بين المواقيت في شاشة «المواقيت» حسب اختيارك من الإعدادات.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.xxl))
        }
    }
}

@Composable
private fun GapRow(gap: Gap, fmt: Fmt, longest: Int) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                gap.from.labelAr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                fmt.hhmm(gap.from.minuteOfDay),
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = TabularFigures),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            Modifier.width(92.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // the measure: the gap as a share of the longest one
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(gap.minutes.toFloat() / longest.toFloat())
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                fmt.duration(gap.minutes),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TabularFigures),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(
                gap.to.labelAr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                fmt.hhmm(gap.to.minuteOfDay),
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = TabularFigures),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * INTERVAL — «احسب الوقت بين موعدين اثنين ايا كان الصنف»: pick any two
 * moments (a prayer, sunrise, a worship window, a night marker, a sunnah
 * window) and get the distance between them, plus how long until it happens
 * from now. Useful for i'tikaf planning (Fajr → Duha) and more.
 */
@Composable
fun IntervalScreen(state: AppState, fmt: Fmt, onBack: () -> Unit) {
    val nowState = rememberNowState()
    val now = nowState.value
    val timeline = rememberTimeline(state, now)
    val nowMinute = state.cityHourOfDay(now) * 60

    var from by remember { mutableStateOf(timeline.firstOrNull { it.kind == Moments.FAJR }) }
    var to by remember { mutableStateOf(timeline.firstOrNull { it.kind == Moments.DUHA_START }) }
    var picking by remember { mutableStateOf<Moment?>(null) }

    MiqatScaffold("احسب بين ميقوتين", onBack = onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg),
        ) {
            Spacer(Modifier.height(Space.sm))
            Text(
                "اختر أي ميقوتين — من أي نوع — وستعرف المدة بينهما",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.md))

            NavRow(
                "من",
                from?.let { "${it.labelAr} — ${fmt.hhmm(it.minuteOfDay)}" } ?: "اختر",
                onClick = { picking = from },
            )
            Divider(Modifier.padding(horizontal = Space.lg))
            NavRow(
                "إلى",
                to?.let { "${it.labelAr} — ${fmt.hhmm(it.minuteOfDay)}" } ?: "اختر",
                onClick = { picking = to },
            )
            Divider(Modifier.padding(horizontal = Space.lg))

            if (from != null && to != null) {
                Spacer(Modifier.height(Space.lg))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(Radius.panel),
                        )
                        .padding(Space.lg),
                ) {
                    Text(
                        "المدة بين ${from!!.labelAr} و ${to!!.labelAr}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        fmt.duration(Moments.interval(from!!, to!!)),
                        style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = TabularFigures),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(Space.sm))
                    val until = Moments.until(to!!, nowMinute.toInt())
                    Text(
                        "من الآن حتى ${to!!.labelAr}: ${fmt.duration(until)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (from!!.category != to!!.category) {
                        Spacer(Modifier.height(Space.xs))
                        Text(
                            "موعدان من تصنيفين مختلفين — ${from!!.category.labelAr} و ${to!!.category.labelAr}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(Space.xxl))
        }
    }

    picking?.let { current ->
        MomentPickerDialog(
            moments = timeline,
            current = current,
            fmt = fmt,
            onPick = { picked ->
                if (picking?.let { it === from } == true) from = picked else to = picked
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}

@Composable
private fun MomentPickerDialog(
    moments: List<Moment>,
    current: Moment?,
    fmt: Fmt,
    onPick: (Moment) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختر ميقوتاً") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                MomentCategory.entries.forEach { category ->
                    val items = moments.filter { it.category == category }
                    if (items.isEmpty()) return@forEach
                    Text(
                        category.labelAr,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Space.sm, bottom = Space.xs),
                    )
                    items.forEach { m ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(m) }
                                .padding(vertical = Space.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                m.labelAr,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (m === current) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                fmt.hhmm(m.minuteOfDay),
                                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TabularFigures),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { Text("إغلاق", modifier = Modifier.clickable { onDismiss() }) },
    )
}

/** The day's timeline — shared by the gaps, interval and sunnah screens. */
@Composable
fun rememberTimeline(state: AppState, now: Long): List<Moment> {
    val (y, m, d) = state.cityDate(now)
    val (ty, tm, td) = state.cityTomorrow(now)
    return remember(y, m, d, state.city.calcMethod, state.display.methodOverride, state.adjusts) {
        val today = state.times(y, m, d)
        val tomorrow = state.times(ty, tm, td)
        Moments.timeline(today, tomorrow.fajr, PRAYER_NAMES, ::minutesOf)
    }
}
