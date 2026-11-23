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

@Composable
internal fun ShelfCard(
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
internal fun HeroCard(
    state: AppState,
    nowState: androidx.compose.runtime.State<Long>,
    fmt: Fmt,
    times: app.miqat.core.LocalPrayerTimes,
    tomorrowFajr: Double,
    nowHour: Double,
    nowMinutes: Int,
    upcoming: List<Moment>,
) {
    val scheme = MaterialTheme.colorScheme
    // ONLY this leaf recomposes on the 1 Hz tick; the rest of Home is minute-
    // granular. The timers use the live second, so the countdown stays true.
    val now = nowState.value
    val liveHour = state.cityHourOfDay(now)
    val timers = app.miqat.core.Timers.of(
        app.miqat.core.prayerPoints(times, tomorrowFajr, PRAYER_NAMES),
        liveHour,
        state.notifications.iqamaMinutes,
        iqamaFor = { state.iqamaMinutesFor(it) },
    )
    // The hero is the app's one rich moment. In the dark scheme it is the
    // hour's real sky; in the paper scheme it is the same hour ENGRAVED —
    // cream bands and deep-gold marks, not a dark postcard on a light page.
    val light = isLightTheme()
    val skyPhase = Sky.phaseOf(liveHour, times.sunrise, times.maghrib)
    val skyBands = if (light) Sky.lightBands(skyPhase) else Sky.bands(skyPhase)
    val ground = if (light) PaperGround else Color(0xFF06070D)
    val inkMain = if (light) InkText else OnDark
    val inkDim = if (light) InkTextDim else OnDarkDim
    val inkFaint = if (light) PaperFaint else OnDarkFaint
    val warmInk = if (light) PaperWarm else OnDarkWarm
    val track = if (light) PaperTrack else OnDarkTrack
    val calmInk = if (light) PaperGold else Gold
    val soonInk = if (light) PaperSaffron else UrgencySoon
    val imminentInk = if (light) PaperEmber else UrgencyImminent
    val tealInk = if (light) PaperTeal else Teal
    val horizonInk = if (light) PaperGold else Gold
    val horizonWarm = if (light) PaperEmber else Sunset

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
                        skyBands, startY = 0f, endY = h,
                    ),
                    size = androidx.compose.ui.geometry.Size(w, h),
                )
                // the ember lies on the horizon (light comes from the sun)
                val ember = if (light) PaperEmber else Sunset
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(ember.copy(alpha = if (light) 0.10f else 0.20f),
                            ember.copy(alpha = 0f)),
                        center = Offset(w * (1f - progress), h),
                        radius = h * 0.75f,
                    ),
                    radius = h * 0.75f,
                    center = Offset(w * (1f - progress), h),
                )
                // the stars are small, quiet, and only in the dark upper sky:
                // a paper sky does not pretend to be a night photograph.
                if (!light) {
                    listOf(0.12f to 0.12f, 0.27f to 0.22f, 0.44f to 0.10f, 0.61f to 0.18f,
                        0.78f to 0.09f, 0.90f to 0.24f, 0.35f to 0.31f).forEachIndexed { i, (fx, fy) ->
                        drawCircle(
                            Color(0xFFEBE8E0).copy(alpha = 0.20f + (i % 3) * 0.08f),
                            1.4f, Offset(w * fx, h * fy),
                        )
                    }
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
                if (light) {
                    // a printed sun: a deep-gold limb, a saffron core, and a
                    // quiet halo — no photograph glow on paper.
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(PaperSaffron.copy(alpha = 0.16f * a),
                                PaperSaffron.copy(alpha = 0f)),
                            center = Offset(sx, sy2), radius = 78f,
                        ),
                        radius = 78f, center = Offset(sx, sy2),
                    )
                    drawCircle(PaperWarm.copy(alpha = a), 21f, Offset(sx, sy2))
                    drawCircle(PaperSunCore.copy(alpha = a), 15f, Offset(sx, sy2))
                    drawCircle(PaperSunHighlight.copy(alpha = 0.70f * a), 6f,
                        Offset(sx - 3f, sy2 - 3f))
                } else {
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
                    color = inkDim,
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
                            color = tealInk,
                        )
                        Text(
                            fmt.hms(timers.iqamaSeconds),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = Cairo, fontSize = 52.sp,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = TabularFigures,
                            ),
                            color = tealInk,
                        )
                        Text(
                            "منذ أذان " + (timers.currentPrayer ?: "الصلاة") + " " +
                                fmt.hms(timers.sinceSeconds),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Plex),
                            color = inkMain,
                        )
                        // the ADHAN→IQAMA window, filled by what has passed
                        val whole = (timers.sinceSeconds + timers.iqamaSeconds).coerceAtLeast(1L)
                        Box(
                            Modifier.fillMaxWidth().padding(top = Space.sm)
                                .height(4.dp).clip(RoundedCornerShape(2.dp))
                                .background(track),
                        ) {
                            Box(
                                Modifier.fillMaxWidth(timers.sinceSeconds.toFloat() / whole)
                                    .height(4.dp).clip(RoundedCornerShape(2.dp))
                                    .background(tealInk),
                            )
                        }
                        Text(
                            "الموعد التالي " + (timers.nextPrayer ?: "—") + " · " +
                                fmt.duration(timers.nextIn.coerceAtLeast(0)),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                            color = inkDim,
                        )
                    }

                    app.miqat.core.IqamaPhase.SINCE -> {
                        Text(
                            "منذ إقامة " + (timers.sinceIqamaPrayer ?: "الصلاة"),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                            color = warmInk,
                        )
                        Text(
                            fmt.hms(timers.sinceIqamaSeconds),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = Cairo, fontSize = 52.sp,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = TabularFigures,
                            ),
                            color = warmInk,
                        )
                        Text(
                            "داخل وقت " + (timers.currentPrayer ?: "الصلاة"),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Plex),
                            color = inkMain,
                        )
                        Text(
                            "الموعد التالي " + (timers.nextPrayer ?: "—") + " · " +
                                fmt.duration(timers.nextIn.coerceAtLeast(0)),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                            color = inkDim,
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
                            app.miqat.core.Urgency.CALM -> calmInk
                            app.miqat.core.Urgency.SOON -> soonInk
                            else -> imminentInk
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
                            color = inkMain,
                        )
                    }
                }
            }
        }

        // ── THE HORIZON ──────────────────────────────────────────────
        Box(
            Modifier.fillMaxWidth().height(1.dp).background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, horizonInk.copy(alpha = .35f),
                        horizonWarm.copy(alpha = .7f), horizonInk.copy(alpha = .35f), Color.Transparent),
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
                            color = if (next) imminentInk else if (passed)
                                inkFaint else inkDim,
                        )
                        Text(
                            fmt.hhmmShort(minute),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = Kufi, fontSize = 12.sp,
                                fontFeatureSettings = TabularFigures,
                            ),
                            color = if (next) imminentInk else if (passed)
                                inkFaint else inkMain,
                        )
                        if (next) {
                            Spacer(Modifier.height(3.dp))
                            Box(
                                Modifier.size(4.dp).clip(CircleShape)
                                    .background(imminentInk),
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
                    .background(track),
            ) {
                Box(
                    Modifier.fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(listOf(horizonInk, horizonWarm)),
                        ),
                )
            }
            Spacer(Modifier.height(Space.md))
            // «بعد ذلك» — kept, but as a quiet line of the day's next facts
            if (upcoming.isNotEmpty()) {
                Text(
                    "بعد ذلك",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                    color = inkDim,
                    modifier = Modifier.padding(horizontal = Space.md),
                )
                upcoming.take(2).forEach { m ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = Space.md, vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(m.labelAr, style = MaterialTheme.typography.bodySmall.copy(fontFamily = Plex),
                            color = inkMain)
                        Text(fmt.hhmmShort(m.minuteOfDay), style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = Kufi, fontFeatureSettings = TabularFigures,
                        ), color = inkDim)
                    }
                }
            }
            Spacer(Modifier.height(Space.md))
        }
    }
}

internal fun travelledFraction(times: app.miqat.core.LocalPrayerTimes, nowHour: Double): Float {
    val fajr = minutesOf(times.fajr).toDouble()
    val isha = minutesOf(times.isha).toDouble().let { if (it < fajr) it + 1440.0 else it }
    val now = nowHour * 60.0
    val m = if (now < fajr) now + 1440.0 else now
    return (((m - fajr) / (isha - fajr).coerceAtLeast(1.0)).coerceIn(0.0, 1.0)).toFloat()
}
