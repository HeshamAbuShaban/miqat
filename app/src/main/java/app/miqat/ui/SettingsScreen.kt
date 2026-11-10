package app.miqat.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.miqat.core.MethodTable
import app.miqat.core.NotifyKind

/**
 * The settings TREE (R6) — the donor's 13 preference screens are the
 * completeness checklist; each section here is a real SCREEN with real
 * controls, not a bottom sheet pretending to be settings.
 *
 * Sections:
 *  1. الموقع       — city, coordinates, timezone verification (donor: DSTPreferences)
 *  2. طريقة الحساب  — calc method + high-latitude rule + manual adjusts
 *  3. الإشعارات     — the four kinds, per-prayer control, sounds/vibration
 *  4. التقويم       — Hijri offset + calendar (donor: CalendarPreferences)
 *  5. المظهر        — theme, digits, 12/24h
 *  6. عن التطبيق    — version, engine info, licences (Cairo is SIL OFL)
 */
@Composable
fun SettingsScreen(
    state: AppState,
    fmt: Fmt,
    onOpen: (SettingsRoute) -> Unit,
) {
    val d = state.display
    val n = state.notifications
    val methodId = d.methodOverride ?: state.city.calcMethod
    val method = MethodTable[methodId]

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text(
            "الإعدادات",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.sm),
        )
        SectionLabel("الموقع", Modifier.padding(horizontal = Space.lg, vertical = Space.sm))
        NavRow(
            "المدينة",
            "${state.city.ar.ifBlank { state.city.en }} · ${state.city.countryCode} · UTC+${fmt.n(state.utcOffset.toInt())}",
            onClick = { onOpen(SettingsRoute.Location) },
        )
        Divider(Modifier.padding(horizontal = Space.lg))
        NavRow(
            "التحقق من الموقع",
            "إحداثيات ${fmt.decimal(state.city.lat, 4)}، ${fmt.decimal(state.city.lon, 4)} · ${methodNameAr(state.method.id)}",
            onClick = { onOpen(SettingsRoute.Location) },
        )
        Divider(Modifier.padding(horizontal = Space.lg))

        SectionLabel("الحساب", Modifier.padding(horizontal = Space.lg, vertical = Space.sm))
        NavRow(
            "طريقة الحساب",
            "${methodNameAr(method.id)} · زاوية الفجر ${fmt.decimalUnit(method.fajrAngle, 1, "°")}",
            onClick = { onOpen(SettingsRoute.Calculation) },
        )
        Divider(Modifier.padding(horizontal = Space.lg))
        NavRow(
            "تعديل الأوقات يدوياً",
            if (state.adjusts.hasAny) "تعديلات سارية" else "بلا تعديلات",
            onClick = { onOpen(SettingsRoute.Calculation) },
        )
        Divider(Modifier.padding(horizontal = Space.lg))

        SectionLabel("الإشعارات", Modifier.padding(horizontal = Space.lg, vertical = Space.sm))
        NavRow(
            "أنواع الإشعارات",
            "تمهيدية · مباشرة · تذكيرية · تحفيزية",
            onClick = { onOpen(SettingsRoute.Notifications) },
        )
        Divider(Modifier.padding(horizontal = Space.lg))
        NavRow(
            "الأذان لكل صلاة",
            "${fmt.n(n.directPerPrayer.count { it })} من ${fmt.n(6)} مفعّلة",
            onClick = { onOpen(SettingsRoute.Notifications) },
        )
        Divider(Modifier.padding(horizontal = Space.lg))
        NavRow(
            "صوت الأذان",
            app.miqat.core.AdhanPlayer.PACKS.firstOrNull { it.first == n.adhanPack }?.second ?: "معياري",
            onClick = { onOpen(SettingsRoute.Adhan) },
        )
        Divider(Modifier.padding(horizontal = Space.lg))

        SectionLabel("الاستعداد", Modifier.padding(horizontal = Space.lg, vertical = Space.sm))
        NavRow(
            "الاستعداد والتنبيه",
            "تنبيه مبكر ${fmt.n(state.display.urgencySoonMinutes)} د · المدد المعروضة",
            onClick = { onOpen(SettingsRoute.Awareness) },
        )
        Divider(Modifier.padding(horizontal = Space.lg))

        SectionLabel("التقويم", Modifier.padding(horizontal = Space.lg, vertical = Space.sm))
        NavRow(
            "التقويم الهجري",
            "تعديل ${fmt.n(state.display.hijriOffsetDays)} يوم · أم القرى",
            onClick = { onOpen(SettingsRoute.Calendar) },
        )
        Divider(Modifier.padding(horizontal = Space.lg))

        SectionLabel("المظهر", Modifier.padding(horizontal = Space.lg, vertical = Space.sm))
        NavRow(
            "الخط والأرقام",
            if (d.arabicIndicDigits) "أرقام عربية-هندية" else "أرقام لاتينية",
            onClick = { onOpen(SettingsRoute.Appearance) },
        )
        Divider(Modifier.padding(horizontal = Space.lg))
        NavRow(
            "الساعة",
            if (d.use12h) "12 ساعة" else "24 ساعة",
            onClick = { onOpen(SettingsRoute.Appearance) },
        )
        Divider(Modifier.padding(horizontal = Space.lg))

        SectionLabel("حول التطبيق", Modifier.padding(horizontal = Space.lg, vertical = Space.sm))
        NavRow("عن الميقات", "الإصدار · المحرك · الرخص", onClick = { onOpen(SettingsRoute.About) })
        Spacer(Modifier.height(Space.xl))
    }
}

/** Arabic names for the donor's calculation methods (ids from MethodTable). */
fun methodNameAr(id: String): String = when (id) {
    "makkah" -> "أم القرى"
    "mwl" -> "رابطة العالم الإسلامي"
    "egypt" -> "الهيئة المصرية العامة للمساحة"
    "tehran" -> "معهد الجيوفيزياء — طهران"
    "jafari" -> "الجعفري"
    "karachi" -> "جامعة العلوم الإسلامية — كراتشي"
    "isna" -> "الجمعية الإسلامية لأمريكا الشمالية"
    "emirates" -> "الإمارات"
    "kuwait" -> "الكويت"
    "qatar" -> "قطر"
    "oman" -> "عُمان"
    "omanmuscat" -> "مسقط"
    "morocco" -> "المغرب"
    "algeria" -> "الجزائر"
    "tunisia" -> "تونس"
    "libya" -> "ليبيا"
    "iraq" -> "العراق"
    "jordan" -> "الأردن"
    "palestine" -> "فلسطين"
    "syria" -> "سوريا"
    "yemen" -> "اليمن"
    "sudan" -> "السودان"
    "turkey" -> "تركيا"
    "france" -> "فرنسا"
    "russia" -> "روسيا"
    "kazakhstan" -> "كازاخستان"
    "tajikistan" -> "طاجيكستان"
    "indonesia" -> "إندونيسيا"
    "malaysia" -> "ماليزيا"
    "maldives" -> "المالديف"
    "southkorea" -> "كوريا الجنوبية"
    "london" -> "لندن"
    "paris" -> "باريس"
    "moscow" -> "موسكو"
    "custom" -> "مخصّص"
    "none" -> "بلا حساب"
    else -> id
}

/** The routes the settings tree opens. */
enum class SettingsRoute { Location, Calculation, Notifications, Adhan, Awareness, Calendar, Appearance, About }

@Composable
fun SettingsScreenTitle(text: String, subtitle: String? = null) {
    Text(
        text,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.sm),
    )
    if (subtitle != null) {
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Space.lg),
        )
        Spacer(Modifier.height(Space.sm))
    }
}
