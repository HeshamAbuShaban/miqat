package app.miqat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import java.util.Calendar
import java.util.TimeZone
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miqat.core.DstInfo
import app.miqat.core.DstTransition
import app.miqat.core.LocalPrayerTimes
import app.miqat.core.Moments
import app.miqat.core.Urgency
import app.miqat.core.UrgencyRules
import app.miqat.core.Hijri
import app.miqat.core.IslamicDays
import app.miqat.core.Qibla
import app.miqat.core.WorshipWindows
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import app.miqat.core.Moment
import app.miqat.core.MomentCategory

/** The four top-level destinations — one per CORE, no dumping ground. */
enum class TopLevel(val label: String, val glyph: Glyph) {
    Home("الرئيسية", Glyph.HORIZON),
    Times("المواقيت", Glyph.TICKS),
    Fasting("الصيام", Glyph.BRACKET),
    Calendar("التقويم", Glyph.CELL),
    Acts("الطاعات", Glyph.SPAN),
}

/** Sub-routes pushed on top of a top-level destination (their own screens). */
sealed interface Route {
    data class PrayerDetailArgs(val index: Int) : Route
    data object LocationSettings : Route
    data object CalculationSettings : Route
    data object NotificationsSettings : Route
    data object AwarenessSettings : Route
    data object AdhanSettings : Route
    data object VerifySettings : Route
    data object IndicatorsSettings : Route
    data object Qibla : Route
    data object Distance : Route
    data object Acts : Route
    data object Settings : Route
    data object Tasbih : Route
    /** The tools are a CONTEXTUAL layer, not a dumping-ground tab. */
    data object More : Route
    data class ActDetail(val id: String) : Route
    data object CalendarSettings : Route
    data object AppearanceSettings : Route
    data object AboutSettings : Route
    data class DayDetail(val year: Int, val month: Int, val day: Int) : Route
    data object Gaps : Route
    data object Interval : Route
    data object Sunnah : Route
}

/** The worship windows that deserve their own screen (R5 «زوائد غابت»). */

