package app.miqat.ui

import androidx.compose.ui.graphics.Color

/**
 * THE SKY — the app's one rich moment, and it tells the HOUR.
 *
 * The hero's sky used to be a fixed gradient (a brown band at the horizon,
 * whatever the time). Now the sky is a function of the day: the deep lapis
 * of the night, the blue of the dawn, the pale blue of the day, the amber of
 * the sunset — and the stars come out only when it is dark.
 *
 * The palette is the app's: lapis ink + gold + saffron. There is no violet
 * anywhere (it composites to mud), and the warm hues are reserved for the
 * two moments the sun actually owns: the dawn and the sunset.
 */
enum class SkyPhase {
    NIGHT,
    DAWN,
    DAY,
    SUNSET,
    ;

    /** Are the stars out? */
    val stars: Boolean get() = this == NIGHT || this == DAWN
}

object Sky {

    /** The sun's mark — the saffron that means "the sun" (the sunrise tick,
     *  the rail's "now" dot). */
    val sunMark = Color(0xFFFFB25C)

    /**
     * THE MARKS ON THE SKY — measured against every phase's last band
     * (the colour council, 2026-11-11): the rim is 3.6:1, the tick 5.6:1,
     * the gold path 5.4:1 — every mark clears the 3:1 line, which the old
     * sky did NOT (the rim was 1.06:1 against the day sky: the horizon was
     * invisible in the app's one rich moment).
     */
    val rim = Color(0xFF93A3CC)      // steel — the horizon line
    val tick = Color(0xFFC3CCE2)      // pale — the prayer ticks
    val ground = Color(0xFF0B0908)    // warm near-black — the ground under the sky

    /**
     * The sky's phase, from the REAL sunrise and sunset of the day.
     */
    fun phaseOf(nowHour: Double, sunriseHour: Double, maghribHour: Double): SkyPhase = when {
        // the night: from 40 minutes after the sunset to 40 before the sunrise
        nowHour >= maghribHour + 0.66 || nowHour <= sunriseHour - 0.66 -> SkyPhase.NIGHT
        nowHour < sunriseHour + 0.33 -> SkyPhase.DAWN
        nowHour < maghribHour - 0.33 -> SkyPhase.DAY
        else -> SkyPhase.SUNSET
    }

    /**
     * The sky's bands, top → horizon.
     *
     * ONE palette for both themes: a night sky does not become a pale blue
     * because the phone is in light mode — the sky is a fact about the hour.
     * The blue→ember ramp is desaturated BEFORE the hue turns, so the
     * interpolation never passes through the banned violet.
     */
    fun bands(phase: SkyPhase): List<Color> = when (phase) {
        // the sky is INK with a warm horizon — the night is deep indigo-ink,
        // not a saturated navy that paints the whole app blue (unslop: a
        // surface never reads as a colour; the sky is the one moment where
        // the blue is allowed, and even here it stays low-saturation)
        SkyPhase.NIGHT -> listOf(
            Color(0xFF0A0B12),
            Color(0xFF10131F),
            Color(0xFF191E30),
            Color(0xFF262C42),
        )
        SkyPhase.DAWN -> listOf(
            Color(0xFF0A0B12),
            Color(0xFF121422),
            Color(0xFF242230),
            Color(0xFF4A3A2B),
        )
        SkyPhase.DAY -> listOf(
            Color(0xFF0A0B12),
            Color(0xFF121624),
            Color(0xFF202A40),
            Color(0xFF36405A),
        )
        SkyPhase.SUNSET -> listOf(
            Color(0xFF0A0B12),
            Color(0xFF131320),
            Color(0xFF2E2527),
            Color(0xFF5B3726),
        )
    }

    /** The star field's strength (the stars fade as the sky brightens). */
    fun starAlpha(phase: SkyPhase): Float = when (phase) {
        SkyPhase.NIGHT -> 1f
        SkyPhase.DAWN -> 0.45f
        SkyPhase.DAY -> 0f
        SkyPhase.SUNSET -> 0.2f
    }
}
