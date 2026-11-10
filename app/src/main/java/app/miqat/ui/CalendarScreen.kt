package app.miqat.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.miqat.core.DstInfo
import app.miqat.core.Drift
import app.miqat.core.DriftPoint
import app.miqat.core.Hijri
import app.miqat.core.IslamicDay
import app.miqat.core.MiqatData
import app.miqat.core.Moments
import java.util.Calendar

/**
 * CALENDAR — rebuilt (R4, second pass).
 *
 * His words: «أبرز الهجري ودلّل بالميلادي بشكل ثانوي واضح»، «التحكم في الموعد
 * المراد عرضه و العناصر ما تعرضه مصغرا او مكبرا»، «في طريقة العرض … pertaining
 * to the long term: متى كانت تقام الصلاة في الشهر الفلاني»، and the drift:
 * «اظهار الفروق الزمنية على مدى الأشهر … ان الفجر يكون في وقته العادي ثم
 * يتجه نحو زيادة او نقصان».
 *
 * So the screen has THREE views, each its own job:
 *  1. الشهر — the month, Hijri-first (the Gregorian secondary and smaller).
 *  2. جدول الشهر — the month as a TIMETABLE: every day with the chosen
 *     prayer's time AND its day-to-day change (the drift, visible in the
 *     table itself), plus the day's transition marker when the clock changes.
 *  3. الانحراف على مدى السنة — the chosen prayer across the twelve months, as
 *     a curve with the TOTAL change stated in words.
 */
