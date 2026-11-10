package app.miqat.core

/**
 * Number + clock rendering (research-agenda R7 — «اللغة الممزوجة» fix):
 * Arabic-Indic vs Latin digits, 12/24-hour clock. Every visible number in the
 * app goes through here so the presentation never mixes systems by accident.
 */
object NumFmt {

    private val AR_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    /**
     * Force LTR ordering for numeric runs. Arabic-Indic digits are bidi class
     * AN, so "6:38:04" renders REVERSED inside an RTL paragraph unless the run
     * is isolated (U+2066 LRI … U+2069 PDI). Every clock string goes through
     * here — this is the «لغة ممزوجة» fix at the rendering level.
     */
    private fun ltr(s: String): String = "⁦$s⁩"

    /** Map ASCII digits to Arabic-Indic (٠-٩) when asked. */
    fun digits(s: String, arabicIndic: Boolean): String {
        if (!arabicIndic) return s
        val sb = StringBuilder(s.length)
        for (c in s) sb.append(if (c in '0'..'9') AR_DIGITS[c - '0'] else c)
        return sb.toString()
    }

    /** "HH:MM" from minutes-of-day, honouring the 12/24-hour preference. */
    fun hhmm(minutesOfDay: Int, arabicIndic: Boolean, use12h: Boolean): String {
        val m = ((minutesOfDay % (24 * 60)) + 24 * 60) % (24 * 60)
        val h24 = m / 60
        val mm = m % 60
        return if (use12h) {
            val suffix = if (h24 < 12) "ص" else "م"
            val h12 = when {
                h24 % 12 == 0 -> 12
                else -> h24 % 12
            }
            ltr(digits("%d:%02d".format(h12, mm), arabicIndic)) + " $suffix"
        } else {
            ltr(digits("%02d:%02d".format(h24, mm), arabicIndic))
        }
    }

    /**
     * "H:MM:SS" (elapsed/remaining clocks) — the ticking display. Honours the
     * 12/24-hour preference like every other clock in the app: in 12-hour mode
     * the suffix follows the number («٤:٠٥:٠٩ م»).
     */
    fun hms(totalSeconds: Long, arabicIndic: Boolean, use12h: Boolean = false): String {
        val s = ((totalSeconds % 86400) + 86400) % 86400
        val h = s / 3600
        val mm = s % 3600 / 60
        val ss = s % 60
        return if (use12h) {
            val suffix = if (h < 12) "ص" else "م"
            val h12 = if (h % 12 == 0L) 12 else (h % 12).toInt()
            ltr(digits("%d:%02d:%02d".format(h12, mm, ss), arabicIndic)) + " $suffix"
        } else {
            ltr(digits("%d:%02d:%02d".format(h, mm, ss), arabicIndic))
        }
    }

    /** Plain integer (counts, offsets). */
    fun n(value: Int, arabicIndic: Boolean): String = ltr(digits("$value", arabicIndic))

    /** Decimal with a fixed number of fraction digits (bearings, coordinates). */
    fun decimal(value: Double, fractionDigits: Int, arabicIndic: Boolean): String =
        ltr(digits("%.${fractionDigits}f".format(value), arabicIndic))

    /** Decimal with a trailing unit (° etc.) — the unit stays inside the LTR
     *  isolate so the degree sign never jumps to the wrong side. */
    fun decimalUnit(value: Double, fractionDigits: Int, unit: String, arabicIndic: Boolean): String =
        ltr(digits("%.${fractionDigits}f".format(value), arabicIndic) + unit)

    /** A duration in minutes: "1:17" (h:mm) or "45 د" (under an hour). */
    fun duration(minutes: Int, arabicIndic: Boolean): String {
        val m = kotlin.math.abs(minutes)
        return if (m >= 60) {
            ltr(digits("%d:%02d".format(m / 60, m % 60), arabicIndic))
        } else {
            ltr(digits("%d".format(m), arabicIndic)) + " د"
        }
    }

    /** Signed minutes with an explicit sign — adjustment steppers. */
    fun signedMinutes(value: Int, arabicIndic: Boolean): String {
        val sign = if (value > 0) "+" else if (value < 0) "−" else ""
        return ltr(digits("$sign${kotlin.math.abs(value)}", arabicIndic))
    }
}
