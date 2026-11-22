package app.miqat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import app.miqat.core.MethodTable
import app.miqat.core.NotifyKind

@Composable
fun AwarenessSection(state: AppState, fmt: Fmt) {
    val d = state.display
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg)) {
        SettingsScreenTitle("الاستعداد والتنبيه", "كيف يتغيّر التطبيق مع اقتراب الموعد")
        ToggleRow(
            "تنبيه الاستعداد",
            "يتغيّر شكل العدّاد قبل الموعد بأقل من ${fmt.n(d.urgencySoonMinutes)} دقيقة",
            d.urgencyEnabled,
        ) { v -> state.updateDisplay(d.copy(urgencyEnabled = v)) }
        InfoRow("تنبيه مبكر", "${fmt.n(d.urgencySoonMinutes)} دقيقة")
        InfoRow("لحظة الدخول", "${fmt.n(d.urgencyImminentMinutes)} دقيقة")
        Divider(Modifier.padding(horizontal = Space.lg, vertical = Space.md))
        SectionLabel("المدد المعروضة", Modifier.padding(horizontal = Space.lg))
        ToggleRow(
            "إظهار المدة بين المواقيت",
            "خط يربط كل ميقين بالذي يليه مع مدته",
            d.showGaps,
        ) { v -> state.updateDisplay(d.copy(showGaps = v)) }
        app.miqat.core.MomentCategory.entries.forEach { category ->
            ToggleRow(
                category.labelAr,
                null,
                d.gapCategories.contains(category.labelAr),
            ) { on ->
                val next = d.gapCategories.toMutableSet()
                if (on) next.add(category.labelAr) else next.remove(category.labelAr)
                state.updateDisplay(d.copy(gapCategories = next))
            }
        }
        Spacer(Modifier.height(Space.xl))
    }
}

/** One edge of the quiet window, in 30-minute steps. */


@Composable
internal fun QuietHourRow(label: String, minute: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "−",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.control))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onChange(((minute - 30 + 1440) % 1440)) }
                    .padding(horizontal = 16.dp, vertical = 2.dp),
            )
            Text(
                "%02d:%02d".format(minute / 60, minute % 60),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.width(72.dp),
            )
            Text(
                "+",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.control))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onChange((minute + 30) % 1440) }
                    .padding(horizontal = 16.dp, vertical = 2.dp),
            )
        }
    }
}


@Composable
fun CalendarSection(state: AppState, fmt: Fmt) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg)) {
        SettingsScreenTitle("التقويم الهجري", "تقويم أم القرى — تعديل من ثلاثة أيام")
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.sm),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text("تعديل التاريخ الهجري", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            MinuteStepper(state.display.hijriOffsetDays, { v -> state.updateDisplay(state.display.copy(hijriOffsetDays = v)) }, -3..3, fmt)
        }
        Text(
            "مطابقة رؤية الهلال في بلدك — التوقيت التقريبي قد يتقدم أو يتأخر يوماً.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Space.lg),
        )
        Spacer(Modifier.height(Space.xl))
    }
}