@Composable
fun CalendarScreen(
    state: AppState,
    fmt: Fmt,
    onOpenDay: (Int, Int, Int) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val now = System.currentTimeMillis()
    val (cy, cm, _) = state.cityDate(now)
    var year by remember { mutableIntStateOf(cy) }
    var month by remember { mutableIntStateOf(cm) }
    var view by remember { mutableStateOf(CalendarView.MONTH) }

    val d = state.display
    val cells = remember(year, month, d.hijriOffsetDays) {
        monthGrid(context, year, month, d.hijriOffsetDays)
    }
    val hijriLabel = cells.firstOrNull { it != null }?.let {
        Hijri.MONTHS_AR[(it.hijri.hMonth - 1).coerceIn(0, 11)]
    }
    val events = remember(year, month, d.hijriOffsetDays) {
        monthEvents(context, year, month, d.hijriOffsetDays)
    }
    val transitions = remember(state.city.zoneId, year) {
        state.city.zoneId?.let { DstInfo.transitionsInYear(context, it, year) } ?: emptyList()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.lg),
    ) {
        Spacer(Modifier.height(Space.md))

        // the three views — the screen's map
        Row(
            Modifier.fillMaxWidth().padding(bottom = Space.sm),
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            CalendarView.entries.forEach { v ->
                val selected = v == view
                Text(
                    v.labelAr,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(Radius.control))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { view = v }
                        .padding(vertical = Space.sm),
                    textAlign = TextAlign.Center,
                )
            }
        }

        when (view) {
            CalendarView.MONTH -> {
                MonthHeader(year, month, hijriLabel, fmt) { dir ->
                    if (dir < 0) { month--; if (month < 1) { month = 12; year-- } }
                    else { month++; if (month > 12) { month = 1; year++ } }
                }
                Spacer(Modifier.height(Space.xs))
                Row(Modifier.fillMaxWidth()) {
                    ARABIC_WEEKDAYS.forEach { day ->
                        Text(
                            day,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(Space.xs))
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { cell ->
                            Box(Modifier.weight(1f)) {
                                if (cell == null) {
                                    Box(Modifier.size(54.dp))
                                } else {
                                    DayCell(
                                        day = cell.gregorianDay,
                                        hijri = cell.hijri,
                                        isToday = cell.isToday,
                                        event = cell.event,
                                        shifted = transitions.any {
                                            it.atMillis in dayRange(year, month, cell.gregorianDay)
                                        },
                                        showHijri = state.display.calendarShowHijri,
                                        showGregorian = state.display.calendarShowGregorian,
                                        showEvent = state.display.calendarShowEvents,
                                        fmt = fmt,
                                        onClick = { onOpenDay(year, month, cell.gregorianDay) },
                                    )
                                }
                            }
                        }
                        repeat(7 - week.size) { Box(Modifier.weight(1f)) }
                    }
                }
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
            }

            CalendarView.TABLE -> {
                MonthTable(state, fmt, year, month, transitions, onOpenDay)
            }

            CalendarView.YEAR -> {
                YearDriftView(state, fmt, year)
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

enum class CalendarView(val labelAr: String) {
    MONTH("الشهر"),
    TABLE("جدول الشهر"),
    YEAR("الانحراف السنوي"),
}

@Composable
private fun MonthHeader(
    year: Int,
    month: Int,
    hijriLabel: String?,
    fmt: Fmt,
    onStep: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "الشهر السابق") { onStep(-1) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // the Hijri month leads; the Gregorian is the secondary line
            Text(
                hijriLabel ?: "",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "${fmt.n(month)} / ${fmt.n(year)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "الشهر التالي") { onStep(1) }
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

/**
 * The month as a TIMETABLE: the chosen prayer's time for every day, with the
 * change from the previous day — the drift, in the place where it matters.
 */
@Composable
private fun MonthTable(
    state: AppState,
    fmt: Fmt,
    year: Int,
    month: Int,
    transitions: List<app.miqat.core.DstTransition>,
    onOpenDay: (Int, Int, Int) -> Unit,
) {
    val prayer = state.display.calendarPrayer.coerceIn(0, 5)
    val cal = Calendar.getInstance().apply { clear(); set(year, month - 1, 1) }
    val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val rows = remember(year, month, prayer, state.city.calcMethod, state.display.methodOverride, state.adjusts) {
        (1..days).map { day ->
            val t = state.times(year, month, day).list[prayer]
            day to (((t % 24.0) + 24.0) % 24.0 * 60).toInt()
        }
    }
    var prev: Int? = null

    Column(Modifier.padding(top = Space.sm)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = Space.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "موعد ${PRAYER_NAMES[prayer]} في ${fmt.n(month)}/${fmt.n(year)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "التغيّر اليومي",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Divider()
        rows.forEach { (day, minute) ->
            val delta = prev?.let { minute - it }
            prev = minute
            val shifted = transitions.any { it.atMillis in dayRange(year, month, day) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDay(year, month, day) }
                    .padding(vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        fmt.n(day),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TabularFigures),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(38.dp),
                    )
                    Text(
                        fmt.hhmm(minute),
                        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TabularFigures),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (shifted) {
                        Text(
                            "تغيّر التوقيت",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(end = Space.sm),
                        )
                    }
                    Text(
                        when {
                            delta == null -> ""
                            delta == 0 -> "—"
                            else -> "${fmt.signed(delta)} د"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TabularFigures),
                        color = when {
                            delta == null || delta == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.secondary
                        },
                    )
                }
            }
            Divider()
        }
    }
}

/**
 * The drift across the YEAR: the chosen prayer's time month by month, drawn as
 * a curve, with the TOTAL change stated in words («يتقدّم الفجر ٢٧ دقيقة بين
 * يناير وديسمبر»). The chart is the one rich element; the numbers stay quiet.
 */
@Composable
private fun YearDriftView(state: AppState, fmt: Fmt, year: Int) {
    val prayer = state.display.calendarPrayer.coerceIn(0, 5)
    val points = remember(year, prayer, state.city.calcMethod, state.display.methodOverride, state.adjusts) {
        Drift.acrossYear(year, prayer) { y, m, day ->
            state.times(y, m, day).list
        }
    }
    val total = Drift.totalChange(points)
    val trend = Drift.trend(points)

    Column(Modifier.padding(top = Space.sm)) {
        Text(
            "انحراف ${PRAYER_NAMES[prayer]} على مدى ${fmt.n(year)}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            when {
                total == 0 -> "لا تغيّر يُذكر خلال السنة."
                total > 0 -> "يتأخّر حتى ${fmt.n(total)} دقيقة بين أبعد نقطتين في السنة."
                else -> "يتقدّم حتى ${fmt.n(-total)} دقيقة بين أبعد نقطتين في السنة."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Space.md))
        DriftChart(points, fmt, Modifier.fillMaxWidth().height(180.dp))
        Spacer(Modifier.height(Space.sm))
        // the two ends of the curve, in words
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${ARABIC_MONTHS.getOrElse(points.first().month - 1) { "" }}: ${fmt.hhmm(points.first().minuteOfDay)}",
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = TabularFigures),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${ARABIC_MONTHS.getOrElse(points.last().month - 1) { "" }}: ${fmt.hhmm(points.last().minuteOfDay)}",
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = TabularFigures),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Space.sm))
        Text(
            "الاتجاه العام: ${if (trend == 0) "ثابت" else if (trend > 0) "يتأخّر ${fmt.n(trend)} دقيقة" else "يتقدّم ${fmt.n(-trend)} دقيقة"}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Space.lg))
    }
}

