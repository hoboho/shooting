package ir.hoboho.shooting.core.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberFormatterExtraTest {

    @Test
    fun explicitNegativeSign_isRejected() {
        assertNull(NumberFormatter.parseToman("-5"))
        assertNull(NumberFormatter.parseNonNegativeInt("-5"))
        assertNull(NumberFormatter.parseToman("۱-۵"))
        assertNull(NumberFormatter.parseNonNegativeInt("−5"))
    }

    @Test
    fun persianDigitsAndSeparators_areNormalized() {
        assertEquals(1_800_000L, NumberFormatter.parseToman("۱٬۸۰۰٬۰۰۰"))
        assertEquals(1500, NumberFormatter.parseNonNegativeInt("۱٬۵۰۰"))
        assertEquals(12_345L, NumberFormatter.parseToman("١٢٣٤٥"))
        assertNull(NumberFormatter.parseToman(""))
        assertNull(NumberFormatter.parseToman("abc"))
    }

    @Test
    fun moneyIsNeverFractional() {
        assertEquals(0L, NumberFormatter.parseToman("0"))
        assertNull(NumberFormatter.parseToman("12.5"))
        assertNull(NumberFormatter.parseToman("۱۲٫۵"))
        assertNull(NumberFormatter.parseToman("12abc"))
        assertNull(NumberFormatter.parseNonNegativeInt("12.5"))
    }

    @Test
    fun groupedDisplay_doesNotHideInvalidInput() {
        assertEquals("۱٬۵۰۰", PersianDigits.toPersianGrouped("1500"))
        assertEquals("۱۲.۵", PersianDigits.toPersianGrouped("12.5"))
    }

    @Test
    fun groupingAndUnits_areStable() {
        assertEquals("1,800,000", NumberFormatter.formatPlain(1_800_000L))
        assertEquals("۱٬۸۰۰٬۰۰۰", NumberFormatter.formatPersian(1_800_000L))
        assertTrue(NumberFormatter.formatToman(1_800_000L).endsWith(NumberFormatter.TOMAN))
        assertTrue(NumberFormatter.formatMeters(520).endsWith(NumberFormatter.METER))
    }
}
