package app.miqat.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import app.miqat.core.Moment
import app.miqat.core.MomentCategory

/** The four top-level destinations — one per CORE, no dumping ground. */
enum class TopLevel(val label: String, val glyph: Glyph) {
    Home("الرئيسية", Glyph.HORIZON),
    Times("المواقيت", Glyph.TICKS),
    Fasting("الصيام", Glyph.BRACKET),
    Calendar("التقويم", Glyph.CELL),
    More("المزيد", Glyph.GRID),
}

/** Sub-routes pushed on top of a top-level destination (their own screens). */
sealed interface Route {
    data class PrayerDetailArgs(val index: Int) : Route
    data class WorshipDetail(val kind: WorshipKind) : Route
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
enum class WorshipKind(val label: String, val blurb: String) {
    DUHA("وقت الضحى", "من بعد الشروق إلى ما قبل الظهر"),
    SAHAR("وقت الأسحار", "آخر الليل قبل الفجر، حين يُستغفر"),
    MIDNIGHT("منتصف الليل", "نصف الليل بين المغرب والفجر"),
    LAST_THIRD("الثلث الأخير من الليل", "آخر ثلث الليل — وقت الاستغفار"),
}

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
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                TopLevel.entries.forEach { dest ->
                    NavigationBarItem(
                        selected = top == dest && sub == null,
                        onClick = { top = dest; sub = null },
                        icon = {
                            BrandGlyph(
                                kind = dest.glyph,
                                active = top == dest && sub == null,
                                size = 22.dp,
                            )
                        },
                        label = { Text(dest.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val route = sub) {
                null -> when (top) {
                    TopLevel.Home -> HomeScreen(
                        state, fmt, now,
                        onOpenTimes = { top = TopLevel.Times },
                        onOpenQibla = { sub = Route.Qibla },
                    )
                    TopLevel.Times -> TimesScreen(
                        state, fmt, now,
                        onOpenPrayer = { sub = Route.PrayerDetailArgs(it) },
                        onOpenWorship = { sub = Route.WorshipDetail(it) },
                        onOpenGaps = { sub = Route.Gaps },
                        onOpenDistance = { sub = Route.Distance },
                        onOpenActs = { sub = Route.Acts },
                        onOpenInterval = { sub = Route.Interval },
                        onOpenSunnah = { sub = Route.Sunnah },
                    )
                    TopLevel.Fasting -> FastingScreen(state, fmt, now)
                    TopLevel.Calendar -> CalendarScreen(state, fmt, now, onOpenDay = { y, m, d -> sub = Route.DayDetail(y, m, d) })
                    TopLevel.More -> MoreScreen(state, fmt, now, onOpen = { target ->
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
                }
                is Route.PrayerDetailArgs -> PrayerDetailScreen(state, fmt, route.index) { sub = null }
                is Route.WorshipDetail -> WorshipDetailScreen(state, fmt, route.kind) { sub = null }
                is Route.DayDetail -> DayDetailScreen(state, fmt, route.year, route.month, route.day) { sub = null }
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
            nowHour = nowHour,
            nowMinutes = nowMinutes,
            upcoming = upcomingMoments(timeline, nowMinutes),
        )

        Spacer(Modifier.height(Space.xl))

        // 4 — the day-rail: the day's proportions (and the sunrise, a tick)
        if (state.display.showPrayerStrip) {
            DayRail(
                times = times,
                fmt = fmt,
                nextIndex = nextPrayerIndex(timers),
                nowMinute = nowMinutes.toDouble(),
            )
            Spacer(Modifier.height(Space.xl))
        }

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
        Spacer(Modifier.height(96.dp))
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
private fun HeroCard(
    state: AppState,
    fmt: Fmt,
    times: app.miqat.core.LocalPrayerTimes,
    timers: app.miqat.core.Timers,
    nowHour: Double,
    nowMinutes: Int,
    upcoming: List<Moment>,
) {
    val scheme = MaterialTheme.colorScheme
    // the sky's phase — from the day's REAL sunrise and sunset
    val skyPhase = Sky.phaseOf(
        nowHour,
        sunriseHour = ((times.sunrise % 24.0) + 24.0) % 24.0,
        maghribHour = ((times.maghrib % 24.0) + 24.0) % 24.0,
    )
    // the prayer ticks ON their real times (the arc is a fact, not a drawing)
    val fajrMinute = minutesOf(times.fajr)
    val ishaMinute = minutesOf(times.isha)
    val tickParams = times.list.map {
        DayArc.parameterFor(minutesOf(it).toDouble(), fajrMinute.toDouble(), ishaMinute.toDouble())
    }

    Column(
        Modifier
            .fillMaxWidth()
            .cardSurface(RoundedCornerShape(Radius.hero)),
    ) {
        // the sky — to the card's edge
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(168.dp),
        ) {
            val w = size.width
            val h = size.height
            val horizon = h - 14f
            // RTL: the day reads from the right — the Fajr at the right end,
            // the sun setting towards the left
            val p0 = Offset(w * 0.93f, horizon)
            val p2 = Offset(w * 0.07f, horizon)
            val p1 = Offset(w * 0.5f, 10f)

            fun pointAt(t: Float): Offset {
                val mt = 1 - t
                return Offset(
                    mt * mt * p0.x + 2 * mt * t * p1.x + t * t * p2.x,
                    mt * mt * p0.y + 2 * mt * t * p1.y + t * t * p2.y,
                )
            }

            val sky = Path().apply {
                moveTo(p0.x, p0.y)
                quadraticBezierTo(p1.x, p1.y, p2.x, p2.y)
                lineTo(p2.x, horizon)
                lineTo(p0.x, horizon)
                close()
            }
            val nightSky = scheme.background.luminance() < 0.5f
            drawPath(
                sky,
                Brush.verticalGradient(
                    Sky.bands(skyPhase),
                    startY = 0f,
                    endY = horizon,
                ),
            )

            // the stars — only when the sky is dark enough to hold them
            val starStrength = Sky.starAlpha(skyPhase)
            if (starStrength > 0.01f) {
                val stars = listOf(
                    0.16f to 0.18f, 0.28f to 0.10f, 0.41f to 0.22f, 0.55f to 0.08f,
                    0.67f to 0.19f, 0.78f to 0.12f, 0.88f to 0.24f, 0.36f to 0.31f,
                    0.62f to 0.30f, 0.10f to 0.30f, 0.47f to 0.34f, 0.83f to 0.33f,
                )
                stars.forEachIndexed { i, (fx, fy) ->
                    val p = pointAt(fx)
                    val alpha = (0.30f + 0.35f * ((i % 3) / 2f)) * starStrength
                    drawCircle(
                        Sky.tick.copy(alpha = alpha),
                        radius = if (i % 4 == 0) 2.2f else 1.4f,
                        center = Offset(p.x, p.y * (fy * 2.2f)),
                    )
                }
            }

            // the ground
            drawRect(
                Sky.ground,
                topLeft = Offset(0f, horizon),
                size = androidx.compose.ui.geometry.Size(w, h - horizon),
            )

            // the rim — in two weights: what the day has travelled (gold) and
            // what is left (a hairline). The progress is read as LENGTH.
            val rim = Path().apply {
                moveTo(p0.x, p0.y)
                quadraticBezierTo(p1.x, p1.y, p2.x, p2.y)
            }
            val night = nightSky
            drawPath(
                rim,
                Sky.rim,
                style = Stroke(width = 2f, cap = StrokeCap.Round),
            )
            val travelled = Path()
            val steps = 40
            for (i in 0..steps) {
                val t = 0.07f + 0.86f * travelledFraction(times, nowHour) * i / steps
                val pt = pointAt(t)
                if (i == 0) travelled.moveTo(pt.x, pt.y) else travelled.lineTo(pt.x, pt.y)
            }
            drawPath(
                travelled,
                Brush.horizontalGradient(
                    listOf(
                        if (night) Color(0xFFE8C46A) else Color(0xFF8A6A16),
                        if (night) Color(0xFFFFB25C) else Color(0xFFB4600C),
                    ),
                    startX = p0.x, endX = p2.x,
                ),
                style = Stroke(width = 5f, cap = StrokeCap.Round),
            )

            // the prayer ticks — ON their real times
            tickParams.forEachIndexed { index, p ->
                val c = pointAt(0.07f + 0.86f * p)
                val isNext = timers.nextPrayer == PRAYER_NAMES[index] && !timers.nextIsTomorrow
                val isSun = index == 1
                drawCircle(
                    when {
                        isNext -> Sky.sunMark
                        isSun -> Sky.sunMark
                        else -> Sky.tick
                    },
                    radius = if (isNext) 5f else if (isSun) 3.2f else 3f,
                    center = c,
                )
            }

            // the sun, where the day actually is
            val sun = pointAt(0.07f + 0.86f * travelledFraction(times, nowHour))
            drawCircle(Color(0xFFFFB25C).copy(alpha = .18f), radius = 26f, center = sun)
            drawCircle(Color(0xFFFFB25C).copy(alpha = .35f), radius = 15f, center = sun)
            drawCircle(Color(0xFFFFE0A8), radius = 8.5f, center = sun)
        }

        // the typographic unit: the caption, the NAME, the number
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.lg, vertical = Space.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "الموعد القادم",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
                val chip = timers.stateAr
                if (chip != null) {
                    Text(
                        chip,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.secondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.chip))
                            .background(scheme.surfaceVariant)
                            .padding(horizontal = Space.sm, vertical = 3.dp),
                    )
                }
            }

            Spacer(Modifier.height(Space.xs))

            Text(
                timers.nextPrayer ?: "—",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp,
                ),
                color = scheme.onSurface,
            )

            // the countdown — the seconds smaller, so the clock reads as one
            val urgency = if (state.display.urgencyEnabled) {
                UrgencyRules.of(
                    timers.remainingSeconds,
                    state.display.urgencySoonMinutes,
                    state.display.urgencyImminentMinutes,
                )
            } else Urgency.CALM
            val momentColor = when (urgency) {
                Urgency.CALM -> scheme.onSurface
                Urgency.SOON -> UrgencySoon
                Urgency.IMMINENT, Urgency.NOW -> UrgencyImminent
            }
            BigCountdown(
                text = fmt.hms(timers.remainingSeconds),
                color = momentColor,
                modifier = Modifier.padding(top = Space.xs),
            )
            if (timers.nextIsTomorrow) {
                Text(
                    "غداً",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            // the elapsed — its own line, and only in the first hour
            if (state.display.showTimerElapsed && timers.sinceWorthShowing) {
                Spacer(Modifier.height(Space.xs))
                Text(
                    "مرّ من وقت ${timers.currentPrayer} · ${fmt.hms(timers.sinceSeconds)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            // the iqama — its own line, its own clock
            if (state.display.showTimerIqama && timers.iqamaIn != null) {
                Spacer(Modifier.height(Space.xs))
                Text(
                    "إقامة ${timers.iqamaPrayer} · بعد ${fmt.hms(timers.iqamaSeconds)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.tertiary,
                )
            }
        }

        // the foot — «بعد ذلك»: the shape of the rest of the day
        if (upcoming.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.lg)
                    .padding(bottom = Space.md),
            ) {
                androidx.compose.material3.HorizontalDivider(
                    color = scheme.outlineVariant,
                    modifier = Modifier.padding(bottom = Space.sm),
                )
                Text(
                    "بعد ذلك",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
                upcoming.forEach { moment ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = Space.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            moment.labelAr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface,
                        )
                        Text(
                            fmt.hhmm(moment.minuteOfDay),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFeatureSettings = TabularFigures,
                            ),
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** The day's progress along the arc (0 = the Fajr, 1 = the Isha). */
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
