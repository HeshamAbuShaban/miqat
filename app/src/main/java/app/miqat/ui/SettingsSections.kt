package app.miqat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import app.miqat.core.MethodTable
import app.miqat.core.NotifyKind

/**
 * The settings SECTIONS — one screen each, every control effective
 * (decisions.md 2026-11-10 (5)). Nothing here is decorative: each control
 * writes to [AppState] and immediately changes what the app computes/shows.
 */

@Composable
fun LocationSection(state: AppState, fmt: Fmt, onPickCity: () -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg)) {
        SettingsScreenTitle("الموقع", "المدينة والإحداثيات والمنطقة الزمنية")
        NavRow(
            "المدينة",
            "${state.city.ar.ifBlank { state.city.en }} · ${state.city.countryCode}",
            onPickCity,
        )
        Divider(Modifier.padding(horizontal = Space.lg))
        InfoRow("خط العرض", fmt.decimal(state.city.lat, 4))
        InfoRow("خط الطول", fmt.decimal(state.city.lon, 4))
        InfoRow("إزاحة التوقيت", "UTC+${fmt.n(state.utcOffset.toInt())}")
        InfoRow("المنطقة الزمنية", if (state.display.manualUtcOffsetMinutes == null) "تلقائية" else "يدوية")
        Spacer(Modifier.height(Space.sm))
        Text(
            "الموقع من قاعدة المدن المضمّنة — لا نطلب صلاحيات الموقع ولا نرسل شيئاً لأي جهة.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Space.lg),
        )
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
                MinuteStepper(state.adjusts.forIndex(i), { v -> state.updateAdjusts(state.adjusts.withIndex(i, v)) }, fmt = fmt)
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

@Composable
fun NotificationsSection(state: AppState, fmt: Fmt) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val n = state.notifications
    // Android 13+ asks in CONTEXT: when the user turns a kind on (or previews
    // it), we explain why the permission is needed and ask for it there.
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            app.miqat.core.NotificationPoster.preview(context, NotifyKind.DIRECT, "الظهر")
        }
    }
    var rationale by remember { mutableStateOf(false) }

    fun ensurePermission(onGranted: () -> Unit) {
        if (android.os.Build.VERSION.SDK_INT < 33 ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            onGranted()
        } else {
            rationale = true
        }
    }

    if (rationale) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { rationale = false },
            title = { Text("السماح بالإشعارات") },
            text = {
                Text(
                    "يحتاج التطبيق إذن الإشعارات ليوقظك عند دخول وقت الصلاة. " +
                        "لا يُرسل أي شيء إلى أي جهة — الإشعار على جهازك وحده."
                )
            },
            confirmButton = {
                Text(
                    "السماح",
                    modifier = Modifier.clickable {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        rationale = false
                    },
                )
            },
            dismissButton = {
                Text(
                    "لاحقاً",
                    modifier = Modifier.clickable { rationale = false },
                )
            },
        )
    }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg)) {
        SettingsScreenTitle("الإشعارات", "أربعة أنواع: تمهيدية · مباشرة · تذكيرية · تحفيزية")
        ToggleRow(
            "تمهيدية",
            "تنبيه قبل الموعد بـ ${fmt.n(n.preparatoryLeadMinutes)} دقيقة",
            n.kindOn(NotifyKind.PREPARATORY),
        ) { v -> state.updateNotifications(n.copy(kindEnabled = n.kindEnabled + (NotifyKind.PREPARATORY to v))) }
        ToggleRow(
            "مباشرة",
            "الأذان عند الموعد",
            n.kindOn(NotifyKind.DIRECT),
        ) { v ->
            if (v) ensurePermission {
                state.updateNotifications(n.copy(kindEnabled = n.kindEnabled + (NotifyKind.DIRECT to v)))
            } else {
                state.updateNotifications(n.copy(kindEnabled = n.kindEnabled + (NotifyKind.DIRECT to v)))
            }
        }
        ToggleRow(
            "تذكيرية",
            "بعد الموعد بـ ${fmt.n(n.reminderDelayMinutes)} دقيقة",
            n.kindOn(NotifyKind.REMINDER),
        ) { v -> state.updateNotifications(n.copy(kindEnabled = n.kindEnabled + (NotifyKind.REMINDER to v))) }
        ToggleRow(
            "تحفيزية",
            "المحتوى التحفيزي: فضائل الصيام وأيام الإسلام",
            n.kindOn(NotifyKind.MOTIVATIONAL),
        ) { v -> state.updateNotifications(n.copy(kindEnabled = n.kindEnabled + (NotifyKind.MOTIVATIONAL to v))) }
        ToggleRow(
            "المحتوى التحفيزي مرفق بالإشعارات الأخرى",
            "يظهر آخر المحتوى المقدم مع الإشعار المباشر",
            n.motivationalAppended,
        ) { v -> state.updateNotifications(n.copy(motivationalAppended = v)) }
        Divider(Modifier.padding(horizontal = Space.lg, vertical = Space.md))
        SectionLabel("الأذان لكل صلاة", Modifier.padding(horizontal = Space.lg))
        PRAYER_NAMES.forEachIndexed { i, name ->
            ToggleRow(
                name,
                null,
                n.directOn(i),
            ) { v ->
                val list = n.directPerPrayer.toMutableList().also { it[i] = v }
                state.updateNotifications(n.copy(directPerPrayer = list))
            }
        }
        Divider(Modifier.padding(horizontal = Space.lg, vertical = Space.md))
        // previews («معاينة» — the setting must show its consequence before you
        // keep it; the sound/vibration you hear here is the one you will get)
        NavRow("معاينة الأذان", "صوت واهتزاز وقناة الإشعار المباشر", onClick = {
            ensurePermission {
                app.miqat.core.NotificationPoster.preview(context, NotifyKind.DIRECT, "الظهر")
            }
        })
        Divider(Modifier.padding(horizontal = Space.lg))
        NavRow("معاينة التذكير", "ما يصل بعد الموعد", onClick = {
            app.miqat.core.NotificationPoster.preview(context, NotifyKind.REMINDER, "الظهر")
        })
        Divider(Modifier.padding(horizontal = Space.lg))
        NavRow("معاينة المحتوى التحفيزي", "ما يرافق الإشعارات الأخرى", onClick = {
            app.miqat.core.NotificationPoster.preview(context, NotifyKind.MOTIVATIONAL, "")
        })
        Divider(Modifier.padding(horizontal = Space.lg))
        ToggleRow("صوت الأذان", null, n.adhanSound) { v -> state.updateNotifications(n.copy(adhanSound = v)) }
        ToggleRow("الاهتزاز", null, n.vibrate) { v -> state.updateNotifications(n.copy(vibrate = v)) }
        ToggleRow("إشعار ملء الشاشة", "تنبيه يوقف الساعة بنقرة", n.fullScreenAlarm) { v -> state.updateNotifications(n.copy(fullScreenAlarm = v)) }
        ToggleRow(
            "ساعات الهدوء",
            "من ٢٢:٠٠ إلى ٠٥:٠٠ — لا إشعارات",
            n.quietFromMinute >= 0,
        ) { v ->
            state.updateNotifications(
                n.copy(quietFromMinute = if (v) 22 * 60 else -1, quietToMinute = if (v) 5 * 60 else -1)
            )
        }
        Spacer(Modifier.height(Space.xl))
    }
}

@Composable
fun CalendarSection(state: AppState, fmt: Fmt) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg)) {
        SettingsScreenTitle("التقويم الهجري", "أم القرى — Adjustable ±3 days")
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
        Spacer(Modifier.height(Space.xl))
    }
}

@Composable
fun AboutSection() {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg)) {
        SettingsScreenTitle("عن الميقات", "مواقيت الصلاة والصيام — يعمل بلا إنترنت")
        InfoRow("الإصدار", "0.1.0")
        InfoRow("المحرك", "PrayTimes port — تطابق 87.78% @ ±1 دقيقة")
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

internal val PRAYER_NAMES = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
