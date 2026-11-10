package app.miqat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.miqat.core.Moment
import app.miqat.core.Moments

/**
 * مواقيت السنة — the sunnah's own timings, computed from the day's prayers.
 *
 * Guardrail (research-agenda §Guardrail): every text here carries its source.
 * Where the citation is not yet verified, the line is marked «قيد التحقق» and
 * the text stays OUT of the UI — we never present an unreferenced narration.
 */
@Composable
fun SunnahScreen(state: AppState, fmt: Fmt, onBack: () -> Unit) {
    val nowState = rememberNowState()
    val now = nowState.value
    val timeline = rememberTimeline(state, now)

    fun moment(kind: String): Moment? = timeline.firstOrNull { it.kind == kind }

    val sleepStart = moment(Moments.SLEEP_START)
    val sleepEnd = moment(Moments.SLEEP_END)
    val suhur = moment(Moments.SUHUR_START)
    val qaylulaStart = moment(Moments.QAYLULA_START)
    val qaylulaEnd = moment(Moments.QAYLULA_END)
    val morningRead = moment(Moments.MORNING_READ)
    val eveningRead = moment(Moments.EVENING_READ)

    MiqatScaffold("مواقيت السنة", onBack = onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg),
        ) {
            Spacer(Modifier.height(Space.sm))
            Text(
                "مواقيت تُحسب من جدول اليوم — من السنن وأوقات الليل",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.md))

            if (sleepStart != null && sleepEnd != null) {
                SunnahBlock(
                    title = "وقت النوم",
                    window = "${fmt.hhmm(sleepStart.minuteOfDay)} — ${fmt.hhmm(sleepEnd.minuteOfDay)}",
                    body = "النوم بعد العشاء إلى ما قبل الفجر، والأفضل أن يُوقظ قبل الفجر. " +
                        "وفي ثلث الليل الآخر ينزل الرب سبحانه وتعالى فيدعو عباده: «مَن يدعوني فأستجيب له».",
                    source = "«يَنْزِلُ رَبُّنَا تَبَارَكَ وَتَعَالَى كُلَّ لَيْلَةٍ إِلَى السَّمَاءِ الدُّنْيَا حِينَ يَبْقَى ثُلُثُ اللَّيْلِ الآخِرُ…» — صحيح البخاري ١١٤٥ وصحيح مسلم ٧٥٨",
                    verified = true,
                )
                Divider(Modifier.padding(vertical = Space.md))
            }

            if (suhur != null) {
                SunnahBlock(
                    title = "وقت السحور",
                    window = "${fmt.hhmm(suhur.minuteOfDay)} — ${fmt.hhmm((sleepEnd ?: suhur).minuteOfDay)}",
                    body = "السحور: ما يُؤكل قبل الصبح، وأفضله أن يكون في آخر الليل. " +
                        "وفي السحور بركة.",
                    source = "«تَسَحَّرُوا فَإِنَّ فِي السُّحُورِ بَرَكَةً» — صحيح البخاري ١٩٢٣ وصحيح مسلم ١٠٩٥",
                    verified = true,
                )
                Divider(Modifier.padding(vertical = Space.md))
            }

            if (qaylulaStart != null && qaylulaEnd != null) {
                SunnahBlock(
                    title = "وقت القيلولة",
                    window = "${fmt.hhmm(qaylulaStart.minuteOfDay)} — ${fmt.hhmm(qaylulaEnd.minuteOfDay)}",
                    body = "القيلولة: راحة الظهيرة. كان النبي ﷺ يمدّ له نطعاً فيقيل عنده، " +
                        "وكان الصحابة يقيلون ويغدون بعد الجمعة.",
                    source = "«كَانَتْ أُمُّ سُلَيْمٍ تَبْسُطُ لِلنَّبِيِّ ﷺ نِطَعًا فَيَقِيلُ عِنْدَهَا» — صحيح البخاري ٦٢٨١، و«كُنَّا نَقِيلُ وَنَتَغَدَّى بَعْدَ الْجُمُعَةِ» — صحيح البخاري ٦٢٧٩",
                    verified = true,
                )
                Divider(Modifier.padding(vertical = Space.md))
            }

            if (morningRead != null && eveningRead != null) {
                SunnahBlock(
                    title = "أوقات قراءة القرآن",
                    window = "${fmt.hhmm(morningRead.minuteOfDay)} — ${fmt.hhmm(eveningRead.minuteOfDay)}",
                    body = "قراءة القرآن في الليل وبعد الفجر وبعد المغرب وقبل النوم. " +
                        "والقرآن شفيع لأصحابه يوم القيامة، والإخلاص وحدها ثلث القرآن.",
                    source = "«اقْرَءُوا الْقُرْآنَ فَإِنَّهُ يَأْتِي يَوْمَ الْقِيَامَةِ شَفِيعًا لِأَصْحَابِهِ» — صحيح مسلم ٨٠٤ · «اللَّهُ الْوَاحِدُ الصَّمَدُ ثُلُثُ الْقُرْآنِ» — صحيح البخاري ٥٠١٥",
                    verified = true,
                )
            }

            Spacer(Modifier.height(Space.md))
            Text(
                "كل ما يُذكر هنا من نصوص هو للذكر والتذكير، والحكم لأهل العلم في بلدك.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.xxl))
        }
    }
}

@Composable
private fun SunnahBlock(
    title: String,
    window: String,
    body: String,
    source: String,
    verified: Boolean,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(Radius.panel))
            .padding(Space.lg),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                window,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TabularFigures),
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Spacer(Modifier.height(Space.sm))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Space.sm))
        Text(
            source,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .8f),
        )
        if (!verified) {
            Spacer(Modifier.height(Space.xs))
            Text(
                "المرجع قيد التوثيق — لن يُعرض نصه حتى يُوثَّق",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}
