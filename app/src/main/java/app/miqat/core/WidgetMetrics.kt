package app.miqat.core

import kotlin.math.roundToInt

/** The widgets' proportions — one testable source for the picker, the layout
 *  and the renderer. The whole card is now the hero: its arc, ground and text
 *  positions are FRACTIONS of the panel, never a stretched sky band. */
object WidgetMetrics {
    const val LAUNCHER_COLUMNS = 5
    const val FULL_CELLS_W = 4
    const val FULL_CELLS_H = 2
    const val COMPACT_CELLS_W = 2
    const val COMPACT_CELLS_H = 1
    const val FULL_MIN_W_DP = 250
    const val FULL_MIN_H_DP = 125
    const val COMPACT_MIN_W_DP = 110
    const val COMPACT_MIN_H_DP = 55

    /** The hero inside the FULL card, as fractions of its height. */
    const val ARC_APEX_FRACTION = 0.16f
    const val ARC_BASELINE_FRACTION = 0.46f
    const val GROUND_START_FRACTION = 0.52f
    const val CORNER_DP = 20f
    const val PAD_DP = 12f

    private const val MAX_W_DP = 600
    private const val MAX_H_DP = 400
    private const val MIN_W_PX = 160
    private const val MIN_H_PX = 96
    private const val MAX_W_PX = 1600
    private const val MAX_H_PX = 1000

    val fullAspect: Float = FULL_MIN_W_DP / FULL_MIN_H_DP.toFloat()
    val compactAspect: Float = COMPACT_MIN_W_DP / COMPACT_MIN_H_DP.toFloat()
    val fullScreenWidthRatio: Float = FULL_CELLS_W.toFloat() / LAUNCHER_COLUMNS
    val compactScreenWidthRatio: Float = COMPACT_CELLS_W.toFloat() / LAUNCHER_COLUMNS

    fun bitmapWidthPx(widthDp: Float, density: Float): Int =
        (widthDp * density).roundToInt().coerceIn(MIN_W_PX, MAX_W_PX)

    fun bitmapHeightPx(heightDp: Float, density: Float): Int =
        (heightDp * density).roundToInt().coerceIn(MIN_H_PX, MAX_H_PX)

    /** Midpoint of the four AppWidget options, clamped; height stays near the
     *  declared 2:1 so a resized panel is never drawn at a fiction ratio. */
    fun panelSizeDp(minW: Int, maxW: Int, minH: Int, maxH: Int): Pair<Float, Float> {
        val w = (((minW.takeIf { it > 0 } ?: FULL_MIN_W_DP) +
            (maxW.takeIf { it > 0 } ?: FULL_MIN_W_DP)) / 2f)
            .coerceIn(FULL_MIN_W_DP.toFloat(), MAX_W_DP.toFloat())
        val hMid = ((minH.takeIf { it > 0 } ?: FULL_MIN_H_DP) +
            (maxH.takeIf { it > 0 } ?: FULL_MIN_H_DP)) / 2f
        val aspectH = w / fullAspect
        val h = hMid.coerceIn(aspectH * 0.6f, aspectH * 1.6f)
            .coerceIn(FULL_MIN_H_DP.toFloat(), MAX_H_DP.toFloat())
        return w to h
    }
}
