package app.miqat.core

import java.util.Calendar
import java.util.TimeZone

/**
 * THE CALENDAR WINDOW — one model, three readings of the same days.
 *
 * The founder's complaint (2026-11-11): the calendar carried the title of the
 * month it BEGAN with, so a Gregorian month that turns the Hijri month
 * halfway through was titled with the first half only — and 99% of Gregorian
 * months are split. The cure is not a cleverer title: it is an EXPLICIT AXIS.
 *
 *  - [CalendarContext.HIJRI] — the grid is one Hijri month (29–30 days); the
 *    Gregorian date is the second number in the cell.
 *  - [CalendarContext.GREGORIAN] — the grid is one Gregorian month; the Hijri
 *    date is the second number.
 *  - [CalendarContext.ACTS] — the Gregorian axis with the worship layer in
 *    front (the sigils come forward).
 *
 * The title always names the SPINE; the other axis is printed as a SPAN
 * («٢١ جمادى الأولى – ٢٠ جمادى الآخرة»), never as a name. The grid marks the
 * turn with a hairline and a placard, so a reader can never mistake two
 * months for one.
 */
enum class CalendarContext(val labelAr: String, val noteAr: String) {
    HIJRI("الهجري", "الشهر الهجري كاملًا على المحور"),
    GREGORIAN("الميلادي", "الشهر الميلادي على المحور"),
    ACTS("الطاعات", "ما في الشهر من صيام ومناسبات"),

    ;

    companion object {
        fun next(c: CalendarContext): CalendarContext = entries[(c.ordinal + 1) % entries.size]
    }
}

/** One day of the window. */
data class CalendarCell(
    val gregorianYear: Int,
    val gregorianMonth: Int,
    val gregorianDay: Int,
    val hijriYear: Int,
    val hijriMonth: Int,
    val hijriDay: Int,
    /** 0 = Saturday (the grid's first column in RTL). */
    val weekday: Int,
    val isFast: Boolean,
    val isRite: Boolean,
    val isToday: Boolean,
    /** The OTHER axis turns on this day. */
    val turnsHere: Boolean,
    /** The placard that names the new month («جمادى الآخرة» / «ديسمبر»). */
    val turnsToLabel: String,
)

/** One month, on one axis. */
data class CalendarWindow(
    val context: CalendarContext,
    /** The month's days, in order. */
    val days: List<CalendarCell>,
    /** The empty cells before the first day (the weekday of the 1st). */
    val leadBlanks: Int,
    /** The spine: (year, month) — the Gregorian pair or the Hijri pair. */
    val spineYear: Int,
    val spineMonth: Int,
    /** The other axis: the (month, year) pairs the window covers. */
    val otherMonths: List<Pair<Int, Int>>,
    val otherYears: List<Int>,
) {
    /** The spine's own name — the title's first line. */
    val titleAr: String
        get() = when (context) {
            CalendarContext.HIJRI ->
                "${Hijri.MONTHS_AR[(spineMonth - 1).coerceIn(0, 11)]} ${spineYear}"
            else -> "${MONTHS_AR_GREGORIAN[(spineMonth - 1).coerceIn(0, 11)]} $spineYear"
        }

    /** The other axis's span — the title's second line (never a bare name). */
    val subtitleAr: String
        get() {
            if (otherMonths.isEmpty()) return ""
            val months = otherMonths.map { (m, _) ->
                when (context) {
                    CalendarContext.HIJRI ->
                        MONTHS_AR_GREGORIAN[(m - 1).coerceIn(0, 11)]
                    else -> Hijri.MONTHS_AR[(m - 1).coerceIn(0, 11)]
                }
            }
            val first = days.firstOrNull()
            val last = days.lastOrNull()
            val range = when {
                context == CalendarContext.HIJRI && first != null && last != null ->
                    "${first.gregorianDay} ${MONTHS_AR_GREGORIAN[(first.gregorianMonth - 1).coerceIn(0, 11)]}" +
                        " – ${last.gregorianDay} ${MONTHS_AR_GREGORIAN[(last.gregorianMonth - 1).coerceIn(0, 11)]}"
                months.size == 1 -> months.first()
                else -> months.first() + " – " + months.last()
            }
            val year = if (otherYears.size == 1) " ${otherYears.first()}" else ""
            return range + year
        }
}