@Composable
fun MiqatApp(state: AppState, fmt: Fmt) {
    // ONE clock for the whole app (gate 0.2): the hero, the strip, the times
    // list and the tools all read this same instant — two clocks is how the
    // location bug happened.
    val now = rememberNowState()
    var top by remember { mutableStateOf(TopLevel.Home) }
    var sub by remember { mutableStateOf<Route?>(null) }
    var pickerOpen by remember { mutableStateOf(false) }

    // Android back navigates the app's own stack: a sub-screen returns to its
    // parent, a non-home tab returns home, and only the home tab exits.
    BackHandler(enabled = sub != null || top != TopLevel.Home) {
        if (sub != null) sub = null else top = TopLevel.Home
    }

    // The sub-screen replaces the body but keeps the bottom bar visible so the
    // user never loses the map.
    Scaffold(
        bottomBar = {
            // ONE drawn ruler — no Material NavigationBar, no pill, no
            // elevation, no container. Five equal slots, fixed silhouettes;
            // selection is a colour + one 16x2dp printed-index rule.
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
                Box(
                    Modifier.fillMaxWidth().height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                        .height(66.dp)
                        .shadow(
                            elevation = 18.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = Color.Black,
                            spotColor = Color.Black,
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                        .border(1.dp, MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(24.dp))
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TopLevel.entries.forEach { dest ->
                        val selected = top == dest // the parent stays marked on sub-screens
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    top = dest
                                    sub = null
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(Modifier.height(2.dp)) {
                                if (selected) {
                                    Box(
                                        Modifier.size(width = 16.dp, height = 2.dp)
                                            .background(MaterialTheme.colorScheme.primary),
                                    )
                                }
                            }
                            Box(
                                Modifier.weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                BrandGlyph(
                                    kind = dest.glyph,
                                    active = selected,
                                    size = 22.dp,
                                )
                            }
                            Text(
                                dest.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 10.dp),
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF04050A),
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0A0D18), Color(0xFF04050A)),
                    ),
                ),
        ) {
            when (val route = sub) {
                null -> when (top) {
                    TopLevel.Home -> HomeScreen(
                        state, fmt, now,
                        onOpenTimes = { top = TopLevel.Times },
                        onOpenQibla = { sub = Route.Qibla },
                        onOpenPrayer = { sub = Route.PrayerDetailArgs(it) },
                        onOpenMore = { sub = Route.More },
                        onOpenFasting = { top = TopLevel.Fasting },
                        onOpenDistance = { sub = Route.Distance },
                        onOpenSunnah = { sub = Route.Sunnah },
                    )
                    TopLevel.Times -> TimesScreen(
                        state, fmt, now,
                        onOpenPrayer = { sub = Route.PrayerDetailArgs(it) },
                        onOpenActs = { sub = Route.Acts },
                    )
                    TopLevel.Fasting -> FastingScreen(state, fmt, now)
                    TopLevel.Calendar -> CalendarScreen(
                        state, fmt, now,
                        onOpenDay = { y, m, d -> sub = Route.DayDetail(y, m, d) },
                        onOpenPrayer = { sub = Route.PrayerDetailArgs(it) },
                    )
                    TopLevel.Acts -> ActsScreen(
                        state, fmt, now,
                        onOpenAct = { sub = Route.ActDetail(it) },
                        onOpenTasbih = { sub = Route.Tasbih },
                        onOpenDistance = { sub = Route.Distance },
                    )
                }
                is Route.PrayerDetailArgs -> PrayerDetailScreen(state, fmt, route.index) { sub = null }
                is Route.DayDetail -> DayDetailScreen(
                    state, fmt, route.year, route.month, route.day,
                    onBack = { sub = null },
                    onOpenPrayer = { i -> sub = Route.PrayerDetailArgs(i) },
                    onOpenTasbih = { sub = Route.Tasbih },
                )
                Route.Gaps -> GapsScreen(state, fmt) { sub = null }
                Route.Interval -> IntervalScreen(state, fmt) { sub = null }
                Route.Sunnah -> SunnahScreen(state, fmt) { sub = null }
                Route.LocationSettings -> MiqatScaffold("الإعدادات", onBack = { sub = null }) {
                    LocationSection(state, fmt) { pickerOpen = true }
                }
                Route.CalculationSettings -> MiqatScaffold("الإعدادات", onBack = { sub = null }) {
                    CalculationSection(state, fmt)
                }
                Route.NotificationsSettings -> MiqatScaffold("الإعدادات", onBack = { sub = null }) {
                    NotificationsSection(state, fmt)
                }
                Route.VerifySettings -> VerifyScreen(state, fmt) { sub = null }
                Route.IndicatorsSettings -> MiqatScaffold("الإعدادات", onBack = { sub = null }) {
                    IndicatorsSection(state, fmt)
                }
                Route.Qibla -> QiblaScreen(state, fmt) { sub = null }
                Route.Acts -> ActsScreen(state, fmt, now, onOpenAct = { sub = Route.ActDetail(it) }, onOpenTasbih = { sub = Route.Tasbih })
                Route.Settings -> SettingsScreen(state, fmt, onOpen = { r ->
                    sub = when (r) {
                        SettingsRoute.Location -> Route.LocationSettings
                        SettingsRoute.Calculation -> Route.CalculationSettings
                        SettingsRoute.Notifications -> Route.NotificationsSettings
                        SettingsRoute.Awareness -> Route.AwarenessSettings
                        SettingsRoute.Adhan -> Route.AdhanSettings
                        SettingsRoute.Verify -> Route.VerifySettings
                        SettingsRoute.Indicators -> Route.IndicatorsSettings
                        SettingsRoute.Calendar -> Route.CalendarSettings
                        SettingsRoute.Appearance -> Route.AppearanceSettings
                        SettingsRoute.About -> Route.AboutSettings
                    }
                })
                    Route.More -> MoreScreen(state, fmt, now, onOpen = { target ->
                        sub = when (target) {
                            MoreTarget.QIBLA -> Route.Qibla
                            MoreTarget.TASBIH -> Route.Tasbih
                            MoreTarget.ACTS -> Route.Acts
                            MoreTarget.SUNNAH -> Route.Sunnah
                            MoreTarget.DISTANCE -> Route.Distance
                            MoreTarget.GAPS -> Route.Gaps
                            MoreTarget.INTERVAL -> Route.Interval
                            MoreTarget.SETTINGS -> Route.Settings
                        }
                    })
                Route.Tasbih -> TasbihScreen { sub = null }
                is Route.ActDetail -> ActDetailScreen(state, fmt, now, route.id) { sub = null }
                Route.Distance -> DistanceScreen(state, fmt) { sub = null }
                Route.AdhanSettings -> MiqatScaffold("الإعدادات", onBack = { sub = null }) {
                    AdhanSection(state, fmt)
                }
                Route.AwarenessSettings -> MiqatScaffold("الإعدادات", onBack = { sub = null }) {
                    AwarenessSection(state, fmt)
                }
                Route.CalendarSettings -> MiqatScaffold("الإعدادات", onBack = { sub = null }) {
                    CalendarSection(state, fmt)
                }
                Route.AppearanceSettings -> MiqatScaffold("الإعدادات", onBack = { sub = null }) {
                    AppearanceSection(state, fmt)
                }
                Route.AboutSettings -> MiqatScaffold("عن التطبيق", onBack = { sub = null }) {
                    AboutSection()
                }
            }
        }
    }

    if (pickerOpen) {
        CityPickerSheet(
            fromCity = state.city,
            arabicIndic = state.display.arabicIndicDigits,
            onSelect = { state.selectCity(it); pickerOpen = false },
            onDismiss = { pickerOpen = false },
        )
    }
}

