package app.miqat.core

/**
 * The citation FACADE — the API the UI and the notifications already use.
 *
 * The data moved to the versioned asset `citations-v1.json` (2026-11-18);
 * this object keeps one stable doorway so no screen has to know about the
 * asset, and no citation is written twice in Kotlin.
 */
object AdhkarCitations {

    /** «رواه: <المصدر> — رقم الحديث غير متحقق منه» */
    const val COLLECTION_ONLY = "رواه: %s — رقم الحديث غير متحقق منه"

    /** Called by the app state / services before any citation is printed. */
    fun load(context: android.content.Context) {
        Citations.load(context)
    }

    /** The verified citation for a donor prayer_event id, or null if unknown. */
    fun citationFor(eventId: Int): String? = Citations.byId(eventId)?.claim()

    /** The citation line to print under a text. */
    fun lineFor(eventId: Int): String? = Citations.byId(eventId)?.line()

    /** The donor's truncated excerpt we deliberately do not print. */
    fun useFullWording(eventId: Int): Boolean = Citations.byId(eventId)?.fullWording ?: false
}
