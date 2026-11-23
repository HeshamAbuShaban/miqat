package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The picker's advertised proportions must match what the renderer draws. */
class WidgetMetricsTest {

    @Test fun `both widgets keep the 2 by 1 proportion`() {
        assertEquals(2f, WidgetMetrics.fullAspect, 0.001f)
        assertEquals(2f, WidgetMetrics.compactAspect, 0.001f)
    }

    @Test fun `the screen fractions are the cell counts over five columns`() {
        assertEquals(0.8f, WidgetMetrics.fullScreenWidthRatio, 0.001f)
        assertEquals(0.4f, WidgetMetrics.compactScreenWidthRatio, 0.001f)
    }

    @Test fun `the hero sits on real fractions of the whole card`() {
        assertEquals(0.16f, WidgetMetrics.ARC_APEX_FRACTION, 0.001f)
        assertEquals(0.46f, WidgetMetrics.ARC_BASELINE_FRACTION, 0.001f)
        assertTrue(WidgetMetrics.GROUND_START_FRACTION > WidgetMetrics.ARC_BASELINE_FRACTION)
        assertEquals(20f, WidgetMetrics.CORNER_DP, 0.001f)
        assertEquals(12f, WidgetMetrics.PAD_DP, 0.001f)
    }

    @Test fun `the bitmap follows the panel options and keeps its aspect close`() {
        val (w, h) = WidgetMetrics.panelSizeDp(250, 350, 125, 175)
        assertEquals(300f, w, 0.001f)
        assertEquals(150f, h, 0.001f)
        // missing options fall back to the declared minimums
        val (smallW, smallH) = WidgetMetrics.panelSizeDp(0, 0, 0, 0)
        assertEquals(250f, smallW, 0.001f)
        assertEquals(125f, smallH, 0.001f)
    }

    @Test fun `bitmap pixels are clamped`() {
        assertTrue(WidgetMetrics.bitmapWidthPx(320f, 3f) > WidgetMetrics.bitmapWidthPx(320f, 2f))
        assertEquals(160, WidgetMetrics.bitmapWidthPx(1f, 1f))
        assertEquals(1600, WidgetMetrics.bitmapWidthPx(9999f, 3f))
    }
}