/** 1 Hz aligned ticker; leaves read [State.value] so recomposition stays scoped. */
@Composable
fun rememberNowState(): State<Long> {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            val n = System.currentTimeMillis()
            now.longValue = n
            delay(1000 - n % 1000)
        }
    }
    return now
}

/**
 * HOME — the hero and the context, nothing else. Every extra item was moved to
 * its own screen (Times / Calendar / Settings) because the v1 home was a stack.
 */
/**
 * HOME — a COMPOSITION, not a list (the design council, 2026-11-11).
 *
 * Four zones, and the day's essentials fit in one screen:
 *  1. the identity (the city + the Hijri date),
 *  2. the occasion band (only on a special day),
 *  3. THE HERO — the sky, the next prayer, its countdown, and the foot
 *     («بعد ذلك»: the shape of the rest of the day),
 *  4. the day-rail — the day's proportions, and the reading card.
 *
 * What was CUT (and why): the six-column prayer strip (the rail says it
 * better), the "الأقرب" row (it is «بعد ذلك» now), the night-markers line
 * (the sunrise is a tick on the rail), and the qibla line (it is a tile in
 * «المزيد», not a fact on the home).
 */
@Composable
fun HomeScreen(
    state: AppState,
    fmt: Fmt,
    nowState: State<Long>,
    onOpenTimes: () -> Unit,
    onOpenQibla: () -> Unit = {},
    onOpenPrayer: (Int) -> Unit = {},
    onOpenMore: () -> Unit = {},
    onOpenFasting: () -> Unit = {},
    onOpenDistance: () -> Unit = {},
    onOpenSunnah: () -> Unit = {},
) {
    val context = LocalContext.current
    val now = nowState.value
    val (y, m, d) = state.cityDate(now)
    val times = state.times(y, m, d)
    val nowHour = state.cityHourOfDay(now)
    val (tyHome, tmHome, tdHome) = state.cityTomorrow(now)
    val tomorrowHome = state.times(tyHome, tmHome, tdHome)
    // THE timers — one model, one clock, and every feature with its own line
    val timers = app.miqat.core.Timers.of(
        app.miqat.core.prayerPoints(times, tomorrowHome.fajr, PRAYER_NAMES),
        nowHour,
        state.notifications.iqamaMinutes,
        iqamaFor = { state.iqamaMinutesFor(it) },
    )
    val timeline = rememberTimeline(state, now)
    val nowMinutes = (nowHour * 60).toInt()
    // how much the home carries (one choice, not a dozen switches)
    val density = state.display.homeDensity

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.lg),
    ) {
        Spacer(Modifier.height(Space.md))

        // 1 — the identity: the place and the day
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (state.usingGps) "موقعك الحالي" else state.city.ar.ifBlank { state.city.en },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (state.usingGps) {
                    // never let the header claim a city the times are NOT from
                    Text(
                        "من نظام الموقع",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Hijri.label(Hijri.today(now, state.display.hijriOffsetDays), fmt.ar),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // the calendar is tabular, not a sighting — say so
                Text(
                    app.miqat.core.IslamicDays.HIJRI_APPROX,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Spacer(Modifier.width(Space.sm))
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .clickable { onOpenMore() },
                contentAlignment = Alignment.Center,
            ) {
                BrandGlyph(kind = Glyph.GRID, active = false, size = 14.dp)
            }
        }

        // 2 — the occasion band (only on a day that has one)
        IslamicDays.todayCard(Hijri.today(now, state.display.hijriOffsetDays))?.let { (title, sub) ->
            Spacer(Modifier.height(Space.md))
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(Radius.row),
                    )
                    .padding(Space.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(4.dp)),
                )
                Spacer(Modifier.width(Space.md))
                Column {
                    Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(Space.lg))

        // 3 — THE hero: the one rich moment
        HeroCard(
            state = state,
            fmt = fmt,
            times = times,
            timers = timers,
            now = now,
            nowHour = nowHour,
            nowMinutes = nowMinutes,
            upcoming = upcomingMoments(timeline, nowMinutes),
        )

        Spacer(Modifier.height(Space.md))
        // «الأقرب إليك» — three REAL cards, each opening its own screen
        val imsakMinute = minutesOf(times.fajr) - state.imsakLeadMinutes
        val saharMoment = timeline.firstOrNull { it.kind == app.miqat.core.Moments.SAHAR_START }
        val sinceImsak = (nowMinutes - imsakMinute).coerceAtLeast(0)
        Text(
            "الأقرب إليك",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Space.sm),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = Space.sm),
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            ShelfCard(
                label = "صيام · منذ الإمساك",
                value = fmt.duration(sinceImsak),
                meta = "متبقٍ " + fmt.duration((minutesOf(times.maghrib) - nowMinutes).coerceAtLeast(0)),
                tint = MaterialTheme.colorScheme.secondary,
                onClick = onOpenFasting,
            )
            ShelfCard(
                label = "احسب بين ميقاتين",
                value = "المسافة",
                meta = "بين أي لحظتين",
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = onOpenDistance,
            )
            ShelfCard(
                label = "بعد المغرب",
                value = "الأسحار",
                meta = "يبدأ " + (saharMoment?.let { fmt.hhmmShort(it.minuteOfDay) } ?: "—"),
                tint = Gold,
                onClick = onOpenSunnah,
            )
        }
        Spacer(Modifier.height(Space.xl))

        // the daylight-saving notice — the clock will change, and that moves
        // every prayer's wall-clock time with it (FULL only: a notice belongs
        // to the "everything" density)
        if (state.display.showDstNotice && density == app.miqat.core.HomeDensity.FULL) {
            val transition = DstInfo.nextTransition(
                context,
                zone = state.city.zone,
                zoneId = state.effectiveZoneId,
                nowMillis = now,
            )
            if (transition != null) {
                val days = (transition.atMillis - now) / 86_400_000L
                if (days in 0..30) {
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        timeInMillis = transition.atMillis
                    }
                    val dd = cal.get(Calendar.DAY_OF_MONTH)
                    val mm = cal.get(Calendar.MONTH) + 1
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .45f),
                                RoundedCornerShape(Radius.row),
                            )
                            .padding(Space.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                        )
                        Spacer(Modifier.width(Space.md))
                        Text(
                            "تغيّر التوقيت ${transition.directionAr}: الساعة " +
                                "${DstTransition.fmtHours(kotlin.math.abs(transition.deltaHours))} " +
                                "يوم ${fmt.n(dd)}/${fmt.n(mm)} — فتتغيّر مواعيد الصلوات في تلك الليلة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.height(Space.lg))
                }
            }
        }

        // the reading card — one verse a day, with its source. Deliberately
        // quiet: it is the last thing on the screen, not a headline.
        if (state.display.showDailyReminder &&
            density != app.miqat.core.HomeDensity.COMPACT
        ) {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.timeInMillis = now
            val reminder = app.miqat.core.DailyReminders.forNow(
                nowHour, cal.get(Calendar.DAY_OF_YEAR),
            )
            val (verse, source) = app.miqat.core.DailyReminders.line(reminder)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.card))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(Space.lg),
            ) {
                Text(
                    verse,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    source,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(Space.lg))
        }

        // the qibla — a tile, not a fact: one tap to the compass
        if (state.display.showNearestMoment &&
            density != app.miqat.core.HomeDensity.COMPACT
        ) {
            val bearing = Qibla.bearing(state.city.lat, state.city.lon)
            val km = Qibla.distanceKm(state.city.lat, state.city.lon)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenQibla() }
                    .padding(vertical = Space.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "القبلة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${fmt.decimalUnit(bearing, 1, "°")}، " +
                        (if (km < 1.0) "أقل من ١ كم" else "${fmt.n(km.toInt())} كم"),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TabularFigures),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        // clearance under the floating navigation — nothing ends half-cut
        Spacer(Modifier.height(56.dp))
    }
}

