package app.miqat.core

/**
 * THE PROXIMITY MODEL — what the app may say, where, and when.
 *
 * The founder (2026-11-18): «اهتم بإظهار الأقرب فالأقرب … تجنب إظهار ما ليس
 * بقريب مثل صلاة العيد … إظهار فجر اليوم التالي في شاشة الصيام شيء غير ذكي».
 *
 * One classifier for the whole app. A slot is judged against ONE absolute
 * timeline (the same unit [Timers] uses: minutes from today's midnight, so
 * yesterday is negative and tomorrow is +1440). No surface invents its own
 * "is it near?" rule, and no duration wraps a passed moment into a future
 * one — that wrap is how the twelve-hour lie came back.
 *
 * The builder owns the day as well: [DayLine.of] calls [Moments.timeline] with
 * NO rite day, so the Eid prayer exists only when the caller passes an actual
 * Eid day. The old `hijriDay in 10..13` made Eid appear on four days.
 *
 * Pure Kotlin — no Android, no Compose, fully JVM-testable.
 */

/** The surface asking — the same slot may be relevant here and premature there. */
enum class Surface { HOME, TIMES, FASTING, ACTS, WIDGET, NOTIFY, DISTANCE, GAP, DAY }

enum class Verdict { DUE, NEAR, UPCOMING, NOT_RELEVANT }

/** The six timer kinds — one wording each, no synonyms (council P0.4). */
enum class TimerKind {
    PRAYER_REMAINING, ACT_REMAINING, SINCE_IQAMA, IQAMA_REMAINING,
    AVAILABLE, BETWEEN_ACTS, ELAPSED,
}

/**
 * A window between TWO timed acts, in absolute minutes. The founder wants
 * the remaining window shown as a NUMBER and as a SHAPE ([GapBridge]).
 * [open] is the premature guard: a window that has not begun has no
 * "remaining" — only its total may be printed.
 */
data class Span(val fromMin: Int, val toMin: Int, val nowMin: Int) {
    val totalMin: Int get() = toMin - fromMin
    val remainingMin: Int get() = (toMin - nowMin).coerceIn(0, totalMin.coerceAtLeast(0))
    val open: Boolean get() = nowMin in fromMin until toMin
    val frac: Float get() = remainingMin.toFloat() / totalMin.coerceAtLeast(1)
    val consumedFrac: Float get() = ((nowMin - fromMin).toFloat() / totalMin.coerceAtLeast(1)).coerceIn(0f, 1f)
}

/** One appointed slot on the absolute timeline. */
data class Slot(
    val kind: String,
    val labelAr: String,
    val category: MomentCategory,
    /** Absolute minutes from today's midnight: <0 yesterday, >1440 tomorrow. */
    val atMin: Int,
    val isEvent: Boolean = true,
    val estimated: Boolean = false,
    val endMin: Int? = null,
    val dayOffset: Int = 0,
)

/** The day's slots, built ONCE (no per-screen timeline builders). */
data class DayLine(val slots: List<Slot>) {
    fun at(kind: String): Slot? = slots.firstOrNull { it.kind == kind }

    companion object {
        /**
         * [eidDay] is the one and only way the Eid prayer enters the day —
         * the caller passes it only for the real Eid day (1 Shawwal or
         * 10 Dhul-Hijjah), never for tashreeq.
         */
        fun of(
            times: LocalPrayerTimes,
            tomorrowFajr: Double,
            labels: List<String>,
            minuteOf: (Double) -> Int,
            imsakLeadMinutes: Int = 15,
            eidDay: Boolean = false,
        ): DayLine {
            val slots = Moments.timeline(
                times, tomorrowFajr, labels, minuteOf,
                hijriMonth = 0, hijriDay = 0, // NO rite day: Eid is opt-in below
                imsakLeadMinutes = imsakLeadMinutes,
            ).map {
                Slot(it.kind, it.labelAr, it.category, it.minute, it.isEvent)
            }.toMutableList()

            if (eidDay) {
                val sunrise = minuteOf(times.sunrise)
                slots += Slot(Moments.EID_PRAYER, "صلاة العيد", MomentCategory.EID, sunrise + 20)
                slots += Slot(
                    Moments.EID_PRAYER_END, "نهاية صلاة العيد", MomentCategory.EID,
                    sunrise + 75, isEvent = false, endMin = sunrise + 75,
                )
            }
            // tomorrow's Fajr closes the night. It is a TOMORROW slot, so
            // the classifier can lock it out of screens where it is premature.
            slots += Slot(
                Moments.FAJR, labels[0], MomentCategory.PRAYER,
                minuteOf(tomorrowFajr) + 1440, dayOffset = 1,
            )
            return DayLine(slots.sortedBy { it.atMin })
        }
    }
}

/** A surface's admission rule — the one place "near" is defined. */
data class ContextRule(
    val surface: Surface,
    val categories: Set<MomentCategory>,
    val kinds: Set<String>? = null,
    val nearMin: Int = 60,
    val lookMin: Int = 720,
    /** Past events stay DUE for this long, then vanish (no 12-hour elapsed). */
    val elapsedCeilMin: Int = 60,
    val allowTomorrow: Boolean = false,
    /** A kind that must have entered before tomorrow may be revealed. */
    val tomorrowUnlockedBy: String? = null,
    val excludedTomorrowCategories: Set<MomentCategory> = emptySet(),
    val maxTomorrowMin: Int = 720,
)

data class Judged(
    val slot: Slot,
    val verdict: Verdict,
    /** Signed: negative = passed, positive = ahead. NEVER wrapped by 1440. */
    val startIn: Int,
    val elapsedMin: Int = 0,
)

