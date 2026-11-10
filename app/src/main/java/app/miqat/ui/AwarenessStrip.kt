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
import app.miqat.core.Appointment
import app.miqat.core.DayClock
import app.miqat.core.Moment
import app.miqat.core.Moments
import app.miqat.core.Urgency
import app.miqat.core.UrgencyRules

/**
 * THE AWARENESS STRIP — the neovim idea, in time.
 *
 * In neovim the status line tells you how many lines you are from the top and
 * from the bottom. Here the "lines" are the appointed times: the strip says
 * how long it has been since the one you are standing on, and how long until
 * the next — and, when a time has ENTERED, it says so.
 *
 * It replaces the duplicated rows on the home screen (the v2 home showed the
 * same countdown twice); one strip, one voice, near the top where the eye
 * lands first.
 */
@Composable
fun AwarenessStrip(
    state: AppState,
    fmt: Fmt,
    nowMillis: Long,
    appointments: List<Appointment>,
    moments: List<Moment>,
    modifier: Modifier = Modifier,
) {
    val nowHour = state.cityHourOfDay(nowMillis)
    val nowMinutes = (nowHour * 60).toInt()
    val seg = DayClock.position(appointments, nowHour)
    val scheme = MaterialTheme.colorScheme

    // the nearest moment of ANY kind (a prayer, a window, a sunnah time)
    val nextMoment = moments
        .filter { it.isEvent }
        .minByOrNull { Moments.until(it, nowMinutes) }
    val momentMinutes = nextMoment?.let { Moments.until(it, nowMinutes) }

    Column(
        modifier
            .fillMaxWidth()
            .background(scheme.surface, RoundedCornerShape(Radius.panel))
            .padding(horizontal = Space.lg, vertical = Space.md),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // now — the quiet anchor
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
            // the state: has a time entered? is one approaching?
            val urgency = if (state.display.urgencyEnabled && seg != null) {
                UrgencyRules.of(
                    seg.remainingSeconds,
                    state.display.urgencySoonMinutes,
                    state.display.urgencyImminentMinutes,
                )
            } else Urgency.CALM
            val chipText = when {
                urgency == Urgency.NOW -> "حان الآن: ${seg?.next?.labelAr}"
                urgency == Urgency.IMMINENT -> "استعدّ: ${seg?.next?.labelAr}"
                urgency == Urgency.SOON -> "اقترب: ${seg?.next?.labelAr}"
                else -> "داخل وقت ${seg?.current?.labelAr ?: ""}".trim()
            }
            Text(
                chipText,
                style = MaterialTheme.typography.labelMedium,
                color = when (urgency) {
                    Urgency.IMMINENT, Urgency.NOW -> UrgencyImminent
                    Urgency.SOON -> UrgencySoon
                    Urgency.CALM -> scheme.secondary
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.chip))
                    .background(scheme.surfaceVariant)
                    .padding(horizontal = Space.sm, vertical = 3.dp),
            )
        }

        Spacer(Modifier.height(Space.sm))

        // the neovim distances: the line above, the line below
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val sinceText = seg?.let {
                "منذ ${it.current.labelAr} ${fmt.hms(it.elapsedSeconds)}"
            } ?: ""
            val untilText = nextMoment?.let { m ->
                val label = if (m.kind in PRAYER_KEY) m.labelAr else m.labelAr
                "قبل $label ${fmt.hms(momentMinutes?.times(60)?.toLong() ?: 0L)}"
            } ?: ""
            Text(
                sinceText,
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = TabularFigures),
                color = scheme.onSurfaceVariant,
            )
            Text(
                untilText,
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = TabularFigures),
                color = scheme.onSurfaceVariant,
            )
        }
    }
}