/** The Gregorian months' names in Arabic. */
val MONTHS_AR_GREGORIAN = listOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر",
)

/**
 * Builds a calendar window for any context — the ONE place the calendar's
 * geometry is decided (the screen, the ledger and the exported card all read
 * it, so they can never disagree).
 */
object CalendarWindows {

    /**
     * @param context what the grid is organized around
     * @param gregorianYear/gregorianMonth the Gregorian anchor: in the
     *   Gregorian/acts contexts it IS the month; in the Hijri context it is
     *   the anchor date the Hijri month is derived from
     * @param hijriOffset the user's own sighting adjustment (days)
     * @param today the city's today (year, month, day)
     */
    fun build(
        context: CalendarContext,
        gregorianYear: Int,
        gregorianMonth: Int,
        hijriOffset: Int,
        today: Triple<Int, Int, Int>,
    ): CalendarWindow {
        if (context == CalendarContext.HIJRI) {
            return hijriWindow(gregorianYear, gregorianMonth, hijriOffset, today)
        }
        val cal = Calendar.getInstance().apply { clear(); set(gregorianYear, gregorianMonth - 1, 1) }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val leadBlanks = cal.get(Calendar.DAY_OF_WEEK) % 7
        val cells = ArrayList<CalendarCell>(daysInMonth)
        var lastMonth = 0
        var lastYear = 0
        for (d in 1..daysInMonth) {
            val h = Hijri.at(gregorianYear, gregorianMonth, d, hijriOffset)
            val turn = lastMonth != 0 && (h.hMonth != lastMonth || h.hYear != lastYear)
            val placard = if (turn) {
                "${Hijri.MONTHS_AR[(h.hMonth - 1).coerceIn(0, 11)]} ${h.hMonth}"
            } else ""
            lastMonth = h.hMonth
            lastYear = h.hYear
            cells.add(
                cell(
                    gy = gregorianYear, gm = gregorianMonth, gd = d,
                    hy = h.hYear, hm = h.hMonth, hd = h.hDay,
                    today = today,
                    turnsHere = turn,
                    placard = placard,
                ),
            )
        }
        return CalendarWindow(
            context = context,
            days = cells,
            leadBlanks = leadBlanks,
            spineYear = gregorianYear,
            spineMonth = gregorianMonth,
            otherMonths = cells.map { it.hijriMonth to it.hijriYear }.distinct(),
            otherYears = cells.map { it.hijriYear }.distinct(),
        )
    }

    /** The Gregorian month that holds a Hijri month (the Hijri mode's navigation). */
    fun anchorForHijriMonth(hYear: Int, hMonth: Int, hijriOffset: Int): Pair<Int, Int> {
        val icu = android.icu.util.IslamicCalendar(
            android.icu.util.ULocale("en@calendar=islamic-umalqura"),
        ).apply {
            clear()
            set(Calendar.YEAR, hYear)
            set(Calendar.MONTH, hMonth - 1)
            set(Calendar.DAY_OF_MONTH, 15)
        }
        val millis = icu.timeInMillis + hijriOffset * 86_400_000L
        val g = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
        return g.get(Calendar.YEAR) to (g.get(Calendar.MONTH) + 1)
    }

