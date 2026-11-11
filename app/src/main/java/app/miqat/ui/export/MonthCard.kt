package app.miqat.ui.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import app.miqat.core.Hijri
import app.miqat.core.IslamicDay
import app.miqat.ui.DayArc
import app.miqat.ui.Fmt
import app.miqat.ui.PRAYER_NAMES
import app.miqat.ui.list
import app.miqat.ui.AppState
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * THE MONTH CARD — the month's ledger as an image you can share.
 *
 * The export is not a screenshot: it is a designed page, in the app's own
 * palette, with the honesty the app keeps everywhere:
 *  - the method and its confidence are printed at the foot;
 *  - the computed bounds carry «تقديري بحسب الاصطلاح»;
 *  - nothing is stated that the app does not also state.
 *
 * The text is drawn with the platform's own text engine (so the Arabic
 * shaping and the bidi order are the platform's, not a guess), in the app's
 * bundled Cairo.
 */
object MonthCard {

    // the palette (the app's dark tokens)
    private const val BG = 0xFF060B1C.toInt()
    private const val PANEL = 0xFF0E1526.toInt()
    private const val OUTLINE = 0xFF26314F.toInt()
    private const val TEXT = 0xFFEDEBE3.toInt()
    private const val DIM = 0xFF9AA0B8.toInt()
    private const val GOLD = 0xFFE8C46A.toInt()
    private const val SAFFRON = 0xFFFFB25C.toInt()
    private const val MaterialThemeDim = 0xFF6E7893.toInt()

    private const val WIDTH = 1080
    private const val MARGIN = 56f
    private const val HEADER = 470
    private const val ROW = 64
    private const val FOOTER = 210

    /** One row of the ledger: a day, its Hijri day, its time and its change. */
    data class Row(
        val gregorianDay: Int,
        val hijriDay: Int,
        val timeLabel: String,
        val delta: Int?,
        val isFriday: Boolean,
    )

    /**
     * Build the month's rows for one prayer.
     */
    fun rows(
        state: AppState,
        fmt: Fmt,
        year: Int,
        month: Int,
        prayer: Int,
    ): List<Row> {
        val out = ArrayList<Row>(31)
        var previous: Int? = null
        val days = java.util.Calendar.getInstance().apply {
            clear(); set(year, month - 1, 1)
        }.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        for (day in 1..days) {
            val t = state.times(year, month, day).list[prayer]
            val minute = (((t % 24.0) + 24.0) % 24.0 * 60.0).roundToInt()
            val hijri = Hijri.at(year, month, day, state.display.hijriOffsetDays)
            out.add(
                Row(
                    gregorianDay = day,
                    hijriDay = hijri.hDay,
                    timeLabel = fmt.hhmm(minute),
                    delta = previous?.let { minute - it },
                    isFriday = dayOfWeek(year, month, day) == 5,
                ),
            )
            previous = minute
        }
        return out
    }

    private fun dayOfWeek(year: Int, month: Int, day: Int): Int =
        java.util.Calendar.getInstance().apply { clear(); set(year, month - 1, day) }
            .get(java.util.Calendar.DAY_OF_WEEK)

    /**
     * The month's label — from the SAME window the screen draws, so the card
     * and the grid can never call the same month by two names (they did, in
     * six months of 2026 — the design council, 2026-11-11).
     */
    fun hijriMonthLabel(year: Int, month: Int, offsetDays: Int = 0): String {
        val window = app.miqat.core.CalendarWindows.build(
            context = app.miqat.core.CalendarContext.GREGORIAN,
            gregorianYear = year,
            gregorianMonth = month,
            hijriOffset = offsetDays,
            today = Triple(year, month, 1),
        )
        return window.subtitleAr
    }

    /**
     * Render the month as a card.
     */
    fun render(
        context: Context,
        state: AppState,
        fmt: Fmt,
        year: Int,
        month: Int,
        prayer: Int,
        rows: List<Row>,
    ): Bitmap {
        val height = HEADER + rows.size * ROW + FOOTER
        val bmp = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(BG)

        val regular = androidx.core.content.res.ResourcesCompat
            .getFont(context, app.miqat.R.font.cairo_regular)
            ?: Typeface.create("sans-serif", Typeface.NORMAL)
        val medium = androidx.core.content.res.ResourcesCompat
            .getFont(context, app.miqat.R.font.cairo_medium)
            ?: Typeface.create("sans-serif", Typeface.NORMAL)
        val bold = androidx.core.content.res.ResourcesCompat
            .getFont(context, app.miqat.R.font.cairo_bold)
            ?: Typeface.create("sans-serif", Typeface.BOLD)

        fun paint(color: Int, size: Float, face: Typeface) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = face
            textAlign = Paint.Align.RIGHT
        }

