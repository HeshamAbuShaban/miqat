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
fun AppearanceSection(state: AppState, fmt: Fmt) {
    val d = state.display
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg)) {
        SettingsScreenTitle("المظهر", "الخط والأرقام والساعة")
        ToggleRow(
            "الوضع الداكن",
            "فاتح أو داكن — الخط قاهرى في الوضعين",
            d.darkTheme,
        ) { v -> state.updateDisplay(d.copy(darkTheme = v)) }
        ToggleRow(
            "الأرقام العربية-الهندية",
            if (d.arabicIndicDigits) "٠١٢٣" else "0123",
            d.arabicIndicDigits,
        ) { v -> state.updateDisplay(d.copy(arabicIndicDigits = v)) }
        ToggleRow(
            "نظام 12 ساعة",
            if (d.use12h) "ص / م" else "24 ساعة",
            d.use12h,
        ) { v -> state.updateDisplay(d.copy(use12h = v)) }

        Spacer(Modifier.height(Space.lg))
        SettingsScreenTitle("الهُوية", "لونُ التطبيق ورسمُه — اختر ما يشبهك")

        // the accent — the "ours" colour, and the ONLY role that moves
        Text(
            "لونُ التطبيق",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Space.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Accent.entries.forEach { accent ->
                val selected = d.accent == accent.name
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(Radius.card))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { state.updateDisplay(d.copy(accent = accent.name)) }
                        .padding(Space.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(if (isLightTheme()) accent.light else accent.dark)),
                    )
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        accent.labelAr,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(Space.lg))

        Spacer(Modifier.height(Space.xl))
    }
}


@Composable
fun AboutSection() {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg)) {
        SettingsScreenTitle("عن الميقات", "مواقيت الصلاة والصيام — يعمل بلا إنترنت")
        InfoRow("الإصدار", "0.1.0")
        InfoRow("المحرك", "نقل محرك PrayTimes — تطابق ٨٧٫٧٨٪ عند ± دقيقة")
        InfoRow("قاعدة المدن", "218,032 مدينة مضمّنة")
        InfoRow("الاتصال", "لا يوجد — صفر تتبّع")
        Divider(Modifier.padding(horizontal = Space.lg, vertical = Space.md))
        SectionLabel("الرخص", Modifier.padding(horizontal = Space.lg))
        Text(
            "خط القاهرى: SIL Open Font License 1.1.\nأوقات الصلاة محسوبة من معادلات فلكية معايَرة على 59 طريقة حفظية.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Space.lg),
        )
        Spacer(Modifier.height(Space.xl))
    }
}


internal val PRAYER_NAMES: List<String> get() = app.miqat.core.Prayers.NAMES