/** One timer line: a prefix + a number. The UI formats [seconds] with Fmt. */
data class TimerLine(
    val prefixAr: String,
    val kind: TimerKind,
    val seconds: Long,
    val label2Ar: String? = null,
)

object Proximity {

    /** The rules. The founder's stricter fasting rule: never tomorrow's Fajr. */
    fun rules(surface: Surface): ContextRule = when (surface) {
        Surface.HOME -> ContextRule(
            surface, setOf(MomentCategory.PRAYER, MomentCategory.FASTING, MomentCategory.NIGHT),
            nearMin = 60, lookMin = 720, allowTomorrow = true,
            tomorrowUnlockedBy = Moments.ISHA,
            excludedTomorrowCategories = setOf(MomentCategory.EID, MomentCategory.HAJJ),
        )
        Surface.TIMES, Surface.DAY -> ContextRule(
            surface, MomentCategory.entries.toSet(), nearMin = 60, lookMin = 1440,
            allowTomorrow = true, tomorrowUnlockedBy = Moments.ISHA,
            excludedTomorrowCategories = setOf(MomentCategory.EID, MomentCategory.HAJJ),
        )
        // THE founder's exact complaint: tomorrow's Fajr is NOT a fact of the
        // fasting screen, before OR after the iftar. Only today's fast exists.
        Surface.FASTING -> ContextRule(
            surface, setOf(MomentCategory.FASTING, MomentCategory.PRAYER),
            kinds = setOf(Moments.IMSAK, Moments.IFTAR, Moments.FAJR),
            nearMin = 90, lookMin = 1440, allowTomorrow = false,
        )
        Surface.ACTS -> ContextRule(
            surface, MomentCategory.entries.toSet(), nearMin = 120, lookMin = 1440,
        )
        Surface.WIDGET, Surface.NOTIFY -> ContextRule(
            surface, setOf(MomentCategory.PRAYER), nearMin = 60, lookMin = 720,
            allowTomorrow = true, tomorrowUnlockedBy = Moments.ISHA,
        )
        Surface.DISTANCE, Surface.GAP -> ContextRule(
            surface, MomentCategory.entries.toSet(), nearMin = 0, lookMin = 1440,
        )
    }

    fun judge(slot: Slot, now: Int, rule: ContextRule, line: DayLine): Judged {
        val startIn = slot.atMin - now
        fun no() = Judged(slot, Verdict.NOT_RELEVANT, startIn)

        if (slot.category !in rule.categories) return no()
        if (rule.kinds != null && slot.kind !in rule.kinds) return no()
        // a boundary (e.g. "end of the window") is never a candidate fact
        if (!slot.isEvent) return no()

        if (slot.dayOffset > 0) {
            if (!rule.allowTomorrow || slot.category in rule.excludedTomorrowCategories) return no()
            val unlocked = rule.tomorrowUnlockedBy?.let { kind ->
                line.at(kind)?.let { now >= it.atMin }
            } ?: true
            if (!unlocked) return no()
            if (startIn > rule.maxTomorrowMin) return no()
        }
        // no 1440-wrap: a long-passed moment is silent, not "in 16 hours"
        if (startIn < 0 && -startIn > rule.elapsedCeilMin) return no()
        if (startIn > rule.lookMin) return no()

        val verdict = when {
            startIn <= 0 -> Verdict.DUE
            startIn <= rule.nearMin -> Verdict.NEAR
            else -> Verdict.UPCOMING
        }
        return Judged(slot, verdict, startIn, if (startIn < 0) -startIn else 0)
    }

    /** The facts a surface may show, nearest first. */
    fun of(line: DayLine, now: Int, surface: Surface): List<Judged> {
        val rule = rules(surface)
        return line.slots.map { judge(it, now, rule, line) }
            .filter { it.verdict != Verdict.NOT_RELEVANT }
            .sortedWith(
                compareBy<Judged> {
                    when (it.verdict) {
                        Verdict.DUE -> 0
                        Verdict.NEAR -> 1
                        Verdict.UPCOMING -> 2
                        Verdict.NOT_RELEVANT -> 3
                    }
                }.thenBy { kotlin.math.abs(it.startIn) }
            )
    }

    /** The primary fact is the next PRAYER — night markers are context lines. */
    fun primary(line: DayLine, now: Int, surface: Surface): Judged? {
        val facts = of(line, now, surface)
        return facts.firstOrNull { it.slot.category == MomentCategory.PRAYER }
            ?: facts.firstOrNull()
    }

    /** The ONE vocabulary for every timer line (no «بعد» / «يبقى» / «مرّ من»). */
    fun timerLine(
        kind: TimerKind,
        seconds: Long,
        labelAr: String,
        label2Ar: String? = null,
    ): TimerLine = when (kind) {
        TimerKind.PRAYER_REMAINING -> TimerLine("بقي لـ$labelAr", kind, seconds)
        TimerKind.ACT_REMAINING -> TimerLine("بقي من وقت $labelAr", kind, seconds)
        TimerKind.SINCE_IQAMA -> TimerLine("منذ الإقامة · $labelAr", kind, seconds)
        TimerKind.IQAMA_REMAINING -> TimerLine("بقي إلى إقامة $labelAr", kind, seconds)
        TimerKind.AVAILABLE -> TimerLine("المتاح حتى $labelAr", kind, seconds)
        TimerKind.BETWEEN_ACTS -> TimerLine("بين $labelAr و${label2Ar.orEmpty()}", kind, seconds)
        TimerKind.ELAPSED -> TimerLine("منذ أذان $labelAr", kind, seconds)
    }
}
