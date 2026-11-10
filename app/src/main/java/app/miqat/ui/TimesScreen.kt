package app.miqat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.miqat.core.Appointment
import app.miqat.core.DayClock
import app.miqat.core.Moments
import app.miqat.core.Urgency
import app.miqat.core.UrgencyRules
import app.miqat.core.WorshipWindows

/** Minutes-of-day for a fractional hour (same round-half-up as the donor). */
fun minutesOf(hours: Double): Int {
    if (hours.isNaN()) return 0
    val total = (((hours + 0.5 / 60.0) % 24.0) + 24.0) % 24.0
    return (total * 60).toInt() % (24 * 60)
}

/**
 * TIMES — the day's schedule in one place: the six prayers (each with its own
 * dual clock) and the worship windows (each opening ITS OWN screen, not a line
 * in a list). This screen replaces the v1 stack of unrelated cards.
 */
@Composable
fun TimesScreen(
    state: AppState,
    fmt: Fmt,
    nowState: State<Long>,
    onOpenPrayer: (Int) -> Unit,
    onOpenWorship: (WorshipKind) -> Unit,
    onOpenGaps: () -> Unit,
    onOpenDistance: () -> Unit,
    onOpenInterval: () -> Unit,
    onOpenSunnah: () -> Unit,
) {
    val now = nowState.value
    val (y, m, d) = state.cityDate(now)
    val (ty, tm, td) = state.cityTomorrow(now)
    val times = state.times(y, m, d)
    val tomorrow = state.times(ty, tm, td)
    val nowHour = state.cityHourOfDay(now)
    val seg = DayClock.position(
        PRAYER_KEY.mapIndexed { i, k -> Appointment(k, PRAYER_NAMES[i], times.list[i]) },
        nowHour,
    )

    val night = WorshipWindows.night(maghrib = times.maghrib, fajrNext = tomorrow.fajr)
    val duha = WorshipWindows.duha(sunrise = times.sunrise, dhuhr = times.dhuhr)
    val sahar = WorshipWindows.sahar(maghrib = times.maghrib, fajrNext = tomorrow.fajr)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg),
    ) {
        Spacer(Modifier.height(Space.md))
        Text(
            "مواقيت اليوم",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Space.sm))

        val timeline = rememberTimeline(state, now)
        val gapAfter = Moments.gaps(timeline).associateBy { it.from.kind }

        PRAYER_NAMES.forEachIndexed { i, name ->
            val isNext = seg?.next?.key == PRAYER_KEY[i]
            val isCurrent = seg?.current?.key == PRAYER_KEY[i]
            val urgency = if (state.display.urgencyEnabled && isNext && seg != null) {
                UrgencyRules.of(
                    seg.remainingSeconds,
                    state.display.urgencySoonMinutes,
                    state.display.urgencyImminentMinutes,
                )
            } else Urgency.CALM
            PrayerRow(
                name = name,
                time = fmt.hhmm(minutesOf(times.list[i])),
                clock = when {
                    isNext -> "بعد " + fmt.hms(seg.remainingSeconds)
                    isCurrent -> "منذ " + fmt.hms(seg.elapsedSeconds)
                    else -> null
                },
                highlighted = isNext,
                urgency = urgency,
                onClick = { onOpenPrayer(i) },
            )
            // the gap to the next moment — drawn, not stacked
            val gap = gapAfter[PRAYER_KEY[i]]
            if (state.display.showGaps && gap != null &&
                state.display.gapCategories.contains(gap.to.category.labelAr)
            ) {
                GapBridge(gap.minutes, fmt.duration(gap.minutes))
            }
            Divider()
        }

        Spacer(Modifier.height(Space.lg))
        Text(
            "أوقات أخرى",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            "لكل واحدٍ منها نافذتُه وشرحُه ودليلُه — اضغط لتفصيلها.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Space.sm))

        WorshipRow(
            "وقت الضحى",
            "${fmt.hhmm(minutesOf(duha.startHour))} — ${fmt.hhmm(minutesOf(duha.endHour))}",
            "من بعد الشروق إلى ما قبل الظهر",
        ) { onOpenWorship(WorshipKind.DUHA) }
        Divider()
        WorshipRow(
            "وقت الأسحار",
            "${fmt.hhmm(minutesOf(sahar.startHour))} — ${fmt.hhmm(minutesOf(sahar.endHour))}",
            "آخر الليل قبل الفجر، حين يُستغفر",
        ) { onOpenWorship(WorshipKind.SAHAR) }
        Divider()
        WorshipRow(
            "منتصف الليل",
            fmt.hhmm(minutesOf(night.midnightHour)),
            "نصف الليل بين المغرب والفجر",
        ) { onOpenWorship(WorshipKind.MIDNIGHT) }
        Divider()
        WorshipRow(
            "الثلث الأخير من الليل",
            fmt.hhmm(minutesOf(night.lastThirdStartHour)),
            "آخر ثلث الليل — وقت الاستغفار",
        ) { onOpenWorship(WorshipKind.LAST_THIRD) }
        Divider()

        Spacer(Modifier.height(Space.lg))
        Text(
            "أدوات",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Space.xs))
        NavRow("كم بعدت عن هذا الوقت؟", "قياس البُعد عن أي ميقوت", onOpenDistance)
        Divider(Modifier.padding(horizontal = Space.lg))
        NavRow("المدد بين المواقيت", "كم يبقى بين كل موعد والذي يليه", onOpenGaps)
        Divider(Modifier.padding(horizontal = Space.lg))
        NavRow("احسب بين ميقوتين", "أي ميقوتين كان — من أي نوع", onOpenInterval)
        Divider(Modifier.padding(horizontal = Space.lg))
        NavRow("مواقيت السنة", "النوم والسحور والقيلولة وقراءة القرآن", onOpenSunnah)
        Spacer(Modifier.height(Space.xxl))
    }
}

