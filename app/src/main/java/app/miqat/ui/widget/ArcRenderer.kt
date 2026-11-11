package app.miqat.ui.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.compose.ui.graphics.toArgb
import app.miqat.ui.DayArc
import app.miqat.ui.Sky
import app.miqat.ui.SkyPhase

/**
 * THE HERO, MINIATURISED — the widget draws the app's own horizon.
 *
 * The widget cannot host a Compose canvas, so the SAME sky, the SAME
 * geometry ([DayArc]) and the SAME marks are drawn into a bitmap with the
 * platform's own Canvas. One renderer's facts: the sky follows the hour,
 * the ticks sit at the prayers' REAL times, the next prayer is the only
 * loud tick, and the sun hides when it is night (the widget council,
 * 2026-11-11 — the widget used to letterbox a fixed bitmap into a third of
 * its width, with a tick template that meant nothing).
 */
object ArcRenderer {

    // the widget's palette — the app's night tokens (the widget is always
    // the night object on the home screen, even in light mode)
    private const val HAIRLINE = 0xFF93A3CC.toInt()
    private const val SAFFRON = 0xFFFFB25C.toInt()
    private const val TICK = 0xFFC3CCE2.toInt()
    private const val GROUND = 0xFF010206.toInt()

    /**
     * @param travelled the day's progress (0..1 along the arc)
     * @param ticks the six prayer ticks at their real times (0..1), from
     *   [DayArc.tickParams] — the same numbers the hero reads
     * @param nextIndex which tick is the next prayer
     * @param skyPhase the hour's phase — the sky is a fact about the hour
     */
    fun render(
        width: Int,
        height: Int,
        travelled: Float,
        ticks: List<Float>,
        nextIndex: Int,
        skyPhase: SkyPhase,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val w = width.toFloat()
        val h = height.toFloat()
        val scale = (h / 96f).coerceIn(0.6f, 3f)
        val baseY = h * 0.82f
        val apexY = h * 0.18f

        // the sky: the hour's four bands, top → horizon
        val bands = Sky.bands(skyPhase).map { it.toArgb() }
        val sky = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h,
                bands.toIntArray(), floatArrayOf(0f, 0.34f, 0.68f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w, h, sky)

        // the stars, out only when it is dark
        if (skyPhase.stars) {
            val star = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TICK }
            listOf(
                0.09f to 0.20f, 0.24f to 0.44f, 0.38f to 0.12f, 0.55f to 0.30f,
                0.68f to 0.16f, 0.83f to 0.38f, 0.93f to 0.24f,
            ).forEach { (fx, fy) ->
                canvas.drawCircle(w * fx, h * fy, 1.4f * scale, star)
            }
        }

        // the ground under the sky (the hero's ground band)
        val ground = Paint().apply { color = GROUND }
        canvas.drawRect(0f, baseY + 1f, w, h, ground)

        // the horizon: the whole line, thin
        val rim = Path().apply {
            val (x0, y0) = DayArc.point(0f, w, h, baseY, apexY)
            val (x1, y1) = DayArc.point(1f, w, h, baseY, apexY)
            moveTo(x0, y0)
            quadTo(w * 0.5f, apexY, x1, y1)
        }
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = HAIRLINE
            strokeWidth = 2f * scale
        }
        canvas.drawPath(rim, rimPaint)

        // the travelled part: gold → saffron, the day's walked path
        val gold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 4.5f * scale
            shader = LinearGradient(
                w * DayArc.RIGHT, 0f, w * DayArc.LEFT, 0f,
                0xFFE8C46A.toInt(), SAFFRON, Shader.TileMode.CLAMP,
            )
        }
        val steps = 40
        val travelledPath = Path()
        for (i in 0..steps) {
            val t = DayArc.travelled(travelled) * i / steps
            val (x, y) = DayArc.point(t, w, h, baseY, apexY)
            if (i == 0) travelledPath.moveTo(x, y) else travelledPath.lineTo(x, y)
        }
        canvas.drawPath(travelledPath, gold)

        // the prayer ticks — at their REAL times; ONE loud tick (the next
        // prayer), the sunrise in saffron, the rest pale
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        ticks.forEachIndexed { index, t ->
            val (x, y) = DayArc.point(t, w, h, baseY, apexY)
            val isNext = index == nextIndex
            dot.color = if (isNext || index == 1) SAFFRON else TICK
            canvas.drawCircle(x, y, (if (isNext) 5f else 3.2f) * scale, dot)
        }

        // the sun, where the day actually is — hidden at night (the stars
        // take the sky's turn)
        if (skyPhase != SkyPhase.NIGHT) {
            val (sx, sy) = DayArc.point(DayArc.travelled(travelled), w, h, baseY, apexY)
            dot.color = Color.argb(60, 0xFF, 0xB2, 0x5C)
            canvas.drawCircle(sx, sy, 13f * scale, dot)
            dot.color = Color.argb(110, 0xFF, 0xB2, 0x5C)
            canvas.drawCircle(sx, sy, 7.5f * scale, dot)
            dot.color = 0xFFFFE0A8.toInt()
            canvas.drawCircle(sx, sy, 4.2f * scale, dot)
        }

        return bmp
    }
}
