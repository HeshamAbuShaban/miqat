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
 * THE BRAND GLYPHS — the navigation drawn from the app's own geometry.
 *
 * The four Material icons that shipped first (Home, List, DateRange, Settings)
 * are the most generic in the whole set — and "a list" for a prayer schedule
 * is a tell. These five are drawn from the app's shapes: the horizon (the
 * home), the ticked horizon (the times), the open bracket (the fast's
 * window), the ledger cell (the calendar), and the grid (the settings).
 *
 * One size, one stroke weight, one colour rule: gold when the destination is
 * the current one, dim otherwise.
 */
enum class Glyph { HORIZON, TICKS, BRACKET, CELL, SPAN, GRID }

@Composable
fun BrandGlyph(
    kind: Glyph,
    active: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
) {
    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = if (active) 2.4f else 1.7f

        when (kind) {
            Glyph.HORIZON -> {
                // the arc + the sun at its right end
                val p = Path().apply {
                    moveTo(w * 0.08f, h * 0.72f)
                    quadraticTo(w * 0.5f, h * 0.06f, w * 0.92f, h * 0.72f)
                }
                drawPath(p, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
                drawCircle(color, w * 0.055f, Offset(w * 0.92f, h * 0.72f))
            }

            Glyph.TICKS -> {
                // the arc + the prayer ticks
                val p = Path().apply {
                    moveTo(w * 0.08f, h * 0.74f)
                    quadraticTo(w * 0.5f, h * 0.14f, w * 0.92f, h * 0.74f)
                }
                drawPath(p, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
                // ticks sit ON the curve (the old y formula floated 0.28h
                // above it): the exact quadratic, with the sunrise omitted
                listOf(0f, 0.47f, 0.68f, 0.86f, 1f).forEach { t ->
                    val mt = 1 - t
                    val x = w * (0.08f + 0.84f * t)
                    val y = h * (mt * mt * 0.74f + 2 * mt * t * 0.14f + t * t * 0.74f)
                    drawLine(
                        color = color,
                        start = Offset(x, y),
                        end = Offset(x, y - h * 0.07f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
            }

            Glyph.BRACKET -> {
                // the fast's window: an open bracket with the sun inside
                val y = h * 0.56f
                drawLine(
                    color = color,
                    start = Offset(w * 0.14f, y),
                    end = Offset(w * 0.86f, y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                // a SPAN: the legs RISE from the endpoints to the window,
                // and the sun sits ON the bar — not floating over a box
                drawLine(
                    color = color,
                    start = Offset(w * 0.14f, y),
                    end = Offset(w * 0.14f, h * 0.30f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.86f, y),
                    end = Offset(w * 0.86f, h * 0.30f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawCircle(color, w * 0.075f, Offset(w * 0.5f, y))
            }

            Glyph.CELL -> {
                // the ledger cell + the sigil strip
                val r = w * 0.12f
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.14f, h * 0.14f),
                    size = androidx.compose.ui.geometry.Size(w * 0.72f, h * 0.72f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                    style = Stroke(width = stroke),
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.26f, h * 0.68f),
                    end = Offset(w * 0.42f, h * 0.68f),
                    strokeWidth = stroke * 1.4f,
                    cap = StrokeCap.Round,
                )
                drawCircle(color, w * 0.06f, Offset(w * 0.58f, h * 0.68f))
                drawCircle(color, w * 0.05f, Offset(w * 0.72f, h * 0.68f))
            }

            Glyph.SPAN -> {
                // the ACT RAIL in miniature: a span with a dot at its hour
                drawLine(
                    color = color,
                    start = Offset(w * 0.12f, h * 0.58f),
                    end = Offset(w * 0.88f, h * 0.58f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.34f, h * 0.34f),
                    end = Offset(w * 0.34f, h * 0.82f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.72f, h * 0.34f),
                    end = Offset(w * 0.72f, h * 0.82f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawCircle(color, w * 0.08f, Offset(w * 0.53f, h * 0.58f))
            }

            Glyph.GRID -> {
                // settings are ADJUSTMENTS: three sliders in the app's own
                // line-and-dot language (a 2×2 dot grid says "grid", not this)
                listOf(0.3f, 0.5f, 0.7f).forEachIndexed { i, fy ->
                    drawLine(
                        color = color,
                        start = Offset(w * 0.14f, h * fy),
                        end = Offset(w * 0.86f, h * fy),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                    drawCircle(
                        color = color,
                        radius = w * 0.07f,
                        center = Offset(w * listOf(0.32f, 0.66f, 0.5f)[i], h * fy),
                    )
                }
            }
        }
    }
}
