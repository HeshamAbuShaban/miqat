package app.miqat.core

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The compass (S4) — a REAL heading, not a number.
 *
 * The screen's job is to point: the phone's own magnetometer gives the
 * magnetic heading, the local magnetic declination turns it into a TRUE north
 * heading (the qibla bearing is a true-north bearing), and the qibla needle is
 * the difference between the two. A low-pass filter keeps it steady, and a
 * three-degree lock is the moment of alignment the user feels.
 *
 * On a device without a magnetometer (or an emulator) the compass says so
 * plainly instead of pretending — the qibla bearing and the distance are still
 * exact.
 */
class CompassSensor(private val context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val accelerometer: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val magnetometer: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    /** True when the device can actually sense a heading. */
    val available: Boolean
        get() = sensorManager != null && accelerometer != null && magnetometer != null

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    /** The smoothed true-north heading, in degrees (0 = north). */
    var heading: Float = 0f
        private set

    private var started = false

    /** Called with the new true-north heading whenever it changes. */
    var onHeading: ((Float) -> Unit)? = null

    fun start() {
        if (started || !available) return
        started = true
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager?.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        if (!started) return
        started = false
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravity, 0, 3)
                hasGravity = true
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                hasGeomagnetic = true
            }
        }
        if (!hasGravity || !hasGeomagnetic) return

        val rotation = FloatArray(9)
        if (!SensorManager.getRotationMatrix(rotation, null, gravity, geomagnetic)) return
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotation, orientation)

        // the magnetic azimuth, then the local declination → true north
        val magnetic = Math.toDegrees(orientation[0].toDouble()).toFloat()
        val declination = declinationDegrees()
        val trueNorth = (magnetic + declination + 360f) % 360f

        heading = smooth(heading, trueNorth)
        onHeading?.invoke(heading)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /** The local magnetic declination (true north − magnetic north). */
    fun declinationDegrees(): Float {
        val fix = lastFix ?: return 0f
        val field = GeomagneticField(
            fix.latitude.toFloat(), fix.longitude.toFloat(), fix.altitude.toFloat(),
            System.currentTimeMillis(),
        )
        return field.declination
    }

    /** The city the fix belongs to (set by the caller). */
    data class Fix(val latitude: Double, val longitude: Double, val altitude: Double = 0.0)
    var lastFix: Fix? = null

    /** Low-pass the heading, taking the short way round the circle. */
    private fun smooth(current: Float, target: Float): Float {
        if (current == 0f && target == 0f) return target
        var delta = (target - current) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return (current + delta * 0.15f + 360f) % 360f
    }

    companion object {
        /** The angle of the qibla needle relative to the phone's heading. */
        fun needleAngle(qiblaBearing: Float, heading: Float): Float =
            ((qiblaBearing - heading) % 360f + 360f) % 360f

        /** Is the needle aligned (the lock)? */
        fun aligned(angle: Float, tolerance: Float = 3f): Boolean =
            abs(((angle + 180f) % 360f) - 180f) <= tolerance

        /** The great-circle bearing to the Kaaba, in degrees (true north). */
        fun qiblaBearing(latitude: Double, longitude: Double): Float {
            val kaabaLat = Math.toRadians(21.4225)
            val kaabaLon = Math.toRadians(39.8262)
            val lat = Math.toRadians(latitude)
            val lon = Math.toRadians(longitude)
            val dLon = kaabaLon - lon
            val y = sin(dLon) * cos(kaabaLat)
            val x = cos(lat) * sin(kaabaLat) - sin(lat) * cos(kaabaLat) * cos(dLon)
            val deg = Math.toDegrees(atan2(y, x))
            return (((deg % 360.0) + 360.0) % 360.0).toFloat()
        }

        /** The distance to the Kaaba, km (haversine). */
        fun qiblaDistanceKm(latitude: Double, longitude: Double): Double {
            val r = 6371.0
            val lat1 = Math.toRadians(latitude)
            val lat2 = Math.toRadians(21.4225)
            val dLat = lat2 - lat1
            val dLon = Math.toRadians(39.8262 - longitude)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
            return 2 * r * atan2(sqrt(a), sqrt(1 - a))
        }
    }
}
