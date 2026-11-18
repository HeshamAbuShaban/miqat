package app.miqat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miqat.core.RailEntry
import app.miqat.core.RailState

/**
 * THE DAY-LINE — the day's appointments as one readable line.
 *
 * The founder: «the line of all prayers times is a good idea but poor in its
 * current state, not well informative». The council's hierarchy: NOW, then
 * the NEXT prayer, then the NAMES, then the clocks — and the DISTANCE
 * between ticks IS the gap (no extra numerals).
 *
 * The line spans Fajr→Isha (the appointments) with a small night margin at
 * each end, so at 03:00 the now-mark sits on the NIGHT side instead of being
 * clamped onto Fajr — truth without spending half the width on night.
 *
 * Next is SAFFRON (the clock), as in the hero, the widget and DESIGN.md.
 * The gold stays the active destination only.
 */
@Composable
fun DayRail(
    entries: List<RailEntry>,
    fmt: Fmt,
    nowMinute: Double,
    onOpenPrayer: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val labelW = 46.dp
    val fajr = entries.firstOrNull { it.isPrayer }?.minute?.toDouble() ?: 0.0
    val isha = entries.lastOrNull { it.isPrayer }?.minute?.toDouble() ?: 1440.0
    val span = (isha - fajr).coerceAtLeast(1.0)

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val railW = maxWidth.value
        val margin = railW * 0.10f
        val inner = railW - 2 * margin
        fun xOf(minute: Double): Float {
            val p = ((minute - fajr) / span).coerceIn(0.0, 1.0).toFloat()
            return margin + inner * (1f - p) // RTL: Fajr at the right
        }
        fun nowX(): Float = when {
            nowMinute < fajr -> railW - 5f           // the night before Fajr
            nowMinute > isha -> 5f                   // the night after Isha
            else -> xOf(nowMinute)
        }

        Column(Modifier.fillMaxWidth()) {
            // A — names + clocks for 0,2,4
            RailLabels(entries, fmt, railW, labelW, ::xOf, upper = true)
            // the line
            Canvas(Modifier.fillMaxWidth().height(24.dp)) {
                val w = size.width
                val lineY = 17f
                val m = w * 0.10f
                // the night margins are dimmer segments of the SAME line
                drawLine(scheme.outlineVariant, Offset(0f, lineY), Offset(m, lineY), 1f)
                drawLine(scheme.outlineVariant, Offset(w - m, lineY), Offset(w, lineY), 1f)
                drawLine(scheme.outline, Offset(m, lineY), Offset(w - m, lineY), 1f)

                entries.forEach { e ->
                    val x = xOf(e.minute.toDouble())
                    when {
                        e.isSunrise -> drawCircle(Sky.sunMark, 2.5f, Offset(x, lineY))
                        e.state == RailState.NEXT -> {
                            drawLine(Sky.sunMark, Offset(x, lineY - 11f), Offset(x, lineY), 3f)
                            drawCircle(Sky.sunMark, 3.5f, Offset(x, lineY - 11f))
                        }
                        e.state == RailState.CURRENT -> {
                            drawLine(scheme.primary, Offset(x, lineY - 9f), Offset(x, lineY), 2.5f)
                            drawCircle(scheme.primary, 3f, Offset(x, lineY - 9f))
                        }
                        else -> drawLine(
                            if (e.state == RailState.PASSED) scheme.outline else scheme.outlineVariant,
                            Offset(x, lineY - 5f), Offset(x, lineY), 1.5f,
                        )
                    }
                }
                // NOW — a playhead crossing the line, not a shy dot
                val nx = nowX()
                drawLine(Sky.sunMark, Offset(nx, 4f), Offset(nx, 21f), 2f)
            }
            // B — names + clocks for 1,3,5
            RailLabels(entries, fmt, railW, labelW, ::xOf, upper = false)

            // honest touch targets: a tick opens its OWN detail (a real
            // action, not a decorative button), 48dp tall, no ripple
            entries.forEach { e ->
                val x = (xOf(e.minute.toDouble()) - 24f).coerceIn(0f, railW - 48f)
                Box(
                    Modifier
                        .offset(x = x.dp, y = (-24).dp)
                        .width(48.dp)
                        .height(48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onOpenPrayer(e.index) }
                        .semantics { contentDescription = "تفاصيل ${e.name}" },
                )
            }
        }
    }
}

@Composable
private fun RailLabels(
    entries: List<RailEntry>,
    fmt: Fmt,
    railWidth: Float,
    labelWidth: Dp,
    xOf: (Double) -> Float,
    upper: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().height(34.dp)) {
        entries.filter { (it.index % 2 == 0) == upper }.forEach { e ->
            val raw = xOf(e.minute.toDouble()) - labelWidth.value / 2f
            val x = raw.coerceIn(0f, railWidth - labelWidth.value)
            val align = when {
                raw <= 0f -> TextAlign.End      // an edge label hangs inward
                raw >= railWidth - labelWidth.value -> TextAlign.Start
                else -> TextAlign.Center
            }
            Column(
                Modifier.offset(x = x.dp).width(labelWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    e.name,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    textAlign = align,
                    color = when {
                        e.state == RailState.NEXT -> scheme.onSurface
                        e.isSunrise -> Sky.sunMark
                        e.state == RailState.PASSED -> scheme.onSurfaceVariant.copy(alpha = 0.65f)
                        else -> scheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
                Text(
                    fmt.hhmmShort(e.minute),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontFeatureSettings = TabularFigures,
                    ),
                    textAlign = align,
                    color = if (e.state == RailState.NEXT) Sky.sunMark else scheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}
