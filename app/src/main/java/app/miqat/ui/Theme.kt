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
val Plum = Color(0xFF2A1B33)          // the night
val GoldDim = Color(0xFF8A7530)
val Cream = Color(0xFFF8E7AE)
val Amber = Color(0xFFFFB25C)         // "the moment" colour (next prayer)
val Teal = Color(0xFF7FD0C8)
val Blue = Color(0xFF5A74E8)
val Sunset = Color(0xFFFF7A59)
val Crimson = Color(0xFFE5484D)

// urgency (the app reacts as the appointed time approaches — «اقترب الموعد»)
val UrgencySoon = Color(0xFFFFB74D)     // amber — the same gold as the brand
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

// dark scheme
val NightBg = Color(0xFF0A0D1C)
val NightSurface = Color(0xFF12172B)
val NightSurfaceHigh = Color(0xFF1A2138)
val NightOutline = Color(0xFF28304C)
val NightText = Color(0xFFEDEBE3)
val NightTextDim = Color(0xFF98A2C4)

// light scheme (paper, not white — the brand reads as ink on warm paper)
val PaperBg = Color(0xFFF6F1E4)
val PaperSurface = Color(0xFFFFFCF4)
val PaperSurfaceHigh = Color(0xFFEFE7D4)
val PaperOutline = Color(0xFFD6CBB2)
val InkText = Color(0xFF16181F)
val InkTextDim = Color(0xFF5A5F72)

private val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF241C00),
    primaryContainer = Color(0xFF3A2F10),
    onPrimaryContainer = Cream,
    secondary = Teal,
    onSecondary = Color(0xFF00312C),
    tertiary = Amber,
    onTertiary = Color(0xFF3A2500),
    tertiaryContainer = Color(0xFF3B2A08),
    onTertiaryContainer = Cream,
    background = NightBg,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = NightTextDim,
    outline = NightOutline,
    outlineVariant = Color(0xFF1A2140),
    error = Crimson,
    onError = Color(0xFF2B0608),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF7A5F13),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF3E4B4),
    onPrimaryContainer = Color(0xFF2A2000),
    secondary = Color(0xFF1F6A62),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF8A5200),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE3C2),
    onTertiaryContainer = Color(0xFF2B1800),
    background = PaperBg,
    onBackground = InkText,
    surface = PaperSurface,
    onSurface = InkText,
    surfaceVariant = PaperSurfaceHigh,
    onSurfaceVariant = InkTextDim,
    outline = PaperOutline,
    outlineVariant = Color(0xFFE6DEC8),
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
fun MiqatTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    // The app is ARABIC-FIRST: the layout direction is RTL regardless of the
    // device locale. Without this, an en-US device renders the Arabic UI
    // mirrored (prayer order reversed, nav bar wrong) — the "mixed language"
    // slop Hesham called out. Direction is a property of the PRODUCT.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = MiqatType,
            content = content,
        )
    }
}
