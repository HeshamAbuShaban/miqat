package app.miqat.core

/**
 * Arabic normalization for tolerant search (R2).
 *
 * The donor's city names are stored with their full orthography; a user typing
 * «الاهرام» (no hamza, ta-marbuta as he) must still find «الأهرام». The same
 * folding is applied when the asset is BUILT (tools/build_miqat_assets.py), so
 * the comparison happens on indexed columns.
 */
object ArabicFold {

    private val DIACRITICS = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")
    private val TATWEEL = "\u0640"

    private val MAP: Map<Char, Char> = mapOf(
        'أ' to 'ا', 'إ' to 'ا', 'آ' to 'ا', 'ٱ' to 'ا',
        'ة' to 'ه', 'ى' to 'ي', 'ئ' to 'ي', 'ؤ' to 'و',
    )

    fun fold(s: String): String {
        if (s.isEmpty()) return ""
        var out = StringBuilder(s.length)
        for (c in s) {
            out.append(MAP[c] ?: c)
        }
        return out.toString()
            .replace(TATWEEL, "")
            .replace(DIACRITICS, "")
            .lowercase()
            .trim()
    }
}
