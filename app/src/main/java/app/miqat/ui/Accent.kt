package app.miqat.ui

/**
 * THE ACCENT — the identity the user chooses (the founder, 2026-11-11:
 * «أنا أرى أنك تتيح تغيير الألوان من الإعدادات لمزيد»).
 *
 * The palette's MEANINGS never move: saffron is always "now", teal is always
 * "a window", cream is always "an occasion". Only the "ours / chosen" role —
 * the brand, the active destination, the selection — takes the accent, so a
 * different accent re-skins the whole app without breaking the legend.
 */
enum class Accent(
    val labelAr: String,
    /** the dark scheme's "ours" */
    val dark: Long,
    val darkOn: Long,
    val darkContainer: Long,
    val darkOnContainer: Long,
    /** the light scheme's "ours" */
    val light: Long,
    val lightOn: Long,
    val lightContainer: Long,
    val lightOnContainer: Long,
) {
    GOLD(
        "الذهبي", 0xFFE8C46A, 0xFF241C00, 0xFF16213F, 0xFFE8C46A,
        0xFF6F5610, 0xFFFFFFFF, 0xFFDEE6F4, 0xFF2A3A5C,
    ),
    TEAL(
        "الفيروزي", 0xFF7FD0C8, 0xFF00312C, 0xFF10302F, 0xFF7FD0C8,
        0xFF16655D, 0xFFFFFFFF, 0xFFD9EDE9, 0xFF133F3A,
    ),
    EMERALD(
        "الزمردي", 0xFF6FD3A0, 0xFF0C2A1C, 0xFF123226, 0xFF6FD3A0,
        0xFF2C6B4C, 0xFFFFFFFF, 0xFFDCEFE1, 0xFF1B4432,
    ),
    ;

    companion object {
        fun of(name: String?): Accent =
            entries.firstOrNull { it.name == name } ?: GOLD
    }
}

/**
 * THE HERO'S ILLUSTRATION — the app's one rich moment, in more than one form
 * (the founder: «حتى الرسومات التوضيحية … يمكن التبديل إليها في الإعدادات»).
 *
 *  - [ARC] — the horizon: the quadratic arc, the sun on it, the gold travelled
 *    path. The signature.
 *  - [DIAL] — the dial: the day as a circle, the prayers as marks around it,
 *    the sun turning. A quieter, more "instrument" reading.
 */
enum class HeroStyle(val labelAr: String, val noteAr: String) {
    ARC("القوس", "أفقُ اليوم"),
    DIAL("الحلقة", "اليومُ دائرةً"),

    ;

    companion object {
        fun of(name: String?): HeroStyle =
            entries.firstOrNull { it.name == name } ?: ARC
    }
}