@Composable
private fun PrayerRow(
    name: String,
    time: String,
    clock: String?,
    highlighted: Boolean,
    urgency: Urgency = Urgency.CALM,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = Space.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                color = when (urgency) {
                    Urgency.IMMINENT, Urgency.NOW -> UrgencyImminent
                    Urgency.SOON -> UrgencySoon
                    Urgency.CALM -> if (highlighted) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurface
                },
            )
            if (clock != null) {
                Text(
                    clock,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            time,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TabularFigures),
            color = when (urgency) {
                Urgency.IMMINENT, Urgency.NOW -> UrgencyImminent
                Urgency.SOON -> UrgencySoon
                Urgency.CALM -> if (highlighted) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun WorshipRow(
    title: String,
    value: String,
    blurb: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = Space.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(Space.md))
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
    }
}

/** A worship window gets a FULL SCREEN: what it is, when it is, and its source. */
@Composable
fun WorshipDetailScreen(state: AppState, fmt: Fmt, kind: WorshipKind, onBack: () -> Unit) {
    val nowState = rememberNowState()
    val now = nowState.value
    val (year, month, day) = state.cityDate(now)
    val (ty, tm, td) = state.cityTomorrow(now)
    val times = state.times(year, month, day)
    val tomorrow = state.times(ty, tm, td)
    val night = WorshipWindows.night(maghrib = times.maghrib, fajrNext = tomorrow.fajr)
    val duha = WorshipWindows.duha(sunrise = times.sunrise, dhuhr = times.dhuhr)
    val sahar = WorshipWindows.sahar(maghrib = times.maghrib, fajrNext = tomorrow.fajr)

    val content: Quad = when (kind) {
        WorshipKind.DUHA -> Quad(
            "من بعد الشروق إلى ما قبل الظهر",
            fmt.hhmm(minutesOf(duha.startHour)), fmt.hhmm(minutesOf(duha.endHour)),
            "صلاة الضحى — سنّة مؤكدة",
            "وقت الضحى رُكنٌ من أركان الصلاة التي سنّها النبي ﷺ. يبدأ بعد ارتفاع الشمس قدر رمح، " +
                "وينتهي قبل دخول وقت الظهر، ويُسنّ أن يُصليها من كثر أهلها.",
        )
        WorshipKind.SAHAR -> Quad(
            "آخر الليل قبل الفجر",
            fmt.hhmm(minutesOf(sahar.startHour)), fmt.hhmm(minutesOf(sahar.endHour)),
            "﴿…وَبِٱلْأَسْحَارِ هُمْ يَسْتَغْفِرُونَ﴾ — الذاريات ١٨",
            "وقت الأسحار هو آخر الليل قبل طلوع الفجر، وفيه يُستغفر ويُدعى. " +
                "ووقت السحور — الطعام الذي يُؤكل قبل الصبح — وقتٌ من أوّله، والسحور لا يلزم الفطر.",
        )
        WorshipKind.MIDNIGHT -> Quad(
            "نصف الليل بين المغرب والفجر",
            fmt.hhmm(minutesOf(night.midnightHour)), fmt.hhmm(minutesOf(night.midnightHour)),
            "منتصف الليل",
            "نعدّ الليل من المغرب إلى الفجر، فنتصفه بينهما. " +
                "وهو وقتٌ من أوقات الاستغفار والدعاء والقيام.",
        )
        WorshipKind.LAST_THIRD -> Quad(
            "آخر ثلث الليل",
            fmt.hhmm(minutesOf(night.lastThirdStartHour)), fmt.hhmm(minutesOf(tomorrow.fajr)),
            "الثلث الأخير من الليل",
            "آخر ثلث الليل بعد منتصفه، وهو وقتٌ للتقرب إلى الله بالقيام والدعاء والاستغفار، " +
                "وهو وقتٌ نعلمه من حديث النَّوميّ عن رجوع النبي ﷺ من قنادته.",
        )
    }

    MiqatScaffold(kind.label, onBack = onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg),
        ) {
            Spacer(Modifier.height(Space.sm))
            // the window itself — the reason the screen exists
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .5f),
                        RoundedCornerShape(20.dp),
                    )
                    .padding(Space.lg),
            ) {
                Column {
                    Text(
                        content.window,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        if (content.start == content.end) content.start else "${content.start} — ${content.end}",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(Space.sm))
                    ApproxChip()
                }
            }
            Spacer(Modifier.height(Space.lg))
            Text(
                "ما هو هذا الوقت؟",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                content.note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.lg))
            Text(
                "دليله",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                content.source,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.lg))
            Divider()
            InfoRow("التاريخ", "${fmt.n(year)}/${fmt.n(month)}/${fmt.n(day)}")
            InfoRow(
                "أصل الحساب",
                when (kind) {
                    WorshipKind.DUHA -> "من الشروق إلى الظهر"
                    else -> "من المغرب إلى الفجر"
                },
            )
            InfoRow("الطبيعة", "تقديري بحسب الاصطلاح")
            Spacer(Modifier.height(Space.xxl))
        }
    }
}

private data class Quad(
    val window: String,
    val start: String,
    val end: String,
    val source: String,
    val note: String,
)