/** The next three appointments of the day — «بعد ذلك». */
private fun upcomingMoments(timeline: List<Moment>, nowMinutes: Int): List<Moment> =
    timeline
        .filter {
            it.minute > nowMinutes &&
                it.isEvent &&
                it.category != MomentCategory.FASTING &&
                it.kind != Moments.SUNRISE
        }
        .sortedBy { it.minute }
        .take(3)

/** The index of the next prayer among the six (for the rail's gold tick). */
private fun nextPrayerIndex(timers: app.miqat.core.Timers): Int {
    val name = timers.nextPrayer ?: return 0
    val i = PRAYER_NAMES.indexOf(name)
    return if (i in 0 until PRAYER_NAMES.size) i else 0
}

/**
 * THE HERO — the app's one rich moment.
 *
 * The sky bleeds to the card's edge (it IS the card, not a rectangle inside
 * it); the prayer's name is the big element (the number is the answer, the
 * name is the question); the seconds are smaller than the minutes (the clock
 * reads as one object); and the foot carries the day's shape — «بعد ذلك».
 */
@Composable
private fun ShelfCard(
    label: String, value: String, meta: String, tint: Color, onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .width(172.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.surface)
            .clickable(onClick = onClick)
            .padding(Space.md),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
            color = scheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontFamily = Kufi),
            color = tint)
        Text(meta, style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
            color = scheme.onSurfaceVariant)
    }
}

