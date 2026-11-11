package app.miqat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miqat.core.LocalPrayerTimes

/**
 * THE DAY-RAIL — the day's shape, flattened.
 *
 * The hero's arc shows the day as a curve; the rail shows the same curve as
 * a line: a hairline with the six times standing ON their real positions —
 * so the distance between two ticks IS the gap between two prayers — the
 * sunrise as a saffron mark, and "now" as a small saffron dot.
 *
 * It replaces the six-column text strip (which repeated what the arc already
 * said in a different visual language) and the night-markers line (the
 * sunrise is a tick here).
 *
 * The labels alternate above and below the line — the evening prayers sit
 * close together, and a timeline that collides is a broken timeline.
 */
@Composable
fun DayRail(
    times: LocalPrayerTimes,
    fmt: Fmt,
    nextIndex: Int,
    nowMinute: Double,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val minutes = listOf(
        times.fajr, times.sunrise, times.dhuhr, times.asr, times.maghrib, times.isha,
    ).map { (((it % 24.0) + 24.0) % 24.0 * 60.0).toInt() }

    val fajr = minutes[0].toDouble()
    val isha = minutes[5].toDouble().let { if (it < fajr) it + 1440.0 else it }
    val span = (isha - fajr).coerceAtLeast(1.0)
    fun param(minute: Double): Float {
        var m = minute
        if (m < fajr) m += 1440.0
        return ((m - fajr) / span).coerceIn(0.0, 1.0).toFloat()
    }
    val params = minutes.map { param(it.toDouble()) }
    val nowParam = param(nowMinute)
    // in RTL the reading starts at the right: the Fajr on the right, the Isha left
    fun xOf(p: Float, width: Float): Float = width * (1f - p)

    val labelWidth = 44.dp

    BoxWithConstraints(modifier.fillMaxWidth()) {
    val railWidth = maxWidth.value
    Column(Modifier.fillMaxWidth()) {
        // the labels ABOVE the line
        Box(
            Modifier
                .fillMaxWidth()
                .height(18.dp),
        ) {
            minutes.forEachIndexed { index, minute ->
                if (index % 2 != 0) return@forEachIndexed
                RailLabel(
                    minute = minute,
                    fmt = fmt,
                    x = railWidth * (1f - params[index]) - labelWidth.value / 2f,
                    width = labelWidth,
                    isNext = index == nextIndex,
                )
            }
        }

        // the line, the ticks, the marks
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(26.dp),
        ) {
            val w = size.width
            val lineY = size.height - 7f
            drawLine(
                color = scheme.outline,
                start = Offset(0f, lineY),
                end = Offset(w, lineY),
                strokeWidth = 1f,
            )
            params.forEachIndexed { index, p ->
                val x = xOf(p, w)
                val isNext = index == nextIndex
                val isSun = index == 1
                drawLine(
                    color = when {
                        isNext -> scheme.primary
                        isSun -> Sky.sunMark
                        else -> scheme.outlineVariant
                    },
                    start = Offset(x, lineY - if (isNext) 12f else 6f),
                    end = Offset(x, lineY),
                    strokeWidth = if (isNext) 3f else 1.5f,
                )
                if (isNext) drawCircle(scheme.primary, 4f, Offset(x, lineY - 12f))
            }
            // "now" — a small saffron dot on the line
            drawCircle(Sky.sunMark, 3f, Offset(xOf(nowParam, w), lineY))
        }

        // the labels BELOW the line
        Box(
            Modifier
                .fillMaxWidth()
                .height(18.dp),
        ) {
            minutes.forEachIndexed { index, minute ->
                if (index % 2 == 0) return@forEachIndexed
                RailLabel(
                    minute = minute,
                    fmt = fmt,
                    x = railWidth * (1f - params[index]) - labelWidth.value / 2f,
                    width = labelWidth,
                    isNext = index == nextIndex,
                )
            }
        }
    }

    }
}

/** One time label, sitting on its tick. */
@Composable
private fun RailLabel(
    minute: Int,
    fmt: Fmt,
    x: Float,
    width: androidx.compose.ui.unit.Dp,
    isNext: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = fmt.hhmmShort(minute),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.sp,
            fontFeatureSettings = TabularFigures,
        ),
        color = if (isNext) scheme.onSurface else scheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier
            .offset(x = x.dp)
            .width(width),
    )
}

