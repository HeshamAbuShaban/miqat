package app.miqat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.miqat.core.OfficialTimes
import app.miqat.core.Reconciliation

/**
 * VERIFICATION — «تحقّق من المواعيد»: the user types the official timetable of
 * their city; the app shows, prayer by prayer, how far its own computation is
 * from it, and can adopt the differences as the manual adjustments.
 *
 * The copy is the honest one: a published table is not always right either, so
 * the app shows the differences rather than declaring a winner — and it never
 * sends anything anywhere.
 */
@Composable
fun VerifyScreen(state: AppState, fmt: Fmt, onBack: () -> Unit) {
    val now = System.currentTimeMillis()
    val (y, m, d) = state.cityDate(now)
    val computed = state.times(y, m, d)
    val computedMinutes = computed.list.map { minutesOf(it) }

    var fajr by remember { mutableStateOf("") }
    var sunrise by remember { mutableStateOf("") }
    var dhuhr by remember { mutableStateOf("") }
    var asr by remember { mutableStateOf("") }
    var maghrib by remember { mutableStateOf("") }
    var isha by remember { mutableStateOf("") }
    var applied by remember { mutableStateOf(false) }

    val fields: List<Pair<String, (String) -> Unit>> = listOf(
        "الفجر" to { v: String -> fajr = v },
        "الشروق" to { v: String -> sunrise = v },
        "الظهر" to { v: String -> dhuhr = v },
        "العصر" to { v: String -> asr = v },
        "المغرب" to { v: String -> maghrib = v },
        "العشاء" to { v: String -> isha = v },
    )
    val values = listOf(fajr, sunrise, dhuhr, asr, maghrib, isha)
    val parsed = values.map { OfficialTimes.parse(it) }
    val complete = parsed.all { it != null }
    val official = if (complete) OfficialTimes(
        parsed[0]!!, parsed[1]!!, parsed[2]!!, parsed[3]!!, parsed[4]!!, parsed[5]!!,
    ) else null
    val reconciliation = official?.let { Reconciliation(computedMinutes, it.asList()) }

    MiqatScaffold("تحقّق من المواعيد", onBack = onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg),
        ) {
            Spacer(Modifier.height(Space.sm))
            Text(
                "أدخل مواقيت مدينتك الرسمية — من المسجد أو الجهة التي تعتمدها — " +
                    "وستريك الفرق بين ما حسبه التطبيق وما هو معتمد عندك.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.sm))
            Text(
                "لا يُرسل شيء إلى أي جهة — المقارنة على جهازك وحده.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.md))

            fields.forEachIndexed { i, (label, setter) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = Space.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    OutlinedTextField(
                        value = values[i],
                        onValueChange = setter,
                        modifier = Modifier.width(120.dp),
                        singleLine = true,
                        placeholder = { Text("٥:١٠", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textAlign = TextAlign.Center,
                            fontFeatureSettings = TabularFigures,
                        ),
                    )
                }
            }

            if (reconciliation != null) {
                Spacer(Modifier.height(Space.md))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(Radius.panel))
                        .padding(Space.lg),
                ) {
                    Text(
                        "الفرق بين الموعدين",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(Space.sm))
                    PRAYER_NAMES.forEachIndexed { i, name ->
                        val delta = reconciliation.deltas[i]
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "${fmt.hhmm(computedMinutes[i])} → ${fmt.hhmm(reconciliation.official[i])}" +
                                    "  (${fmt.signed(delta)} د)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TabularFigures),
                                color = when {
                                    kotlin.math.abs(delta) <= 1 -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.tertiary
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        if (reconciliation.maxDelta <= 1) {
                            "موافقك مع الحساب ضمن دقيقة — لا حاجة إلى تعديل."
                        } else {
                            "أقصى فرق ${fmt.n(reconciliation.maxDelta)} دقيقة."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (reconciliation.maxDelta > 1) {
                        Spacer(Modifier.height(Space.md))
                        Text(
                            if (applied) "اعتُمدت الفروق كتعديلات يدوية" else "اعتمد الفروق كتعديلات",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (applied) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radius.control))
                                .background(
                                    if (applied) MaterialTheme.colorScheme.surfaceVariant
                                    else MaterialTheme.colorScheme.primary
                                )
                                .clickable {
                                    if (!applied) {
                                        state.updateAdjusts(reconciliation.asAdjusts())
                                        state.markOfficial(true)
                                        applied = true
                                    }
                                }
                                .padding(vertical = Space.md),
                        )
                        Spacer(Modifier.height(Space.xs))
                        Text(
                            "التعديل يظهر الآن في كل المواقيت، ويبقى السبب («تعديل يدوي») ظاهراً " +
                                "في صفحة كل وقت — حتى لا يلبس على أحد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(Space.xxl))
        }
    }
}
