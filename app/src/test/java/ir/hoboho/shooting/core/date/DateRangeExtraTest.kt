package ir.hoboho.shooting.core.date

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DateRangeExtraTest {

    @Test
    fun epochMilli_convertsBackToTheSameJalaliDay() {
        val epochDay = JalaliCalendar.toEpochDay(1405, 8, 11)
        val epochMilli = java.time.LocalDate.ofEpochDay(epochDay)
            .atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        // The app's helper uses the device zone; with UTC it must round-trip.
        val originalZone = System.getProperty("user.timezone")
        try {
            System.setProperty("user.timezone", "UTC")
            assertEquals(epochDay, JalaliCalendar.fromEpochMilli(epochMilli))
            assertEquals(1405, JalaliCalendar.toJalali(JalaliCalendar.fromEpochMilli(epochMilli)).year)
        } finally {
            if (originalZone != null) System.setProperty("user.timezone", originalZone)
        }
    }

    @Test
    fun range_isInclusiveAtBothEnds() {
        val range = DateRange(20_000L, 20_002L)
        assertTrue(range.contains(20_000L))
        assertTrue(range.contains(20_001L))
        assertTrue(range.contains(20_002L))
        assertFalse(range.contains(19_999L))
        assertFalse(range.contains(20_003L))
        assertEquals(3, range.dayCount)
    }

    @Test
    fun reversedRange_isRejected() {
        var threw = false
        try {
            DateRange(20_002L, 20_000L)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun jalaliMonthAndYearBoundaries_areExact() {
        val first = JalaliCalendar.toEpochDay(1403, 1, 1)
        val insideSecondMonth = first + 40L // Ordibehesht, not Farvardin
        assertEquals(JalaliCalendar.toEpochDay(1403, 2, 1), JalaliCalendar.firstDayOfMonth(insideSecondMonth))
        assertEquals(JalaliCalendar.toEpochDay(1403, 2, 31), JalaliCalendar.lastDayOfMonth(insideSecondMonth))
        // 1403 is a leap year: Esfand has 30 days.
        assertEquals(30, JalaliCalendar.daysInMonth(1403, 12))
        assertEquals(JalaliCalendar.toEpochDay(1403, 12, 30), JalaliCalendar.lastDayOfYear(first))
        assertEquals(JalaliCalendar.toEpochDay(1404, 1, 1), JalaliCalendar.lastDayOfYear(first) + 1)
    }
}
