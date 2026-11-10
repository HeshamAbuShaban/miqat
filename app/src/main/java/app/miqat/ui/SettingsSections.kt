package app.miqat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
        NavRow("المدينة", "${state.city.ar.ifBlank { state.city.en }} · ${state.city.countryCode}", onPickCity)
        Divider(Modifier.padding(horizontal = Space.lg))
        InfoRow("خط العرض", "%.4f".format(state.city.lat))
        InfoRow("خط الطول", "%.4f".format(state.city.lon))
        InfoRow("إزاحة التوقيت", "UTC+${state.utcOffset}")
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
        Text(
            "اضغط لاختيار طريقة الحساب المحفوظة داخل التطبيق — بلا إنترنت.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Space.lg),
        )
        MethodPicker(state)
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
private fun MethodPicker(state: AppState) {
    val ids = remember { MethodTable.ids().sorted() }
    Column(Modifier.padding(horizontal = Space.lg, vertical = Space.sm)) {
        ids.forEach { id ->
            val selected = (state.display.methodOverride ?: state.city.calcMethod) == id
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { state.updateDisplay(state.display.copy(methodOverride = id)) }
                    .padding(vertical = Space.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    methodNameAr(id),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                if (selected) {
                    Text(
                        "مفعّلة",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        if (state.display.methodOverride != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { state.updateDisplay(state.display.copy(methodOverride = null)) }
                    .padding(vertical = Space.sm),
            ) {
                Text(
                    "العودة إلى طريقة المدينة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

@Composable
fun NotificationsSection(state: AppState, fmt: Fmt) {
    val n = state.notifications
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
        ) { v -> state.updateNotifications(n.copy(kindEnabled = n.kindEnabled + (NotifyKind.DIRECT to v))) }
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
        ToggleRow("صوت الأذان", null, n.adhanSound) { v -> state.updateNotifications(n.copy(adhanSound = v)) }
        ToggleRow("الاهتزاز", null, n.vibrate) { v -> state.updateNotifications(n.copy(vibrate = v)) }
        ToggleRow("إشعار ملء الشاشة", "تنبيه يوقف الساعة بنقرة", n.fullScreenAlarm) { v -> state.updateNotifications(n.copy(fullScreenAlarm = v)) }
        ToggleRow("عدّاد دائم", "إشعار مستمر بالمتبقي", n.continuousNotification) { v -> state.updateNotifications(n.copy(continuousNotification = v)) }
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
