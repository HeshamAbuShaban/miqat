package app.miqat.core

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * The acts catalog — STATIC content that ships as an asset (versioned, cited),
 * never computed. The engine supplies only the LIVE fields (the window, the
 * state, the neighbours).
 *
 * The split matters: the words are reviewed once and versioned; the times are
 * computed per day. Nothing in the catalog is a ruling — it states what an
 * act is, why it matters, and where its reference is; where a citation is not
 * verified, the UI shows the «تقديري بحسب الاصطلاح» chip instead.
 */
data class Act(
    val id: String,
    val type: String,       // window | point | boundary
    val name: String,
    val what: String,
    val why: String,
    val rule: String,
    val source: String?,
    val startKind: String,
    val endKind: String,
    val category: String,
) {
    val isWindow: Boolean get() = type == "window"
    val isBoundary: Boolean get() = type == "boundary"
}

object ActsCatalog {

    private const val ASSET = "acts-v1.json"

    @Volatile
    private var cached: List<Act>? = null

    /** The catalog, read once from the asset. */
    fun acts(context: Context): List<Act> {
        cached?.let { return it }
        val app = context.applicationContext
        val json = try {
            val dir = File(app.filesDir.parentFile, "acts").apply { mkdirs() }
            val out = File(dir, ASSET)
            if (!out.exists() || out.length() == 0L) {
                app.assets.open(ASSET).use { input ->
                    out.outputStream().use { o -> input.copyTo(o) }
                }
            }
            out.readText()
        } catch (e: Exception) {
            app.assets.open(ASSET).use { it.readBytes().toString(Charsets.UTF_8) }
        }
        val root = JSONObject(json)
        val arr = root.getJSONArray("acts")
        val out = ArrayList<Act>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Act(
                    id = o.getString("id"),
                    type = o.getString("type"),
                    name = o.getString("name"),
                    what = o.getString("what"),
                    why = o.getString("why"),
                    rule = o.getString("rule"),
                    source = if (o.isNull("source")) null else o.getString("source"),
                    startKind = o.getString("start"),
                    endKind = o.getString("end"),
                    category = o.getString("category"),
                ),
            )
        }
        cached = out
        return out
    }

    /** The live state of an act, right now. */
    enum class ActState { UPCOMING, RUNNING, PASSED }

    data class Live(
        val act: Act,
        val startMinute: Int,
        val endMinute: Int,
        val state: ActState,
        /** Minutes until it starts (or until it ends, when running). */
        val minutesAway: Int,
        /** Minutes since the window opened (null unless it is running). */
        val elapsedMinutes: Int? = null,
    )

    /**
     * The live view of every act for today, computed from the day's timeline.
     */
    fun live(acts: List<Act>, timeline: List<Moment>, nowMinute: Int): List<Live> {
        val byKind = timeline.associateBy { it.kind }
        return acts.mapNotNull { act ->
            val start = byKind[act.startKind]?.minute ?: return@mapNotNull null
            val end = byKind[act.endKind]?.minute ?: start
            val state = when {
                nowMinute < start -> ActState.UPCOMING
                nowMinute in start until end -> ActState.RUNNING
                else -> ActState.PASSED
            }
            val away = when (state) {
                ActState.UPCOMING -> start - nowMinute
                ActState.RUNNING -> end - nowMinute
                ActState.PASSED -> 1440 - nowMinute + start
            }
            // the dual clock of the act: how long it has been open, and how
            // long it stays open (R1: the remaining is primary, the elapsed
            // is secondary)
            val elapsed = if (state == ActState.RUNNING) nowMinute - start else null
            Live(act, start, end, state, away, elapsed)
        }.sortedBy { it.startMinute }
    }
}
