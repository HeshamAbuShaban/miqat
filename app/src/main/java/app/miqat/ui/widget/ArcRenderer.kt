package app.miqat.ui.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import app.miqat.ui.DayArc

/**
 * The arc, drawn for the widget.
 *
 * The widget cannot host a Compose canvas, so the SAME horizon is drawn into
 * a bitmap with the platform's own Canvas — the identical geometry
 * ([DayArc]), the identical palette. The shape is the brand; it must not
 * change because it moved to the home screen.
 */
object ArcRenderer {

    // the palette (the app's dark tokens — the widget is always the night sky)
    private const val HAIRLINE = 0xFF2C3A5C.toInt()
    private const val GOLD = 0xFFE8C46A.toInt()
    private const val SAFFRON = 0xFFFFB25C.toInt()
    private const val TICK = 0xFF8E8672.toInt()

    /**
     * @param travelled the day's progress (0..1 along the arc)
     * @param nextTick the next prayer's tick position (0..1)
     */
    fun render(width: Int, height: Int, travelled: Float, nextTick: Float): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val w = width.toFloat()
        val h = height.toFloat()
        val baseY = h - 3f
        val apexY = h * DayArc.APEX

        // the horizon: the whole line, thin
        val rim = Path().apply {
            val (x0, y0) = DayArc.point(0f, w, h, baseY, apexY)
            val (x1, y1) = DayArc.point(1f, w, h, baseY, apexY)
            moveTo(x0, y0)
            quadTo(w * 0.5f, apexY, x1, y1)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        paint.color = HAIRLINE
        paint.strokeWidth = 2f * (h / 110f)
        canvas.drawPath(rim, paint)

        // the travelled part: gold
        val gold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 5f * (h / 110f)
            shader = android.graphics.LinearGradient(
                0f, 0f, w, 0f,
                GOLD, SAFFRON, android.graphics.Shader.TileMode.CLAMP,
            )
        }
        val steps = 40
        val travelledPath = Path()
        for (i in 0..steps) {
            val t = DayArc.travelled(travelled) * (i.toFloat() / steps)
            val (x, y) = DayArc.point(t, w, h, baseY, apexY)
            if (i == 0) travelledPath.moveTo(x, y) else travelledPath.lineTo(x, y)
        }
        canvas.drawPath(travelledPath, gold)

        // the prayer ticks
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        DayArc.TICKS.forEachIndexed { index, t ->
            val (x, y) = DayArc.point(t, w, h, baseY, apexY)
            dot.color = if (index == 0 || t == nextTick) GOLD else TICK
            canvas.drawCircle(x, y, if (t == nextTick) 5f * (h / 110f) else 3.2f * (h / 110f), dot)
        }

        // the sun, where the day actually is
        val sunX = DayArc.point(DayArc.travelled(travelled), w, h, baseY, apexY).first
        val sunY = DayArc.point(DayArc.travelled(travelled), w, h, baseY, apexY).second
        dot.color = Color.argb(60, 0xFF, 0xB2, 0x5C)
        canvas.drawCircle(sunX, sunY, 13f * (h / 110f), dot)
        dot.color = Color.argb(110, 0xFF, 0xB2, 0x5C)
        canvas.drawCircle(sunX, sunY, 7.5f * (h / 110f), dot)
        dot.color = 0xFFFFE0A8.toInt()
        canvas.drawCircle(sunX, sunY, 4.2f * (h / 110f), dot)

        return bmp
    }
}
