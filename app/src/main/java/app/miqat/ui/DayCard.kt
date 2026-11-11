package app.miqat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.miqat.core.Hijri
import app.miqat.core.IslamicDay
import app.miqat.core.IslamicDays
import app.miqat.core.Moment
import app.miqat.core.MomentCategory
import app.miqat.core.Moments

/**
 * THE DAY CARD (Design Council) — «ما هذا اليوم؟» in one screen, with no
 * stacked cards.
 *
 * Four visual registers, one shape each, the same shapes in the month cell:
 *  - PRAYERS → points on a spine (the SPACING between them IS the gap)
 *  - WINDOWS → bands behind the spine (intervals, never rows)
 *  - FASTS   → brackets around the day
 *  - HAJJ    → a pin card above the spine
 */
@Composable
fun DayCard(
    state: AppState,
    fmt: Fmt,
    year: Int,
    month: Int,
    day: Int,
    timeline: List<Moment>,
    hijri: IslamicDay,
    onOpenPrayer: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val fastDay = isVoluntaryFast(hijri)
    val rite = hajjRite(hijri)

    Column(
        Modifier
            .fillMaxWidth()
            .cardSurface(RoundedCornerShape(Radius.panel))
            .padding(Space.lg),
    ) {
        // both dates, equal weight — the Hijri and the Gregorian are one day
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                Hijri.label(hijri, fmt.ar),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
            Text(
                "${fmt.n(day)} / ${fmt.n(month)} / ${fmt.n(year)}",
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TabularFigures),
                color = scheme.onSurface,
            )
        }

        val event = eventName(hijri)
        if (event != null) {
            Spacer(Modifier.height(Space.xs))
            Text(event, style = MaterialTheme.typography.bodyMedium, color = scheme.tertiary)
        }

        // the HAJJ pin — ordinal, not clocked, so it is a card, not a band
        if (rite != null) {
            Spacer(Modifier.height(Space.md))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.row))
                    .background(scheme.tertiaryContainer.copy(alpha = .45f))
                    .padding(Space.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(scheme.tertiary, RoundedCornerShape(4.dp)),
                )
                Spacer(Modifier.width(Space.md))
                Column {
                    Text(rite, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                    Text(
                        riteNote(hijri),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(Space.md))
        DaySpine(
            fmt = fmt,
            timeline = timeline,
            modifier = Modifier.fillMaxWidth(),
            onOpenPrayer = onOpenPrayer,
        )

        if (fastDay) {
            Spacer(Modifier.height(Space.sm))
            Text(
                "يوم صيام تطوعي — الإمساك ${fmt.hhmm(momentMinute(timeline, Moments.IMSAK))} " +
                    "والإفطار ${fmt.hhmm(momentMinute(timeline, Moments.IFTAR))}",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.secondary,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                "الإمساك ماضٍ بقدر إعداداتك قبل الفجر — ${app.miqat.core.NotificationPoster.ESTIMATED}",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The spine: time flows DOWN, and the distances are real — the gap between two
 * prayers is the empty space between their points. The windows are bands
 * behind the spine; the prayers are points on it.
 */
@Composable
private fun DaySpine(
    fmt: Fmt,
    timeline: List<Moment>,
    modifier: Modifier = Modifier,
    onOpenPrayer: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val prayers = timeline.filter { it.category == MomentCategory.PRAYER }
    if (prayers.isEmpty()) return

    val start = prayers.first().minute
    val end = prayers.last().minute
    val span = (end - start).coerceAtLeast(60)
    val scale = 0.30f // dp per minute
    val heightDp = (span * scale).coerceIn(180f, 340f)

    fun yOf(minute: Int): Float = ((minute - start).toFloat() / span) * heightDp

    // the windows of the day — bands behind the spine
    val bandSpecs = listOf(
        Triple(Moments.IMSAK, Moments.IFTAR, MomentCategory.FASTING),
        Triple(Moments.DUHA_START, Moments.DUHA_END, MomentCategory.WORSHIP),
        Triple(Moments.SAHAR_START, Moments.SLEEP_END, MomentCategory.NIGHT),
    )

    Box(modifier.height(heightDp.dp)) {
        // the bands — on the SAME track as the points (the start side, right in
        // RTL), so a window reads as a span behind the instants it covers
        // TopStart = the reading start (the right in RTL) — the same track as
        // the prayer points
        Column(
            Modifier
                .align(Alignment.TopStart)
                .padding(start = 1.dp),
        ) {
        bandSpecs.forEach { (fromKind, toKind, category) ->
            val from = timeline.firstOrNull { it.kind == fromKind } ?: return@forEach
            val to = timeline.firstOrNull { it.kind == toKind } ?: return@forEach
            val top = yOf(from.minute).coerceAtLeast(0f)
            val bottom = yOf(to.minute).coerceAtMost(heightDp)
            if (bottom <= top) return@forEach
            Box(
                Modifier
                    .padding(top = top.dp)
                    .width(7.dp)
                    .height((bottom - top).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when (category) {
                            MomentCategory.FASTING -> scheme.secondary.copy(alpha = .55f)
                            MomentCategory.WORSHIP -> scheme.tertiary.copy(alpha = .45f)
                            else -> scheme.primary.copy(alpha = .28f)
                        },
                    ),
            )
        }
        }
        // the spine line
        Box(
            Modifier
                .width(1.dp)
                .height(heightDp.dp)
                .background(scheme.outline)
                .align(Alignment.TopStart),
        )
        // the prayer points + their labels, placed at their real times
        prayers.forEach { p ->
            val y = yOf(p.minute)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = y.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(9.dp)
                        .background(scheme.primary, RoundedCornerShape(5.dp)),
                )
                Spacer(Modifier.width(Space.md))
                Text(
                    p.labelAr,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurface,
                    modifier = Modifier.clickable { onOpenPrayer(p.slotIndex()) },
                )
                Spacer(Modifier.weight(1f))
                Text(
                    fmt.hhmm(p.minute),
                    style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TabularFigures),
                    color = scheme.onSurface,
                )
            }
        }
    }
}

/** The prayer slot of a moment (the order the app uses everywhere). */
private fun Moment.slotIndex(): Int = when (kind) {
    Moments.FAJR -> 0
    Moments.SUNRISE -> 1
    Moments.DHUHR -> 2
    Moments.ASR -> 3
    Moments.MAGHRIB -> 4
    else -> 5
}

private fun momentMinute(timeline: List<Moment>, kind: String): Int =
    timeline.firstOrNull { it.kind == kind }?.minute ?: 0

/** Is this day one of the voluntary fast days? */
fun isVoluntaryFast(h: IslamicDay): Boolean =
    IslamicDays.isMondayThursday(h) || IslamicDays.isWhiteDays(h) ||
        IslamicDays.isDayOfArafah(h) || IslamicDays.isAshura(h) ||
        IslamicDays.isSixOfShawwal(h)

/** The Hajj rite of the day, if any (8–13 Dhul-Hijjah). */
fun hajjRite(h: IslamicDay): String? = when {
    h.hMonth == 12 && h.hDay == 8 -> "الإحرام — أول أيام منى"
    h.hMonth == 12 && h.hDay == 9 -> "يوم عرفة — تكبير عرفة من الظهر إلى العصر"
    h.hMonth == 12 && h.hDay in 10..12 -> "رمي الجمار — أيام التشريق"
    else -> null
}

private fun riteNote(h: IslamicDay): String = when {
    h.hMonth == 12 && h.hDay == 8 -> "بداية أيام التشريق"
    h.hMonth == 12 && h.hDay == 9 -> "صيام يوم عرفة — رواه مسلم ١١٦٢"
    h.hMonth == 12 && h.hDay in 10..12 -> "من بعد الزوال"
    else -> ""
}

private fun eventName(h: IslamicDay): String? = when {
    IslamicDays.isEidAlFitr(h) -> "عيد الفطر المبارك"
    IslamicDays.isEidAlAdha(h) -> "عيد الأضحى المبارك"
    IslamicDays.isDayOfArafah(h) -> "يوم عرفة"
    IslamicDays.isAshura(h) -> "عاشوراء"
    IslamicDays.isWhiteDays(h) -> "أيام البيض"
    IslamicDays.isSixOfShawwal(h) -> "ستّ من شوال"
    IslamicDays.isEveOfRamadan(h) -> "ليلة رمضان"
    else -> null
}
