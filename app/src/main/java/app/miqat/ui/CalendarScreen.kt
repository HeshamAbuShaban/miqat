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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.miqat.core.Hijri
import app.miqat.core.IslamicDay
import app.miqat.core.IslamicDays
import app.miqat.core.Moment
import app.miqat.core.Moments
import java.util.Calendar

/**
 * CALENDAR (R4) — the month, computed on demand («وضع المدار»: only the visible
 * month is computed, and only while this screen is open).
 *
 * Each cell carries the Gregorian day and the Hijri day; an event dot marks
 * the Islamic days worth knowing (eids, Arafah, Ashura, the white days). Under
 * the grid, the month's events are listed — a calendar that only shows dots
 * makes you tap every day to find out what they mean.
 */
@Composable
fun CalendarScreen(
    state: AppState,
    fmt: Fmt,
    onOpenDay: (Int, Int, Int) -> Unit,
) {
    val now = System.currentTimeMillis()
    val (cy, cm, cd) = state.cityDate(now)
    var year by remember { mutableIntStateOf(cy) }
    var month by remember { mutableIntStateOf(cm) }

    // the visible month's grid — computed lazily, only this month
    val cells = remember(year, month, state.city.lat, state.city.lon, state.utcOffset) {
        monthGrid(year, month, state.display.hijriOffsetDays)
    }
    val hijriLabel = cells.firstOrNull { it != null }?.let {
        Hijri.MONTHS_AR[(it.hijri.hMonth - 1).coerceIn(0, 11)]
    }
    val events = remember(year, month, state.display.hijriOffsetDays) {
        monthEvents(year, month, state.display.hijriOffsetDays)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.lg),
    ) {
        Spacer(Modifier.height(Space.md))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "الشهر السابق") {
                month--; if (month < 1) { month = 12; year-- }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${fmt.n(month)} / ${fmt.n(year)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (hijriLabel != null) {
                    Text(
                        "$hijriLabel ${fmt.n(cells.firstNotNullOfOrNull { it?.hijri?.hYear } ?: 0)}هـ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "الشهر التالي") {
                month++; if (month > 12) { month = 1; year++ }
            }
        }
        Spacer(Modifier.height(Space.xs))
        Row(Modifier.fillMaxWidth()) {
            ARABIC_WEEKDAYS.forEach { d ->
                Text(
                    d,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(Space.xs))
        // a plain 7-column grid (six rows) — no lazy viewport, so the screen
        // has no dead band and the events below scroll with it
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { cell ->
                    Box(Modifier.weight(1f)) {
                        if (cell == null) {
                            Box(Modifier.size(52.dp))
                        } else {
                            DayCell(
                                day = cell.gregorianDay,
                                hijri = cell.hijri,
                                isToday = cell.isToday,
                                event = cell.event,
                                fmt = fmt,
                                onClick = { onOpenDay(year, month, cell.gregorianDay) },
                            )
                        }
                    }
                }
                repeat(7 - week.size) { Box(Modifier.weight(1f)) }
            }
        }

        // the month's events — the dots explained
        if (events.isNotEmpty()) {
            Divider(Modifier.padding(top = Space.sm))
            Column(Modifier.padding(vertical = Space.sm)) {
                events.forEach { e ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDay(year, month, e.gregorianDay) }
                            .padding(vertical = Space.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                        )
                        Spacer(Modifier.width(Space.md))
                        Text(
                            e.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${fmt.n(e.hijriDay)} ${Hijri.MONTHS_AR[(e.hijriMonth - 1).coerceIn(0, 11)]} · ${fmt.n(e.gregorianDay)} ${fmt.n(month)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (year != cy || month != cm) {
            Text(
                "العودة إلى شهر اليوم",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { year = cy; month = cm }
                    .padding(vertical = Space.sm),
            )
        }
        Spacer(Modifier.height(Space.md))
    }
}

@Composable
private fun IconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = desc, tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun DayCell(
    day: Int,
    hijri: IslamicDay,
    isToday: Boolean,
    event: String?,
    fmt: Fmt,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(2.dp)
            .clip(RoundedCornerShape(Radius.row))
            .background(
                if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            )
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            fmt.n(day),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            fmt.n(hijri.hDay),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (event != null) {
            Box(
                Modifier
                    .padding(top = 2.dp)
                    .size(5.dp)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape),
            )
        }
    }
}

/** The visible month's cells (Gregorian day + its Hijri day + event marker). */
private data class Cell(
    val gregorianDay: Int,
    val hijri: IslamicDay,
    val isToday: Boolean,
    val event: String?,
)

/** An event of the month, with the Gregorian day it falls on. */
private data class MonthEvent(
    val title: String,
    val gregorianDay: Int,
    val hijriDay: Int,
    val hijriMonth: Int,
)

private fun monthGrid(year: Int, month: Int, hijriOffset: Int): List<Cell?> {
    val cal = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, 1)
    }
    val firstWeekday = cal.get(Calendar.DAY_OF_WEEK) % 7 // Saturday = 0
    val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val today = Calendar.getInstance()
    val cells = mutableListOf<Cell?>()
    repeat(firstWeekday) { cells.add(null) }
    for (d in 1..days) {
        val hijri = Hijri.at(year, month, d)
        cells.add(
            Cell(
                gregorianDay = d,
                hijri = hijri,
                isToday = today.get(Calendar.YEAR) == year &&
                    today.get(Calendar.MONTH) + 1 == month &&
                    today.get(Calendar.DAY_OF_MONTH) == d,
                event = eventTitle(hijri),
            ),
        )
    }
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}

