package app.miqat.ui

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miqat.core.DstInfo
import app.miqat.core.Hijri
import app.miqat.core.IslamicDay
import app.miqat.core.MiqatData
import app.miqat.core.Moments
import java.util.Calendar

/**
 * CALENDAR — a worship ledger, not a grid of numbers (Design Council).
 *
 *  - the MONTH is the hub: one Hijri number per day + a fixed three-slot
 *    SIGIL STRIP (fast / event / rite) whose shapes never move;
 *  - the DAY is a sheet: the spine with the prayers as points, the windows as
 *    bands, the fast brackets, the Hajj pin;
 *  - the YEAR is an overlay from the prayer strip.
 *  - the MONTH LEDGER (every day's time) is a pull-up sheet, not a peer view;
 *  - "which prayer?" is a chip on the strip, not a settings prerequisite.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    state: AppState,
    fmt: Fmt,
    nowState: androidx.compose.runtime.State<Long>,
    onOpenDay: (Int, Int, Int) -> Unit,
    onOpenPrayer: (Int) -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val now = nowState.value
    val (cy, cm, _) = state.cityDate(now)
    var year by remember { mutableIntStateOf(cy) }
    var month by remember { mutableIntStateOf(cm) }
    var stripPrayer by remember { mutableIntStateOf(state.display.calendarPrayer) }
    var ledgerOpen by remember { mutableStateOf(false) }
    var yearOpen by remember { mutableStateOf(false) }
    var driftOpen by remember { mutableStateOf(false) }

    val d = state.display
    // WHAT THE CALENDAR IS ORGANIZED AROUND — the founder's "the method
    // highlights a context": the Hijri month, the Gregorian month, or the
    // worship the month holds.
    var axis by remember {
        mutableStateOf(
            when (runCatching { app.miqat.core.CalendarContext.valueOf(d.calendarContext) }.getOrNull()) {
                app.miqat.core.CalendarContext.GREGORIAN -> app.miqat.core.CalendarContext.GREGORIAN
                else -> app.miqat.core.CalendarContext.HIJRI // ACTS was never an axis
            },
        )
    }
    // A LENS, not a peer tab: the founder called the three-way split stupid.
    // The Hijri/Gregorian spine stays; this only dims what is not being sought.
    var lens by remember { mutableStateOf("الكل") }
    val lenses = listOf("الكل", "صيام", "مناسبات")
    val cityToday = state.cityDate(now)
    // the swipe: the month follows the finger and turns at the end (in RTL,
    // dragging to the LEFT is the next month)
    var swipe by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val swipeScope = androidx.compose.runtime.rememberCoroutineScope()
    val window = remember(axis, year, month, d.hijriOffsetDays, cityToday) {
        app.miqat.core.CalendarWindows.build(
            context = axis,
            gregorianYear = year,
            gregorianMonth = month,
            hijriOffset = d.hijriOffsetDays,
            today = cityToday,
        )
    }
    // the days the GRID shows — the strip, the ledger and the export read them
    // (they used to read the Gregorian anchor, and so described another month)
    val windowDays = remember(window) {
        window.days.map { Triple(it.gregorianYear, it.gregorianMonth, it.gregorianDay) }
    }
    fun stepYear(delta: Int) {
        if (axis == app.miqat.core.CalendarContext.HIJRI) {
            val a = app.miqat.core.CalendarWindows.anchorForHijriMonth(
                window.spineYear + delta, window.spineMonth, d.hijriOffsetDays,
            )
            year = a.first
            month = a.second
        } else {
            year += delta
        }
    }

    // the step: a Hijri month in the Hijri context, a Gregorian month in the
    // Gregorian contexts
    fun stepMonth(delta: Int) {
        if (axis == app.miqat.core.CalendarContext.HIJRI) {
            var hy = window.spineYear
            var hm = window.spineMonth + delta
            if (hm > 12) { hm = 1; hy++ }
            if (hm < 1) { hm = 12; hy-- }
            val anchor = app.miqat.core.CalendarWindows
                .anchorForHijriMonth(hy, hm, d.hijriOffsetDays)
            year = anchor.first
            month = anchor.second
        } else {
            month += delta
            if (month < 1) { month = 12; year-- }
            if (month > 12) { month = 1; year++ }
        }
    }
    val transitions = remember(state.city.zone, state.effectiveZoneId, year) {
        DstInfo.transitionsInYear(
            context,
            zone = state.city.zone,
            zoneId = state.effectiveZoneId,
            year = year,
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.lg),
    ) {
        Text(
            "التقويم",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = Kufi, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Space.sm))

        // THE SPINE — the calendar is organized around ONE axis, not three
        // competing tabs (the founder: «التقسيم للتبويب غبي»).
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            listOf(
                app.miqat.core.CalendarContext.HIJRI,
                app.miqat.core.CalendarContext.GREGORIAN,
            ).forEach { c ->
                val selected = c == axis
                Text(
                    c.labelAr,
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = Plex),
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surface,
                        )
                        .clickable {
                            axis = c
                            state.updateDisplay(d.copy(calendarContext = c.name))
                        }
                        .padding(horizontal = Space.md, vertical = 7.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            // «اليوم» — always visible; the way back is one tap
            val todayHijri = app.miqat.core.Hijri.at(
                cityToday.first, cityToday.second, cityToday.third, d.hijriOffsetDays,
            )
            val onToday = when (axis) {
                app.miqat.core.CalendarContext.HIJRI ->
                    window.spineYear == todayHijri.hYear && window.spineMonth == todayHijri.hMonth
                else -> window.spineYear == cityToday.first && window.spineMonth == cityToday.second
            }
            Text(
                "اليوم",
                style = MaterialTheme.typography.labelMedium,
                color = if (onToday) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.chip))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        val anchor = state.cityDate(now)
                        year = anchor.first
                        month = anchor.second
                    }
                    .padding(horizontal = Space.md, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(Space.sm))
        // THE LENS — acts are a way of looking at the same ledger
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                "العدسة",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            lenses.forEach { l ->
                val active = l == lens
                Text(
                    l,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.chip))
                        .background(
                            if (active) MaterialTheme.colorScheme.surfaceVariant
                            else androidx.compose.ui.graphics.Color.Transparent,
                        )
                        .clickable { lens = l }
                        .padding(horizontal = Space.sm, vertical = 3.dp),
                )
            }
        }
        Spacer(Modifier.height(Space.sm))
        // «أقرب موعد ميقات» — one line, from the ONE proximity model; tapping
        // it opens that prayer's own detail (no new screen, no second clock)
        val nearToday = app.miqat.core.Dates.nextDay(
            cityToday.first, cityToday.second, cityToday.third,
        )
        val nearLine = app.miqat.core.DayLine.of(
            state.times(cityToday.first, cityToday.second, cityToday.third),
            state.times(nearToday.first, nearToday.second, nearToday.third).fajr,
            PRAYER_NAMES, ::minutesOf, state.imsakLeadMinutes,
        )
        val nearest = app.miqat.core.NearestMoment.find(
            nearLine, (state.cityHourOfDay(now) * 60).toInt(),
        )
        if (nearest != null) {
            val line = app.miqat.core.Proximity.timerLine(
                app.miqat.core.TimerKind.PRAYER_REMAINING,
                nearest.signedMin * 60L, nearest.slot.labelAr,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable {
                        onOpenPrayer(PRAYER_KEY.indexOf(nearest.slot.kind).coerceAtLeast(0))
                    }
                    .padding(horizontal = Space.md, vertical = Space.sm),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(6.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                )
                Spacer(Modifier.width(Space.sm))
                Text(
                    "أقرب موعد: " + nearest.slot.labelAr,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = Plex),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (nearest.dayLabelAr != null) "${nearest.dayLabelAr} ${fmt.hhmm(nearest.clockMin)}"
                    else fmt.duration(nearest.signedMin),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Kufi, fontFeatureSettings = TabularFigures,
                    ),
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(Modifier.height(Space.sm))
        }

        // year hop — the month arrows below step one month; these step a
        // whole year on the SAME spine (no date picker, no new dependency)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            IconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "السنة السابقة") {
                stepYear(-1)
            }
            Text(
                "${fmt.n(window.spineYear)} ${if (axis == app.miqat.core.CalendarContext.HIJRI) "هـ" else "م"}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Space.md),
            )
            IconButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "السنة التالية") {
                stepYear(1)
            }
        }

        // the header — the SPINE names itself; the other axis is a SPAN, so a
        // month that turns halfway through is never hidden by a half-title
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // in RTL the RIGHT button is «previous» — the arrows were swapped
            IconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "الشهر السابق") {
                stepMonth(-1)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    window.titleAr,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    window.subtitleAr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // the calendar is tabular (Umm al-Qura), not a sighting
                Text(
                    app.miqat.core.IslamicDays.HIJRI_APPROX,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            IconButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "الشهر التالي") {
                stepMonth(1)
            }
        }

        Spacer(Modifier.height(Space.sm))
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

        // the month grid — the spine's days, with the turn marked
        val all = List<app.miqat.core.CalendarCell?>(window.leadBlanks) { null } + window.days
        Column(
            Modifier
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            swipe += amount
                        },
                        onDragEnd = {
                            val w = size.width.toFloat()
                            if (kotlin.math.abs(swipe) > w * 0.3f) {
                                // RTL: dragging LEFT is the NEXT month
                                stepMonth(if (swipe < 0f) 1 else -1)
                            }
                            swipeScope.launch {
                                val from = swipe
                                androidx.compose.animation.core.animate(
                                    initialValue = from,
                                    targetValue = 0f,
                                    animationSpec = androidx.compose.animation.core.tween(180),
                                ) { value, _ -> swipe = value }
                            }
                        },
                        onDragCancel = {
                            swipeScope.launch {
                                val from = swipe
                                androidx.compose.animation.core.animate(
                                    initialValue = from,
                                    targetValue = 0f,
                                    animationSpec = androidx.compose.animation.core.tween(180),
                                ) { value, _ -> swipe = value }
                            }
                        },
                    )
                }
                .graphicsLayer {
                    // the grid follows the finger; the month turns at the end
                    translationX = swipe
                    alpha = 1f - (kotlin.math.abs(swipe) / (size.width * 0.45f)).coerceAtMost(0.45f)
                },
        ) {
        all.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { cell ->
                    Box(Modifier.weight(1f)) {
                        if (cell == null) {
                            Box(Modifier.size(58.dp))
                        } else {
                            DayCell(
                                cell = cell,
                                primaryHijri = axis == app.miqat.core.CalendarContext.HIJRI,
                                emphasiseActs = false,
                                showPrimary = if (axis == app.miqat.core.CalendarContext.HIJRI)
                                    d.calendarShowHijri else d.calendarShowGregorian,
                                showSecondary = if (axis == app.miqat.core.CalendarContext.HIJRI)
                                    d.calendarShowGregorian else d.calendarShowHijri,
                                showEvents = d.calendarShowEvents,
                                lens = lens,
                                fmt = fmt,
                                onClick = {
                                    onOpenDay(cell.gregorianYear, cell.gregorianMonth, cell.gregorianDay)
                                },
                            )
                        }
                    }
                }
                repeat(7 - week.size) { Box(Modifier.weight(1f)) }
            }
        }
        }

        Spacer(Modifier.height(Space.md))

        // the DRIFT — a collapsed pill that expands into the real axis.
        // Same PrayerStrip, same data; only its reveal changes.
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .animateContentSize(
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                    ),
                ),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { driftOpen = !driftOpen }
                    .padding(horizontal = Space.md, vertical = Space.md),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    "انحراف ${PRAYER_NAMES[stripPrayer]} · هذا الشهر",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = Plex),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (driftOpen) "−" else "+",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = Kufi),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (driftOpen) {
                PrayerStrip(
                    state = state,
                    fmt = fmt,
                    days = windowDays,
                    prayer = stripPrayer,
                    onPick = { stripPrayer = it; state.updateDisplay(d.copy(calendarPrayer = it)) },
                    onLongPress = { yearOpen = true },
                )
            }
        }

        Spacer(Modifier.height(Space.sm))
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { ledgerOpen = true }
                .padding(vertical = Space.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "دفتر الشهر — وقت ${PRAYER_NAMES[stripPrayer]} يوماً بيوم",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "افتح",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(Space.md))
    }

    if (ledgerOpen) {
        ModalBottomSheet(
            onDismissRequest = { ledgerOpen = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            MonthLedger(
                state = state,
                fmt = fmt,
                days = windowDays,
                prayer = stripPrayer,
                title = window.titleAr,
                onOpenDay = { day ->
                    ledgerOpen = false
                    onOpenDay(windowDays.first().first, windowDays.first().second, day)
                },
                onShare = {
                    // the month's ledger, as a card you can share
                    val rows = app.miqat.ui.export.MonthCard.rows(
                        state, fmt, windowDays, stripPrayer,
                    )
                    val bitmap = app.miqat.ui.export.MonthCard.render(
                        context, state, fmt, windowDays, stripPrayer, rows,
                    )
                    app.miqat.ui.export.MonthShare.share(
                        context,
                        bitmap,
                        "دفتر ${PRAYER_NAMES[stripPrayer]} — " +
                            app.miqat.ui.export.MonthCard.hijriMonthLabel(year, month, state.display.hijriOffsetDays) +
                            " ${fmt.n(year)} · ${state.city.ar.ifBlank { state.city.en }}",
                    )
                },
            )
        }
    }
    if (yearOpen) {
        ModalBottomSheet(
            onDismissRequest = { yearOpen = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            YearDriftSheet(state, fmt, year, stripPrayer)
        }
    }
}

/**
 * The month cell: ONE Hijri number, the Gregorian small, and the three-slot
 * sigil strip — fast (a bar), event (a lozenge), rite (a pin). The slots are
 * always reserved, so the grid never jumps.
 */
