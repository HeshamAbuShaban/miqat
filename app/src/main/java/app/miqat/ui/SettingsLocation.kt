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
fun LocationSection(state: AppState, fmt: Fmt, onPickCity: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val gps = state.gpsFix
    val confidence = state.confidence

    // The permission is asked HERE, in context, when the user asks for GPS.
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.values.any { it }) {
            state.refreshGps()
        } else {
            // declined: we fall back to the picked city — and say so
            state.clearGps()
        }
    }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg)) {
        SettingsScreenTitle("الموقع", "من أين تأتي المواقيت")

        // the confidence of the whole chain — never hidden
        val locWarning = state.locationFix.warning
        if (locWarning != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .55f),
                        RoundedCornerShape(Radius.row),
                    )
                    .padding(Space.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(4.dp)),
                )
                Spacer(Modifier.width(Space.md))
                Text(
                    locWarning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(Space.md))
        }

        NavRow(
            "مصدر الموقع",
            state.locationFix.source.labelAr,
            onClick = {
                if (gps != null) {
                    state.clearGps()
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                }
            },
        )
        if (gps != null) {
            InfoRow("إحداثيات الموقع", "${fmt.decimal(gps.latitude, 4)}، ${fmt.decimal(gps.longitude, 4)}")
            InfoRow("منطقة الموقع", "منطقة الجهاز — تُحسب المواقيت على الإحداثيات نفسها")
        }
        Divider(Modifier.padding(horizontal = Space.lg))
        NavRow(
            "المدينة المختارة",
            "${state.city.ar.ifBlank { state.city.en }} · ${state.city.countryCode}",
            onPickCity,
        )
        Divider(Modifier.padding(horizontal = Space.lg))
        InfoRow("خط العرض", fmt.decimal(state.locationFix.latitude, 4))
        InfoRow("خط الطول", fmt.decimal(state.locationFix.longitude, 4))
        InfoRow("إزاحة التوقيت", "UTC+${fmt.n(state.locationFix.utcOffsetHours.toInt())}")
        InfoRow("ثقة الحساب", confidence.labelAr)
        InfoRow("إزاحة التوقيت اليدوية", if (state.display.manualUtcOffsetMinutes == null) "تلقائية" else "يدوية")
        Spacer(Modifier.height(Space.md))
        Text(
            "الموقع من قاعدة المدن المضمّنة أو من نظام الموقع بإذنك — لا نطلب الإذن إلا حين تختار " +
                "ذلك، ولا نرسل شيئاً لأي جهة.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        
        Spacer(Modifier.height(Space.md))
        ToggleRow(
            "أظهر المدينة في الأداة",
            "اسم مدينتك في ودجت الشاشة الرئيسية — يظهر فقط حين تأتي المواقيت من المدينة نفسها، لا من تحديد الموقع",
            state.display.widgetShowCity,
        ) { v -> state.updateDisplay(state.display.copy(widgetShowCity = v)) }
Spacer(Modifier.height(Space.xl))
    }
}


@Composable
fun CalculationSection(state: AppState, fmt: Fmt) {
    val d = state.display
    val methodId = d.methodOverride ?: state.city.calcMethod
    val method = MethodTable[methodId]
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg)) {
        SettingsScreenTitle("طريقة الحساب", methodNameAr(methodId))
        InfoRow("الطريقة", methodNameAr(methodId))
        InfoRow("زاوية الفجر", fmt.decimalUnit(method.fajrAngle, 1, "°"))
        InfoRow(
            "المغرب",
            if (method.maghribMinutesMode) "بعد الغروب بـ ${fmt.n(method.maghribValue.toInt())} دقيقة"
            else "زاوية ${fmt.decimalUnit(method.maghribValue, 1, "°")}",
        )
        InfoRow(
            "العشاء",
            if (method.ishaMinutesMode) "بعد المغرب بـ ${fmt.n(method.ishaValue.toInt())} دقيقة"
            else "زاوية ${fmt.decimalUnit(method.ishaValue, 1, "°")}",
        )
        InfoRow(
            "العصر",
            "بظل ${fmt.n(method.asrShadow)}" + if (method.asrShadow == 2) " (حنفي)" else " (شافعي)",
        )
        InfoRow(
            "قاعدة الاتساع",
            when (method.highLatRule) {
                app.miqat.core.HighLatitudeRule.NONE -> "لا تُطبق"
                app.miqat.core.HighLatitudeRule.MID_NIGHT -> "منتصف الليل"
                app.miqat.core.HighLatitudeRule.ONE_SEVENTH_NIGHT -> "سُبع الليل"
                app.miqat.core.HighLatitudeRule.ANGLE_NIGHT -> "نسبة من الليل بالزاوية"
            },
        )
        Divider(Modifier.padding(horizontal = Space.lg, vertical = Space.md))
        SectionLabel("طريقة بديلة", Modifier.padding(horizontal = Space.lg))
        MethodPickerRow(state)
        Divider(Modifier.padding(horizontal = Space.lg, vertical = Space.md))
        SectionLabel("تعديل يدوي بالدقائق", Modifier.padding(horizontal = Space.lg))
        Text(
            "للمدن التي يختلف فيها مواعيدها عن الحساب. تُعرض بجانب كل وقت قيمته الأصلية والحساب الأصلي.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.xs),
        )
        PRAYER_NAMES.forEachIndexed { i, name ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                MinuteStepper(state.adjusts.forIndex(i), { v -> state.updateAdjusts(state.adjusts.withIndex(i, v)) }, -60..60, fmt)
            }
        }
        Spacer(Modifier.height(Space.xl))
    }
}


@Composable
private fun MethodPickerRow(state: AppState) {
    var open by remember { mutableStateOf(false) }
    val current = state.display.methodOverride ?: state.city.calcMethod
    NavRow(
        "الطريقة المستخدمة",
        methodNameAr(current) + if (state.display.methodOverride != null) " (بديلة)" else " (طريقة المدينة)",
        onClick = { open = true },
    )
    if (open) {
        MethodPickerDialog(
            current = current,
            onPick = { id ->
                state.updateDisplay(state.display.copy(methodOverride = id))
                open = false
            },
            onDismiss = { open = false },
        )
    }
}

/** The method chooser as its own dialog — 59 options need search, not a scroll. */


@Composable
private fun MethodPickerDialog(
    current: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val ids = remember { MethodTable.ids().sorted() }
    val filtered = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) ids else ids.filter {
            it.contains(q) || methodNameAr(it).contains(query.trim())
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
                .padding(Space.lg),
        ) {
            Text(
                "طريقة الحساب",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Space.sm))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("ابحث…", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            )
            Spacer(Modifier.height(Space.sm))
            LazyColumn(Modifier.weight(1f, fill = false)) {
                items(filtered) { id ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(id) }
                            .padding(vertical = Space.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            methodNameAr(id),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (id == current) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                        if (id == current) {
                            Text(
                                "مفعّلة",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Divider()
                }
            }
            Spacer(Modifier.height(Space.sm))
            Row(
                Modifier.fillMaxWidth().clickable { onDismiss() },
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("إغلاق", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
