package app.miqat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.miqat.core.Timers

/**
 * THE TIMER LINES — each feature on its own line, with its own clock.
 *
 * This block replaces the old "awareness strip", which mixed three different
 * times into one row: a huge elapsed counter («منذ الفجر ١٢:٤١:٢٠») beside a
 * countdown to a non-appointment. The rule now:
 *
 *  - the countdown to the next prayer lives in the HERO (one number, the
 *    biggest thing on the screen);
 *  - the elapsed since the prayer that just entered is its own line, and only
 *    for the first hour — after that it is noise;
 *  - the iqama (the second call) is its own line with its own countdown,
 *    attached to the prayer it belongs to.
 *
 * Each line can be switched off in the settings; nothing here is mandatory.
 */
@Composable
fun TimerLines(
    state: AppState,
    fmt: Fmt,
    nowMillis: Long,
    timers: Timers,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val nowMinutes = (state.cityHourOfDay(nowMillis) * 60).toInt()

    Column(
        modifier
            .fillMaxWidth()
            .background(scheme.surface, RoundedCornerShape(Radius.panel))
            .padding(horizontal = Space.lg, vertical = Space.md),
    ) {
        // the header: the anchor (now) and the state (a chip, not a clock)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(7.dp)
                        .background(scheme.primary, CircleShape),
                )
                Spacer(Modifier.width(Space.sm))
                Text(
                    "الآن ${fmt.hhmm(nowMinutes)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TabularFigures),
                    color = scheme.onSurfaceVariant,
                )
            }
            val chip = timers.stateAr
            if (chip != null) {
                Text(
                    chip,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.secondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.chip))
                        .background(scheme.surfaceVariant)
                        .padding(horizontal = Space.sm, vertical = 3.dp),
                )
            }
        }

        // LINE 1 — the prayer that has started (its own clock, bounded)
        if (state.display.showTimerElapsed && timers.sinceWorthShowing) {
            Spacer(Modifier.height(Space.sm))
            Hairline()
            Spacer(Modifier.height(Space.sm))
            TimerRow(
                label = "مرّ من وقت ${timers.currentPrayer}",
                value = fmt.hms(timers.sinceSeconds),
                valueColor = scheme.onSurface,
            )
        }

        // LINE 2 — the iqama (the second call), its own clock
        if (state.display.showTimerIqama && timers.iqamaIn != null) {
            Spacer(Modifier.height(Space.sm))
            Hairline()
            Spacer(Modifier.height(Space.sm))
            TimerRow(
                label = "إقامة ${timers.iqamaPrayer ?: ""}",
                value = "بعد ${fmt.hms(timers.iqamaSeconds)}",
                valueColor = scheme.tertiary,
            )
        }
    }
}

/** One line: the thing on the right (the reading start), its clock on the left. */
@Composable
private fun TimerRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TabularFigures),
            color = valueColor,
            textAlign = TextAlign.Start,
        )
    }
}

/** A 1dp hairline — the separator, not a card border. */
@Composable
private fun Hairline() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}