/**
 * The cell: TWO numbers (the spine's is the big one), the sigil strip, and —
 * where the other axis turns — a gold hairline and the placard naming the new
 * month. The turn is a MARK, never a tint: a tint would turn the calendar
 * into a two-tone board (the design council, 2026-11-11).
 */
@Composable
private fun DayCell(
    cell: app.miqat.core.CalendarCell,
    primaryHijri: Boolean,
    emphasiseActs: Boolean,
    showPrimary: Boolean,
    showSecondary: Boolean,
    showEvents: Boolean,
    lens: String,
    fmt: Fmt,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    // the lens dims UNMATCHED days; nothing disappears (the ledger stays whole)
    val relevant = when (lens) {
        "صيام" -> cell.isFast
        "مناسبات" -> cell.isRite
        else -> true
    }
    val cellAlpha = if (relevant) 1f else 0.32f
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.row))
            .background(if (cell.isToday) scheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
            .graphicsLayer { alpha = cellAlpha }
            .clickable { onClick() }
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (cell.turnsHere) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)
                    .height(1.dp)
                    .background(scheme.primary),
            )
        }
        if (cell.isToday) {
            Box(
                Modifier
                    .width(12.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(app.miqat.ui.Sky.sunMark),
            )
        }
        if (showPrimary) {
            Text(
                text = if (primaryHijri) fmt.n(cell.hijriDay) else fmt.n(cell.gregorianDay),
                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TabularFigures),
                color = if (cell.isToday) scheme.onPrimaryContainer else scheme.onSurface,
            )
        }
        if (showSecondary) {
            Text(
                text = if (primaryHijri) fmt.n(cell.gregorianDay) else fmt.n(cell.hijriDay),
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = TabularFigures),
                color = scheme.onSurfaceVariant,
            )
        }
        if (cell.turnsHere && cell.turnsToLabel.isNotBlank()) {
            Text(
                text = cell.turnsToLabel,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.primary,
                maxLines = 1,
            )
        }
        if (showEvents && (emphasiseActs || cell.isFast || cell.isRite)) {
            Spacer(Modifier.height(3.dp))
            // BADGES, not hairlines: SHAPE says the type, SIZE says the level
            // (the founder's card-type language, without a game score).
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = androidx.compose.ui.Alignment.Bottom,
            ) {
                // ONE type (a fast bar), FOUR cuts — rank is the ink inside
                // the same family, never a new colour, size-only signal or
                // game level. Same grammar in every screen that shows a fast.
                when (cell.fastKind) {
                    // R1 — nafl: raw cut stone, hollow, square corners
                    app.miqat.core.IslamicDays.FastDay.MON_THU -> Box(
                        Modifier.width(12.dp).height(6.dp)
                            .border(1.dp, scheme.secondary, RoundedCornerShape(0.dp)),
                    )
                    // R2 — sunnah: dressed once, a trace of ink inside
                    app.miqat.core.IslamicDays.FastDay.WHITE,
                    app.miqat.core.IslamicDays.FastDay.ASHURA,
                    app.miqat.core.IslamicDays.FastDay.SIX_SHAWWAL -> Box(
                        Modifier.width(16.dp).height(7.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(scheme.secondary.copy(alpha = 0.10f))
                            .border(1.5.dp, scheme.secondary, RoundedCornerShape(1.dp)),
                    )
                    // R3 — Arafah: double-ruled, the inner rule IS the mark
                    app.miqat.core.IslamicDays.FastDay.ARAFAH -> Box(
                        Modifier.width(20.dp).height(7.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .border(2.dp, scheme.secondary, RoundedCornerShape(1.dp)),
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) {
                        Box(
                            Modifier.width(14.dp).height(2.dp).background(scheme.secondary),
                        )
                    }
                    // R4 — Ramadan: filled, with ONE gold inlay at the start
                    app.miqat.core.IslamicDays.FastDay.RAMADAN -> Box(
                        Modifier.width(44.dp).height(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(scheme.secondary),
                        contentAlignment = androidx.compose.ui.Alignment.CenterEnd,
                    ) {
                        Box(Modifier.width(6.dp).height(2.dp).background(scheme.primary))
                    }
                    else -> Unit // Eid and tashreeq veto the fast badge
                }
                when (cell.occasion) {
                    app.miqat.core.DayOccasion.ARAFAH -> Box(
                        Modifier.size(12.dp).graphicsLayer { rotationZ = 45f }
                            .clip(RoundedCornerShape(2.dp))
                            .background(scheme.onTertiaryContainer),
                    )
                    app.miqat.core.DayOccasion.EID -> Box(
                        Modifier.width(8.dp).height(13.dp)
                            .clip(RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp))
                            .background(scheme.onTertiaryContainer),
                    )
                    app.miqat.core.DayOccasion.HAJJ_RITE -> Box(
                        Modifier.width(8.dp).height(13.dp)
                            .clip(RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp))
                            .border(1.5.dp, scheme.onTertiaryContainer,
                                RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp)),
                    )
                    app.miqat.core.DayOccasion.EVE_RAMADAN -> Box(
                        Modifier.width(12.dp).height(6.dp)
                            .border(1.5.dp, scheme.onTertiaryContainer, RoundedCornerShape(2.dp)),
                    )
                    else -> Unit
                }
            }
        }
    }
}

