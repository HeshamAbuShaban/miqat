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
     *  the "now" dot on the rail). */
    val sunMark = Color(0xFFFFB25C)

    /**
     * The phase of the sky, from the REAL sunrise and sunset of the day (the
     * app knows them — that is the whole point).
     */
    fun phaseOf(nowHour: Double, sunriseHour: Double, maghribHour: Double): SkyPhase = when {
        // the night: from 40 minutes after the sunset to 40 before the sunrise
        nowHour >= maghribHour + 0.66 || nowHour <= sunriseHour - 0.66 -> SkyPhase.NIGHT
        nowHour < sunriseHour + 0.33 -> SkyPhase.DAWN
        nowHour < maghribHour - 0.33 -> SkyPhase.DAY
        else -> SkyPhase.SUNSET
    }

    /** The sky's bands, top → horizon. */
    fun bands(phase: SkyPhase, dark: Boolean): List<Color> = when {
        dark -> when (phase) {
            SkyPhase.NIGHT -> listOf(
                Color(0xFF04070F),
                Color(0xFF0B1226),
                Color(0xFF141E3C),
                Color(0xFF1E2A4C),
            )
            SkyPhase.DAWN -> listOf(
                Color(0xFF050912),
                Color(0xFF101B36),
                Color(0xFF24304A),
                Color(0xFF7A5A42),
            )
            SkyPhase.DAY -> listOf(
                Color(0xFF0B1226),
                Color(0xFF18244A),
                Color(0xFF27365C),
                Color(0xFF41527C),
            )
            SkyPhase.SUNSET -> listOf(
                Color(0xFF080D1E),
                Color(0xFF182348),
                Color(0xFF5A4560),
                Color(0xFF8A5533),
            )
        }
        else -> when (phase) {
            SkyPhase.NIGHT -> listOf(
                Color(0xFF1B2540),
                Color(0xFF33436B),
                Color(0xFF5A6C94),
                Color(0xFF8A93B0),
            )
            SkyPhase.DAWN -> listOf(
                Color(0xFF8FA6C8),
                Color(0xFFBFC9DA),
                Color(0xFFE7D9C4),
                Color(0xFFF0C79A),
            )
            SkyPhase.DAY -> listOf(
                Color(0xFF9DB8D8),
                Color(0xFFBDD0E6),
                Color(0xFFDDE5EF),
                Color(0xFFE9E6DC),
            )
            SkyPhase.SUNSET -> listOf(
                Color(0xFF6E7FA8),
                Color(0xFFB39BA6),
                Color(0xFFE8B489),
                Color(0xFFF2CE9A),
            )
        }
    }

    /** The ground below the horizon. */
    fun ground(phase: SkyPhase, dark: Boolean): Color = when {
        dark -> when (phase) {
            SkyPhase.NIGHT -> Color(0xFF04060D)
            SkyPhase.DAWN -> Color(0xFF070A16)
            SkyPhase.DAY -> Color(0xFF080C1A)
            SkyPhase.SUNSET -> Color(0xFF0A0A14)
        }
        else -> when (phase) {
            SkyPhase.NIGHT -> Color(0xFF6E7590)
            SkyPhase.DAWN -> Color(0xFFC7BBA6)
            SkyPhase.DAY -> Color(0xFFD5CFBE)
            SkyPhase.SUNSET -> Color(0xFFC7B49A)
        }
    }

    /** The star field's strength (the stars fade as the sky brightens). */
    fun starAlpha(phase: SkyPhase): Float = when (phase) {
        SkyPhase.NIGHT -> 1f
        SkyPhase.DAWN -> 0.45f
        SkyPhase.DAY -> 0f
        SkyPhase.SUNSET -> 0.2f
    }

    /** The horizon rim: the untravelled part of the line. */
    fun rim(phase: SkyPhase, dark: Boolean): Color = when {
        dark -> when (phase) {
            SkyPhase.NIGHT -> Color(0xFF2C3A5C)
            SkyPhase.DAWN -> Color(0xFF3A4468)
            SkyPhase.DAY -> Color(0xFF3F4E75)
            SkyPhase.SUNSET -> Color(0xFF4A4560)
        }
        else -> when (phase) {
            SkyPhase.NIGHT -> Color(0xFF9AA4C0)
            SkyPhase.DAWN -> Color(0xFFB9B6BE)
            SkyPhase.DAY -> Color(0xFFCFC4A6)
            SkyPhase.SUNSET -> Color(0xFFB89A82)
        }
    }
}
