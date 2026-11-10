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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miqat.core.Appointment
import app.miqat.core.DayClock
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

/** The four top-level destinations — one per CORE, no dumping ground. */
enum class TopLevel(val label: String, val icon: ImageVector) {
    Home("الرئيسية", Icons.Default.Home),
    Times("المواقيت", Icons.Default.List),
    Calendar("التقويم", Icons.Default.DateRange),
    Settings("الإعدادات", Icons.Default.Settings),
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
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
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
                        onOpenInterval = { sub = Route.Interval },
                        onOpenSunnah = { sub = Route.Sunnah },
                    )
                    TopLevel.Calendar -> CalendarScreen(state, fmt, now, onOpenDay = { y, m, d -> sub = Route.DayDetail(y, m, d) })
                    TopLevel.Settings -> SettingsScreen(state, fmt, onOpen = { r ->
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
    val appointments = PRAYER_KEY.mapIndexed { i, k -> Appointment(k, PRAYER_NAMES[i], times.list[i]) }
    val seg = DayClock.position(appointments, nowHour)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.lg),
    ) {
        Spacer(Modifier.height(Space.md))
        // identity row: place + hijri date
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
            Text(
                Hijri.label(Hijri.today(now, state.display.hijriOffsetDays), fmt.ar),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Space.lg))

        // THE hero — one only
        HeroCard(state, fmt, times, seg, nowHour)

        Spacer(Modifier.height(Space.md))

        // the day's context, if any (Islamic day)
        IslamicDays.todayCard(Hijri.today(now, state.display.hijriOffsetDays))?.let { (title, sub) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(14.dp),
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
            Spacer(Modifier.height(Space.md))
        }

        // the awareness strip (neovim's "how far are you from the lines") —
        // ONE place that says: now, what time has entered, and what is next
        if (state.display.showAwarenessStrip) {
            val timeline = rememberTimeline(state, now)
            AwarenessStrip(
                state = state,
                fmt = fmt,
                nowMillis = now,
                appointments = PRAYER_KEY.mapIndexed { i, k ->
                    Appointment(k, PRAYER_NAMES[i], times.list[i])
                },
                moments = timeline,
            )
            Spacer(Modifier.height(Space.md))
        }

        // today's times — one compact strip, the schedule at a glance
        Spacer(Modifier.height(Space.md))
        Text(
            "مواقيت اليوم",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Space.xs))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PRAYER_NAMES.forEachIndexed { i, name ->
                val isNext = seg?.next?.key == PRAYER_KEY[i]
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isNext) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        fmt.hhmm(minutesOf(times.list[i])),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isNext) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        Divider()

        // the daylight-saving notice — the clock will change, and that moves
        // every prayer's wall-clock time with it
        if (state.display.showDstNotice) {
            val zoneId = state.city.zoneId
            val transition = if (zoneId != null) {
                DstInfo.nextTransition(context, zoneId, now)
            } else null
            if (transition != null) {
                val days = (transition.atMillis - now) / 86_400_000L
                if (days in 0..30) {
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        timeInMillis = transition.atMillis
                    }
                    val d = cal.get(Calendar.DAY_OF_MONTH)
                    val m = cal.get(Calendar.MONTH) + 1
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
                                "يوم ${fmt.n(d)}/${fmt.n(m)} — فتتغيّر مواعيد الصلوات في تلك الليلة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.height(Space.md))
                }
            }
        }

        // the nearest moment of ANY kind — the next prayer, the next window,
        // whatever comes first (the app's one piece of "intelligence" on home)
        val (ty2, tm2, td2) = state.cityTomorrow(now)
        val tomorrowTimes = state.times(ty2, tm2, td2)
        val timeline = rememberTimeline(state, now)
        val nowMinutes = minutesOf(nowHour)
        val nearest = timeline
            .filter { it.isEvent }
            .minByOrNull { Moments.until(it, nowMinutes) }

        if (nearest != null) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = Space.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("الأقرب", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(nearest.labelAr, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    fmt.hhmm(nearest.minuteOfDay),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Divider()
        }

        // qibla — one line, and it opens the COMPASS
        val bearing = Qibla.bearing(state.city.lat, state.city.lon)
        val km = Qibla.distanceKm(state.city.lat, state.city.lon)
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onOpenQibla() }
                .padding(vertical = Space.md),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("القبلة", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${fmt.decimalUnit(bearing, 1, "°")}، " +
                    (if (km < 1.0) "أقل من ١ كم" else "${fmt.n(km.toInt())} كم"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(Space.xxl))
    }
}

