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
import androidx.compose.ui.graphics.Color
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
    // These screens show minutes, not seconds: subscribe to a minute bucket
    // so the 1 Hz tick does not rebuild their trees.
    val minuteBucket by androidx.compose.runtime.derivedStateOf {
        nowState.value / 60_000L
    }
    val now = minuteBucket * 60_000L
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
                    app.miqat.core.NumFmt.digits(window.titleAr, fmt.ar),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    app.miqat.core.NumFmt.digits(window.subtitleAr, fmt.ar),
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
                                    animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
                            ),
                                ) { value, _ -> swipe = value }
                            }
                        },
                        onDragCancel = {
                            swipeScope.launch {
                                val from = swipe
                                androidx.compose.animation.core.animate(
                                    initialValue = from,
                                    targetValue = 0f,
                                    animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
                            ),
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

        // the LEGEND — inside the month card, as in the reference
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Space.md, vertical = Space.sm),
            horizontalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            val sigils = if (isLightTheme()) listOf(
                "ص · نفل" to Color(0xFF1F6A62),
                "ص · سنة" to Color(0xFF14655C),
                "ع · مناسبة" to Color(0xFF8A6410),
                "ع · كبرى" to Color(0xFF96500A),
            ) else listOf(
                "ص · نفل" to Color(0xFF86C9BE),
                "ص · سنة" to Color(0xFF5CC4B8),
                "ع · مناسبة" to Color(0xFFEBD9A8),
                "ع · كبرى" to Color(0xFFF0C875),
            )
            sigils.forEach { (label, c) ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(c))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
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

        Spacer(Modifier.height(Space.md))

        // the DRIFT — one shared Expando, the very same PrayerStrip data
        Expando(
            id = "drift",
            openId = if (driftOpen) "drift" else null,
            onToggle = { driftOpen = it == "drift" },
            title = "انحراف ${PRAYER_NAMES[stripPrayer]} · هذا الشهر",
        ) {
            PrayerStrip(
                state = state,
                fmt = fmt,
                days = windowDays,
                prayer = stripPrayer,
                onPick = { stripPrayer = it; state.updateDisplay(d.copy(calendarPrayer = it)) },
                onLongPress = { yearOpen = true },
            )
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
