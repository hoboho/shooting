package ir.hoboho.shooting.core.format

import ir.hoboho.shooting.core.date.JalaliCalendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FormatterTest {

    @Test
    fun persianDigits_roundTrip() {
        assertEquals("۱۴۰۵/۰۸/۱۱", PersianDigits.toPersian("1405/08/11"))
        assertEquals("1405/08/11", PersianDigits.toLatin("۱۴۰۵/۰۸/۱۱"))
        assertEquals("1405", PersianDigits.toLatin("١٤٠٥")) // Arabic-Indic digits
        assertEquals("۱٬۵۰۰", PersianDigits.toPersianGrouped("۱۵۰۰"))
        assertEquals("1,500", PersianDigits.toLatin("۱٬۵۰۰"))
    }

    @Test
    fun numberInput_isNormalized() {
        assertEquals("1500", PersianDigits.normalizeNumberInput("۱٬۵۰۰"))
        assertEquals("1250", PersianDigits.normalizeNumberInput("1,250"))
        assertEquals(1500, NumberFormatter.parseNonNegativeInt("۱٬۵۰۰"))
        assertEquals(1500L, NumberFormatter.parseToman("1,500"))
        assertNull(NumberFormatter.parseToman("-5"))
        assertNull(NumberFormatter.parseNonNegativeInt(""))
    }

    @Test
    fun money_isIntegerTomanAndNeverFloatingPoint() {
        assertEquals("1,800,000", NumberFormatter.formatPlain(1_800_000L))
        assertEquals("۱٬۸۰۰٬۰۰۰", NumberFormatter.formatPersian(1_800_000L))
        assertEquals("۱٬۸۰۰٬۰۰۰ تومان", NumberFormatter.formatToman(1_800_000L))
        assertEquals("۵۲۰ متر", NumberFormatter.formatMeters(520))
    }

    @Test
    fun dates_useJalaliCalendar() {
        val day = LocalDate.of(2026, 11, 2).toEpochDay()
        assertEquals("۱۴۰۵/۰۸/۱۱", DateFormatter.format(day))
        assertEquals("۱۴۰۵/۰۸/۱۱", DateFormatter.format(day, persianDigits = true))
        assertEquals("1405/08/11", JalaliCalendar.toJalali(day).toString())
        assertTrue(DateFormatter.formatLong(day).contains("آبان"))
        assertTrue(DateFormatter.formatRange(ir.hoboho.shooting.core.date.DateRange.single(day)).contains("۱۴۰۵"))
    }
}
