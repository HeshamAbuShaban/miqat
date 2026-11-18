package app.miqat.core

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * ONE versioned citation asset for the reminder texts — the founder
 * (2026-11-18): «لا تحقن النص هكذا … اعتمد أسلوب احترافي … ربما جيسون».
 *
 * The application TEXTS stay in the donor DB (one copy, never duplicated in
 * Kotlin). Only the references live here: a source, a number when it is
 * verified, a grade when one is vouched for, and the context in which the
 * record is used. An unverified record never prints a number.
 *
 * [parse] is pure, so the asset can be gated in JVM tests.
 */
data class Citation(
    val id: Int,
    val source: String,
    val number: String?,
    val verified: Boolean,
    val grade: String? = null,
    val note: String? = null,
    val fullWording: Boolean = false,
) {
    /** The honest line: a full reference only when the number was verified. */
    fun line(): String = when {
        verified && !number.isNullOrBlank() -> "$source $number"
        else -> AdhkarCitations.COLLECTION_ONLY.format(source)
    }

    /** What notification selection needs: a claim only when we can stand by it. */
    fun claim(): String = when {
        verified && !number.isNullOrBlank() -> "$source $number"
        else -> "$source — رقم الحديث غير متحقق منه"
    }
}

object Citations {

    const val ASSET = "citations-v1.json"

    @Volatile
    private var cached: List<Citation>? = null

    fun parse(json: String): List<Citation> {
        val root = JSONObject(json)
        val arr = root.getJSONArray("citations")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Citation(
                id = o.getInt("id"),
                source = o.getString("source"),
                number = if (o.isNull("number")) null else o.getString("number"),
                verified = o.optBoolean("verified", false),
                grade = if (o.isNull("grade")) null else o.getString("grade"),
                note = if (o.isNull("note")) null else o.getString("note"),
                fullWording = o.optBoolean("fullWording", false),
            )
        }
    }

    /** Read once, from the bundled asset (offline, no network, no new dep). */
    fun load(context: Context): List<Citation> {
        cached?.let { return it }
        val app = context.applicationContext
        val text = try {
            val dir = File(app.filesDir.parentFile, "citations").apply { mkdirs() }
            val out = File(dir, ASSET)
            if (!out.exists() || out.length() == 0L) {
                app.assets.open(ASSET).use { input -> out.outputStream().use { input.copyTo(it) } }
            }
            out.readText()
        } catch (e: Exception) {
            app.assets.open(ASSET).use { it.readBytes().toString(Charsets.UTF_8) }
        }
        val parsed = parse(text)
        cached = parsed
        return parsed
    }

    fun byId(id: Int): Citation? = cached?.firstOrNull { it.id == id }
}
