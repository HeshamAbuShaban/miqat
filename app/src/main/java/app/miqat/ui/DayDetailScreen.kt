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
fun DayDetailScreen(
    state: AppState,
    fmt: Fmt,
    year: Int,
    month: Int,
    day: Int,
    onBack: () -> Unit,
    onOpenPrayer: (Int) -> Unit,
    onOpenTasbih: () -> Unit,
) {
    // REAL date arithmetic: the old `month + 1` kept the day, so Jan 31 asked
    // for Feb 31 and got a normalised, wrong night.
    val (ty, tm, td) = app.miqat.core.Dates.nextDay(year, month, day)

    val times = state.times(year, month, day)
    val tomorrow = state.times(ty, tm, td)
    val hijri = app.miqat.core.Hijri.at(year, month, day, state.display.hijriOffsetDays)
    val timeline = remember(year, month, day, state.city.calcMethod, state.display.methodOverride, state.adjusts) {
        app.miqat.core.Moments.timeline(
            times, tomorrow.fajr, PRAYER_NAMES, ::minutesOf,
            hijriMonth = hijri.hMonth, hijriDay = hijri.hDay,
            imsakLeadMinutes = state.imsakLeadMinutes,
        )
    }
    val dayKey = "%04d-%02d-%02d".format(year, month, day)
    val logged = state.display.dayActLog
    var showLog by remember { mutableStateOf(false) }

    MiqatScaffold(app.miqat.core.Hijri.label(hijri, fmt.ar), onBack = onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.lg),
        ) {
            Spacer(Modifier.height(Space.sm))
            // the spine: the prayer points are TAPPABLE now, not a dead picture
            DayCard(
                state = state, fmt = fmt,
                year = year, month = month, day = day,
                timeline = timeline, hijri = hijri,
                onOpenPrayer = onOpenPrayer,
            )
            Spacer(Modifier.height(Space.lg))

            Text(
                "ما تفعله في هذا اليوم",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Space.xs))
            NavRow(
                "سجّل طاعة",
                "صيام · صلاة · ذكر — يُحفظ على جهازك فقط",
                onClick = { showLog = !showLog },
            )
            if (showLog) {
                listOf("fast" to "صيام", "prayer" to "صلاة", "dhikr" to "ذكر").forEach { (kind, label) ->
                    val key = "$dayKey:$kind"
                    val on = key in logged
                    Text(
                        (if (on) "✓ " else "") + label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (on) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                state.updateDisplay(
                                    state.display.copy(
                                        dayActLog = if (on) logged - key else logged + key,
                                    ),
                                )
                            }
                            .padding(vertical = Space.sm, horizontal = Space.md),
                    )
                }
            }
            Divider()
            NavRow(
                "افتح الموعد",
                "تفاصيل أول موعد في هذا اليوم",
                onClick = { onOpenPrayer(0) },
            )
            Divider()
            NavRow(
                "أضف ذكرًا",
                "التسبيح مرجعٌ مقرون بالتذكير، لا شاشة منفصلة",
                onClick = onOpenTasbih,
            )
            Spacer(Modifier.height(Space.md))
            Text(
                "المواقيت محسوبة عند الطلب — بلا جدولة مسبقة لكل أيام السنة.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.xxl))
        }
    }
}
