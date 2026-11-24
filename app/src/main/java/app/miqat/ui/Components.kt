package app.miqat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miqat.core.DisplayPrefs
import app.miqat.core.NumFmt

/**
 * THE BIG COUNTDOWN — the hero's number, as one object.
 *
 * The seconds are smaller and quieter than the minutes: a ticking clock that
 * shouts in three weights is three clocks. The digits keep the tabular
 * figures, so the number never reflows its neighbours.
 */
@Composable
fun BigCountdown(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    sizeSp: Float = 32f,
) {
    val annotated = remember(text, color) {
        buildAnnotatedString {
            val idx = text.lastIndexOf(':')
            if (idx > 0) {
                append(text.substring(0, idx + 1))
                withStyle(SpanStyle(fontSize = (sizeSp * 0.55f).sp, color = color.copy(alpha = 0.78f))) {
                    append(text.substring(idx + 1))
                }
            } else {
                append(text)
            }
        }
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.displayMedium.copy(
            fontSize = sizeSp.sp,
            fontFeatureSettings = TabularFigures,
        ),
        color = color,
        modifier = modifier,
    )
}

/**
 * A CARD — the fill, the radius, and the 1dp edge.
 *
 * The design system has always said «Card: radius 14, fill surface, 1dp
 * outline» — but the code never drew the outline, and the page→card step is
 * 1.07:1, so the cards had no perceptible edge. That is the single cheapest-
 * looking thing in the app (the colour council, 2026-11-11).
 */
@Composable
fun Modifier.cardSurface(shape: RoundedCornerShape): Modifier {
    val scheme = MaterialTheme.colorScheme
    return this
        .clip(shape)
        .background(scheme.surface)
        .border(1.dp, scheme.outline, shape)
}

/** Number rendering bound to the user's display prefs (never mixed systems). */
class Fmt(val prefs: DisplayPrefs) {
    val ar: Boolean get() = prefs.arabicIndicDigits
    fun hhmm(minutes: Int) = NumFmt.hhmm(minutes, ar, prefs.use12h)
    /** A clock time without the ص/م suffix — for diagrams (the day-rail). */
    fun hhmmShort(minutes: Int) = NumFmt.hhmmShort(minutes, ar, prefs.use12h)
    /** A DURATION (the ticking countdown) — never carries a ص/م suffix. */
    fun hms(seconds: Long) = NumFmt.hms(seconds, ar)
    /** A secondary ticking duration: m:ss under an hour, h:mm above it. */
    fun tick(seconds: Long) = NumFmt.tick(seconds, ar)
    /** A window as two CLOCK times: «٤:٣٣ ص — ٦:٠٢ م», never two bare numbers. */
    fun range(fromMinute: Int, toMinute: Int) = "${hhmm(fromMinute)} — ${hhmm(toMinute)}"

    /** A CLOCK time given in fractional local hours (the engine's unit). */
    fun hmsOf(hours: Double) = NumFmt.clockHMS(
        (Math.floor(((hours % 24.0) + 24.0) % 24.0 * 3600.0)).toLong(), ar, prefs.use12h,
    )
    fun n(v: Int) = NumFmt.n(v, ar)
    fun decimal(v: Double, digits: Int = 1) = NumFmt.decimal(v, digits, ar)
    /** A duration in minutes: "1:17" (h:mm) or "45 د" (under an hour). */
    fun duration(minutes: Int): String = NumFmt.duration(minutes, ar)
    fun decimalUnit(v: Double, digits: Int = 1, unit: String) = NumFmt.decimalUnit(v, digits, unit, ar)
    fun signed(v: Int) = NumFmt.signedMinutes(v, ar)
}

