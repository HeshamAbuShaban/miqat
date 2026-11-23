package app.miqat.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miqat.R

/**
 * The Miqat design system (docs/design-brief.md §2 — «قوس اليوم»).
 *
 * ONE source of truth for colour, type and spacing; screens never hardcode a
 * colour literal. Cairo is the Arabic display/body face (SIL OFL, bundled in
 * res/font — from the donor's own assets, licence permitting).
 *
 * The palette follows the prayer of the moment: night blue → dawn → amber →
 * sunset → deep navy. Contrast pairs are checked for WCAG AA on both schemes.
 */

// --- brand palette ---------------------------------------------------------

val Gold = Color(0xFFD4A857)          // the wordmark gold — primary accent
val Saffron = Color(0xFFF0C875)       // the live moment (the sun, the urgency)
val Lapis = Color(0xFF101B36)         // the sky's mid-band — lapis ink, never violet
val Cream = Color(0xFFF8E7AE)
val Amber = Color(0xFFF0C875)         // "the moment" colour (next prayer)
val Teal = Color(0xFF5CC4B8)
val Sunset = Color(0xFFF06544)

// urgency (the app reacts as the appointed time approaches — «اقترب الموعد»)
val UrgencySoon = Color(0xFFF06544)     // saffron — the same token as the sun
val UrgencyImminent = Color(0xFFFF8566) // sunset — warmer, louder, still calm

/** Radius scale (nested radii add up): chips 6, controls 10, rows 12, cards 14,
 *  panels 18, the hero 20. Nothing is a pill. */
object Radius {
    val chip = 6.dp
    val control = 10.dp
    val row = 12.dp
    val card = 14.dp
    val panel = 18.dp
    val hero = 20.dp
}

// dark scheme — the WARM ink ramp (unslop, 2026-11-12): the chrome is near-
// black graphite with a warm bias, so the surface never reads as a colour.
// The blue lives ONLY in the sky (the hero's moment) — a dashboard-wide blue
// cast was the app's worst tell.
val NightBg = Color(0xFF0A0D18)      // the page — MERIDIAN night
val NightSurface = Color(0xFF161A28) // the card, one quiet step up
val NightSurfaceHigh = Color(0xFF1E2438) // raised: chips, the active tab
val NightOutline = Color(0xFF262F48) // the hairline — onSurface at ~10%
val NightOutlineVariant = Color(0xFF2A2724) // the faint hairline inside a card
val NightText = Color(0xFFEDEBE3)    // the ink ramp, cold-off-white
val NightTextDim = Color(0xFF9AA0B8) // dim text, inactive nav, citations (design-direction)

// light scheme (paper, not white — the brand reads as ink on warm paper)
val PaperBg = Color(0xFFF7F4EE)      // dawn paper
val PaperSurface = Color(0xFFFDFBF7)
val PaperSurfaceHigh = Color(0xFFEFE9DF)
val PaperOutline = Color(0xFFD9D1C2) // the hairline
val InkText = Color(0xFF1F1B16)
val InkTextDim = Color(0xFF5E554A)

/**
 * THE PAPER INK RAMP — the light scheme's equivalents of the fixed dark
 * tokens. They are the design-direction's cream/deep-gold translated to the
 * hero's sky, the fast bar and the compass: deep gold reads where the dark
 * scheme uses bright gold, ember replaces sunset, and the faint grey is warm
 * enough to sit on cream without disappearing.
 */
val PaperGold = Color(0xFF6F5610)
val PaperWarm = Color(0xFF8A6410)
val PaperSaffron = Color(0xFF96500A)
val PaperEmber = Color(0xFFB3261E)
val PaperTeal = Color(0xFF1F6A62)
val PaperFaint = Color(0xFF645C4F)
val PaperGround = Color(0xFFEFE9DF)
val PaperTrack = Color(0xFFD9D1C2)
val PaperTrackHigh = Color(0xFFE7E0D3)
val PaperFastMid = Color(0xFFF7F1E6)
val PaperFastEnd = Color(0xFFF1E8DA)
val PaperMint = Color(0xFF86C9BE)
val PaperSunCore = Color(0xFFF0C875)
val PaperSunHighlight = Color(0xFFFFF2C8)

/**
 * Ink for the app's FIXED DARK MOMENTS — the sky, the fast bar, the day-rail
 * ground. Those surfaces are a fact about the hour (the reference keeps the
 * night sky dark in every theme), so they do NOT flip with the light scheme.
 * Their text must therefore stay light too. Before these tokens the hero's
 * countdown used `scheme.onSurface`: in light mode that is dark ink on a dark
 * sky — the whole hero went unreadable. A fixed surface gets fixed ink.
 */
