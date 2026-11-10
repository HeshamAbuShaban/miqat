package app.miqat.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat

/**
 * Where the app's coordinates come from — and how much the user should trust
 * the times that follow (Hesham's brief: «المعيار الذهبي هي قيم الانترنت و قيم
 * الموقع؛ فإذا غاب هذا ورفض المستخدم، ينبّه ويحذّر ويَعتمد إلى الحساب
 * المتوقَّع»).
 *
 * Two honest states, never hidden:
 *  - GPS      — the fix came from the device's location; the times follow it.
 *  - MANUAL   — the coordinates are the ones the user picked from the bundled
 *               city database; the times are computed from them, but the
 *               place itself is NOT confirmed (the user may be travelling).
 *
 * There is no "online times" mode: the app computes locally and never phones
 * home (offline-first, zero telemetry). What replaces it is VERIFICATION —
 * comparing the computed times against an official timetable the user brings
 * (research-agenda R3.3) — which lands with the reconciliation screen.
 */
enum class LocationSource(val labelAr: String) {
    GPS("موقعي (نظام الموقع)"),
    MANUAL("مدينة مختارة"),
}

/** How confident the app may be about the resulting times. */
enum class TimeConfidence(val labelAr: String, val warn: String?) {
    CONFIRMED("مؤكد", null),
    ESTIMATED(
        "تقديري",
        "موقعك غير مُتحقَّق منه — الحساب مبني على المدينة المختارة وقد لا يطابق موقعك الفعلي.",
    ),
}

data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val utcOffsetHours: Double,
    val source: LocationSource,
) {
    val confidence: TimeConfidence
        get() = if (source == LocationSource.GPS) TimeConfidence.CONFIRMED else TimeConfidence.ESTIMATED
}

/** Reads a device location fix — Android's own LocationManager (no Play Services,
 *  so the tree stays F-Droid clean). Returns null when the permission is
 *  missing or no fix is available; the caller then falls back, with a warning. */
object LocationProvider {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

    /** The freshest last-known fix, or null. */
    fun lastKnown(context: Context): LocationFix? {
        if (!hasPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        var best: android.location.Location? = null
        for (p in providers) {
            try {
                val loc = lm.getLastKnownLocation(p) ?: continue
                if (best == null || loc.time > best.time) best = loc
            } catch (_: SecurityException) {
                // permission revoked mid-flight — fall back silently
            } catch (_: IllegalArgumentException) {
                // provider not present on this device
            }
        }
        val loc = best ?: return null
        val tz = java.util.TimeZone.getDefault()
        val offset = tz.getOffset(loc.time).toDouble() / 3_600_000.0
        return LocationFix(loc.latitude, loc.longitude, offset, LocationSource.GPS)
    }
}
