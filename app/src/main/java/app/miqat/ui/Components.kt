package app.miqat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.miqat.core.DisplayPrefs
import app.miqat.core.NumFmt

/** Number rendering bound to the user's display prefs (never mixed systems). */
class Fmt(val prefs: DisplayPrefs) {
    val ar: Boolean get() = prefs.arabicIndicDigits
    fun hhmm(minutes: Int) = NumFmt.hhmm(minutes, ar, prefs.use12h)
    fun hms(seconds: Long) = NumFmt.hms(seconds, ar)
    fun n(v: Int) = NumFmt.n(v, ar)
    fun decimal(v: Double, digits: Int = 1) = NumFmt.decimal(v, digits, ar)
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
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
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
        Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.sm),
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
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 2.dp),
    )
}

/** The chip that marks an approximate, convention-based value («تقديري»). */
@Composable
fun ApproxChip(text: String = "تقديري بحسب الاصطلاح") {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