@Composable
private fun HeroCard(
    state: AppState,
    fmt: Fmt,
    times: app.miqat.core.LocalPrayerTimes,
    seg: app.miqat.core.ClockSegment?,
    nowHour: Double,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .background(scheme.surface, RoundedCornerShape(Radius.hero))
            .padding(Space.lg),
    ) {
        // THE DAY-SKY — the app's one rich moment: the arc is a horizon, the
        // sky above it, and the sun travelling where the day actually is.
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val w = size.width
            val h = size.height
            val horizon = h - 16f
            val p0 = Offset(w * 0.07f, horizon)
            val p2 = Offset(w * 0.93f, horizon)
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
            // the sky's palette follows the theme: night above a dark app,
            // first light above the dawn-paper one
            val nightSky = scheme.background.luminance() < 0.5f
            drawPath(
                sky,
                Brush.verticalGradient(
                    if (nightSky) {
                        listOf(
                            Color(0xFF0E1428),
                            Color(0xFF1E1836),
                            Color(0xFF4A2A3A),
                            Color(0xFF8A5533).copy(alpha = .65f),
                        )
                    } else {
                        listOf(
                            Color(0xFFBBD3E8),
                            Color(0xFFD9DCEB),
                            Color(0xFFF0DCC4),
                            Color(0xFFF3C98F).copy(alpha = .8f),
                        )
                    },
                    startY = 0f,
                    endY = horizon,
                ),
            )

            // the stars, still out above the horizon
            val stars = listOf(
                0.16f to 0.18f, 0.28f to 0.10f, 0.41f to 0.22f, 0.55f to 0.08f,
                0.67f to 0.19f, 0.78f to 0.12f, 0.88f to 0.24f, 0.36f to 0.31f,
                0.62f to 0.30f, 0.10f to 0.30f,
            )
            stars.forEachIndexed { i, (fx, fy) ->
                val p = pointAt(fx)
                val alpha = 0.30f + 0.35f * ((i % 3) / 2f)
                drawCircle(
                    (if (scheme.background.luminance() < 0.5f) Color(0xFFF3E9D2) else Color(0xFF6B6455)).copy(alpha = alpha),
                    radius = if (i % 4 == 0) 2.2f else 1.4f,
                    center = Offset(p.x, p.y * (fy * 2.2f)),
                )
            }

            // the ground
            drawRect(
                if (scheme.background.luminance() < 0.5f) Color(0xFF070912) else Color(0xFFD8CDB4),
                topLeft = Offset(0f, horizon),
                size = androidx.compose.ui.geometry.Size(w, h - horizon),
            )

            // the rim — the horizon line
            val rim = Path().apply {
                moveTo(p0.x, p0.y)
                quadraticBezierTo(p1.x, p1.y, p2.x, p2.y)
            }
            drawPath(
                rim,
                if (scheme.background.luminance() < 0.5f) Color(0xFF6E5A3A) else Color(0xFFA98C4E),
                style = Stroke(width = 2.5f, cap = StrokeCap.Round),
            )

            // where the sun is now
            val mins = times.list.map { ((it + 0.5 / 60.0) % 24.0 + 24.0) % 24.0 * 60 }
            val dayStart = mins[0]
            val dayEnd = mins[5].let { if (it < dayStart) it + 1440 else it }
            val nowMin = nowHour * 60
            val f = (((nowMin - dayStart) / (dayEnd - dayStart)).coerceIn(0.0, 1.0)).toFloat()
            val sun = pointAt(0.07f + 0.86f * f)

            drawCircle(Color(0xFFFFB25C).copy(alpha = .18f), radius = 26f, center = sun)
            drawCircle(Color(0xFFFFB25C).copy(alpha = .35f), radius = 15f, center = sun)
            drawCircle(Color(0xFFFFE0A8), radius = 8.5f, center = sun)

            // the prayer ticks
            listOf(0f, 0.24f, 0.47f, 0.68f, 0.86f, 1f).forEachIndexed { i, f2 ->
                val c = pointAt(f2)
                val isNext = seg != null && seg.next.key == PRAYER_KEY[i] && !seg.nextIsTomorrow
                drawCircle(
                    if (isNext) Color(0xFFE8C46A) else Color(0xFF8E8672),
                    radius = if (isNext) 5f else 3.5f,
                    center = c,
                )
            }
        }

        Spacer(Modifier.height(Space.sm))
        if (seg != null) {
            val urgency = if (state.display.urgencyEnabled) {
                UrgencyRules.of(
                    seg.remainingSeconds,
                    state.display.urgencySoonMinutes,
                    state.display.urgencyImminentMinutes,
                )
            } else Urgency.CALM
            val momentColor = when (urgency) {
                Urgency.CALM -> scheme.onSurface
                Urgency.SOON -> UrgencySoon
                Urgency.IMMINENT, Urgency.NOW -> UrgencyImminent
            }
            Text(
                when (urgency) {
                    Urgency.NOW -> "حان الآن: ${seg.next.labelAr}"
                    Urgency.IMMINENT -> "استعدّ لصلاة ${seg.next.labelAr}"
                    Urgency.SOON -> "اقترب وقت ${seg.next.labelAr}"
                    Urgency.CALM -> "المتبقي لميقات ${seg.next.labelAr}" +
                        if (seg.nextIsTomorrow) " (غداً)" else ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (urgency == Urgency.CALM) scheme.onSurfaceVariant else momentColor,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                fmt.hms(seg.remainingSeconds),
                style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = TabularFigures),
                color = momentColor,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

internal val PRAYER_KEY = listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")

/** A worship-window candidate for the "nearest window" line on home. */
internal data class WindowOption(val label: String, val start: Double, val end: Double)

/** The six times as a list (the order the app uses everywhere). */
internal val LocalPrayerTimes.list: List<Double>
    get() = listOf(fajr, sunrise, dhuhr, asr, maghrib, isha)