private val Color_Transparent = androidx.compose.ui.graphics.Color(0x00000000)

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
 * The prayer strip: the six prayers as chips (pick one) over a line of the
 * month's thirty days — the drift, visible at a glance, in the place where the
 * drift matters.
 */
@Composable
private fun PrayerStrip(
    state: AppState,
    fmt: Fmt,
    days: List<Triple<Int, Int, Int>>,
    prayer: Int,
    onPick: (Int) -> Unit,
    onLongPress: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val points: List<Int> = remember(days, prayer, state.city.calcMethod, state.display.methodOverride, state.adjusts) {
        days.map { (y, m, d) ->
            val t = state.times(y, m, d).list[prayer]
            ((((t % 24.0) + 24.0) % 24.0) * 60).toInt()
        }
    }
    val min = points.minOrNull() ?: 0
    val max = points.maxOrNull() ?: 1
    val span = (max - min).coerceAtLeast(1)

    Column(
        Modifier
            .fillMaxWidth()
            .cardSurface(RoundedCornerShape(Radius.card))
            .padding(Space.md),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            PRAYER_NAMES.forEachIndexed { i, name ->
                val selected = i == prayer
                Text(
                    name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.chip))
                        .background(if (selected) scheme.primary else scheme.surfaceVariant)
                        .clickable { onPick(i) }
                        .padding(horizontal = Space.sm, vertical = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(Space.sm))
        // the drift line: thirty days, the movement is the shape
        androidx.compose.foundation.Canvas(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(Radius.row))
                .clickable { onLongPress() },
        ) {
            if (points.size < 2) return@Canvas
            val w = size.width
            val h = size.height - 16f
            // right-to-left, like the grid above it (day 1 at the right)
            fun x(i: Int) = w * (points.size - 1 - i) / (points.size - 1).toFloat()
            fun y(v: Int) = h - ((v - min).toFloat() / span) * h
            val path = Path()
            points.forEachIndexed { i, v ->
                val p = Offset(x(i), y(v))
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, scheme.primary, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
            // the first and the last, marked
            drawCircle(scheme.primary, 4f, Offset(x(0), y(points.first())))
            drawCircle(scheme.tertiary, 4f, Offset(x(points.lastIndex), y(points.last())))
        }
        Spacer(Modifier.height(Space.xs))
        Text(
            "أوّل الشهر ${fmt.hhmm(points.first().toInt())} — آخر الشهر ${fmt.hhmm(points.last().toInt())} " +
                "(الفرق ${fmt.n(max - min)} دقيقة) — اضغط لعرض السنة",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
        )
    }
}

