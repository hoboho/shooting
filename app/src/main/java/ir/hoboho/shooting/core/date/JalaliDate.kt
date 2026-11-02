package ir.hoboho.shooting.core.date

/**
 * A Jalali calendar date. Pure presentation value: [JalaliCalendar] converts it
 * to and from the Gregorian epoch days stored in the database.
 */
data class JalaliDate(val year: Int, val month: Int, val day: Int) : Comparable<JalaliDate> {

    init {
        require(year in JalaliCalendar.FIRST_YEAR..JalaliCalendar.LAST_YEAR) {
            "Jalali year out of range: $year"
        }
        require(month in 1..12) { "Jalali month must be in 1..12, was $month" }
        require(day in 1..JalaliCalendar.daysInMonth(year, month)) {
            "Invalid Jalali day $day for $year/$month"
        }
    }

    override fun compareTo(other: JalaliDate): Int = when {
        year != other.year -> year.compareTo(other.year)
        month != other.month -> month.compareTo(other.month)
        else -> day.compareTo(other.day)
    }

    /** ASCII "YYYY/MM/DD", for machine-readable exports. */
    override fun toString(): String = "%04d/%02d/%02d".format(year, month, day)

    companion object {
        val monthNames: List<String> = listOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
        )

        /** Index 0 = شنبه (Saturday). */
        val weekdayNames: List<String> = listOf(
            "شنبه", "یکشنبه", "دوشنبه", "سهشنبه", "چهارشنبه", "پنجشنبه", "جمعه",
        )

        val shortWeekdayNames: List<String> = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

        fun monthName(month: Int): String = monthNames[month - 1]

        /** «شهریور ۱۴۰۵» */
        fun monthLabel(year: Int, month: Int): String =
            "${monthName(month)} ${ir.hoboho.shooting.core.format.PersianDigits.toPersian(year.toString())}"
    }
}
