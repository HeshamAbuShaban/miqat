package app.miqat.core

/**
 * THE WIDGETS' PROPORTIONS — one testable source for the ratios the picker
 * advertises and the renderer draws.
 *
 * Before this object the full widget drew its sky at a HARDCODED 96dp height
 * whatever the panel's width, so a resized widget stretched the horizon (the
 * sky is the app's one rich moment — stretching it is a visible lie). The
 * cell grid, the fraction of a launcher screen and the sky's aspect are now
 * properties; the XML info uses the same numbers in res/values/dimens.xml.
 */
object WidgetMetrics {

    /** A stock 5-column launcher (the grid Android's targetCell* assumes). */
    const val LAUNCHER_COLUMNS = 5

    const val FULL_CELLS_W = 4
    const val FULL_CELLS_H = 2
    const val COMPACT_CELLS_W = 2
    const val COMPACT_CELLS_H = 1

    /** The minimum sizes declared in the appwidget-provider (dp). */
    const val FULL_MIN_W_DP = 250
    const val FULL_MIN_H_DP = 125
    const val COMPACT_MIN_W_DP = 110
    const val COMPACT_MIN_H_DP = 55

    /** Width / height of the SKY BAND inside the full widget. */
    const val SKY_ASPECT = 2.8f

    /** The whole widget's aspect (both sizes keep the same 2:1 proportion). */
    val fullAspect: Float = FULL_MIN_W_DP / FULL_MIN_H_DP.toFloat()
    val compactAspect: Float = COMPACT_MIN_W_DP / COMPACT_MIN_H_DP.toFloat()

    /** The fraction of the launcher's width each widget occupies. */
    val fullScreenWidthRatio: Float = FULL_CELLS_W.toFloat() / LAUNCHER_COLUMNS
    val compactScreenWidthRatio: Float = COMPACT_CELLS_W.toFloat() / LAUNCHER_COLUMNS

    /** The sky bitmap's height follows the panel's real width, not a magic dp. */
    fun skyHeightPx(widthPx: Int): Int =
        (widthPx / SKY_ASPECT).toInt().coerceIn(72, 480)
}
