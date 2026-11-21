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
 * DUOTONE ICONS — the approved HTML family: one filled body at 14–22%
 * opacity plus one 2dp outline, on one 24-unit grid. Five destinations,
 * five distinct objects, one alphabet. Selection changes COLOUR only.
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
        val w = 2f * s
        val fill = color.copy(alpha = 0.18f)

        when (kind) {
            // HOME — a house with a roof (the ink-filled silhouette behind it)
            Glyph.HORIZON -> {
                val body = Path().apply {
                    moveTo(3f * s, 11f * s)
                    lineTo(12f * s, 3f * s)
                    lineTo(21f * s, 11f * s)
                    lineTo(21f * s, 20f * s)
                    lineTo(3f * s, 20f * s)
                    close()
                }
                drawPath(body, fill)
                drawPath(body, color, style = Stroke(width = w, cap = StrokeCap.Round))
            }

            // TIMES — a clock: the disc, then the two hands
            Glyph.TICKS -> {
                drawCircle(fill, 9f * s, p(12f, 12f))
                drawCircle(color, 9f * s, p(12f, 12f), style = Stroke(width = w))
                drawLine(color, p(12f, 7f), p(12f, 12f), w, StrokeCap.Round)
                drawLine(color, p(12f, 12f), p(15f, 14f), w, StrokeCap.Round)
            }

            // FASTING — a disc with the fast's rule through it
            Glyph.BRACKET -> {
                drawCircle(color, 9f * s, p(12f, 12f), style = Stroke(width = w))
                drawLine(color, p(12f, 4f), p(12f, 20f), w, StrokeCap.Round)
            }

            // CALENDAR — the page, its header rule, and two binding rings
            Glyph.CELL -> {
                val page = androidx.compose.ui.geometry.Rect(4f * s, 5f * s, 20f * s, 20f * s)
                drawRoundRect(fill, page.topLeft, page.size,
                    androidx.compose.ui.geometry.CornerRadius(3f * s, 3f * s))
                drawRoundRect(color, page.topLeft, page.size,
                    androidx.compose.ui.geometry.CornerRadius(3f * s, 3f * s),
                    style = Stroke(width = w))
                drawLine(color, p(4f, 10f), p(20f, 10f), w)
                listOf(9f, 15f).forEach { x ->
                    drawLine(color, p(x, 3f), p(x, 7f), w, StrokeCap.Round)
                }
            }

            // ACTS — the star/flower: one filled body, one outline
            Glyph.SPAN -> {
                val star = Path().apply {
                    moveTo(12f * s, 3f * s)
                    quadraticTo(14f * s, 8f * s, 14f * s, 11f * s)
                    quadraticTo(16f * s, 10f * s, 16f * s, 12f * s)
                    quadraticTo(16f * s, 15f * s, 14f * s, 17f * s)
                    quadraticTo(14f * s, 20f * s, 12f * s, 21f * s)
                    quadraticTo(10f * s, 20f * s, 10f * s, 17f * s)
                    quadraticTo(8f * s, 15f * s, 8f * s, 12f * s)
                    quadraticTo(8f * s, 10f * s, 10f * s, 11f * s)
                    quadraticTo(10f * s, 8f * s, 12f * s, 3f * s)
                    close()
                }
                drawPath(star, fill)
                drawPath(star, color, style = Stroke(width = 1.8f * s))
            }

            // CONTEXTUAL TOOLS — four squares, as in the HTML header
            Glyph.GRID -> {
                listOf(4f, 14f).forEach { x ->
                    listOf(4f, 14f).forEach { y ->
                        val r = androidx.compose.ui.geometry.Rect(x * s, y * s, (x + 6) * s, (y + 6) * s)
                        drawRoundRect(color, r.topLeft, r.size,
                            androidx.compose.ui.geometry.CornerRadius(1.5f * s, 1.5f * s),
                            style = Stroke(width = w))
                    }
                }
            }
        }
    }
}
