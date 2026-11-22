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
internal fun MonthLedger(
    state: AppState,
    fmt: Fmt,
    days: List<Triple<Int, Int, Int>>,
    prayer: Int,
    title: String,
    onOpenDay: (Int) -> Unit,
    onShare: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val rows = remember(days, prayer, state.city.calcMethod, state.display.methodOverride, state.adjusts) {
        days.map { (y, m, d) ->
            val t = state.times(y, m, d).list[prayer]
            d to (((t % 24.0) + 24.0) % 24.0 * 60).toInt()
        }
    }
    var prev by remember { mutableStateOf<Int?>(null) }
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.lg),
    ) {
        Text(
            "دفتر الشهر — ${PRAYER_NAMES[prayer]} في $title",
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface,
            modifier = Modifier.padding(bottom = Space.sm),
        )
        // share the month as a card
        NavRow(
            "مشاركة دفتر الشهر",
            "صورة واحدة فيها كل أيام الشهر — تُرسل كما هي",
            onShare,
        )
        Divider(Modifier.padding(horizontal = Space.lg))
        rows.forEach { (day, minute) ->
            val delta = prev?.let { minute - it }
            prev = minute
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDay(day) }
                    .padding(vertical = Space.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        fmt.n(day),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TabularFigures),
                        color = scheme.onSurface,
                        modifier = Modifier.width(36.dp),
                    )
                    Text(
                        fmt.hhmm(minute),
                        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TabularFigures),
                        color = scheme.onSurface,
                    )
                }
                Text(
                    when {
                        delta == null -> ""
                        delta == 0 -> "—"
                        else -> "${fmt.signed(delta)} د"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TabularFigures),
                    color = if (delta == null || delta == 0) scheme.onSurfaceVariant else scheme.secondary,
                )
            }
            androidx.compose.material3.HorizontalDivider(color = scheme.outlineVariant)
        }
        Spacer(Modifier.height(Space.xxl))
    }
}

/** The year overlay: the chosen prayer across the twelve months. */
