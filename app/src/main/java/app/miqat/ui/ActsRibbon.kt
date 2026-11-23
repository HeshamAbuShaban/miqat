package app.miqat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miqat.core.ActsCatalog
import app.miqat.core.ClosingList
import app.miqat.core.CloseRole
import app.miqat.core.DayLine
import app.miqat.core.Hijri
import app.miqat.core.Moments
import app.miqat.core.Moment
import app.miqat.core.Proximity
import app.miqat.core.TimerKind

@Composable
internal fun DayRibbon(
    state: AppState,
    year: Int,
    month: Int,
    dayOfMonth: Int,
    timeline: List<Moment>,
    live: List<ActsCatalog.Live>,
    nowMinute: Int,
    fmt: Fmt,
    scheme: androidx.compose.material3.ColorScheme,
) {
    val running = live.firstOrNull { it.state == ActsCatalog.ActState.RUNNING }
    val total = live.size
    val runningPos = if (running != null) live.sortedBy { it.startMinute }.indexOfFirst { it == running } + 1 else 0
    val countText = if (running != null)
        "${fmt.n(runningPos)} من ${fmt.n(total)} طاعات"
    else "${fmt.n(total)} طاعات اليوم"

    val imsakMin = timeline.firstOrNull { it.kind == Moments.IMSAK }?.minute ?: 0
    val fajrMin = timeline.firstOrNull { it.kind == Moments.FAJR }?.minute ?: 0
    val duhaMin = timeline.firstOrNull { it.kind == Moments.DUHA_START }?.minute ?: 0
    val ishaMin = timeline.firstOrNull { it.kind == Moments.ISHA }?.minute ?: 0
    val saharMin = timeline.firstOrNull { it.kind == Moments.SAHAR_START }?.minute ?: 0

    val anchors = listOf(
        "الإمساك" to imsakMin,
        "الفجر" to fajrMin,
        "الضحى" to duhaMin,
        "العشاء" to ishaMin,
        "الأسحار" to saharMin,
    )

    val windows = live.filter { it.act.isWindow }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surface)
            .border(1.dp, scheme.outline, RoundedCornerShape(16.dp))
            .padding(horizontal = Space.lg, vertical = 14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "مسار اليوم",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                color = scheme.onSurfaceVariant,
            )
            Text(
                countText,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
                color = scheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Space.sm))
        BoxWithConstraints(
            Modifier.fillMaxWidth().height(24.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(scheme.outline),
            )
            windows.forEach { item ->
                val a = item.startMinute.coerceIn(0, 1440)
                val b = item.endMinute.coerceIn(0, 1440)
                if (b > a) {
                    val left = maxWidth * (0.92f - 0.84f * (b / 1440f))
                    val width = (maxWidth * (0.84f * ((b - a) / 1440f))).coerceAtLeast(1.dp)
                    val alpha = if (item.state == ActsCatalog.ActState.PASSED) 0.35f else 1f
                    Box(
                        Modifier
                            .offset(x = left, y = 9.dp)
                            .width(width)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(scheme.secondary.copy(alpha = alpha)),
                    )
                }
            }
            val nx = maxWidth * (0.92f - 0.84f * (nowMinute.coerceIn(0, 1440) / 1440f))
            Box(
                Modifier
                    .offset(x = (nx - 5.dp).coerceAtLeast(0.dp), y = 7.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Sky.sunMark)
                    .border(2.dp, scheme.background, CircleShape),
            )
        }
        Spacer(Modifier.height(Space.xs))
        BoxWithConstraints(
            Modifier.fillMaxWidth().height(14.dp),
        ) {
            // The anchors sit at their REAL minute, so imsak/fajr often land
            // on top of each other. Keep the first of any colliding pair
            // instead of printing two words on the same spot.
            val placed = mutableListOf<Dp>()
            anchors
                .sortedBy { (_, minute) ->
                    maxWidth * (0.92f - 0.84f * (minute.coerceIn(0, 1440) / 1440f))
                }
                .forEach { (label, minute) ->
                    val x = maxWidth * (0.92f - 0.84f * (minute.coerceIn(0, 1440) / 1440f))
                    if (placed.any { kotlin.math.abs(x.value - it.value) < 52f }) return@forEach
                    placed += x
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.offset(
                            x = (x - 16.dp).coerceIn(0.dp, (maxWidth - 36.dp).coerceAtLeast(0.dp)),
                        ),
                    )
                }
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Space.sm, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Plex),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(Space.sm))
        Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
    }
}

