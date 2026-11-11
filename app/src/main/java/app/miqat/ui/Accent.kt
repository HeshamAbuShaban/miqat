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
        "الذهبي", 0xFFE8C46A, 0xFF241C00, 0xFF2C2518, 0xFFF0DCA0,
        0xFF6F5610, 0xFFFFFFFF, 0xFFF0E7CE, 0xFF4A3A10,
    ),
    TEAL(
        "الفيروزي", 0xFF7FD0C8, 0xFF00312C, 0xFF142A27, 0xFFB8EDE6,
        0xFF1F6A62, 0xFFFFFFFF, 0xFFDDEDE8, 0xFF123F3A,
    ),
    EMERALD(
        "الزمردي", 0xFF6FD3A0, 0xFF0C2A1C, 0xFF15291E, 0xFFB9EBCB,
        0xFF2C6B4C, 0xFFFFFFFF, 0xFFE1EEE3, 0xFF1B4432,
    ),
    ;

    companion object {
        fun of(name: String?): Accent =
            entries.firstOrNull { it.name == name } ?: GOLD
    }
}
