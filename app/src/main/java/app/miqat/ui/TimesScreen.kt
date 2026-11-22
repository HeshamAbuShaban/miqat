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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.miqat.core.Moments
import app.miqat.core.Proximity
import app.miqat.core.Surface
import app.miqat.core.TimerKind
import app.miqat.core.Urgency
import app.miqat.core.UrgencyRules
import app.miqat.core.WorshipWindows

/** Minutes-of-day for a fractional hour (same round-half-up as the donor). */
fun minutesOf(hours: Double): Int = app.miqat.core.Prayers.minutesOf(hours)

/**
 * TIMES — the day's schedule in one place: the six prayers (each with its own
 * dual clock) and the worship windows (each opening ITS OWN screen, not a line
 * in a list). This screen replaces the v1 stack of unrelated cards.
 */
@Composable
fun TimesScreen(
    state: AppState,
    fmt: Fmt,
    nowState: State<Long>,
    onOpenPrayer: (Int) -> Unit,
    onOpenActs: () -> Unit = {},
) {
    val now = nowState.value
    val (y, m, d) = state.cityDate(now)
    val (ty, tm, td) = state.cityTomorrow(now)
    val times = state.times(y, m, d)
    val tomorrow = state.times(ty, tm, td)
    val nowHour = state.cityHourOfDay(now)
    // THE timers — the same model the home uses (one clock for the app)
    val timers = app.miqat.core.Timers.of(
        app.miqat.core.prayerPoints(times, tomorrow.fajr, PRAYER_NAMES),
        nowHour,
        state.notifications.iqamaMinutes,
        iqamaFor = { state.iqamaMinutesFor(it) },
    )

    val night = WorshipWindows.night(maghrib = times.maghrib, fajrNext = tomorrow.fajr)
    val duha = WorshipWindows.duha(sunrise = times.sunrise, dhuhr = times.dhuhr)
    val sahar = WorshipWindows.sahar(maghrib = times.maghrib, fajrNext = tomorrow.fajr)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg),
    ) {
        Spacer(Modifier.height(Space.md))
        Text(
            "مواقيت اليوم",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Space.sm))

        // the bridges are between PRAYERS, never between a prayer and a
        // sunnah marker parked on the same minute (that printed «٠ د»).
        // Each bridge is a SPAN: a number AND a proportional window.
        val nowMinute = (nowHour * 60).toInt()
        val points = times.list.map(::minutesOf) + (minutesOf(tomorrow.fajr) + 1440)
        val labels = PRAYER_NAMES + "الفجر غداً"
        val spans = (0 until 6).map { i ->
            app.miqat.core.Span(points[i], points[i + 1], nowMinute) to
                (labels[i] to labels[i + 1])
        }

        PRAYER_NAMES.forEachIndexed { i, name ->
            // after the Isha the countdown BELONGS to tomorrow's Fajr — the
            // night must not go silent, and the row says «غداً»
            val isNext = timers.nextPrayer == name
            val isCurrent = timers.currentPrayer == name
            val urgency = if (state.display.urgencyEnabled && isNext) {
                UrgencyRules.of(
                    timers.remainingSeconds,
                    state.display.urgencySoonMinutes,
                    state.display.urgencyImminentMinutes,
                )
            } else Urgency.CALM
            PrayerRow(
                name = name,
                time = fmt.hhmm(minutesOf(times.list[i])) +
                    if (isNext && timers.nextIsTomorrow) " · غداً" else "",
                clock = when {
                    isNext -> Proximity.timerLine(
                        TimerKind.PRAYER_REMAINING, timers.remainingSeconds, name,
                    ).prefixAr + " " + fmt.hms(timers.remainingSeconds)
                    // the elapsed only exists inside the prayer's own window
                    isCurrent && timers.sinceWorthShowing ->
                        Proximity.timerLine(
                            TimerKind.ELAPSED, timers.sinceSeconds, name,
                        ).prefixAr + " " + fmt.hms(timers.sinceSeconds)
                    else -> null
                },
                highlighted = isNext,
                urgency = urgency,
                onClick = { onOpenPrayer(i) },
            )
            // the window to the NEXT PRAYER — a real span, never a ٠ د marker
            val (span, pair) = spans[i]
            if (state.display.showGaps && span.totalMin > 0 &&
                state.display.gapCategories.contains(app.miqat.core.MomentCategory.PRAYER.labelAr)
            ) {
                GapBridge(span, pair.first, pair.second, fmt)
            }
            Divider()
        }

        // ONE entry to the acts that already own these windows; the day's
        // times are the whole first screen (the founder's nearest-first)
        Spacer(Modifier.height(Space.lg))
        NavRow(
            "مواقيت العبادات",
            "الضحى والأسحار والليل وأوقات السنة — في تبويب الطاعات",
            onOpenActs,
        )
        Spacer(Modifier.height(Space.xxl))
    }
}

@Composable
private fun PrayerRow(
    name: String,
    time: String,
    clock: String?,
    highlighted: Boolean,
    urgency: Urgency = Urgency.CALM,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = Space.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                color = when (urgency) {
                    Urgency.IMMINENT, Urgency.NOW -> UrgencyImminent
                    Urgency.SOON -> UrgencySoon
                    Urgency.CALM -> if (highlighted) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurface
                },
            )
            if (clock != null) {
                Text(
                    clock,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            time,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TabularFigures),
            color = when (urgency) {
                Urgency.IMMINENT, Urgency.NOW -> UrgencyImminent
                Urgency.SOON -> UrgencySoon
                Urgency.CALM -> if (highlighted) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun WorshipRow(
    title: String,
    value: String,
    blurb: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = Space.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // teal = a worship WINDOW (the palette's one rule: a point is gold,
        // a span is teal) — so the window rows never look like the prayer rows
        Box(
            Modifier
                .size(7.dp)
                .background(MaterialTheme.colorScheme.secondary, CircleShape),
        )
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(Space.md))
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
    }
}

/** A worship window gets a FULL SCREEN: what it is, when it is, and its source. */
