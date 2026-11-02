package ir.hoboho.shooting.core.date

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate

class JalaliCalendarTest {

    private fun epoch(y: Int, m: Int, d: Int): Long = LocalDate.of(y, m, d).toEpochDay()

    @Test
    fun gregorianToJalali_knownAnchors() {
        assertEquals(JalaliDate(1400, 1, 1), JalaliCalendar.toJalali(epoch(2021, 3, 21)))
        assertEquals(JalaliDate(1401, 1, 1), JalaliCalendar.toJalali(epoch(2022, 3, 21)))
        assertEquals(JalaliDate(1402, 1, 1), JalaliCalendar.toJalali(epoch(2023, 3, 21)))
        assertEquals(JalaliDate(1403, 1, 1), JalaliCalendar.toJalali(epoch(2024, 3, 20)))
        assertEquals(JalaliDate(1403, 12, 30), JalaliCalendar.toJalali(epoch(2025, 3, 20)))
        assertEquals(JalaliDate(1404, 1, 1), JalaliCalendar.toJalali(epoch(2025, 3, 21)))
        assertEquals(JalaliDate(1405, 1, 1), JalaliCalendar.toJalali(epoch(2026, 3, 21)))
        assertEquals(JalaliDate(1405, 8, 11), JalaliCalendar.toJalali(epoch(2026, 11, 2)))
        assertEquals(JalaliDate(1357, 11, 22), JalaliCalendar.toJalali(epoch(1979, 2, 11)))
    }

    @Test
    fun jalaliToGregorian_knownAnchors() {
        assertEquals(epoch(2021, 3, 21), JalaliCalendar.toEpochDay(JalaliDate(1400, 1, 1)))
        assertEquals(epoch(2024, 3, 20), JalaliCalendar.toEpochDay(JalaliDate(1403, 1, 1)))
        assertEquals(epoch(2025, 3, 20), JalaliCalendar.toEpochDay(JalaliDate(1403, 12, 30)))
        assertEquals(epoch(2026, 11, 2), JalaliCalendar.toEpochDay(JalaliDate(1405, 8, 11)))
    }

    @Test
    fun leapYears_matchOfficialCalendar() {
        // The official calendar breaks the naive 33-year cycle at 1403/1404.
        val officialLeaps = setOf(1391, 1395, 1399, 1403, 1408, 1412, 1416, 1420)
        for (year in 1390..1420) {
            assertEquals("leap year $year", year in officialLeaps, JalaliCalendar.isLeapYear(year))
        }
        assertEquals(29, JalaliCalendar.daysInMonth(1404, 12))
        assertEquals(30, JalaliCalendar.daysInMonth(1403, 12))
    }

    @Test
    fun monthLengths_followJalaliRules() {
        for (month in 1..6) assertEquals(31, JalaliCalendar.daysInMonth(1405, month))
        for (month in 7..11) assertEquals(30, JalaliCalendar.daysInMonth(1405, month))
        assertEquals(12, JalaliDate.monthNames.size)
        assertEquals("شهریور", JalaliDate.monthName(6))
        assertEquals("اسفند", JalaliDate.monthName(12))
    }

    @Test
    fun roundTrip_everyDayOfTwentyYears() {
        val start = JalaliCalendar.toEpochDay(JalaliDate(1390, 1, 1))
        val end = JalaliCalendar.toEpochDay(JalaliDate(1410, 12, 29))
        var day = start
        while (day <= end) {
            val jalali = JalaliCalendar.toJalali(day)
            assertEquals("round trip for epoch $day", day, JalaliCalendar.toEpochDay(jalali))
            day++
        }
    }

    @Test
    fun weekdayAndMonthBoundaries() {
        // 1970-01-01 was a Thursday, index 5 in a Saturday-first Persian week.
        assertEquals(5, JalaliCalendar.dayOfWeek(0L))
        assertEquals("پنجشنبه", JalaliCalendar.weekdayName(0L))
        val today = epoch(2026, 11, 2)
        assertEquals(JalaliCalendar.toEpochDay(JalaliDate(1405, 8, 1)), JalaliCalendar.firstDayOfMonth(today))
        assertEquals(JalaliCalendar.toEpochDay(JalaliDate(1405, 8, 30)), JalaliCalendar.lastDayOfMonth(today))
        assertEquals(JalaliCalendar.toEpochDay(JalaliDate(1405, 1, 1)), JalaliCalendar.firstDayOfYear(today))
        // 2026-11-02 is a Monday (index 2 in the Saturday-first Persian week).
        assertEquals(2, JalaliCalendar.dayOfWeek(today))
        assertEquals(today, JalaliCalendar.startOfWeek(today) + 2)
    }

    @Test
    fun dateRange_containsAndCount() {
        val range = DateRange.thisMonth(epoch(2026, 11, 2))
        assertTrue(range.contains(epoch(2026, 11, 2)))
        assertFalse(range.contains(epoch(2026, 12, 1)))
        assertEquals(30, range.dayCount)
        assertTrue(DateRange.thisYear(epoch(2026, 11, 2)).dayCount in 365..366)
    }

    @Test
    fun invalidJalaliDate_isRejected() {
        try {
            JalaliDate(1404, 12, 30) // 1404 is not a leap year
            fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("Invalid Jalali day"))
        }
        try {
            JalaliCalendar.toJalali(Long.MIN_VALUE)
            fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("outside"))
        }
    }
}
