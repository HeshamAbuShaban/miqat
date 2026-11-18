package app.miqat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ONE ALPHABET — five marks cut from the same object.
 *
 * The council's ruling: the old glyphs were drawn one at a time, each with
 * its own skeleton, and they MUTATED on selection (1.7dp → 2.4dp + a pill).
 * A navigation set must be one family: one 24-unit box, one baseline
 * y=18, one arc P(3,18)→P(12,4.5)→P(21,18), one fixed 2dp stroke.
 * Selection changes COLOUR only — never the silhouette.
 */
enum class Glyph { HORIZON, TICKS, BRACKET, CELL, SPAN, GRID }

@Composable
fun BrandGlyph(
    kind: Glyph,
    active: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
) {
    val color = if (active) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier.size(size)) {
        val s = this.size.width / 24f
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        val w = 2f * s // the ONE stroke, fixed in both states

        fun arc(): Path = Path().apply {
            moveTo(3f * s, 18f * s)
            quadraticTo(12f * s, 4.5f * s, 21f * s, 18f * s)
        }
        fun base() = drawLine(color, p(3f, 18f), p(21f, 18f), w, StrokeCap.Round)

        when (kind) {
            // the horizon itself — nothing added
            Glyph.HORIZON -> drawPath(arc(), color, style = Stroke(width = w, cap = StrokeCap.Round))

            // three real prayer dots ON the curve (the quadratic, not floated)
            Glyph.TICKS -> {
                drawPath(arc(), color, style = Stroke(width = w, cap = StrokeCap.Round))
                listOf(0.28f, 0.5f, 0.72f).forEach { t ->
                    val mt = 1 - t
                    val x = mt * mt * 3f + 2 * mt * t * 12f + t * t * 21f
                    val y = mt * mt * 18f + 2 * mt * t * 4.5f + t * t * 18f
                    drawCircle(color, 1.5f * s, p(x, y))
                }
            }

            // a fast's window measured on the horizon: two posts
            Glyph.BRACKET -> {
                base()
                listOf(6f, 18f).forEach { x ->
                    drawLine(color, p(x, 18f), p(x, 10f), w, StrokeCap.Round)
                }
            }

            // the calendar page: its bottom edge IS the baseline
            Glyph.CELL -> {
                drawRoundRect(
                    color,
                    topLeft = p(3f, 6f),
                    size = androidx.compose.ui.geometry.Size(18f * s, 12f * s),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * s, 3f * s),
                    style = Stroke(width = w),
                )
                drawLine(color, p(7f, 14f), p(13f, 14f), 3f * s, StrokeCap.Round)
            }

            // the act rail: a window and one now-playhead
            Glyph.SPAN -> {
                base()
                drawLine(color, p(12f, 18f), p(21f, 18f), 3f * s, StrokeCap.Round)
                drawLine(color, p(12f, 10f), p(12f, 18f), w, StrokeCap.Round)
            }

            // the contextual drawer: three gnomons, three shadow lengths
            Glyph.GRID -> {
                base()
                listOf(7f to 13f, 12f to 9f, 17f to 11f).forEach { (x, y) ->
                    drawLine(color, p(x, 18f), p(x, y), w, StrokeCap.Round)
                    drawCircle(color, 1.5f * s, p(x, y))
                }
            }
        }
    }
}
