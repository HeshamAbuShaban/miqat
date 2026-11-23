package app.miqat.ui.widget

import androidx.compose.ui.graphics.Color
import app.miqat.R
import app.miqat.ui.Gold
import app.miqat.ui.InkText
import app.miqat.ui.InkTextDim
import app.miqat.ui.NightOutline
import app.miqat.ui.NightSurface
import app.miqat.ui.NightText
import app.miqat.ui.NightTextDim
import app.miqat.ui.PaperEmber
import app.miqat.ui.PaperGold
import app.miqat.ui.PaperGround
import app.miqat.ui.PaperOutline
import app.miqat.ui.PaperSaffron
import app.miqat.ui.PaperSurface
import app.miqat.ui.PaperTeal
import app.miqat.ui.Sky
import app.miqat.ui.SkyPhase
import app.miqat.ui.Teal
import app.miqat.ui.UrgencyImminent
import app.miqat.ui.UrgencySoon

/**
 * THE WIDGET'S TWO THEMES — the app's own tokens, never a separate brown ramp.
 *
 * Dark keeps the hour's real sky and adds light ink over it. Light is the
 * app's paper engraving: `Sky.lightBands` for the hour, cream ground, ink
 * hairline, deep-gold travelled path and next tick, no stars, a small sun ring.
 */
data class WidgetPalette(
    val dark: Boolean,
    val backgroundRes: Int,
    val background: Color,
    val outline: Color,
    val text: Color,
    val dim: Color,
    val gold: Color,
    val teal: Color,
    val urgencySoon: Color,
    val urgencyImminent: Color,
    val ground: Color,
    val arc: Color,
    val tick: Color,
    val sun: Color,
    val lightSkyBands: Map<SkyPhase, List<Color>>,
) {
    fun skyBands(phase: SkyPhase): List<Color> =
        if (dark) Sky.bands(phase) else lightSkyBands.getValue(phase)

    companion object {
        val DARK = WidgetPalette(
            dark = true,
            backgroundRes = R.drawable.widget_bg,
            background = NightSurface,
            outline = NightOutline,
            text = NightText,
            dim = NightTextDim,
            gold = Gold,
            teal = Teal,
            urgencySoon = UrgencySoon,
            urgencyImminent = UrgencyImminent,
            ground = Sky.ground,
            arc = Sky.rim,
            tick = Sky.tick,
            sun = Sky.sunMark,
            lightSkyBands = SkyPhase.entries.associateWith { Sky.lightBands(it) },
        )

        val LIGHT = WidgetPalette(
            dark = false,
            backgroundRes = R.drawable.widget_bg_light,
            background = PaperSurface,
            outline = PaperOutline,
            text = InkText,
            dim = InkTextDim,
            gold = PaperGold,
            teal = PaperTeal,
            urgencySoon = PaperSaffron,
            urgencyImminent = PaperEmber,
            ground = PaperGround,
            arc = InkText,
            tick = InkTextDim,
            sun = PaperGold,
            lightSkyBands = SkyPhase.entries.associateWith { Sky.lightBands(it) },
        )

        fun of(darkTheme: Boolean): WidgetPalette = if (darkTheme) DARK else LIGHT
    }
}
