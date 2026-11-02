package ir.hoboho.shooting.core.format

import ir.hoboho.shooting.core.date.DateRange
import ir.hoboho.shooting.core.date.JalaliCalendar
import ir.hoboho.shooting.core.date.JalaliDate

/** Jalali date presentation, built on stored epoch-day values. */
object DateFormatter {

    /** "۱۴۰۵/۰۸/۱۱" */
    fun format(epochDay: Long, persianDigits: Boolean = true): String {
        val j = JalaliCalendar.toJalali(epochDay)
        val raw = "%04d/%02d/%02d".format(j.year, j.month, j.day)
        return if (persianDigits) PersianDigits.toPersian(raw) else raw
    }

    /** "۱۱ آبان ۱۴۰۵" */
    fun formatLong(epochDay: Long, persianDigits: Boolean = true): String {
        val j = JalaliCalendar.toJalali(epochDay)
        val raw = "${j.day} ${JalaliDate.monthName(j.month)} ${j.year}"
        return if (persianDigits) PersianDigits.toPersian(raw) else raw
    }

    /** "سهشنبه ۱۱ آبان ۱۴۰۵" */
    fun formatLongWithWeekday(epochDay: Long): String =
        PersianDigits.toPersian("${JalaliCalendar.weekdayName(epochDay)} ${formatLong(epochDay, false)}")

    /** "آبان ۱۴۰۵" */
    fun formatMonth(epochDay: Long): String {
        val j = JalaliCalendar.toJalali(epochDay)
        return PersianDigits.toPersian(JalaliDate.monthLabel(j.year, j.month))
    }

    /** "۱۴۰۵/۰۱/۰۱ — ۱۴۰۵/۰۱/۳۱" */
    fun formatRange(range: DateRange, persianDigits: Boolean = true): String =
        "${format(range.startEpochDay, persianDigits)} — ${format(range.endEpochDay, persianDigits)}"
}
