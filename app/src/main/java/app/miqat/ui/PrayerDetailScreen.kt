package app.miqat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.miqat.core.MethodTable
import app.miqat.core.formatHms

/**
 * PRAYER DETAIL — «لماذا هذا الوقت؟» as a full screen (R3): the honest
 * provenance of one computed time, both values when the user adjusted it, and
 * the two warning registers (حتى لا يلبس على الناس / توخي وحذر).
 */
@Composable
fun PrayerDetailScreen(state: AppState, fmt: Fmt, index: Int, onBack: () -> Unit) {
    val now = System.currentTimeMillis()
    val (y, m, d) = state.cityDate(now)
    val raw = state.rawTimes(y, m, d)
    val shown = state.times(y, m, d)
    val method = MethodTable[state.display.methodOverride ?: state.city.calcMethod]
    val seasonal = method.seasonal.firstOrNull {
        it.fromMMdd <= m * 100 + d && m * 100 + d <= it.toMMdd
    }
    val userAdj = state.adjusts.forIndex(index)
    val rawHours = raw.list[index]
    val shownHours = shown.list[index]

    MiqatScaffold(PRAYER_NAMES[index], onBack = onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg),
        ) {
            Spacer(Modifier.height(Space.sm))
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(20.dp),
                    )
                    .padding(Space.lg),
            ) {
                Column {
                    Text(
                        "المعروض",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        fmt.hhmm(minutesOf(shownHours)),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "المحسوب ${formatHms(rawHours)} — العرض مقرّب للدقيقة نصفاً للأعلى",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (userAdj != 0) {
                Spacer(Modifier.height(Space.md))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .6f),
                            RoundedCornerShape(14.dp),
                        )
                        .padding(Space.md),
                ) {
                    Column {
                        Text(
                            "تعديل يدوي على هذا الوقت",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Text(
                            "بعد تعديلك ${fmt.signed(userAdj)} دقيقة — الحساب الأصلي ${formatHms(rawHours)}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(Space.lg))
            Text(
                "من أين جاء هذا الوقت؟",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Space.xs))
            InfoRow("الطريقة", method.id)
            InfoRow("زاوية الفجر", fmt.decimalUnit(method.fajrAngle, 1, "°"))
            InfoRow(
                "المغرب",
                if (method.maghribMinutesMode) "بعد الغروب بـ ${method.maghribValue} دقيقة"
                else "زاوية ${fmt.decimalUnit(method.maghribValue, 1, "°")}",
            )
            InfoRow(
                "العشاء",
                if (method.ishaMinutesMode) "بعد المغرب بـ ${method.ishaValue} دقيقة"
                else "زاوية ${fmt.decimalUnit(method.ishaValue, 1, "°")}",
            )
            InfoRow(
                "العصر",
                "بظل ${method.asrShadow}" + if (method.asrShadow == 2) " (حنفي)" else " (شافعي)",
            )
            val margins = listOfNotNull(
                "الفجر" to method.margins.fajr, "الشروق" to method.margins.sunrise,
                "الظهر" to method.margins.dhuhr, "العصر" to method.margins.asr,
                "المغرب" to method.margins.maghrib, "العشاء" to method.margins.isha,
            ).filter { it.second != 0.0 }.joinToString("، ") { "${it.first} +${it.second.toInt()}" }
            if (margins.isNotBlank()) InfoRow("احتياط الحساب", margins)
            if (seasonal != null) {
                InfoRow(
                    "نافذة تعديل موسمي",
                    "من ${seasonal.fromMMdd / 100}/${seasonal.fromMMdd % 100} إلى ${seasonal.toMMdd / 100}/${seasonal.toMMdd % 100}",
                )
            }
            InfoRow(
                "قاعدة الاتساع العالي",
                when (method.highLatRule) {
                    app.miqat.core.HighLatitudeRule.NONE -> "لا تُطبق"
                    app.miqat.core.HighLatitudeRule.MID_NIGHT -> "منتصف الليل"
                    app.miqat.core.HighLatitudeRule.ONE_SEVENTH_NIGHT -> "سُبع الليل"
                    app.miqat.core.HighLatitudeRule.ANGLE_NIGHT -> "نسبة من الليل بالزاوية"
                },
            )
            InfoRow("المنطقة الزمنية", "UTC+${state.utcOffset}")

            if (kotlin.math.abs(state.city.lat) >= 45.0 &&
                method.highLatRule != app.miqat.core.HighLatitudeRule.NONE
            ) {
                Spacer(Modifier.height(Space.md))
                Text(
                    "حساب تقريبي في هذه المنطقة",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    "في خطوط العرض العالية تُقدَّر أوقات الفجر والعشاء بنسب الليل، وقد يتقدّم الموعد أو يتأخر دقائق.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(Space.lg))
            Divider()
            Spacer(Modifier.height(Space.md))
            Text(
                "حتى لا يلبس على الناس: هذه القيم من حساب التطبيق؛ في الحالات الخاصة فتاوى ومواعيد بلدك هي المرجع.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.xxl))
        }
    }
}