    /** The Hijri month that holds the anchor — its days, each with its date. */
    private fun hijriWindow(
        gregorianYear: Int,
        gregorianMonth: Int,
        hijriOffset: Int,
        today: Triple<Int, Int, Int>,
    ): CalendarWindow {
        val anchor = Hijri.at(gregorianYear, gregorianMonth, 15, hijriOffset)
        val hYear = anchor.hYear
        val hMonth = anchor.hMonth
        // the ICU's own Hijri month: its first day and its length
        val icu = android.icu.util.IslamicCalendar(
            android.icu.util.ULocale("en@calendar=islamic-umalqura"),
        ).apply {
            clear()
            set(Calendar.YEAR, hYear)
            set(Calendar.MONTH, hMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val length = icu.getActualMaximum(Calendar.DAY_OF_MONTH)
        // the user's sighting adjustment shifts the whole month
        val startMillis = icu.timeInMillis + hijriOffset * 86_400_000L
        val utc = TimeZone.getTimeZone("UTC")
        val cells = ArrayList<CalendarCell>(length)
        var lastGregorianMonth = 0
        for (day in 1..length) {
            val millis = startMillis + (day - 1) * 86_400_000L
            val g = Calendar.getInstance(utc).apply { timeInMillis = millis }
            val gy = g.get(Calendar.YEAR)
            val gm = g.get(Calendar.MONTH) + 1
            val gd = g.get(Calendar.DAY_OF_MONTH)
            val h = Hijri.at(gy, gm, gd, hijriOffset)
            val turn = lastGregorianMonth != 0 && gm != lastGregorianMonth
            lastGregorianMonth = gm
            cells.add(
                cell(
                    gy = gy, gm = gm, gd = gd,
                    hy = h.hYear, hm = h.hMonth, hd = h.hDay,
                    today = today,
                    turnsHere = turn,
                    placard = if (turn) MONTHS_AR_GREGORIAN[(gm - 1).coerceIn(0, 11)] else "",
                ),
            )
        }
        val lead = cells.firstOrNull()?.weekday ?: 0
        return CalendarWindow(
            context = CalendarContext.HIJRI,
            days = cells,
            leadBlanks = lead,
            spineYear = hYear,
            spineMonth = hMonth,
            otherMonths = cells.map { it.gregorianMonth to it.gregorianYear }.distinct(),
            otherYears = cells.map { it.gregorianYear }.distinct(),
        )
    }

    /** One cell, with the fasting marks the calendar shows. */
    private fun cell(
        gy: Int,
        gm: Int,
        gd: Int,
        hy: Int,
        hm: Int,
        hd: Int,
        today: Triple<Int, Int, Int>,
        turnsHere: Boolean,
        placard: String,
    ): CalendarCell {
        // the REAL weekday: with 0 the grid never marked Monday/Thursday as
        // fast days, and disagreed with the day card (the council, 2026-11-11)
        val weekday = Calendar.getInstance().apply { clear(); set(gy, gm - 1, gd) }
            .get(Calendar.DAY_OF_WEEK)
        val hijri = IslamicDay(hy, hm, hd, weekday)
        return CalendarCell(
            gregorianYear = gy,
            gregorianMonth = gm,
            gregorianDay = gd,
            hijriYear = hy,
            hijriMonth = hm,
            hijriDay = hd,
            // the weekday as the grid counts it (0 = Saturday, the first
            // column in RTL) — and the same value the fasting rules test
            weekday = if (weekday == Calendar.SATURDAY) 7 else weekday - 1,
            isFast = IslamicDays.isWhiteDays(hijri) ||
                IslamicDays.isMondayThursday(hijri) ||
                IslamicDays.isDayOfArafah(hijri) ||
                IslamicDays.isAshura(hijri) ||
                IslamicDays.isSixOfShawwal(hijri),
            isRite = hajjRiteOf(hijri) != null,
            isToday = gy == today.first && gm == today.second && gd == today.third,
            turnsHere = turnsHere,
            turnsToLabel = placard,
        )
    }

    /** The Hajj rite of a day, if any. */
    private fun hajjRiteOf(h: IslamicDay): String? = when {
        h.hMonth == 12 && h.hDay == 8 -> "الإحرام — أول أيام منى"
        h.hMonth == 12 && h.hDay == 9 -> "يوم عرفة — تكبير عرفة من الظهر إلى العصر"
        h.hMonth == 12 && h.hDay in 10..12 -> "رمي الجمار — أيام التشريق"
        else -> null
    }
}
