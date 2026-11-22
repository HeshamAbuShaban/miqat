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
internal fun PrayerStrip(
    state: AppState,
    fmt: Fmt,
    days: List<Triple<Int, Int, Int>>,
    prayer: Int,
    onPick: (Int) -> Unit,
    onLongPress: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val points: List<Int> = remember(days, prayer, state.city.calcMethod, state.display.methodOverride, state.adjusts) {
        days.map { (y, m, d) ->
            val t = state.times(y, m, d).list[prayer]
            ((((t % 24.0) + 24.0) % 24.0) * 60).toInt()
        }
    }
    val min = points.minOrNull() ?: 0
    val max = points.maxOrNull() ?: 1
    val span = (max - min).coerceAtLeast(1)

    Column(
        Modifier
            .fillMaxWidth()
            .cardSurface(RoundedCornerShape(Radius.card))
            .padding(Space.md),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            PRAYER_NAMES.forEachIndexed { i, name ->
                val selected = i == prayer
                Text(
                    name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.chip))
                        .background(if (selected) scheme.primary else scheme.surfaceVariant)
                        .clickable { onPick(i) }
                        .padding(horizontal = Space.sm, vertical = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(Space.sm))
        // the drift line: thirty days, the movement is the shape
        androidx.compose.foundation.Canvas(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(Radius.row))
                .clickable { onLongPress() },
        ) {
            if (points.size < 2) return@Canvas
            val w = size.width
            val h = size.height - 16f
            // right-to-left, like the grid above it (day 1 at the right)
            fun x(i: Int) = w * (points.size - 1 - i) / (points.size - 1).toFloat()
            fun y(v: Int) = h - ((v - min).toFloat() / span) * h
            val path = Path()
            points.forEachIndexed { i, v ->
                val p = Offset(x(i), y(v))
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, scheme.primary, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
            // the first and the last, marked
            drawCircle(scheme.primary, 4f, Offset(x(0), y(points.first())))
            drawCircle(scheme.tertiary, 4f, Offset(x(points.lastIndex), y(points.last())))
        }
        Spacer(Modifier.height(Space.xs))
        Text(
            "أوّل الشهر ${fmt.hhmm(points.first().toInt())} — آخر الشهر ${fmt.hhmm(points.last().toInt())} " +
                "(الفرق ${fmt.n(max - min)} دقيقة) — اضغط لعرض السنة",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
        )
    }
}

/** The month ledger: every day's time for the chosen prayer, with its change. */
