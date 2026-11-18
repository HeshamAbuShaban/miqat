package app.miqat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miqat.core.Hijri
import app.miqat.core.IslamicDay
import app.miqat.core.IslamicDays
import app.miqat.core.NotificationPoster
import app.miqat.core.WorshipWindows
import java.util.Calendar

/**
 * الصيام — THE FASTING, a first-class act of worship.
 *
 * The app had the imsak and the iftar as two moments on a timeline and
 * nothing else; a Muslim's fasting day had no home. This screen gives it one:
 * the fast's own arc, its own countdown, the four anchors (the imsak, the
 * Fajr, the iftar, the next Fajr), the night, and the days of voluntary
 * fasting.
 *
 * THE DISCIPLINE (the models council, 2026-11-11):
 *  - the app states TIMES and quotes NARRATIONS with their numbers;
 *  - it never issues a ruling (no «مستحبّ», no «لا صيام اليوم»);
 *  - the imsak is OUR convention (a lead before the Fajr), so it carries
 *    «تقديري بحسب الاصطلاح» — the iftar does not, because it is the sunset.
 */
@Composable
fun FastingScreen(
    state: AppState,
    fmt: Fmt,
    nowState: androidx.compose.runtime.State<Long>,
) {
    val scheme = MaterialTheme.colorScheme
    val now = nowState.value
    val (y, m, d) = state.cityDate(now)
    val times = state.times(y, m, d)
    val (ty, tm, td) = state.cityTomorrow(now)
    val tomorrow = state.times(ty, tm, td)
    val nowMinute = (state.cityHourOfDay(now) * 60).toInt()

    // the four anchors of the fast
    val fajr = minutesOf(times.fajr)
    val iftar = minutesOf(times.maghrib)
    val imsak = fajr - state.imsakLeadMinutes
    val nextFajr = minutesOf(tomorrow.fajr)
    val imsakPassed = nowMinute >= imsak
    val iftarPassed = nowMinute >= iftar
    // PROXIMITY (the founder, 2026-11-18): the fast is TODAY's fast.
    // Tomorrow's Fajr is not a fact of this screen — showing it all night was
    // "شيء غير ذكي". After the iftar the honest fact is the time since it.
    val remainingMinutes = (iftar - nowMinute).coerceAtLeast(0)
    val countdownLabel = if (iftarPassed) "انتهى صيام اليوم" else "بقي على الإفطار"
    // (the proximity gate that keeps tomorrow's Fajr off this screen is
    // pinned by ProximityTest — a UI crash-check has no place here)
    // the fast's own progress (0 = the imsak, 1 = the iftar)
    val span = (iftar - imsak).coerceAtLeast(1)
    val progress = ((nowMinute - imsak).toFloat() / span).coerceIn(0f, 1f)
    val elapsedMinutes = (nowMinute - imsak).coerceAtLeast(0)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg),
    ) {
        Spacer(Modifier.height(Space.md))

        // the title + the day's Hijri date
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "الصيام",
                style = MaterialTheme.typography.headlineSmall,
                color = scheme.onBackground,
            )
            Text(
                Hijri.label(Hijri.at(y, m, d), fmt.ar),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Space.lg))

        // THE FAST'S ARC — the same shape as the hero, but the span is the
        // fast, not the day: from the imsak to the iftar.
        Column(
            Modifier
                .fillMaxWidth()
                .cardSurface(RoundedCornerShape(Radius.panel))
                .padding(Space.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            ) {
                val w = size.width
                val h = size.height
                val horizon = h - 14f
                // RTL: the imsak at the right, the iftar at the left
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

                val arc = Path().apply {
                    moveTo(p0.x, p0.y)
                    quadraticBezierTo(p1.x, p1.y, p2.x, p2.y)
                }
                // the window arc is TEAL — the palette's meaning for a span
                // (it used to borrow the sky's steel blue: a cold line on a
                // warm panel, and a colour that meant nothing)
                drawPath(
                    arc,
                    scheme.secondary.copy(alpha = if (iftarPassed) 0.25f else 0.55f),
                    style = Stroke(width = 2f, cap = StrokeCap.Round),
                )

                // the fast already travelled
                if (progress > 0.001f) {
                    val travelled = Path()
                    val steps = 40
                    for (i in 0..steps) {
                        val t = progress * i / steps
                        val pt = pointAt(t)
                        if (i == 0) travelled.moveTo(pt.x, pt.y) else travelled.lineTo(pt.x, pt.y)
                    }
                    drawPath(
                        travelled,
                        Brush.horizontalGradient(
                            listOf(Color(0xFFE8C46A), Color(0xFFFFB25C)),
                            startX = p0.x, endX = p2.x,
                        ),
                        style = Stroke(width = 5f, cap = StrokeCap.Round),
                    )
                }

                // The Fajr tick — the moment the fast becomes obligatory,
                // drawn INSIDE the window it belongs to (council P2.2).
                val fastT = ((fajr - imsak).toFloat() / span).coerceIn(0f, 1f)
                val fajrPoint = pointAt(fastT)
                fun normalAt(t: Float): androidx.compose.ui.geometry.Offset {
                    val tx = 2 * (1 - t) * (p1.x - p0.x) + 2 * t * (p2.x - p1.x)
                    val ty = 2 * (1 - t) * (p1.y - p0.y) + 2 * t * (p2.y - p1.y)
                    val len = kotlin.math.hypot(tx, ty).coerceAtLeast(0.001f)
                    var n = androidx.compose.ui.geometry.Offset(-ty / len, tx / len)
                    if (n.y > 0f) n = androidx.compose.ui.geometry.Offset(-n.x, -n.y)
                    return n
                }
                val fn = normalAt(fastT)
                drawLine(
                    Color(0xFFFFB25C),
                    fajrPoint + androidx.compose.ui.geometry.Offset(fn.x * 4f, fn.y * 4f),
                    fajrPoint - androidx.compose.ui.geometry.Offset(fn.x * 4f, fn.y * 4f),
                    strokeWidth = 2f,
                )

                // THE PLAYHEAD — alive only while the fast is actually open.
                // The old dot sat clamped on an endpoint for most of the day:
                // that is the founder's «الخط غير حيوي ولا يشير لشيء».
                when {
                    progress <= 0f -> {
                        drawCircle(
                            Color(0xFFFFB25C), 5f, pointAt(0f),
                            style = Stroke(width = 2f),
                        )
                    }
                    progress >= 1f -> {
                        // the window has closed: the marker has LEFT the arc
                        drawCircle(Color(0xFFFFB25C), 3.5f, Offset(p2.x, horizon))
                    }
                    else -> {
                        val here = pointAt(progress)
                        val n = normalAt(progress)
                        drawLine(
                            Color(0xFFFFB25C),
                            here + androidx.compose.ui.geometry.Offset(n.x * 3f, n.y * 3f),
                            here + androidx.compose.ui.geometry.Offset(n.x * 9f, n.y * 9f),
                            strokeWidth = 2.5f,
                        )
                        drawCircle(Color(0xFFFFE0A8), 3.5f, here)
                    }
                }
                // sunset is the clock's moment — saffron ring, not a gold dot
                drawCircle(
                    Color(0xFFFFB25C), 5f, pointAt(1f),
                    style = Stroke(width = 2f),
                )
                drawCircle(scheme.secondary.copy(alpha = 0.8f), 4f, pointAt(0f))
            }

            Spacer(Modifier.height(Space.sm))

            Text(
                countdownLabel,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
            if (iftarPassed) {
                Text(
                    app.miqat.core.Proximity.timerLine(
                        app.miqat.core.TimerKind.ELAPSED,
                        (nowMinute - iftar).coerceAtLeast(0) * 60L, "الإفطار",
                    ).prefixAr + " " + fmt.hms((nowMinute - iftar).coerceAtLeast(0) * 60L),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                )
            } else {
                BigCountdown(
                    text = fmt.hms(remainingMinutes * 60L),
                    color = scheme.onSurface,
                )
            }
            if (imsakPassed && !iftarPassed) {
                Text(
                    app.miqat.core.Proximity.timerLine(
                        app.miqat.core.TimerKind.ELAPSED, elapsedMinutes * 60L, "الإمساك",
                    ).prefixAr + " " + fmt.hms(elapsedMinutes * 60L),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Space.md))

        // the four anchors — a 2×2 grid, not four rows: this is what stops
        // the screen from being a list
        val anchors = mutableListOf(
            Triple("الإمساك", fmt.hhmm(imsak), if (imsakPassed) "مضى" else "بقي ${fmt.n(imsak - nowMinute)} د"),
            Triple("الفجر", fmt.hhmm(fajr), if (nowMinute >= fajr) "مضى" else "بقي ${fmt.n(fajr - nowMinute)} د"),
            Triple("الإفطار", fmt.hhmm(iftar), if (iftarPassed) "مضى" else "بقي ${fmt.n(remainingMinutes)} د"),
        )
        Column(Modifier.fillMaxWidth()) {
            anchors.chunked(2).forEach { pair ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    pair.forEach { (label, time, meta) ->
                        Column(
                            Modifier
                                .weight(1f)
                                .cardSurface(RoundedCornerShape(Radius.card))
                                .padding(Space.md),
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant,
                            )
                            Text(
                                time,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFeatureSettings = TabularFigures,
                                ),
                                color = scheme.onSurface,
                            )
                            Text(
                                meta,
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Space.sm))
            }
        }

        // the imsak's honesty: our lead, not a ruling
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.row))
                .background(scheme.surfaceVariant.copy(alpha = .5f))
                .padding(Space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .background(scheme.tertiary, CircleShape),
            )
            Spacer(Modifier.width(Space.sm))
            Text(
                "الإمساك ${fmt.n(state.imsakLeadMinutes)} دقيقة قبل الفجر — ${NotificationPoster.ESTIMATED}",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Space.md))

        // أيام الصيام — the voluntary fasts, with the texts they rest on
        Column(
            Modifier
                .fillMaxWidth()
                .cardSurface(RoundedCornerShape(Radius.panel))
                .padding(Space.lg),
        ) {
            Text(
                "أيام الصيام التطوعي",
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                "ما يعرضه التطبيق قائمةُ أيام الصيام المعروفة، لا فتوى",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.sm))
            FastingRow(
                "الأيام البيض ١٣–١٥",
                "من كل شهر هجري",
                "﴿أَمَرَنَا رَسُولُ اللَّهِ ﷺ أَنْ نَصُومَ مِنَ الشَّهْرِ ثَلاَثَةَ أَيَّامِ الْبِيضِ﴾ — سنن النسائي ٢٤٢٢",
            )
            FastingRow(
                "الاثنين والخميس",
                "كل أسبوع",
                "﴿كَانَ النَّبِيُّ ﷺ يَتَحَرَّى صَوْمَ الِاثْنَيْنِ وَالْخَمِيسِ﴾ — سنن الترمذي ٧٤٥",
            )
            FastingRow(
                "يوم عرفة",
                "٩ ذي الحجة",
                "﴿صِيَامُ يَوْمِ عَرَفَةَ أَحْتَسِبُ عَلَى اللَّهِ أَنْ يُكَفِّرَ السَّنَةَ الَّتِي قَبْلَهُ وَالسَّنَةَ الَّتِي بَعْدَهُ﴾ — صحيح مسلم ١١٦٢",
            )
            FastingRow(
                "عاشوراء",
                "١٠ المحرم",
                "﴿هَذَا يَوْمُ عَاشُورَاءَ، وَلَمْ يُكْتَبْ عَلَيْكُمْ صِيَامُهُ، وَأَنَا صَائِمٌ﴾ — صحيح البخاري ٢٠٠٣",
            )
            FastingRow(
                "ستّ من شوال",
                "٦ شوال",
                "﴿مَنْ صَامَ رَمَضَانَ ثُمَّ أَتْبَعَهُ سِتًّا مِنْ شَوَّالٍ كَانَ كَصِيَامِ الدَّهْرِ﴾ — صحيح مسلم ١١٦٤",
            )
            FastingRow(
                "أيام التشريق",
                "١١–١٣ ذي الحجة",
                "﴿أَيَّامُ التَّشْرِيقِ أَيَّامُ أَكْلٍ وَشُرْبٍ﴾ — صحيح مسلم ١١٤١",
            )
        }

        Spacer(Modifier.height(Space.md))

        // the texts the fast rests on — the anchor and the iftar
        Column(
            Modifier
                .fillMaxWidth()
                .cardSurface(RoundedCornerShape(Radius.panel))
                .padding(Space.lg),
        ) {
            Text(
                "على أيّ نصٍّ يقوم هذا؟",
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
            Spacer(Modifier.height(Space.sm))
            Text(
                "﴿كُلُوا وَاشْرَبُوا حَتَّى يَتَبَيَّنَ لَكُمُ الْخَيْطُ الْأَبْيَضُ مِنَ الْخَيْطِ الْأَسْوَدِ مِنَ الْفَجْرِ﴾ — سورة البقرة ١٨٧",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurface,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                "﴿لَا يَزَالُ النَّاسُ بِخَيْرٍ مَا عَجَّلُوا الْفَطْرَ﴾ — صحيح البخاري ١٩٥٧",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurface,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                "﴿تَسَحَّرْنَا مَعَ النَّبِيِّ ﷺ… قُلْتُ كَمْ كَانَ بَيْنَ الأَذَانِ وَالسَّحُورِ؟ قَالَ قَدْرُ خَمْسِينَ آيَةً﴾ — صحيح البخاري ١٩٢١",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurface,
            )
        }

        Spacer(Modifier.height(Space.xxl))
    }
}

/** One line in the night card. */

@Composable
private fun FastingRow(title: String, whenText: String, source: String) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Space.sm),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
            Text(
                whenText,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
        Text(
            source,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.outline,
        )
    }
}
