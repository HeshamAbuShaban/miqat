package app.miqat.ui.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import androidx.compose.ui.graphics.toArgb
import app.miqat.core.WidgetMetrics
import app.miqat.ui.DayArc
import app.miqat.ui.Sky
import app.miqat.ui.SkyPhase

/**
 * THE HERO, MINIATURISED — the whole card is the app's horizon, not a sky band
 * letterboxed above a block of text. The hour's sky fills the bitmap, the arc
 * and its real prayer ticks sit at the metrics' fractions, the ground starts
 * at 52% so the four lines below stay readable, and the corners are rounded.
 * Text is never drawn here: RemoteViews owns the type.
 */
object ArcRenderer {

    fun render(
        width: Int,
        height: Int,
        travelled: Float,
        ticks: List<Float>,
        nextIndex: Int,
        skyPhase: SkyPhase,
        palette: WidgetPalette,
        cornerRadiusPx: Float,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val w = width.toFloat()
        val h = height.toFloat()
        val scale = (w / 320f).coerceIn(0.6f, 3f)
        val baseY = h * WidgetMetrics.ARC_BASELINE_FRACTION
        val apexY = h * WidgetMetrics.ARC_APEX_FRACTION
        val groundY = h * WidgetMetrics.GROUND_START_FRACTION

        // the phase sky, top → bottom, over the WHOLE card
        val bands = palette.skyBands(skyPhase).map { it.toArgb() }
        canvas.drawRect(0f, 0f, w, h, Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h, bands.toIntArray(),
                floatArrayOf(0f, 0.34f, 0.68f, 1f), Shader.TileMode.CLAMP,
            )
        })

        // stars belong to the dark sky only (paper gets no decoration)
        if (palette.dark && skyPhase.stars) {
            val star = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.tick.copy(alpha = 0.7f * Sky.starAlpha(skyPhase)).toArgb()
            }
            listOf(
                0.09f to 0.16f, 0.24f to 0.36f, 0.38f to 0.09f, 0.55f to 0.25f,
                0.68f to 0.13f, 0.83f to 0.32f, 0.93f to 0.19f,
            ).forEach { (fx, fy) -> canvas.drawCircle(w * fx, h * fy, 1.4f * scale, star) }
        }

        // the ground/scrim, with one hairline where the sky ends
        canvas.drawRect(0f, groundY, w, h, Paint().apply { color = palette.ground.toArgb() })
        canvas.drawLine(0f, groundY, w, groundY, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.outline.toArgb()
            strokeWidth = scale
        })

        val rim = Path().apply {
            val (x0, y0) = DayArc.point(0f, w, h, baseY, apexY)
            val (x1, y1) = DayArc.point(1f, w, h, baseY, apexY)
            moveTo(x0, y0)
            quadTo(w * 0.5f, apexY, x1, y1)
        }
        canvas.drawPath(rim, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = palette.arc.toArgb()
            strokeWidth = 2f * scale
        })

        // deep gold on paper; gold → sun on the night sky (the walked path)
        val walked = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 4.5f * scale
            color = palette.gold.toArgb()
            if (palette.dark) shader = LinearGradient(
                w * DayArc.RIGHT, 0f, w * DayArc.LEFT, 0f,
                palette.gold.toArgb(), palette.sun.toArgb(), Shader.TileMode.CLAMP,
            )
        }
        val path = Path()
        for (i in 0..40) {
            val (x, y) = DayArc.point(DayArc.travelled(travelled) * i / 40f, w, h, baseY, apexY)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, walked)

        val dot = Paint(Paint.ANTI_ALIAS_FLAG)
        ticks.forEachIndexed { index, t ->
            val (x, y) = DayArc.point(DayArc.travelled(t), w, h, baseY, apexY)
            dot.style = Paint.Style.FILL
            dot.color = when {
                index == nextIndex -> (if (palette.dark) palette.sun else palette.gold).toArgb()
                index == 1 -> (if (palette.dark) palette.sun else palette.gold.copy(alpha = 0.7f)).toArgb()
                else -> palette.tick.toArgb()
            }
            canvas.drawCircle(x, y, (if (index == nextIndex) 5f else 3.2f) * scale, dot)
        }

        if (skyPhase != SkyPhase.NIGHT) {
            val (sx, sy) = DayArc.point(DayArc.travelled(travelled), w, h, baseY, apexY)
            if (palette.dark) {
                dot.style = Paint.Style.FILL
                dot.color = palette.sun.copy(alpha = 0.24f).toArgb()
                canvas.drawCircle(sx, sy, 13f * scale, dot)
                dot.color = palette.sun.copy(alpha = 0.43f).toArgb()
                canvas.drawCircle(sx, sy, 7.5f * scale, dot)
                dot.color = palette.text.toArgb()
                canvas.drawCircle(sx, sy, 4.2f * scale, dot)
            } else {
                dot.style = Paint.Style.STROKE
                dot.strokeWidth = 1.6f * scale
                dot.color = palette.sun.toArgb()
                canvas.drawCircle(sx, sy, 6.5f * scale, dot)
            }
        }

        // round the bitmap itself — the card has no square corners to leak
        val radius = cornerRadiusPx.coerceAtMost(minOf(w, h) / 2f)
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(mask).drawRoundRect(
            0f, 0f, w, h, radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() },
        )
        Canvas(bmp).drawBitmap(mask, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        })
        mask.recycle()
        return bmp
    }
}
