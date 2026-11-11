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

val Gold = Color(0xFFE8C46A)          // the wordmark gold — primary accent
val Saffron = Color(0xFFFFB25C)       // the live moment (the sun, the urgency)
val Lapis = Color(0xFF101B36)         // the sky's mid-band — lapis ink, never violet
val Cream = Color(0xFFF8E7AE)
val Amber = Color(0xFFFFB25C)         // "the moment" colour (next prayer)
val Teal = Color(0xFF7FD0C8)
val Sunset = Color(0xFFFF7A59)

// urgency (the app reacts as the appointed time approaches — «اقترب الموعد»)
val UrgencySoon = Color(0xFFFFB25C)     // saffron — the same token as the sun
val UrgencyImminent = Color(0xFFFF7A59) // sunset — warmer, louder, still calm

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
val NightBg = Color(0xFF141210)      // warm ink — the page
val NightSurface = Color(0xFF1C1A18) // the card, one quiet step up
val NightSurfaceHigh = Color(0xFF262421) // raised: chips, the active tab
val NightOutline = Color(0xFF322F2B) // the hairline — onSurface at ~10%
val NightText = Color(0xFFEFE9DE)    // warm off-white
val NightTextDim = Color(0xFFAAA398) // warm grey

// light scheme (paper, not white — the brand reads as ink on warm paper)
val PaperBg = Color(0xFFF7F4EE)      // dawn paper
val PaperSurface = Color(0xFFFDFBF7)
val PaperSurfaceHigh = Color(0xFFEFE9DF)
val PaperOutline = Color(0xFFD9D1C2) // the hairline
val InkText = Color(0xFF1F1B16)
val InkTextDim = Color(0xFF5E554A)

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
    outline = NightOutline,
    outlineVariant = Color(0xFF2A2724),
    // no red in this palette: doubt is dim + a chip (DESIGN.md §2)
    error = Color(0xFFEFE9DE),
    onError = Color(0xFF141210),
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
val Cairo = FontFamily(
    Font(R.font.cairo_light, FontWeight.Light),
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_medium, FontWeight.Medium),
    Font(R.font.cairo_bold, FontWeight.Bold),
)

/** Tabular figures — a ticking clock must not reflow its neighbours
 *  (council: use `tnum` with Arabic-Indic digits). */
val TabularFigures = "tnum"

private val MiqatType = Typography(
    displayLarge = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
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
