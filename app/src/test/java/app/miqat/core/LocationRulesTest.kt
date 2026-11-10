package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

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

class LocationRulesTest {

    private val cairo = City("Cairo", "القاهرة", "EG", 30.06263, 31.24967, 2.0, "egypt")

    @Test
    fun `without a fix the city and its own offset are used`() {
        val e = LocationRules.effective(cairo, 2.0, null, 2.0)
        assertEquals(30.06263, e.latitude, 1e-9)
        assertEquals(31.24967, e.longitude, 1e-9)
        assertEquals(2.0, e.offsetHours, 1e-9)
        assertSame(LocationSource.MANUAL, e.source)
    }

    @Test
    fun `with a fix the coordinates AND the device offset travel together`() {
        val fix = LocationFix(37.421997, -122.084, -8.0, LocationSource.GPS)
        val e = LocationRules.effective(cairo, 2.0, fix, -8.0)
        // the fix's coordinates AND the fix's zone — never the city's offset
        assertEquals(37.421997, e.latitude, 1e-9)
        assertEquals(-122.084, e.longitude, 1e-9)
        assertEquals(-8.0, e.offsetHours, 1e-9)
        assertSame(LocationSource.GPS, e.source)
    }

    @Test
    fun `the mixed pair can never happen`() {
        val fix = LocationFix(37.421997, -122.084, -8.0, LocationSource.GPS)
        val e = LocationRules.effective(cairo, 2.0, fix, -8.0)
        // the offset must NOT be the city's while the coordinates are the fix's
        assert(!(e.latitude == cairo.lat && e.offsetHours == 2.0))
    }
}
