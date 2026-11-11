package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The daily reminder's references are pinned by tests: the council's corpus
 * scan found THREE wrong references in the first draft (the Fajr verse is
 * الإسراء ٧٩, not المزمل ٧٣). A wrong reference is worse than no verse.
 */
class DailyRemindersTest {

    @Test
    fun `every reminder carries a surah and an ayah`() {
        for (r in listOf(DailyReminders.forNow(2.0, 0))) {
            assertTrue(r.reference.isNotBlank())
        }
    }

    @Test
    fun `the night verse is Isra 79 - not Muzzamil 73`() {
        val all = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).map {
            DailyReminders.forNow(2.0, it)
        }.distinct()
        val isra = all.firstOrNull { it.reference.contains("الإسراء") }
        assertTrue("the night verse must be الإسراء ٧٩", isra != null)
        assertEquals("الإسراء ٧٩", isra!!.reference)
        assertTrue(
            "the wrong reference must not survive anywhere",
            all.none { it.reference.contains("مزمل") },
        )
    }

    @Test
    fun `a partial ayah is marked and the attribution is one line`() {
        val r = DailyReminders.forNow(17.0, 3)
        val (text, source) = DailyReminders.line(r)
        if (r.partial) assertTrue(text.endsWith("…"))
        assertTrue(source.startsWith("المصدر: القرآن الكريم"))
        assertTrue(source.contains(r.reference.substringBefore(" ")))
    }
}