/**
 * The drift chart: one line, one accent, a muted baseline — no rainbow, no
 * glow. The y-axis is RELATIVE to the first point (the drift in minutes), so
 * the curve shows the change, not the absolute time.
 */
@Composable
private fun DriftChart(points: List<DriftPoint>, fmt: Fmt, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val deltas = points.map { it.deltaMinutes }
    val minD = (deltas.minOrNull() ?: 0).coerceAtMost(0)
    val maxD = (deltas.maxOrNull() ?: 0).coerceAtLeast(0)
    val span = (maxD - minD).coerceAtLeast(1)

    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            if (points.size < 2) return@Canvas
            val w = size.width
            val h = size.height
            fun x(i: Int) = w * i / (points.size - 1).toFloat()
            fun y(d: Int) = h - ((d - minD).toFloat() / span) * (h - 12f) - 6f

            // the zero baseline — a hairline, not a grid
            drawLine(
                color = scheme.outlineVariant,
                start = Offset(0f, y(0)),
                end = Offset(w, y(0)),
                strokeWidth = 1f,
            )
            val path = Path()
            points.forEachIndexed { i, p ->
                val pt = Offset(x(i), y(p.deltaMinutes))
                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(path, scheme.primary, style = Stroke(2.5f, cap = StrokeCap.Round))
            // the ends, marked
            points.forEachIndexed { i, p ->
                if (i == 0 || i == points.size - 1) {
                    drawCircle(scheme.primary, 5f, Offset(x(i), y(p.deltaMinutes)))
                }
            }
        }
        // the month scale, quiet
        Row(Modifier.fillMaxWidth().padding(top = Space.xs)) {
            listOf(1, 4, 7, 10).forEach { m ->
                Text(
                    ARABIC_MONTHS.getOrElse(m - 1) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = if (m == 1) TextAlign.Start else if (m == 10) TextAlign.End else TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    hijri: IslamicDay,
    isToday: Boolean,
    event: String?,
    shifted: Boolean,
    showHijri: Boolean,
    showGregorian: Boolean,
    showEvent: Boolean,
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
        // the HIJRI day leads (the month view's job), the Gregorian follows it
        if (showHijri) {
            Text(
                fmt.n(hijri.hDay),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
            )
        }
        if (showGregorian) {
            Text(
                fmt.n(day),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showEvent && event != null) {
            Box(
                Modifier
                    .padding(top = 2.dp)
                    .size(5.dp)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape),
            )
        }
        if (shifted) {
            Box(
                Modifier
                    .padding(top = 2.dp)
                    .size(5.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape),
            )
        }
    }
}

private fun dayRange(year: Int, month: Int, day: Int): LongRange {
    val start = Calendar.getInstance().apply { clear(); set(year, month - 1, day, 0, 0, 0) }.timeInMillis
    val end = start + 86_400_000L
    return start until end
}

private data class Cell(
    val gregorianDay: Int,
    val hijri: IslamicDay,
    val isToday: Boolean,
    val event: String?,
)

private data class MonthEvent(
    val title: String,
    val gregorianDay: Int,
    val hijriDay: Int,
    val hijriMonth: Int,
)

private fun monthGrid(context: android.content.Context, year: Int, month: Int, hijriOffset: Int): List<Cell?> {
    val cal = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, 1)
    }
    val firstWeekday = cal.get(Calendar.DAY_OF_WEEK) % 7
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
                event = eventTitle(context, hijri),
            ),
        )
    }
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}

private fun eventTitle(context: android.content.Context, h: IslamicDay): String? {
    val fromTable = MiqatData.dayEvents(context, h.hMonth)
        .firstOrNull { h.hDay in it.startDay..it.endDay }
    return fromTable?.nameAr
}

private fun monthEvents(context: android.content.Context, year: Int, month: Int, hijriOffset: Int): List<MonthEvent> {
    val out = mutableListOf<MonthEvent>()
    val cal = Calendar.getInstance().apply { clear(); set(year, month - 1, 1) }
    val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (d in 1..days) {
        val h = Hijri.at(year, month, d)
        val title = eventTitle(context, h) ?: continue
        out.add(MonthEvent(title, d, h.hDay, h.hMonth))
    }
    return out
}

private val ARABIC_WEEKDAYS = listOf("سبت", "أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة")

private val ARABIC_MONTHS = listOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر",
)

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
