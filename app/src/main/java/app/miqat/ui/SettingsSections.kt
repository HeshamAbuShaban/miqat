package app.miqat.ui

import androidx.compose.foundation.clickable
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

/**
 * The settings SECTIONS — one screen each, every control effective
 * (decisions.md 2026-11-10 (5)). Nothing here is decorative: each control
 * writes to [AppState] and immediately changes what the app computes/shows.
 */

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
        if (confidence.warn != null) {
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
                    confidence.warn,
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
        Divider(Modifier.padding(horizontal = Space.lg, vertical = Space.sm))
        SectionLabel("الصيام", Modifier.padding(horizontal = Space.lg))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("الإمساك قبل الفجر", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            MinuteStepper(state.imsakLeadMinutes, { v ->
                state.imsakLeadMinutes = v.coerceIn(0, 60)
            }, 0..60, fmt)
        }
        Text(
            "الفطر عند الغروب — والمدد معروضة في «مواقيت» و«كم بعدت عن هذا الوقت؟».",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Space.lg),
        )
        SectionLabel("تذكير الصيام", Modifier.padding(horizontal = Space.lg))
        ToggleRow(
            "تنبيه صيام الغد",
            "ذكرى الصيام بعد المغرب أو قبل الفجر",
            n.fastingReminderEnabled,
        ) { v -> state.updateNotifications(n.copy(fastingReminderEnabled = v)) }
        NavRow(
            "وقت التنبيه",
            if (n.fastingReminderTime == "fajr") "قبل الفجر" else "بعد المغرب",
            onClick = {
                state.updateNotifications(
                    n.copy(fastingReminderTime = if (n.fastingReminderTime == "fajr") "maghrib" else "fajr")
                )
            },
        )
        Divider(Modifier.padding(horizontal = Space.lg))
        listOf(
            "monday" to "الاثنين",
            "thursday" to "الخميس",
            "white" to "أيام البيض (١٣–١٥)",
            "arafah" to "يوم عرفة",
            "ashura" to "عاشوراء",
            "shawwal" to "ستّ من شوال",
        ).forEach { (key, label) ->
            ToggleRow(
                label,
                null,
                n.fastingReminderDays.contains(key),
            ) { on ->
                val next = n.fastingReminderDays.toMutableSet()
                if (on) next.add(key) else next.remove(key)
                state.updateNotifications(n.copy(fastingReminderDays = next))
            }
        }
        Divider(Modifier.padding(horizontal = Space.lg, vertical = Space.sm))
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
        NavRow(
            "صلاة الإقامة",
            if (n.iqamaMinutes <= 0) "معطّلة" else "بعد ${fmt.n(n.iqamaMinutes)} دقيقة من الأذان",
            onClick = {
                val options = intArrayOf(0, 10, 15, 20, 30)
                val next = options[(options.indexOf(n.iqamaMinutes) + 1) % options.size]
                state.updateNotifications(n.copy(iqamaMinutes = next))
            },
        )
        Divider(Modifier.padding(horizontal = Space.lg))
        ToggleRow(
            "العدّاد الدائم",
            "إشعار مستمر بالمتبقي على شاشة القفل",
            n.alwaysOnCountdown,
        ) { v ->
            state.updateNotifications(n.copy(alwaysOnCountdown = v))
            app.miqat.core.CountdownController.setEnabled(context, v)
        }
        Divider(Modifier.padding(horizontal = Space.lg))
        NavRow(
            "إذن البطارية",
            "يمنع النظام من إيقاف التنبيهات",
            onClick = { app.miqat.core.CountdownController.openBatterySettings(context) },
        )
        Divider(Modifier.padding(horizontal = Space.lg))
        ToggleRow("الاهتزاز", null, n.vibrate) { v -> state.updateNotifications(n.copy(vibrate = v)) }
        ToggleRow("إشعار ملء الشاشة", "تنبيه يوقف الساعة بنقرة", n.fullScreenAlarm) { v -> state.updateNotifications(n.copy(fullScreenAlarm = v)) }
        ToggleRow(
            "ساعات الهدوء",
            if (n.quietFromMinute < 0) "معطّلة" else
                "من ${fmt.hhmm(n.quietFromMinute)} إلى ${fmt.hhmm(n.quietToMinute)} — لا إشعارات",
            n.quietFromMinute >= 0,
        ) { v ->
            state.updateNotifications(
                n.copy(quietFromMinute = if (v) 22 * 60 else -1, quietToMinute = if (v) 5 * 60 else -1)
            )
        }
        if (n.quietFromMinute >= 0) {
            QuietHourRow("من", n.quietFromMinute) { m ->
                state.updateNotifications(n.copy(quietFromMinute = m))
            }
            QuietHourRow("إلى", n.quietToMinute) { m ->
                state.updateNotifications(n.copy(quietToMinute = m))
            }
        }
        Spacer(Modifier.height(Space.xl))
    }
}

