package app.miqat.core

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Qibla (S4 core): great-circle bearing to the Kaaba (21.4225 N, 39.8262 E),
 * pure math — the compass sensor layer lands later; this object stays
 * JVM-testable (fixture: Makkah itself → 0°, Cairo ≈ 138.4°, NW of Kaaba → ~359°).
 */
object Qibla {

    const val KAABA_LAT = 21.4225
    const val KAABA_LON = 39.8262

    /** Initial great-circle bearing (degrees from true north, 0..360). */
    fun bearing(lat: Double, lon: Double): Double {
        val dLon = Math.toRadians(KAABA_LON - lon)
        val y = sin(dLon) * cos(Math.toRadians(KAABA_LAT))
        val x = cos(Math.toRadians(lat)) * Math.toRadians(KAABA_LAT) -
            sin(Math.toRadians(lat)) * cos(Math.toRadians(KAABA_LAT)) * cos(dLon)
        val brng = Math.toDegrees(atan2(y, x))
        return (brng + 360.0) % 360.0
    }

    /** Great-circle distance to the Kaaba, km. */
    fun distanceKm(lat: Double, lon: Double): Double {
        val r = 6371.0
        val p1 = Math.toRadians(lat)
        val p2 = Math.toRadians(KAABA_LAT)
        val dp = Math.toRadians(KAABA_LAT - lat)
        val dl = Math.toRadians(KAABA_LON - lon)
        val a = sin(dp / 2) * sin(dp / 2) +
            cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * r * atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}
