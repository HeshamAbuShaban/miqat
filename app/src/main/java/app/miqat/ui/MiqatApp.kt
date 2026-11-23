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
import androidx.compose.foundation.layout.navigationBarsPadding
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
    Acts("الطاعات", Glyph.SAJDA),
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
            Box(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
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
        containerColor = if (isLightTheme()) PaperBg else Color(0xFF04050A),
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(pageWash())),
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
    // The 1 Hz tick only reaches the hero's seconds. Everything else reads a
    // MINUTE bucket, so a ticking second no longer rebuilds the sky, the
    // verse card, the rail and the navigation.
    val minuteBucket by androidx.compose.runtime.derivedStateOf {
        nowState.value / 60_000L
    }
    val now = minuteBucket * 60_000L
    val (y, m, d) = state.cityDate(now)
    val times = state.times(y, m, d)
    val nowHour = state.cityHourOfDay(now)
    val (tyHome, tmHome, tdHome) = state.cityTomorrow(now)
    val tomorrowHome = state.times(tyHome, tmHome, tdHome)
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
            nowState = nowState,
            tomorrowFajr = tomorrowHome.fajr,
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
internal val PRAYER_KEY: List<String> get() = app.miqat.core.Prayers.KEYS

/** A worship-window candidate for the "nearest window" line on home. */
internal data class WindowOption(val label: String, val start: Double, val end: Double)

/** The six times as a list (the order the app uses everywhere). */
internal val LocalPrayerTimes.list: List<Double>
    get() = listOf(fajr, sunrise, dhuhr, asr, maghrib, isha)