        // --- the header: the wordmark, the place, the month, the prayer -----
        val title = paint(GOLD, 52f, bold)
        canvas.drawText("الميقات", WIDTH - MARGIN, 92f, title)

        val sub = paint(TEXT, 34f, medium)
        val city = state.city.ar.ifBlank { state.city.en }
        canvas.drawText(city, WIDTH - MARGIN, 156f, sub)

        val meta = paint(DIM, 28f, regular)
        canvas.drawText(
            "دفتر ${PRAYER_NAMES[prayer]} — ${hijriMonthLabel(year, month, state.display.hijriOffsetDays)} ${fmt.n(year)}",
            WIDTH - MARGIN,
            206f,
            meta,
        )
        // the honesty: the Hijri date is tabular, not a sighting
        canvas.drawText(
            app.miqat.core.IslamicDays.HIJRI_APPROX,
            WIDTH - MARGIN,
            240f,
            paint(MaterialThemeDim, 22f, regular),
        )

        // the horizon: the same shape as the app's (a small mark, not a picture)
        drawMiniArc(canvas, 0.52f)

        // --- the rows --------------------------------------------------------
        var y = HEADER.toFloat()
        val day = paint(TEXT, 32f, medium)
        val hDay = paint(DIM, 28f, regular)
        val time = paint(TEXT, 34f, medium)
        val delta = paint(SAFFRON, 26f, regular)
        val hair = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OUTLINE
            strokeWidth = 1f
        }

        rows.forEach { row ->
            // the day (right), the Hijri day, the time (left), the change
            canvas.drawText(fmt.n(row.gregorianDay), WIDTH - MARGIN, y + 36f, day)
            canvas.drawText(fmt.n(row.hijriDay), WIDTH - MARGIN - 78f, y + 34f, hDay)
            val t = paint(TEXT, 34f, medium).apply { textAlign = Paint.Align.LEFT }
            canvas.drawText(row.timeLabel, MARGIN, y + 36f, t)
            if (row.delta != null && row.delta != 0) {
                val d = paint(if (row.delta > 0) SAFFRON else DIM, 26f, regular)
                    .apply { textAlign = Paint.Align.LEFT }
                canvas.drawText(
                    (if (row.delta > 0) "+" else "−") + fmt.n(abs(row.delta)) + " د",
                    MARGIN + 150f,
                    y + 34f,
                    d,
                )
            }
            y += ROW
            canvas.drawLine(MARGIN, y, WIDTH - MARGIN, y, hair)
        }

        // --- the foot: the honesty ------------------------------------------
        // The confidence is printed as it is shown in the app — and the
        // «تقديري بحسب الاصطلاح» chip appears ONLY when the times really are
        // an estimate (a published method has nothing to apologise for).
        val foot = paint(DIM, 24f, regular)
        val confidence = state.confidence()
        canvas.drawText(
            "${confidence.labelAr} — ${confidence.noteAr}",
            WIDTH - MARGIN,
            height - 116f,
            foot,
        )
        val chip = paint(DIM, 22f, regular)
        canvas.drawText(
            "${state.city.ar.ifBlank { state.city.en }} · طريقة الحساب: ${state.method.id}",
            WIDTH - MARGIN,
            height - 76f,
            chip,
        )
        if (confidence == app.miqat.core.TimeConfidence.ESTIMATED) {
            canvas.drawText(
                "تقديري بحسب الاصطلاح",
                WIDTH - MARGIN,
                height - 40f,
                chip,
            )
        }

        return bmp
    }

    /** The small horizon under the header — the app's shape, tiny. */
    private fun drawMiniArc(canvas: Canvas, progress: Float) {
        val path = Path()
        val left = MARGIN
        val right = WIDTH - MARGIN
        val baseY = 320f
        val apexY = 250f
        path.moveTo(left, baseY)
        path.quadTo((left + right) / 2f, apexY, right, baseY)
        val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            color = 0xFF2C3A5C.toInt()
        }
        canvas.drawPath(path, rim)

        val travelled = Path()
        val steps = 40
        for (i in 0..steps) {
            val t = DayArc.travelled(progress) * (i.toFloat() / steps)
            val mt = 1f - t
            val x = mt * mt * left + 2 * mt * t * (left + right) / 2f + t * t * right
            val yy = mt * mt * baseY + 2 * mt * t * apexY + t * t * baseY
            if (i == 0) travelled.moveTo(x, yy) else travelled.lineTo(x, yy)
        }
        val gold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
            color = GOLD
        }
        canvas.drawPath(travelled, gold)

        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SAFFRON }
        val t = DayArc.travelled(progress)
        val mt = 1f - t
        val sunX = mt * mt * left + 2 * mt * t * (left + right) / 2f + t * t * right
        val sunY = mt * mt * baseY + 2 * mt * t * apexY + t * t * baseY
        canvas.drawCircle(sunX, sunY, 7f, dot)
    }
}
