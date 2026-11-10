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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.miqat.core.Moment
import app.miqat.core.Moments

/**
 * «كم بعدت عن هذا الوقت؟» — the neovim idea, on demand.
 *
 * Hesham: «مرادي بنيوفيم يجدر أن يكون على نطاق واسع، اقل القليل يكون عند
 * الطلب لطاعة معينة». So: pick ANY appointed time — a prayer, a worship
 * window, a sunnah time — and the app answers with the distance, the side
 * (before/after), and what that distance is made of.
 */
@Composable
fun DistanceScreen(state: AppState, fmt: Fmt, onBack: () -> Unit) {
    val now = System.currentTimeMillis()
    val timeline = rememberTimeline(state, now)
    val nowMinutes = (state.cityHourOfDay(now) * 60).toInt()

    var picked by remember { mutableStateOf<String?>(null) }
    val moment = timeline.firstOrNull { it.kind == picked } ?: timeline.firstOrNull()

    MiqatScaffold("كم بعدت عن هذا الوقت؟", onBack = onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg),
        ) {
            Spacer(Modifier.height(Space.sm))
            Text(
                "اختر أي ميقوت — صلاة أو نافذة عبادة أو وقت من السنة — وقِس بُعدك عنه.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.md))

            // the answer, first
            if (moment != null) {
                val delta = Moments.until(moment, nowMinutes)
                val signed = moment.minute - nowMinutes
                val after = signed >= 0
                val hours = delta / 60
                val minutes = delta % 60
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(Radius.panel),
                        )
                        .padding(Space.lg),
                ) {
                    Text(
                        if (after) "يبعد عنك" else "مضى عنك",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        "${fmt.n(hours)}:${fmt.n(minutes)}",
                        style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = TabularFigures),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "${if (after) "حتى" else "من"} ${moment.labelAr} — ${fmt.hhmm(moment.minuteOfDay)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(Space.md))
            }

            SectionLabel("الميعاد", Modifier.padding(horizontal = Space.lg))
            timeline.filter { it.isEvent }.forEach { m ->
                MomentRow(m, fmt, nowMinutes, m.kind == picked) { picked = m.kind }
                Divider()
            }
            Spacer(Modifier.height(Space.xl))
        }
    }
}

@Composable
private fun MomentRow(
    moment: Moment,
    fmt: Fmt,
    nowMinutes: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val delta = Moments.until(moment, nowMinutes)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = Space.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(7.dp)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(4.dp),
                    ),
            )
            Spacer(Modifier.width(Space.md))
            Text(
                moment.labelAr,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                fmt.hhmm(moment.minuteOfDay),
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TabularFigures),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(Space.md))
            Text(
                "بعد ${fmt.n(delta / 60)}:${fmt.n(delta % 60)}",
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = TabularFigures),
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