private fun eventTitle(h: IslamicDay): String? = when {
    IslamicDays.isEidAlFitr(h) -> "عيد الفطر"
    IslamicDays.isEidAlAdha(h) -> "عيد الأضحى"
    IslamicDays.isDayOfArafah(h) -> "يوم عرفة"
    IslamicDays.isAshura(h) -> "عاشوراء"
    IslamicDays.isTasuaa(h) -> "تاسوعاء"
    IslamicDays.isWhiteDays(h) -> "أيام البيض"
    IslamicDays.isEveOfRamadan(h) -> "ليلة رمضان"
    else -> null
}

private fun monthEvents(year: Int, month: Int, hijriOffset: Int): List<MonthEvent> {
    val out = mutableListOf<MonthEvent>()
    val cal = Calendar.getInstance().apply { clear(); set(year, month - 1, 1) }
    val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (d in 1..days) {
        val h = Hijri.at(year, month, d)
        val title = eventTitle(h) ?: continue
        out.add(MonthEvent(title, d, h.hDay, h.hMonth))
    }
    return out
}

private val ARABIC_WEEKDAYS = listOf("سبت", "أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة")

/**
 * A single day: its prayers, its worship windows and the gaps between them —
 * computed on demand for that date, so the whole year is never precomputed.
 */
@Composable
fun DayDetailScreen(state: AppState, fmt: Fmt, year: Int, month: Int, day: Int, onBack: () -> Unit) {
    val times = state.times(year, month, day)
    val tomorrow = state.times(
        if (month == 12) year + 1 else year,
        if (month == 12) 1 else month + 1,
        day,
    )
    val timeline = remember(year, month, day, state.city.calcMethod, state.display.methodOverride, state.adjusts) {
        Moments.timeline(times, tomorrow.fajr, PRAYER_NAMES, ::minutesOf)
    }
    val hijri = Hijri.at(year, month, day)

    MiqatScaffold("${fmt.n(day)} / ${fmt.n(month)} / ${fmt.n(year)}", onBack = onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg),
        ) {
            Spacer(Modifier.height(Space.sm))
            Text(
                Hijri.label(hijri, fmt.ar),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.md))

            val gaps = Moments.gaps(timeline).associateBy { it.from.kind }
            PRAYER_NAMES.forEachIndexed { i, name ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = Space.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        fmt.hhmm(minutesOf(times.list[i])),
                        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TabularFigures),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                val gap = gaps[PRAYER_KEY[i]]
                if (gap != null) {
                    GapBridge(gap.minutes, fmt.duration(gap.minutes))
                }
                Divider()
            }

            Spacer(Modifier.height(Space.md))
            Text(
                "أوقات أخرى في هذا اليوم",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Space.xs))
            listOf(
                "وقت الضحى" to Moments.DUHA_START,
                "وقت الأسحار" to Moments.SAHAR_START,
                "منتصف الليل" to Moments.MIDNIGHT,
                "الثلث الأخير من الليل" to Moments.LAST_THIRD,
            ).forEach { (label, kind) ->
                val m = timeline.firstOrNull { it.kind == kind }
                if (m != null) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = Space.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            fmt.hhmm(m.minuteOfDay),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TabularFigures),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(Space.lg))
            Text(
                "محسوبة عند الطلب — بلا جدولة مسبقة لكل أيام السنة.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.xxl))
        }
    }
}
