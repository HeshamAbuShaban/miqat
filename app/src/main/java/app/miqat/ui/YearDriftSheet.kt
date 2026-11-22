package app.miqat.ui

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miqat.core.DstInfo
import app.miqat.core.Hijri
import app.miqat.core.IslamicDay
import app.miqat.core.MiqatData
import app.miqat.core.Moments
import java.util.Calendar

@Composable
internal fun YearDriftSheet(state: AppState, fmt: Fmt, year: Int, prayer: Int) {
    val scheme = MaterialTheme.colorScheme
    val points = remember(year, prayer, state.city.calcMethod, state.display.methodOverride, state.adjusts) {
        app.miqat.core.Drift.acrossYear(year, prayer) { y, m, day -> state.times(y, m, day).list }
    }
    val total = app.miqat.core.Drift.totalChange(points)
    val trend = app.miqat.core.Drift.trend(points)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg),
    ) {
        Text(
            "انحراف ${PRAYER_NAMES[prayer]} على مدى ${fmt.n(year)}",
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface,
        )
        Text(
            when {
                total == 0 -> "لا تغيّر يُذكر خلال السنة."
                total > 0 -> "يتأخّر حتى ${fmt.n(total)} دقيقة بين أبعد نقطتين في السنة."
                else -> "يتقدّم حتى ${fmt.n(-total)} دقيقة بين أبعد نقطتين في السنة."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Space.md))
        androidx.compose.foundation.Canvas(
            Modifier
                .fillMaxWidth()
                .height(160.dp),
        ) {
            if (points.size < 2) return@Canvas
            val deltas = points.map { it.deltaMinutes }
            val minD = (deltas.minOrNull() ?: 0).coerceAtMost(0)
            val maxD = (deltas.maxOrNull() ?: 0).coerceAtLeast(0)
            val span = (maxD - minD).coerceAtLeast(1)
            val w = size.width
            val h = size.height
            // right-to-left, like the grid above it (day 1 at the right)
            fun x(i: Int) = w * (points.size - 1 - i) / (points.size - 1).toFloat()
            fun y(d: Int) = h - ((d - minD).toFloat() / span) * (h - 12f) - 6f
            drawLine(
                color = scheme.outlineVariant,
                start = Offset(0f, y(0)),
                end = Offset(w, y(0)),
                strokeWidth = 1f,
            )
            val path = Path()
            points.forEachIndexed { i, p ->
                val pt = Offset(x(i), y(p.deltaMinutes))
                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(path, scheme.primary, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
            points.forEachIndexed { i, p ->
                if (i == 0 || i == points.size - 1) {
                    drawCircle(scheme.primary, 5f, Offset(x(i), y(p.deltaMinutes)))
                }
            }
        }
        Spacer(Modifier.height(Space.sm))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${ARABIC_MONTHS.getOrElse(points.first().month - 1) { "" }}: ${fmt.hhmm(points.first().minuteOfDay)}",
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = TabularFigures),
                color = scheme.onSurfaceVariant,
            )
            Text(
                "الاتجاه العام: ${if (trend == 0) "ثابت" else if (trend > 0) "يتأخّر ${fmt.n(trend)} دقيقة" else "يتقدّم ${fmt.n(-trend)} دقيقة"}",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
            Text(
                "${ARABIC_MONTHS.getOrElse(points.last().month - 1) { "" }}: ${fmt.hhmm(points.last().minuteOfDay)}",
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = TabularFigures),
                color = scheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Space.xxl))
    }
}

/** A cell of the month grid. */

internal val ARABIC_WEEKDAYS = listOf("سبت", "أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة")

internal val ARABIC_MONTHS = listOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر",
)

/** A single day: its prayers, its windows, and things the user can DO. */
