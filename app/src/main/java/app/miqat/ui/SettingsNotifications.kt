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
        Divider(Modifier.padding(horizontal = Space.lg, vertical = Space.sm))
        SectionLabel("الصيام", Modifier.padding(horizontal = Space.lg))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("الإمساك قبل الفجر", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            MinuteStepper(state.imsakLeadMinutes, { v ->
                state.setImsakLeadMinutes(v)
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
        // per-prayer iqama — a default plus five real overrides (no dumpling
        // ground: this is the one fact it controls)
        val iqamaOptions = listOf(-1, 0, 10, 15, 20, 30)
        listOf(
            "fajr" to "الفجر", "dhuhr" to "الظهر", "asr" to "العصر",
            "maghrib" to "المغرب", "isha" to "العشاء",
        ).forEach { (key, label) ->
            val override = n.iqamaByPrayer[key]
            NavRow(
                "إقامة $label",
                if (override == null) "حسب العامّة — " +
                    (if (n.iqamaMinutes <= 0) "معطّلة" else "بعد ${fmt.n(n.iqamaMinutes)} دقيقة")
                else if (override == 0) "معطّلة"
                else "بعد ${fmt.n(override)} دقيقة من الأذان",
                onClick = {
                    val next = iqamaOptions[(iqamaOptions.indexOf(override ?: -1) + 1) % iqamaOptions.size]
                    val map = if (next < 0) n.iqamaByPrayer - key else n.iqamaByPrayer + (key to next)
                    state.updateNotifications(n.copy(iqamaByPrayer = map))
                },
            )
            Divider(Modifier.padding(horizontal = Space.lg))
        }
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