val OnDark = Color(0xFFEBE8E0)
val OnDarkDim = Color(0xFFC3CCE2)
val OnDarkFaint = Color(0xFF7A8096)
val OnDarkWarm = Color(0xFFEBD9A8)
val OnDarkTrack = Color(0xFF2A3145)

/** True when the active scheme is paper (light) rather than ink (dark). */
@Composable
fun isLightTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() > 0.5f

/** The page's wash — the only surface that legitimately follows the theme. */
@Composable
fun pageWash(): List<Color> = if (isLightTheme()) {
    listOf(PaperBg, Color(0xFFEFE9DF))
} else {
    listOf(NightBg, NightBg)
}

/** The dark scheme for a chosen accent — only the "ours" role moves. */
private fun darkColors(accent: Accent) = darkColorScheme(
    primary = Color(accent.dark),
    onPrimary = Color(accent.darkOn),
    // the containers are warm and dark (the accent's own deep tone)
    primaryContainer = Color(accent.darkContainer),
    onPrimaryContainer = Color(accent.darkOnContainer),
    secondary = Teal,
    onSecondary = Color(0xFF00312C),
    tertiary = Amber,
    onTertiary = Color(0xFF3A2500),
    tertiaryContainer = Color(0xFF2C2518),
    onTertiaryContainer = Cream,
    background = NightBg,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = NightTextDim,
    // the card edge is a HAIRLINE (onSurface at ~10%), not a coloured stroke:
    // the old #33426B drew a blue wireframe around every card
    outline = Color(0xFF262F48),
    outlineVariant = NightOutlineVariant,
    // no red in this palette: doubt is dim + a chip (DESIGN.md §2)
    error = Color(0xFFF06544),
    onError = Color(0xFF0A0D18),
)

/** The light scheme for a chosen accent. */
private fun lightColors(accent: Accent) = lightColorScheme(
    primary = Color(accent.light),
    onPrimary = Color(accent.lightOn),
    primaryContainer = Color(accent.lightContainer),
    onPrimaryContainer = Color(accent.lightOnContainer),
    secondary = Color(0xFF1F6A62),
    onSecondary = Color(0xFFFFFFFF),
    // the saffron, darkened for paper: #B4600C failed AA as body text (4.43)
    tertiary = Color(0xFF96500A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF1E7D6),
    onTertiaryContainer = Color(0xFF4A2A00),
    background = PaperBg,
    onBackground = InkText,
    surface = PaperSurface,
    onSurface = InkText,
    surfaceVariant = PaperSurfaceHigh,
    onSurfaceVariant = InkTextDim,
    outline = PaperOutline,
    outlineVariant = Color(0xFFE7E0D3),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
)

/** Cairo — Arabic first, generous x-height, good at large display sizes. */
/** Noto Kufi — the display face (countdowns, numbers). Bundled, never CDN. */
val Kufi = FontFamily(
    Font(R.font.notokufiarabic_regular, FontWeight.Normal),
    Font(R.font.notokufiarabic_semibold, FontWeight.SemiBold),
    Font(R.font.notokufiarabic_bold, FontWeight.Bold),
)

/** Cairo — kept for numerals: this bundled file has every Arabic-Indic
 *  digit, unlike the Noto Kufi subset (which lacks the zero glyph). */
val Cairo = FontFamily(
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_medium, FontWeight.Medium),
    Font(R.font.cairo_bold, FontWeight.Bold),
)

/** IBM Plex Sans Arabic — the interface body. Bundled, never CDN. */
val Plex = FontFamily(
    Font(R.font.ibmplexsansarabic_regular, FontWeight.Normal),
    Font(R.font.ibmplexsansarabic_medium, FontWeight.Medium),
    Font(R.font.ibmplexsansarabic_semibold, FontWeight.SemiBold),
)

/** Tabular figures — a ticking clock must not reflow its neighbours
 *  (council: use `tnum` with Arabic-Indic digits). */
val TabularFigures = "tnum"

private val MiqatType = Typography(
    displayLarge = TextStyle(fontFamily = Kufi, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = Kufi, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = Kufi, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = Kufi, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = Kufi, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Kufi, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Plex, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Plex, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = Plex, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Plex, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Plex, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Plex, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Plex, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Plex, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Plex, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

/** Spacing scale — one rhythm for the whole app (no ad-hoc paddings). */
object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

@Composable
fun MiqatTheme(
    dark: Boolean = isSystemInDarkTheme(),
    accent: Accent = Accent.GOLD,
    content: @Composable () -> Unit,
) {
    // The app is ARABIC-FIRST: the layout direction is RTL regardless of the
    // device locale. Without this, an en-US device renders the Arabic UI
    // mirrored (prayer order reversed, nav bar wrong) — the "mixed language"
    // slop Hesham called out. Direction is a property of the PRODUCT.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = if (dark) darkColors(accent) else lightColors(accent),
            typography = MiqatType,
            content = content,
        )
    }
}