@Composable
fun AdhanSection(state: AppState, fmt: Fmt) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val n = state.notifications
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg)) {
        SettingsScreenTitle("الأذان", "صوت الأذان والتنبيه عند الموعد")

        ToggleRow(
            "تشغيل الأذان",
            "يُبثّ الأذان مع إشعار الموعد",
            n.adhanSound,
        ) { v -> state.updateNotifications(n.copy(adhanSound = v)) }

        SectionLabel("الأذان", Modifier.padding(horizontal = Space.lg, vertical = Space.xs))
        app.miqat.core.AdhanPlayer.PACKS.forEach { (id, label) ->
            val selected = n.adhanPack == id
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { state.updateNotifications(n.copy(adhanPack = id)) }
                    .padding(vertical = Space.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                )
                if (selected) {
                    Text(
                        "مفعّل",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Divider(Modifier.padding(horizontal = Space.lg))
        }

        Spacer(Modifier.height(Space.sm))
        NavRow(
            "التسجيل",
            if (n.adhanVersion == 2) "التسجيل الثاني" else "التسجيل الأول",
            onClick = {
                state.updateNotifications(n.copy(adhanVersion = if (n.adhanVersion == 2) 1 else 2))
            },
        )
        Divider(Modifier.padding(horizontal = Space.lg))
        NavRow(
            "استمع إلى الأذان",
            "معاينة المقطع المختار",
            onClick = {
                app.miqat.core.AdhanPlayer.play(context, n.adhanPack, 2, n.adhanVersion) {}
            },
        )
        Divider(Modifier.padding(horizontal = Space.lg))
        NavRow("إيقاف المعاينة", "إن كان الصوت يعمل", onClick = {
            app.miqat.core.AdhanPlayer.stop()
        })

        Divider(Modifier.padding(horizontal = Space.lg, vertical = Space.md))
        ToggleRow("الاهتزاز", null, n.vibrate) { v -> state.updateNotifications(n.copy(vibrate = v)) }
        ToggleRow(
            "إشعار ملء الشاشة",
            "توقيف الساعة بنقرة عند الموعد",
            n.fullScreenAlarm,
        ) { v -> state.updateNotifications(n.copy(fullScreenAlarm = v)) }
        Spacer(Modifier.height(Space.xl))
    }
}

/**
 * INDICATORS — what the app shows, and how much of it. Hesham's rule: «too
 * much isn't a restriction, especially when it gives value … you get to
 * config it in the settings».
 */
@Composable
fun IndicatorsSection(state: AppState, fmt: Fmt) {
    val d = state.display
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg)) {
        SettingsScreenTitle("المؤشرات", "ما يظهر في الشاشة الرئيسية والتقويم — كلُّها قابلة للإطفاء")
        SectionLabel("في الرئيسية", Modifier.padding(horizontal = Space.lg))
        ToggleRow(
            "شريط الوعي",
            "الآن، والوقت الذي دخل، وما يبعد عنه",
            d.showAwarenessStrip,
        ) { v -> state.updateDisplay(d.copy(showAwarenessStrip = v)) }
        ToggleRow(
            "أقرب وقت",
            "أقرب موعد من أي نوع — صلاة أو عبادة أو سنة",
            d.showNearestMoment,
        ) { v -> state.updateDisplay(d.copy(showNearestMoment = v)) }
        ToggleRow(
            "تنبيه التوقيت الصيفي",
            "قبل أن تتقدّم الساعة أو تتراجع",
            d.showDstNotice,
        ) { v -> state.updateDisplay(d.copy(showDstNotice = v)) }
        Divider(Modifier.padding(horizontal = Space.lg, vertical = Space.md))
        SectionLabel("في التقويم", Modifier.padding(horizontal = Space.lg))
        ToggleRow("اليوم الهجري", null, d.calendarShowHijri) { v ->
            state.updateDisplay(d.copy(calendarShowHijri = v))
        }
        ToggleRow("اليوم الميلادي", null, d.calendarShowGregorian) { v ->
            state.updateDisplay(d.copy(calendarShowGregorian = v))
        }
        ToggleRow("علامات المناسبات", null, d.calendarShowEvents) { v ->
            state.updateDisplay(d.copy(calendarShowEvents = v))
        }
        ToggleRow("جدول الشهر", "موعد كل يوم مع تغيّره", d.calendarShowTable) { v ->
            state.updateDisplay(d.copy(calendarShowTable = v))
        }
        Spacer(Modifier.height(Space.xs))
        Text(
            "الموعد المعروض في جدول الشهر والانحراف السنوي",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Space.lg),
        )
        PRAYER_NAMES.forEachIndexed { i, name ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { state.updateDisplay(d.copy(calendarPrayer = i)) }
                    .padding(vertical = Space.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                if (d.calendarPrayer == i) {
                    Text(
                        "مختار",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Spacer(Modifier.height(Space.xl))
    }
}

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
private fun QuietHourRow(label: String, minute: Int, onChange: (Int) -> Unit) {
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

internal val PRAYER_NAMES = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