/** The month ledger: every day's time for the chosen prayer, with its change. */
@Composable
private fun MonthLedger(
    state: AppState,
    fmt: Fmt,
    days: List<Triple<Int, Int, Int>>,
    prayer: Int,
    title: String,
    onOpenDay: (Int) -> Unit,
    onShare: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val rows = remember(days, prayer, state.city.calcMethod, state.display.methodOverride, state.adjusts) {
        days.map { (y, m, d) ->
            val t = state.times(y, m, d).list[prayer]
            d to (((t % 24.0) + 24.0) % 24.0 * 60).toInt()
        }
    }
    var prev by remember { mutableStateOf<Int?>(null) }
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.lg),
    ) {
        Text(
            "دفتر الشهر — ${PRAYER_NAMES[prayer]} في $title",
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface,
            modifier = Modifier.padding(bottom = Space.sm),
        )
        // share the month as a card
        NavRow(
            "مشاركة دفتر الشهر",
            "صورة واحدة فيها كل أيام الشهر — تُرسل كما هي",
            onShare,
        )
        Divider(Modifier.padding(horizontal = Space.lg))
        rows.forEach { (day, minute) ->
            val delta = prev?.let { minute - it }
            prev = minute
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDay(day) }
                    .padding(vertical = Space.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        fmt.n(day),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TabularFigures),
                        color = scheme.onSurface,
                        modifier = Modifier.width(36.dp),
                    )
                    Text(
                        fmt.hhmm(minute),
                        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TabularFigures),
                        color = scheme.onSurface,
                    )
                }
                Text(
                    when {
                        delta == null -> ""
                        delta == 0 -> "—"
                        else -> "${fmt.signed(delta)} د"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TabularFigures),
                    color = if (delta == null || delta == 0) scheme.onSurfaceVariant else scheme.secondary,
                )
            }
            androidx.compose.material3.HorizontalDivider(color = scheme.outlineVariant)
        }
        Spacer(Modifier.height(Space.xxl))
    }
}

