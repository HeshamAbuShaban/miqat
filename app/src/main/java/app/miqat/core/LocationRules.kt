package app.miqat.core

/**
 * THE LOCATION RULE — the coordinates and the UTC offset must ALWAYS come from
 * the same source.
 *
 * The bug this guards (found on the device, 2026-11-10): a GPS fix from one
 * place (the emulator's default fix) was combined with the picked city's
 * offset, so the times were for neither place — Fajr at 15:05, and a
 * "20 minutes to Fajr" that meant nothing.
 */
object LocationRules {

    /** The place the times are computed from, with its own offset. */
    data class Effective(
        val latitude: Double,
        val longitude: Double,
        val offsetHours: Double,
        val source: LocationSource,
    )

    /**
     * @param city the picked city
     * @param cityOffsetHours the city's DST-correct offset at that moment
     * @param gps the device fix, when the user allowed the GPS
     * @param deviceOffsetHours the device's own offset (the zone of a fix)
     */
    fun effective(
        city: City,
        cityOffsetHours: Double,
        gps: LocationFix?,
        deviceOffsetHours: Double,
    ): Effective = if (gps != null) {
        // the fix brings its own coordinates AND its own zone
        Effective(gps.latitude, gps.longitude, deviceOffsetHours, LocationSource.GPS)
    } else {
        Effective(city.lat, city.lon, cityOffsetHours, LocationSource.MANUAL)
    }
}
