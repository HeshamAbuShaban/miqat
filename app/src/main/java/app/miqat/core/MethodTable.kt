package app.miqat.core

/**
 * Prayer calculation method table — data-driven, mirroring the donor's string-keyed
 * methods (teardown §2) with margins measured against the donor's own official
 * timetable corpus (docs/engine-parity.md).
 *
 * `fajrAngle` / `ishaAngle` are sun depression degrees; `ishaMinutes` (when set)
 * replaces the isha angle with a fixed offset after maghrib (donor: makkah/qatar = 90).
 * Margins are ihtiyat minutes applied on top of the pure solar math, verified on the
 * donor `staticprayertimes` corpus.
 */
data class Method(
    val id: String,
    val fajrAngle: Double,
    val ishaAngle: Double?,
    val ishaMinutes: Int? = null,
    val maghribMinutes: Int? = null, // angle-based maghrib if null
    val margins: Margins = Margins(),
)

data class Margins(
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0,
)

object MethodTable {
    // VERIFY markers: methods not yet parity-verified against donor corpus (S1 ladder).
    private val table = mapOf(
        "emirates" to Method("emirates", 18.2, 18.2,
            margins = Margins(sunrise = -2, dhuhr = 2, asr = 2, maghrib = 3)),
        "makkah" to Method("makkah", 18.5, null, ishaMinutes = 90),
        "qatar" to Method("qatar", 18.0, null, ishaMinutes = 90),
        "mwl" to Method("mwl", 18.0, 17.0),
        "isna" to Method("isna", 15.0, 15.0),
        "egypt" to Method("egypt", 19.5, 17.5),
        "karachi" to Method("karachi", 18.0, 18.0),
        "kuwait" to Method("kuwait", 18.0, 17.5),
        "dubai" to Method("dubai", 18.2, 18.2),
        "turkey" to Method("turkey", 18.0, 17.0),
        "malaysia" to Method("malaysia", 20.0, 18.0),
        "singapore" to Method("singapore", 20.0, 18.0),
        "tehran" to Method("tehran", 17.7, 14.0, maghribMinutes = 4),
        "libya" to Method("libya", 18.5, 18.5),
        "tunisia" to Method("tunisia", 18.0, 18.0),
        "algeria" to Method("algeria", 18.0, 17.0),
        "jordan" to Method("jordan", 18.0, 17.0),
        "syria" to Method("syria", 15.5, 15.5),
        "iraq" to Method("iraq", 19.5, 17.5),
        "france" to Method("france", 18.0, 18.0),
    )

    operator fun get(id: String): Method =
        table[id] ?: table.getValue("makkah")

    fun ids(): Set<String> = table.keys
}