/** The year overlay: the chosen prayer across the twelve months. */
@Composable
private fun YearDriftSheet(state: AppState, fmt: Fmt, year: Int, prayer: Int) {
    val scheme = MaterialTheme.colorScheme
    val points = remember(year, prayer, state.city.calcMethod, state.display.methodOverride, state.adjusts) {
        app.miqat.core.Drift.acrossYear(year, prayer) { y, m, day -> state.times(y, m, day).list }
    }
    val total = app.miqat.core.Drift.totalChange(points)
    val trend = app.miqat.core.Drift.trend(points)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg),
    ) {
        Text(
            "انحراف ${PRAYER_NAMES[prayer]} على مدى ${fmt.n(year)}",
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface,
        )
        Text(
            when {
                total == 0 -> "لا تغيّر يُذكر خلال السنة."
                total > 0 -> "يتأخّر حتى ${fmt.n(total)} دقيقة بين أبعد نقطتين في السنة."
                else -> "يتقدّم حتى ${fmt.n(-total)} دقيقة بين أبعد نقطتين في السنة."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Space.md))
        androidx.compose.foundation.Canvas(
            Modifier
                .fillMaxWidth()
                .height(160.dp),
        ) {
            if (points.size < 2) return@Canvas
            val deltas = points.map { it.deltaMinutes }
            val minD = (deltas.minOrNull() ?: 0).coerceAtMost(0)
            val maxD = (deltas.maxOrNull() ?: 0).coerceAtLeast(0)
            val span = (maxD - minD).coerceAtLeast(1)
            val w = size.width
            val h = size.height
            // right-to-left, like the grid above it (day 1 at the right)
            fun x(i: Int) = w * (points.size - 1 - i) / (points.size - 1).toFloat()
            fun y(d: Int) = h - ((d - minD).toFloat() / span) * (h - 12f) - 6f
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
            drawPath(path, scheme.primary, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
            points.forEachIndexed { i, p ->
                if (i == 0 || i == points.size - 1) {
                    drawCircle(scheme.primary, 5f, Offset(x(i), y(p.deltaMinutes)))
                }
            }
        }
        Spacer(Modifier.height(Space.sm))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${ARABIC_MONTHS.getOrElse(points.first().month - 1) { "" }}: ${fmt.hhmm(points.first().minuteOfDay)}",
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = TabularFigures),
                color = scheme.onSurfaceVariant,
            )
            Text(
                "الاتجاه العام: ${if (trend == 0) "ثابت" else if (trend > 0) "يتأخّر ${fmt.n(trend)} دقيقة" else "يتقدّم ${fmt.n(-trend)} دقيقة"}",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
            Text(
                "${ARABIC_MONTHS.getOrElse(points.last().month - 1) { "" }}: ${fmt.hhmm(points.last().minuteOfDay)}",
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = TabularFigures),
                color = scheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Space.xxl))
    }
}

