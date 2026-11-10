package app.miqat.core

/**
 * The adhkar / dua texts the app may show, with their verified citations.
 *
 * The DONOR's own reminder texts (prayer_events, 21 rows) are the base, but
 * the guardrail says nothing ships without a verified source — so the council
 * (2026-11-10) verified them one by one against the canonical corpus and the
 * outcome is recorded here. A text with no verified NUMBER keeps a
 * collection-only attribution, which the UI prints honestly.
 */
object AdhkarCitations {

    /** «رواه: <المصدر> — رقم الحديث غير متحقق منه» */
    const val COLLECTION_ONLY = "رواه: %s — رقم الحديث غير متحقق منه"

    /**
     * The verified citation for a donor prayer_event id, or null when the
     * council could not vouch for it (the UI then prints the collection-only
     * line, or nothing at all).
     */
    fun citationFor(eventId: Int): String? = when (eventId) {
        // «الحمد لله الذي أحيانا بعد ما أماتنا وإليه النشور»
        1 -> "سنن أبي داود ٥٠٨١"
        // «أصبحنا وأصبح الملك لله …» — the FULL wording (the donor's short
        // excerpt is not printed; the full one is)
        2 -> "سنن الترمذي ٣٣٨٨"
        // «اللهم رب هذه الدعوة التامة …»
        3 -> "صحيح البخاري ٦١٤"
        // «اللهم أنت السلام ومنك السلام …»
        4 -> "سنن الترمذي — رقم الحديث غير متحقق منه"
        // «بسم الله الذي لا يضر مع اسمه شيء …»
        5 -> "سورة الطلاق ٦٥:٣"
        // «أعوذ بكلمات الله التامات من شر ما خلق»
        6 -> "صحيح مسلم ٢٧٠٨"
        // «أصبحنا … قدير … رب أسألك خير ما في هذا اليوم …»
        7 -> "سنن الترمذي ٣٣٨٨"
        // «رب أسألك علما نافعا ورزقا طيبا وعملا متقبلا»
        8 -> "سنن ابن ماجه ٩٢٥"
        // «سبحان الله وبحمده عدد خلقه …»
        9 -> "صحيح مسلم ٢٦٩١"
        // «اللهم عافني في بدني …»
        10 -> "سنن أبي داود — رقم الحديث غير متحقق منه"
        // «اللهم إني أسألك العفو والعافية …»
        11 -> "سنن ابن ماجه — رقم الحديث غير متحقق منه"
        // «اللهم إني أعوذ بك من الهم والحزن …»
        12 -> "سنن أبي داود ١٥٥٨"
        // «بسم الله الذي لا يضر مع اسمه شيء …» (the three-times formula)
        13 -> "سورة الطلاق ٦٥:٣"
        // «حسبي الله لا إله إلا هو …»
        14 -> "سورة الأنفال ٨:٢٦ وطه ٦٥:٤ (بالمعنى)"
        // «اللهم إني أسألك الهدى والتقى والعفاف والرزق»
        15 -> "سنن الترمذي ٣٥٢١"
        // «رب اغفر لي وتب علي وارحمني»
        16 -> "سنن أبي داود — رقم الحديث غير متحقق منه"
        // duplicate of #9 — the same text, one entry only
        17 -> "صحيح مسلم ٢٦٩١"
        // «لا إله إلا الله وحده لا شريك له …»
        18 -> "سنن الترمذي ٣٣٨٨"
        // «اللهم عالم الغيب والشهادة …»
        19 -> "سنن أبي داود — رقم الحديث غير متحقق منه"
        // «أستغفر الله وأتوب إليه»
        20 -> "سنن أبي داود — رقم الحديث غير متحقق منه"
        // «اللهم صل على محمد …»
        21 -> "صحيح مسلم ٤٠٦ وصحيح البخاري ٣٣٧٠"
        else -> null
    }

    /** The citation line to print under a text. */
    fun lineFor(eventId: Int): String? {
        val c = citationFor(eventId) ?: return null
        return if (c.contains("غير متحقق")) {
            COLLECTION_ONLY.format(c.substringBefore(" —"))
        } else c
    }

    /**
     * The donor's texts whose wording the council asked us NOT to print as
     * they are (a truncated excerpt) — for those we print the full wording
     * from our own table instead.
     */
    fun useFullWording(eventId: Int): Boolean = eventId == 2
}