/**
 * Shared chrome + primitives.
 *
 * Rules that keep the app out of "slop" territory:
 *  - a screen has ONE top app bar and a flat body; no card inside a card
 *  - lists use rows + dividers, not a stack of identical cards
 *  - one accent (gold) carries identity; colour elsewhere means MEANING
 *    (amber = the live moment, teal = worship windows, blue = night)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiqatScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onBack() }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        bottomBar = bottomBar,
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        // The padding is applied HERE, not handed to the caller: a section that
        // ignores the modifier must still clear the app bar (otherwise its
        // title renders underneath it).
        Box(Modifier.fillMaxSize().padding(padding)) { content(Modifier) }
    }
}

/** Small caps-ish group label — structural, not decorative. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 2.dp, bottom = Space.sm),
    )
}

/** A labelled value line — the workhorse of every detail screen. */
@Composable
fun InfoRow(label: String, value: String, valueColor: Color? = null) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFeatureSettings = TabularFigures,
            ),
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f).padding(start = 12.dp),
        )
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = modifier,
    )
}

/** A settings row with a chevron — opens a dedicated screen (never a sheet). */
@Composable
fun NavRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { onClick() }
            .padding(horizontal = Space.lg, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
        Spacer(Modifier.width(Space.sm))
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** A toggle row with a description — the "التوابع" discipline (R6). */
@Composable
fun ToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .padding(horizontal = Space.lg, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** ± stepper for minute adjustments (donor `adjust_min_*` parity). */
@Composable
fun MinuteStepper(
    value: Int,
    onChange: (Int) -> Unit,
    range: IntRange = -30..30,
    fmt: Fmt,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton("−") { if (value > range.first) onChange(value - 1) }
        Text(
            fmt.signed(value),
            style = MaterialTheme.typography.titleMedium,
            color = if (value != 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(56.dp),
        )
        StepButton("+") { if (value < range.last) onChange(value + 1) }
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    Text(
        symbol,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.control))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 2.dp),
    )
}

/**
 * The GAP BRIDGE — the app's signature detail.
 *
 * The time between one appointed moment and the next is drawn as a hairline
 * connector between the two rows, with the duration centred on it. It makes
 * the gaps legible without another card, and it is the same mark everywhere:
 * between prayer rows, in the gaps table, and in the interval tool.
 */
@Composable
fun GapBridge(span: app.miqat.core.Span, fromAr: String, toAr: String, fmt: Fmt) {
    val scheme = MaterialTheme.colorScheme
    val line = app.miqat.core.Proximity.timerLine(
        app.miqat.core.TimerKind.BETWEEN_ACTS,
        span.remainingMin * 60L, fromAr, toAr,
    )
    Column(Modifier.fillMaxWidth().padding(vertical = Space.xs)) {
        // the window as a SHAPE (RTL: right = the act that entered, left =
        // the one waiting). The teal tail is what REMAINS; the grey head has
        // been consumed; the gold tick is the line between them.
        // drawn directly: a `weight(0f)` at frac == 1 or 0 used to crash
        // the whole Times screen (Compose rejects zero weights)
        androidx.compose.foundation.Canvas(
            Modifier
                .fillMaxWidth()
                .height(6.dp),
        ) {
            val w = size.width
            val h = size.height
            val radius = androidx.compose.ui.geometry.CornerRadius(h / 2f, h / 2f)
            val sizeFull = androidx.compose.ui.geometry.Size(w, h)
            drawRoundRect(scheme.outlineVariant, cornerRadius = radius, size = sizeFull)
            if (span.open && span.frac > 0f) {
                val tailW = (w * span.frac.coerceIn(0f, 1f)).coerceAtLeast(1f)
                drawRoundRect(
                    scheme.secondary,
                    topLeft = androidx.compose.ui.geometry.Offset(w - tailW, 0f),
                    size = androidx.compose.ui.geometry.Size(tailW, h),
                    cornerRadius = radius,
                )
                drawLine(
                    scheme.primary,
                    start = androidx.compose.ui.geometry.Offset(w - tailW, 0f),
                    end = androidx.compose.ui.geometry.Offset(w - tailW, h),
                    strokeWidth = 1.5f,
                )
            }
        }
        Text(
            if (span.open) "${line.prefixAr}: ${fmt.duration(span.remainingMin)}"
            else "بين $fromAr و$toAr: ${fmt.duration(span.totalMin)}",
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = TabularFigures),
            color = scheme.onSurfaceVariant,
        )
    }
}

/** The chip that marks an approximate, convention-based value («تقديري»). */
@Composable
fun ApproxChip(text: String = "تقديري بحسب الاصطلاح") {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.chip))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
