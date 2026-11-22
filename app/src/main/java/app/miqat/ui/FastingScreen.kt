package app.miqat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
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
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FastingScreen(
    state: AppState,
    fmt: Fmt,
    nowState: androidx.compose.runtime.State<Long>,
) {
    val scheme = MaterialTheme.colorScheme
    var fastsOpen by androidx.compose.runtime.remember { mutableStateOf(false) }
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
                Hijri.label(Hijri.at(y, m, d, state.display.hijriOffsetDays), fmt.ar),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Space.lg))

        // THE FAST BAR — the approved hero: a measured span, not a drawing
        // of a day. The arc is gone; the Windows and their cited texts stay.
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.panel))) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0A0D18), Color(0xFF0E1424), Color(0xFF121E22),
                            ),
                        ),
                    )
                    .padding(horizontal = Space.lg, vertical = Space.lg),
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "صيام اليوم",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = Kufi, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        ),
                        color = OnDark,
                    )
                    Spacer(Modifier.height(Space.md))
                    Text(
                        if (iftarPassed) "انتهى صيام اليوم" else "الإفطار بعد",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                        color = OnDarkDim,
                    )
                    if (iftarPassed) {
                        Text(
                            app.miqat.core.Proximity.timerLine(
                                app.miqat.core.TimerKind.ELAPSED,
                                (nowMinute - iftar).coerceAtLeast(0) * 60L, "الإفطار",
                            ).prefixAr + " " + fmt.hms((nowMinute - iftar).coerceAtLeast(0) * 60L),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = Kufi),
                            color = OnDark,
                        )
                    } else {
                        BigCountdown(
                            text = fmt.hms(remainingMinutes * 60L),
                            color = Color(0xFFF0C875),
                            sizeSp = 48f,
                        )
                    }
                    if (imsakPassed && !iftarPassed) {
                        Text(
                            app.miqat.core.Proximity.timerLine(
                                app.miqat.core.TimerKind.ELAPSED,
                                elapsedMinutes * 60L, "الإمساك",
                            ).prefixAr + " " + fmt.hms(elapsedMinutes * 60L),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                            color = OnDarkDim,
                        )
                    }
                }
            }
            // the bar itself — track, elapsed fill, three real ticks, two ends
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = Space.md),
            ) {
                val w = size.width
                val h = size.height
                val dp = 1.dp.toPx()
                val y = h / 2f
                val trackH = 24f * dp
                val radius = androidx.compose.ui.geometry.CornerRadius(trackH / 2f, trackH / 2f)
                drawRoundRect(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF132430), Color(0xFF20181A)),
                        startX = w, endX = 0f,
                    ),
                    topLeft = Offset(0f, y - trackH / 2f),
                    size = androidx.compose.ui.geometry.Size(w, trackH),
                    cornerRadius = radius,
                )
                if (progress > 0f) {
                    drawRoundRect(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF3E9C90), Color(0xFF5CC4B8), Color(0xFFC8F0E8)),
                            startX = w, endX = w * (1f - progress),
                        ),
                        topLeft = Offset(w * (1f - progress.coerceIn(0f, 1f)), y - trackH / 2f),
                        size = androidx.compose.ui.geometry.Size(w * progress.coerceIn(0f, 1f), trackH),
                        cornerRadius = radius,
                    )
                }
                listOf(0f, 0.5f, 1f).forEach { t ->
                    drawCircle(Color(0xFF3A9B8E), 1.8f * dp,
                        Offset(w * (1f - t), y))
                }
                drawCircle(Color(0xFFC8F0E8), 2.5f * dp, Offset(w, y)) // imsak
                drawCircle(Color(0xFFF06544), 3.5f * dp, Offset(0f, y)) // iftar
                drawCircle(Color(0xFFF06544).copy(alpha = 0.45f), 7f * dp,
                    Offset(0f, y), style = Stroke(width = 0.8f * dp))
                val nx = w * (1f - progress.coerceIn(0f, 1f))
                drawLine(Color(0xFFFF8566), Offset(nx, y - 20f * dp),
                    Offset(nx, y + 20f * dp), 6f * dp, StrokeCap.Round)
                drawCircle(Color(0xFFFFE9B5), 3.2f * dp, Offset(nx, y))
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Space.md),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "نافذة الصيام " + fmt.range(imsak, iftar),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    "الإفطار " + fmt.hhmm(iftar),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = Kufi),
                    color = if (isLightTheme()) Color(0xFFB3261E) else Color(0xFFFF8566),
                )
            }
            Spacer(Modifier.height(Space.md))
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

        // the six narrated days live in a SHEET, not on the main screen —
        // the founder: the fasting screen is stacked like its trimmed peer
        NavRow(
            "أيام الصيام التطوعي",
            "ستة أيام مذكورة بنصوصها — اضغط للقائمة",
            onClick = { fastsOpen = true },
        )
        Divider(Modifier.padding(horizontal = Space.lg))
        Spacer(Modifier.height(Space.md))

        Spacer(Modifier.height(Space.xxl))

    if (fastsOpen) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { fastsOpen = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.lg),
            ) {
                Text(
                    "أيام الصيام التطوعي",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "ما يعرضه التطبيق قائمةُ أيام الصيام المعروفة، لا فتوى",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Space.sm))
                FastingRow(
                    "الأيام البيض ١٣–١٥",
                    "من كل شهر هجري",
                    "﴿أَمَرَنَا رَسُولُ اللَّهِ ﷺ أَنْ نَصُومَ مِنَ الشَّهْرِ ثَلاَثَةَ أَيَّامِ الْبِيضِ﴾ — سنن النسائي ٢٤٢٢",
                    days = 3,
                )
                FastingRow(
                    "الاثنين والخميس",
                    "كل أسبوع",
                    "﴿كَانَ النَّبِيُّ ﷺ يَتَحَرَّى صَوْمَ الِاثْنَيْنِ وَالْخَمِيسِ﴾ — سنن الترمذي ٧٤٥",
                    days = 2,
                )
                FastingRow(
                    "يوم عرفة",
                    "٩ ذي الحجة",
                    "﴿صِيَامُ يَوْمِ عَرَفَةَ أَحْتَسِبُ عَلَى اللَّهِ أَنْ يُكَفِّرَ السَّنَةَ الَّتِي قَبْلَهُ وَالسَّنَةَ الَّتِي بَعْدَهُ﴾ — صحيح مسلم ١١٦٢",
                    days = 1, rite = true,
                )
                FastingRow(
                    "عاشوراء",
                    "١٠ المحرم",
                    "﴿هَذَا يَوْمُ عَاشُورَاءَ، وَلَمْ يُكْتَبْ عَلَيْكُمْ صِيَامُهُ، وَأَنَا صَائِمٌ﴾ — صحيح البخاري ٢٠٠٣",
                    days = 1, rite = true,
                )
                FastingRow(
                    "ستّ من شوال",
                    "٦ شوال",
                    "﴿مَنْ صَامَ رَمَضَانَ ثُمَّ أَتْبَعَهُ سِتًّا مِنْ شَوَّالٍ كَانَ كَصِيَامِ الدَّهْرِ﴾ — صحيح مسلم ١١٦٤",
                    days = 6,
                )
                FastingRow(
                    "أيام التشريق",
                    "١١–١٣ ذي الحجة",
                    "﴿أَيَّامُ التَّشْرِيقِ أَيَّامُ أَكْلٍ وَشُرْبٍ﴾ — صحيح مسلم ١١٤١",
                    days = 3, rite = true,
                )
                Spacer(Modifier.height(Space.xxl))
            }
        }
    }
}
}

/** One line in the night card. */

@Composable
private fun FastingRow(
    title: String, whenText: String, source: String,
    days: Int, rite: Boolean = false,
) {
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
        // the NUMBER in the narration becomes countable cells — no meter,
        // no "2 of 6" score (we log acts, we never verify them)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(days) {
                Box(
                    Modifier
                        .width(4.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (rite) scheme.onTertiaryContainer else scheme.secondary,
                        ),
                )
            }
        }
        Text(
            source,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.outline,
        )
    }
}