package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The acts catalog's LIVE view: every act carries its own two clocks — the
 * remaining (primary) and the elapsed (secondary) — so the index can show
 * each feature on its own line with its own timer.
 */
class ActsCatalogTest {

    private val act = Act(
        id = "sleep",
        type = "window",
        name = "وقت النوم",
        what = "النوم بعد العشاء",
        why = "في ثلث الليل الآخر يُستيقظ القوم",
        rule = "من بعد العشاء إلى ما قبل الفجر",
        source = null,
        startKind = "sleep_start",
        endKind = "sleep_end",
        category = "سنة",
    )

    private val timeline = listOf(
        Moment("sleep_start", "وقت النوم", MomentCategory.SUNNAH, 1200),
        Moment("sleep_end", "الاستيقاظ", MomentCategory.SUNNAH, 1800),
    )

    @Test
    fun `a running act carries its own elapsed`() {
        val live = ActsCatalog.live(listOf(act), timeline, 1320)
        assertEquals(ActsCatalog.ActState.RUNNING, live[0].state)
        assertEquals(120, live[0].elapsedMinutes)
        assertEquals(480, live[0].minutesAway)
    }

    @Test
    fun `an act that has not opened yet has no elapsed`() {
        val live = ActsCatalog.live(listOf(act), timeline, 1000)
        assertEquals(ActsCatalog.ActState.UPCOMING, live[0].state)
        assertNull(live[0].elapsedMinutes)
        assertEquals(200, live[0].minutesAway)
    }

    @Test
    fun `an act that has passed has no elapsed either`() {
        val live = ActsCatalog.live(listOf(act), timeline, 2000)
        assertEquals(ActsCatalog.ActState.PASSED, live[0].state)
        assertNull(live[0].elapsedMinutes)
    }
}
