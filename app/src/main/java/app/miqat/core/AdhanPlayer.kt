package app.miqat.core

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * The adhan audio (S3). The recordings ship with the app as assets — the donor
 * shipped them too, and Hesham authorised their use (2026-11-10).
 *
 * Each pack is four SEGMENTS (debut → milieu → fin) plus a Fajr variant, so
 * the prayer alarm can play a complete call rather than a single clip.
 */
object AdhanPlayer {

    /** The packs the app ships. */
    const val PACK_STANDARD = "standard"
    const val PACK_MAGHREBI = "maghrebi"
    const val PACK_HARAMAYN = "haramayn"

    val PACKS = listOf(
        PACK_STANDARD to "أذان معياري",
        PACK_MAGHREBI to "أذان مغربي",
        PACK_HARAMAYN to "أذان الحرمين",
    )

    private const val ASSET_DIR = "adhans"

    /** Playback state, shared with the alarm screen. */
    private var player: MediaPlayer? = null
    private var currentPack: String? = null

    val isPlaying: Boolean get() = player?.isPlaying == true

    /**
     * Plays the adhan for [slot] (0 = Fajr … 5 = Isha) from [pack]. Segments
     * are queued so the call plays as one piece.
     */
    fun play(context: Context, pack: String, slot: Int, version: Int = 1, onFinished: () -> Unit = {}) {
        stop()
        val isFajr = slot == 0
        val v = if (version == 2) "02" else "01"
        val segments = listOf("debut", "milieu", "fin")
        val files = segments.mapNotNull { seg ->
            extract(context, pack, v, if (isFajr) "$v-fajr" else "$v-$seg")
        }
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            if (files.isEmpty()) {
                onFinished()
                return
            }
            // The first segment is played from its file; the rest are queued.
            mp.setDataSource(files.first().absolutePath)
            mp.prepare()
            for (i in 1 until files.size) {
                mp.setNextMediaPlayer(
                    MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build(),
                        )
                        setDataSource(files[i].absolutePath)
                        prepare()
                    },
                )
            }
            mp.setOnCompletionListener { stop(); onFinished() }
            mp.start()
            player = mp
            currentPack = pack
        } catch (e: Exception) {
            Log.w("MiqatAdhan", "play failed: ${e.message}")
            stop()
            onFinished()
        }
    }

    fun stop() {
        player?.let {
            try {
                it.stop()
            } catch (_: IllegalStateException) {
            }
            it.release()
        }
        player = null
        currentPack = null
    }

    /** Copies an asset to the cache dir (MediaPlayer needs a file path). */
    private fun extract(context: Context, pack: String, version: String, name: String): File? = try {
        val dir = File(context.cacheDir, ASSET_DIR).apply { mkdirs() }
        val out = File(dir, "$pack-$name.ogg")
        if (!out.exists() || out.length() == 0L) {
            context.assets.open("$ASSET_DIR/$pack/$version/$name.ogg").use { input ->
                FileOutputStream(out).use { output -> input.copyTo(output) }
            }
        }
        out
    } catch (e: Exception) {
        Log.w("MiqatAdhan", "extract $pack/$name failed: ${e.message}")
        null
    }
}
