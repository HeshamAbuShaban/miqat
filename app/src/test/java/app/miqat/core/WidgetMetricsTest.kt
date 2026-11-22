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

    @Test fun `the sky height follows the width`() {
        val small = WidgetMetrics.skyHeightPx(250)
        val wide = WidgetMetrics.skyHeightPx(400)
        assertTrue(wide > small)
        assertEquals((250 / WidgetMetrics.SKY_ASPECT).toInt(), small)
    }
}
