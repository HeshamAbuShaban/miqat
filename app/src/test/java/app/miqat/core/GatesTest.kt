package app.miqat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Layer-0 gates (the advisory council's charge): the app must never claim
 * more certainty than it has.
 */
class GatesTest {

    @Test
    fun `the qualifier rides every window-bound payload`() {
        // the chip exists and says what it must say
        assertEquals("تقديري بحسب الاصطلاح", NotificationPoster.ESTIMATED)
    }

    @Test
    fun `confidence follows the source of the time`() {
        // the user adopted their city's official timetable → confirmed
        assertEquals(
            TimeConfidence.CONFIRMED,
            Confidence.of("makkah", officialAdopted = true, highLatitude = false),
        )
        // a published method → computed
        assertEquals(
            TimeConfidence.COMPUTED,
            Confidence.of("makkah", officialAdopted = false, highLatitude = false),
        )
        assertEquals(
            TimeConfidence.COMPUTED,
            Confidence.of("egypt", officialAdopted = false, highLatitude = false),
        )
        // a local estimate → estimated
        assertEquals(
            TimeConfidence.ESTIMATED,
            Confidence.of("windsor", officialAdopted = false, highLatitude = false),
        )
        // a high-latitude place is never more than estimated
        assertEquals(
            TimeConfidence.ESTIMATED,
            Confidence.of("makkah", officialAdopted = false, highLatitude = true),
        )
    }

    @Test
    fun `the confidence words are plain and honest`() {
        assertTrue(TimeConfidence.CONFIRMED.noteAr.contains("معتمد"))
        assertTrue(TimeConfidence.ESTIMATED.noteAr.contains("قد يختلف"))
    }

    @Test
    fun `the location rule never mixes a place with another place's zone`() {
        val cairo = City("Cairo", "القاهرة", "EG", 30.06, 31.25, 2.0, "egypt")
        val fix = LocationFix(37.42, -122.08, -8.0, LocationSource.GPS)
        val e = LocationRules.effective(cairo, 2.0, fix, -8.0)
        assertEquals(37.42, e.latitude, 1e-9)
        assertEquals(-8.0, e.offsetHours, 1e-9)
        assertFalse(e.latitude == cairo.lat && e.offsetHours == 2.0)
    }
}
