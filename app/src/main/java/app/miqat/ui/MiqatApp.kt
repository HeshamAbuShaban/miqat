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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miqat.core.Appointment
import app.miqat.core.DayClock
import app.miqat.core.LocalPrayerTimes
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
                    TopLevel.Home -> HomeScreen(state, fmt, onOpenTimes = { top = TopLevel.Times })
                    TopLevel.Times -> TimesScreen(
                        state, fmt,
                        onOpenPrayer = { sub = Route.PrayerDetailArgs(it) },
                        onOpenWorship = { sub = Route.WorshipDetail(it) },
                        onOpenGaps = { sub = Route.Gaps },
                        onOpenInterval = { sub = Route.Interval },
                        onOpenSunnah = { sub = Route.Sunnah },
                    )
                    TopLevel.Calendar -> CalendarScreen(state, fmt, onOpenDay = { y, m, d -> sub = Route.DayDetail(y, m, d) })
                    TopLevel.Settings -> SettingsScreen(state, fmt, onOpen = { r ->
                        sub = when (r) {
                            SettingsRoute.Location -> Route.LocationSettings
                            SettingsRoute.Calculation -> Route.CalculationSettings
                            SettingsRoute.Notifications -> Route.NotificationsSettings
                            SettingsRoute.Awareness -> Route.AwarenessSettings
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
fun HomeScreen(state: AppState, fmt: Fmt, onOpenTimes: () -> Unit) {
    val nowState = rememberNowState()
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
            Text(
                state.city.ar.ifBlank { state.city.en },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
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

        // next up — a single line, not a stack of cards
        if (seg != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenTimes() }
                    .padding(vertical = Space.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("المتبقي", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        seg.next.labelAr + if (seg.nextIsTomorrow) " (غداً)" else "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    fmt.hms(seg.remainingSeconds),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Divider()
            Row(
                Modifier.fillMaxWidth().padding(vertical = Space.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("منذ ${seg.current.labelAr}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(fmt.hms(seg.elapsedSeconds), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Divider()
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

        // the nearest worship window — its own screen, one line here
        val (ty, tm, td) = state.cityTomorrow(now)
        val tomorrowTimes = state.times(ty, tm, td)
        val night = WorshipWindows.night(maghrib = times.maghrib, fajrNext = tomorrowTimes.fajr)
        val duha = WorshipWindows.duha(sunrise = times.sunrise, dhuhr = times.dhuhr)
        val sahar = WorshipWindows.sahar(maghrib = times.maghrib, fajrNext = tomorrowTimes.fajr)
        val nowMinutes = minutesOf(nowHour)
        val nextWindow = listOf(
            WindowOption("وقت الضحى", duha.startHour, duha.endHour),
            WindowOption("منتصف الليل", night.midnightHour, night.midnightHour),
            WindowOption("الثلث الأخير", night.lastThirdStartHour, night.lastThirdStartHour),
            WindowOption("وقت الأسحار", sahar.startHour, sahar.endHour),
        ).filter { !it.start.isNaN() }
            .minByOrNull { (minutesOf(it.start) - nowMinutes + 1440) % 1440 }

        if (nextWindow != null) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = Space.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("أقرب وقت", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(nextWindow.label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    fmt.hhmm(minutesOf(nextWindow.start)),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Divider()
        }

        // qibla — one line
        val bearing = Qibla.bearing(state.city.lat, state.city.lon)
        val km = Qibla.distanceKm(state.city.lat, state.city.lon)
        Row(
            Modifier.fillMaxWidth().padding(vertical = Space.md),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("القبلة", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${app.miqat.core.NumFmt.decimal(bearing, 1, fmt.ar)}° · " +
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
            .background(
                scheme.surface,
                RoundedCornerShape(Radius.hero),
            )
            .padding(Space.lg),
    ) {
        Canvas(Modifier.fillMaxWidth().height(72.dp)) {
            val w = size.width
            val bottom = size.height - 8f
            val p0 = Offset(w * 0.08f, bottom)
            val p2 = Offset(w * 0.92f, bottom)
            val p1 = Offset(w * 0.5f, 6f)
            fun pointAt(t: Float): Offset {
                val mt = 1 - t
                return Offset(
                    mt * mt * p0.x + 2 * mt * t * p1.x + t * t * p2.x,
                    mt * mt * p0.y + 2 * mt * t * p1.y + t * t * p2.y,
                )
            }
            drawPath(
                Path().apply {
                    moveTo(p0.x, p0.y)
                    quadraticBezierTo(p1.x, p1.y, p2.x, p2.y)
                },
                scheme.outline,
                style = Stroke(6f, cap = StrokeCap.Round),
            )
            // the live sweep: from Fajr to now
            val mins = times.list.map { ((it + 0.5 / 60) % 24.0 + 24.0) % 24.0 * 60 }
            val dayStart = mins[0]
            val dayEnd = mins[5].let { if (it < dayStart) it + 1440 else it }
            val nowMin = nowHour * 60
            val f = (((nowMin - dayStart) / (dayEnd - dayStart)).coerceIn(0.0, 1.0)).toFloat()
            val tEnd = 0.08f + 0.84f * f
            val sweep = Path()
            val steps = 36
            for (i in 0..steps) {
                val t = tEnd * i / steps
                val pt = pointAt(t)
                if (i == 0) sweep.moveTo(pt.x, pt.y) else sweep.lineTo(pt.x, pt.y)
            }
            drawPath(sweep, scheme.tertiary, style = Stroke(6f, cap = StrokeCap.Round))
            listOf(0f, 0.24f, 0.47f, 0.68f, 0.86f, 1f).forEachIndexed { i, f2 ->
                val c = pointAt(f2)
                val isNext = seg != null && seg.next.key == PRAYER_KEY[i] && !seg.nextIsTomorrow
                drawCircle(if (isNext) scheme.tertiary else scheme.onSurfaceVariant.copy(alpha = .55f), 6f, c)
            }
            drawCircle(scheme.primary, 10f, pointAt(tEnd))
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

internal val LocalPrayerTimes.list: List<Double>
    get() = listOf(fajr, sunrise, dhuhr, asr, maghrib, isha)
