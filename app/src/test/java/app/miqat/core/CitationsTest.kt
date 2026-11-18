package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The citations asset is the single source of truth (founder, 2026-11-18).
 * org.json's JVM stub throws "not mocked", so the test reads the flat records
 * directly — no new dependency, no device. The ASSERTIONS are on the real
 * [Citation.line] logic and on the honesty of the asset itself.
 */
class CitationsTest {

    private val raw = File("src/main/assets/citations-v1.json").readText()

    private fun field(obj: String, key: String): String? =
        Regex("\"$key\"\\s*:\\s*(\"(?:[^\"\\\\]|\\\\.)*\"|null|-?\\d+|true|false)")
            .find(obj)?.groupValues?.get(1)
            ?.let { if (it == "null") null else it.trim('"') }

    private val records: List<Citation> = Regex("\\{[^{}]+\\}").findAll(raw)
        .map { obj ->
            val text = obj.value
            Citation(
                id = field(text, "id")!!.toInt(),
                source = field(text, "source")!!,
                number = field(text, "number"),
                verified = field(text, "verified") == "true",
                fullWording = field(text, "fullWording") == "true",
            )
        }.toList()

    @Test fun `all 21 donor events are cited`() {
        assertEquals(21, records.size)
        assertEquals((1..21).toList(), records.map { it.id }.sorted())
    }

    @Test fun `every record has a source`() {
        assertTrue(records.all { it.source.isNotBlank() })
    }

    @Test fun `an unverified record never prints a number`() {
        val unverified = records.filter { !it.verified }
        assertTrue(unverified.isNotEmpty())
        assertTrue(unverified.all { it.number.isNullOrBlank() })
        unverified.forEach {
            assertTrue(it.line().startsWith("رواه: "))
            assertTrue(it.line().contains("غير متحقق منه"))
        }
    }

    @Test fun `a verified record prints its exact reference`() {
        val one = records.first { it.id == 1 }
        assertEquals("سنن أبي داود", one.source)
        assertEquals("٥٠٨١", one.number)
        assertEquals("سنن أبي داود ٥٠٨١", one.line())
        assertEquals("سنن أبي داود ٥٠٨١", one.claim())
    }

    @Test fun `the full-wording flag survives the move to JSON`() {
        assertEquals(2, records.first { it.fullWording }.id)
    }

    @Test fun `no narration text is duplicated in the citation asset`() {
        assertFalse(raw.contains("سبحان"))
        assertFalse(raw.contains("أصبحنا"))
        assertFalse(raw.contains("الحمد لله"))
    }
}
