package ir.hoboho.shooting.core.date

/**
 * An inclusive Gregorian epoch-day range. Used by every period filter and report.
 */
data class DateRange(val startEpochDay: Long, val endEpochDay: Long) {

    init {
        require(endEpochDay >= startEpochDay) { "End date must not precede start date" }
    }

    operator fun contains(epochDay: Long): Boolean = epochDay in startEpochDay..endEpochDay

    /** Inclusive day count. */
    val dayCount: Int get() = JalaliCalendar.inclusiveDays(startEpochDay, endEpochDay)

    companion object {
        fun single(epochDay: Long) = DateRange(epochDay, epochDay)

        fun thisMonth(today: Long = JalaliCalendar.todayEpochDay()) =
            DateRange(JalaliCalendar.firstDayOfMonth(today), JalaliCalendar.lastDayOfMonth(today))

        fun thisYear(today: Long = JalaliCalendar.todayEpochDay()) =
            DateRange(JalaliCalendar.firstDayOfYear(today), JalaliCalendar.lastDayOfYear(today))

        fun lastDays(days: Int, today: Long = JalaliCalendar.todayEpochDay()): DateRange {
            require(days > 0) { "days must be positive" }
            return DateRange(today - days + 1, today)
        }

        /** Widest supported range, used by "all time" reports. */
        fun all() = DateRange(
            JalaliCalendar.nowruzEpochDay(JalaliCalendar.FIRST_YEAR),
            JalaliCalendar.nowruzEpochDay(JalaliCalendar.LAST_YEAR + 1) - 1,
        )
    }
}