/**
 * THE HERO — "Meridian": a sky, a sun with real light, and one answer.
 *
 * This is the only rich moment on the home. Everything beneath it is a
 * quiet instrument: six appointms on a rule, the day's travelled span,
 * three nearest facts. No arcs, no crop line, no stock containers.
 */
@Composable
private fun HeroCard(
    state: AppState,
    now: Long,
    fmt: Fmt,
    times: app.miqat.core.LocalPrayerTimes,
    timers: app.miqat.core.Timers,
    nowHour: Double,
    nowMinutes: Int,
    upcoming: List<Moment>,
) {
    val scheme = MaterialTheme.colorScheme
    val skyTop = Color(0xFF070B1C)
    val skyMid = Color(0xFF1A1E32)
    val skyWarm = Color(0xFF33252C)
    val skyHorizon = Color(0xFF4A2F28)
    val ground = Color(0xFF06070D)

    // the sun's REAL altitude from the same declination the engine uses
    val (sy, sm, sd) = state.cityDate(now)
    val declDeg = app.miqat.core.PrayerTimes
        .sunPosition(app.miqat.core.PrayerTimes.julian(sy, sm, sd)).first
    val phi = Math.toRadians(state.city.lat)
    val delta = Math.toRadians(declDeg)
    val noon = (times.sunrise + times.maghrib) / 2.0
    val hourAngle = Math.toRadians(15.0 * (nowHour - noon))
    val refr = Math.sin(Math.toRadians(0.83))
    val sinNum = kotlin.math.sin(phi) * kotlin.math.sin(delta) +
        kotlin.math.cos(phi) * kotlin.math.cos(delta) * kotlin.math.cos(hourAngle) + refr
    val sinDen = kotlin.math.sin(phi) * kotlin.math.sin(delta) +
        kotlin.math.cos(phi) * kotlin.math.cos(delta) + refr
    val elevationNorm = (sinNum / sinDen).toFloat().coerceIn(-1f, 1f)
    val progress = travelledFraction(times, nowHour)

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.hero))) {
        // ── THE SKY ──────────────────────────────────────────────────
        Box(Modifier.fillMaxWidth().height(300.dp)) {
            Canvas(Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(skyTop, Color(0xFF0E1528), skyMid, skyWarm, skyHorizon),
                        startY = 0f, endY = h,
                    ),
                    size = androidx.compose.ui.geometry.Size(w, h),
                )
                // the ember lies on the horizon (light comes from the sun)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Sunset.copy(alpha = 0.20f), Sunset.copy(alpha = 0f)),
                        center = Offset(w * (1f - progress), h),
                        radius = h * 0.75f,
                    ),
                    radius = h * 0.75f,
                    center = Offset(w * (1f - progress), h),
                )
                // the stars are small, quiet, and only in the dark upper sky
                listOf(0.12f to 0.12f, 0.27f to 0.22f, 0.44f to 0.10f, 0.61f to 0.18f,
                    0.78f to 0.09f, 0.90f to 0.24f, 0.35f to 0.31f).forEachIndexed { i, (fx, fy) ->
                    drawCircle(
                        Color(0xFFEBE8E0).copy(alpha = 0.20f + (i % 3) * 0.08f),
                        1.4f, Offset(w * fx, h * fy),
                    )
                }
                // the sun: bloom, limb and a bright core — an object, not a bead
                val sx = (w * (1f - progress)).coerceIn(24f, w - 24f)
                // below the horizon the sun is DIM, not deleted — the disc
                // still shows where it truly is under the day (the reference
                // keeps the sun on screen at every hour)
                val below = elevationNorm < 0f
                val sy2 = (h * 0.62f - elevationNorm.coerceIn(-0.45f, 1f) * h * 0.40f)
                    .coerceAtMost(h * 0.95f)
                val a = if (below) 0.28f else 1f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFFFF2C8).copy(alpha = 0.75f * a),
                            Gold.copy(alpha = 0.42f * a), Sunset.copy(alpha = 0f)),
                        center = Offset(sx, sy2), radius = 96f,
                    ),
                    radius = 96f, center = Offset(sx, sy2),
                )
                drawCircle(Gold.copy(alpha = a), 21f, Offset(sx, sy2))
                drawCircle(Color(0xFFF0C875).copy(alpha = a), 17f, Offset(sx, sy2))
                drawCircle(Color(0xFFFFF2C8).copy(alpha = 0.55f * a), 7f,
                    Offset(sx - 3f, sy2 - 3f))
            }
            // the answer, printed on the sky
            Column(
                Modifier.align(Alignment.BottomCenter).padding(horizontal = Space.lg)
                    .padding(bottom = Space.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "الموعد القادم" + if (timers.nextIsTomorrow) " · غداً" else "",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = Plex, fontSize = 11.sp,
                    ),
                    color = scheme.onSurfaceVariant,
                )
                // THREE PHASES, three distinct readings — each owns the
                // hero for its time, and each is visibly different from the
                // next-prayer countdown.
                val phase = when {
                    timers.sinceIqamaPrayer != null -> app.miqat.core.IqamaPhase.SINCE
                    state.display.showTimerIqama && timers.currentPrayer != null &&
                        timers.iqamaIn != null -> app.miqat.core.IqamaPhase.PENDING
                    else -> app.miqat.core.IqamaPhase.BEFORE_ADHAN
                }
                when (phase) {
                    app.miqat.core.IqamaPhase.PENDING -> {
                        Text(
                            "بقي إلى إقامة " + (timers.iqamaPrayer ?: "الصلاة"),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                            color = Teal,
                        )
                        Text(
                            fmt.hms(timers.iqamaSeconds),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = Cairo, fontSize = 52.sp,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = TabularFigures,
                            ),
                            color = Teal,
                        )
                        Text(
                            "منذ أذان " + (timers.currentPrayer ?: "الصلاة") + " " +
                                fmt.hms(timers.sinceSeconds),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Plex),
                            color = scheme.onSurface,
                        )
                        // the ADHAN→IQAMA window, filled by what has passed
                        val whole = (timers.sinceSeconds + timers.iqamaSeconds).coerceAtLeast(1L)
                        Box(
                            Modifier.fillMaxWidth().padding(top = Space.sm)
                                .height(4.dp).clip(RoundedCornerShape(2.dp))
                                .background(scheme.outlineVariant),
                        ) {
                            Box(
                                Modifier.fillMaxWidth(timers.sinceSeconds.toFloat() / whole)
                                    .height(4.dp).clip(RoundedCornerShape(2.dp))
                                    .background(Teal),
                            )
                        }
                        Text(
                            "الموعد التالي " + (timers.nextPrayer ?: "—") + " · " +
                                fmt.duration(timers.nextIn.coerceAtLeast(0)),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                            color = scheme.onSurfaceVariant,
                        )
                    }

                    app.miqat.core.IqamaPhase.SINCE -> {
                        Text(
                            "منذ إقامة " + (timers.sinceIqamaPrayer ?: "الصلاة"),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                            color = Color(0xFFEBD9A8),
                        )
                        Text(
                            fmt.hms(timers.sinceIqamaSeconds),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = Cairo, fontSize = 52.sp,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = TabularFigures,
                            ),
                            color = Color(0xFFEBD9A8),
                        )
                        Text(
                            "داخل وقت " + (timers.currentPrayer ?: "الصلاة"),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Plex),
                            color = scheme.onSurface,
                        )
                        Text(
                            "الموعد التالي " + (timers.nextPrayer ?: "—") + " · " +
                                fmt.duration(timers.nextIn.coerceAtLeast(0)),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                            color = scheme.onSurfaceVariant,
                        )
                    }

                    else -> {
                        val urgency = if (state.display.urgencyEnabled) {
                            app.miqat.core.UrgencyRules.of(
                                timers.remainingSeconds, state.display.urgencySoonMinutes,
                                state.display.urgencyImminentMinutes,
                            )
                        } else app.miqat.core.Urgency.CALM
                        val numberColor = when (urgency) {
                            app.miqat.core.Urgency.CALM -> Gold
                            app.miqat.core.Urgency.SOON -> UrgencySoon
                            else -> UrgencyImminent
                        }
                        val totalMin = timers.remainingSeconds / 60L
                        val big = app.miqat.core.NumFmt.digits(
                            "%02d:%02d".format(totalMin / 60, totalMin % 60), fmt.ar,
                        )
                        val sec = app.miqat.core.NumFmt.digits(
                            ":%02d".format(timers.remainingSeconds % 60), fmt.ar,
                        )
                        // Row children are REVERSED: in RTL the first child
                        // lands on the right, so seconds-first puts them
                        // AFTER the minutes in the visual reading order.
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                sec,
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontFamily = Cairo, fontSize = 24.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFeatureSettings = TabularFigures,
                                ),
                                color = numberColor.copy(alpha = 0.78f),
                            )
                            Text(
                                big,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = Cairo, fontSize = 52.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = TabularFigures,
                                ),
                                color = numberColor,
                            )
                        }
                        Text(
                            timers.nextPrayer ?: "—",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = Kufi, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                            ),
                            color = scheme.onSurface,
                        )
                    }
                }
            }
        }

        // ── THE HORIZON ──────────────────────────────────────────────
        Box(
            Modifier.fillMaxWidth().height(1.dp).background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Gold.copy(alpha = .35f),
                        Sunset.copy(alpha = .7f), Gold.copy(alpha = .35f), Color.Transparent),
                ),
            ),
        )

        // ── THE GROUND ───────────────────────────────────────────────
        Column(Modifier.fillMaxWidth().background(ground)) {
            // six appointed moments on one rule
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Space.sm, vertical = Space.md),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                times.list.forEachIndexed { i, hour ->
                    val name = PRAYER_NAMES[i]
                    val minute = minutesOf(hour)
                    val passed = minute < nowMinutes
                    val next = timers.nextPrayer == name && !timers.nextIsTomorrow
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = Plex, fontSize = 10.sp,
                            ),
                            color = if (next) UrgencyImminent else if (passed)
                                Color(0xFF44485A) else scheme.onSurfaceVariant,
                        )
                        Text(
                            fmt.hhmmShort(minute),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = Kufi, fontSize = 12.sp,
                                fontFeatureSettings = TabularFigures,
                            ),
                            color = if (next) UrgencyImminent else if (passed)
                                Color(0xFF44485A) else scheme.onSurface,
                        )
                        if (next) {
                            Spacer(Modifier.height(3.dp))
                            Box(
                                Modifier.size(4.dp).clip(CircleShape)
                                    .background(UrgencyImminent),
                            )
                        }
                    }
                }
            }
            // the travelled span — a real measure, not a progress metaphor
            Box(
                Modifier.fillMaxWidth().padding(horizontal = Space.md)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(scheme.outlineVariant),
            ) {
                Box(
                    Modifier.fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(listOf(Gold, Sunset)),
                        ),
                )
            }
            Spacer(Modifier.height(Space.md))
            // «بعد ذلك» — kept, but as a quiet line of the day's next facts
            if (upcoming.isNotEmpty()) {
                Text(
                    "بعد ذلك",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Space.md),
                )
                upcoming.take(2).forEach { m ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = Space.md, vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(m.labelAr, style = MaterialTheme.typography.bodySmall.copy(fontFamily = Plex),
                            color = scheme.onSurface)
                        Text(fmt.hhmmShort(m.minuteOfDay), style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = Kufi, fontFeatureSettings = TabularFigures,
                        ), color = scheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(Space.md))
        }
    }
}

private fun travelledFraction(times: app.miqat.core.LocalPrayerTimes, nowHour: Double): Float {
    val fajr = minutesOf(times.fajr).toDouble()
    val isha = minutesOf(times.isha).toDouble().let { if (it < fajr) it + 1440.0 else it }
    val now = nowHour * 60.0
    val m = if (now < fajr) now + 1440.0 else now
    return (((m - fajr) / (isha - fajr).coerceAtLeast(1.0)).coerceIn(0.0, 1.0)).toFloat()
}

internal val PRAYER_KEY = listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")

/** A worship-window candidate for the "nearest window" line on home. */
internal data class WindowOption(val label: String, val start: Double, val end: Double)

/** The six times as a list (the order the app uses everywhere). */
internal val LocalPrayerTimes.list: List<Double>
    get() = listOf(fajr, sunrise, dhuhr, asr, maghrib, isha)
