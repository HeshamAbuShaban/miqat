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
        NavRow(
            "كثافة الرئيسية",
            "${d.homeDensity.labelAr} — ${d.homeDensity.noteAr}",
            onClick = {
                state.updateDisplay(d.copy(homeDensity = app.miqat.core.HomeDensity.next(d.homeDensity)))
            },
        )
        Divider(Modifier.padding(horizontal = Space.lg))
        ToggleRow(
            "سطر المؤقّتات",
            "الآن، والوقت الذي دخل، وما يبعد عنه",
            d.showAwarenessStrip,
        ) { v -> state.updateDisplay(d.copy(showAwarenessStrip = v)) }
        ToggleRow(
            "مرّ من وقت الصلاة",
            "كم مضى منذ دخول وقت الصلاة — في الساعة الأولى فقط",
            d.showTimerElapsed,
        ) { v -> state.updateDisplay(d.copy(showTimerElapsed = v)) }
        ToggleRow(
            "إقامة الصلاة",
            "النداء الثاني، بعددّاد مستقل",
            d.showTimerIqama,
        ) { v -> state.updateDisplay(d.copy(showTimerIqama = v)) }
        ToggleRow(
            "سطر القبلة",
            "القبلة والمسافة على الشاشة الرئيسية",
            d.showNearestMoment,
        ) { v -> state.updateDisplay(d.copy(showNearestMoment = v)) }
        ToggleRow(
            "شريط المواقيت",
            "مواقيت الستة في سطر واحد",
            d.showPrayerStrip,
        ) { v -> state.updateDisplay(d.copy(showPrayerStrip = v)) }
        ToggleRow(
            "دليل اليوم",
            "آية واحدة كل يوم، مع موضعها",
            d.showDailyReminder,
        ) { v -> state.updateDisplay(d.copy(showDailyReminder = v)) }
        ToggleRow(
            "علامات الليل",
            "الشروق والغروب والثلث الأخير",
            d.showNightMarkers,
        ) { v -> state.updateDisplay(d.copy(showNightMarkers = v)) }
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
        SectionLabel("في الطاعات والودجت", Modifier.padding(horizontal = Space.lg))
        ToggleRow(
            "الأحداث المتقاربة",
            "ما يدخل نافذة الموعد الحالي ويخرج معها",
            d.showClosingEvents,
        ) { v -> state.updateDisplay(d.copy(showClosingEvents = v)) }
        ToggleRow(
            "سجلات سريعة",
            "سجّل صلاة أو ذكرًا أو صيامًا بضغطة",
            d.showQuickLog,
        ) { v -> state.updateDisplay(d.copy(showQuickLog = v)) }
        ToggleRow(
            "ما مضى اليوم",
            "قائمة الطاعات التي انتهت",
            d.showPassedActs,
        ) { v -> state.updateDisplay(d.copy(showPassedActs = v)) }
        ToggleRow(
            "الإقامة في الودجت",
            "أذان ← إقامة ← منذ الإقامة، بصيغة مختصرة",
            d.widgetIqamaLine,
        ) { v -> state.updateDisplay(d.copy(widgetIqamaLine = v)) }
        Spacer(Modifier.height(Space.xl))
    }
}