/** A cell of the month grid. */

private val ARABIC_WEEKDAYS = listOf("سبت", "أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة")

private val ARABIC_MONTHS = listOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر",
)

/** A single day: its prayers, its windows, and things the user can DO. */
@Composable
fun DayDetailScreen(
    state: AppState,
    fmt: Fmt,
    year: Int,
    month: Int,
    day: Int,
    onBack: () -> Unit,
    onOpenPrayer: (Int) -> Unit,
    onOpenTasbih: () -> Unit,
) {
    // REAL date arithmetic: the old `month + 1` kept the day, so Jan 31 asked
    // for Feb 31 and got a normalised, wrong night.
    val (ty, tm, td) = app.miqat.core.Dates.nextDay(year, month, day)

    val times = state.times(year, month, day)
    val tomorrow = state.times(ty, tm, td)
    val hijri = app.miqat.core.Hijri.at(year, month, day, state.display.hijriOffsetDays)
    val timeline = remember(year, month, day, state.city.calcMethod, state.display.methodOverride, state.adjusts) {
        app.miqat.core.Moments.timeline(
            times, tomorrow.fajr, PRAYER_NAMES, ::minutesOf,
            hijriMonth = hijri.hMonth, hijriDay = hijri.hDay,
            imsakLeadMinutes = state.imsakLeadMinutes,
        )
    }
    val dayKey = "%04d-%02d-%02d".format(year, month, day)
    val logged = state.display.dayActLog
    var showLog by remember { mutableStateOf(false) }

    MiqatScaffold(app.miqat.core.Hijri.label(hijri, fmt.ar), onBack = onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.lg),
        ) {
            Spacer(Modifier.height(Space.sm))
            // the spine: the prayer points are TAPPABLE now, not a dead picture
            DayCard(
                state = state, fmt = fmt,
                year = year, month = month, day = day,
                timeline = timeline, hijri = hijri,
                onOpenPrayer = onOpenPrayer,
            )
            Spacer(Modifier.height(Space.lg))

            Text(
                "ما تفعله في هذا اليوم",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Space.xs))
            NavRow(
                "سجّل طاعة",
                "صيام · صلاة · ذكر — يُحفظ على جهازك فقط",
                onClick = { showLog = !showLog },
            )
            if (showLog) {
                listOf("fast" to "صيام", "prayer" to "صلاة", "dhikr" to "ذكر").forEach { (kind, label) ->
                    val key = "$dayKey:$kind"
                    val on = key in logged
                    Text(
                        (if (on) "✓ " else "") + label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (on) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                state.updateDisplay(
                                    state.display.copy(
                                        dayActLog = if (on) logged - key else logged + key,
                                    ),
                                )
                            }
                            .padding(vertical = Space.sm, horizontal = Space.md),
                    )
                }
            }
            Divider()
            NavRow(
                "افتح الموعد",
                "تفاصيل أول موعد في هذا اليوم",
                onClick = { onOpenPrayer(0) },
            )
            Divider()
            NavRow(
                "أضف ذكرًا",
                "التسبيح مرجعٌ مقرون بالتذكير، لا شاشة منفصلة",
                onClick = onOpenTasbih,
            )
            Spacer(Modifier.height(Space.md))
            Text(
                "المواقيت محسوبة عند الطلب — بلا جدولة مسبقة لكل أيام السنة.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.xxl))
        }
    }
}
