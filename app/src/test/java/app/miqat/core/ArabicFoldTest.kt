package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Arabic folding (R2 search tolerance) — the build script folds the SAME
 * way (tools/build_miqat_assets.py), so a change on one side must change the
 * other. This test pins the rules.
 */
class ArabicFoldTest {

    @Test
    fun `hamza forms fold to alef`() {
        assertEquals("الاهرام", ArabicFold.fold("الأهرام"))
        assertEquals("الاهرام", ArabicFold.fold("الاهرام"))
        assertEquals("الاماره", ArabicFold.fold("الإمارة"))
    }

    @Test
    fun `ta marbuta and alef maqsura fold`() {
        assertEquals("القاهره", ArabicFold.fold("القاهرة"))
        assertEquals("مصلي", ArabicFold.fold("مصلى"))
    }

    @Test
    fun `diacritics and tatweel are removed`() {
        assertEquals("الحمد لله", ArabicFold.fold("الْحَمْدُ لِلّٰهِ"))
        assertEquals("سلام", ArabicFold.fold("ســلام"))
    }

    @Test
    fun `folding is idempotent and keeps the plain text`() {
        val once = ArabicFold.fold("الأهرام")
        assertEquals(once, ArabicFold.fold(once))
        assertTrue(ArabicFold.fold("Cairo").contains("cairo"))
    }
}
