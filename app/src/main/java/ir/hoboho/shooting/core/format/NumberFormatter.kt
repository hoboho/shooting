package ir.hoboho.shooting.core.format

import kotlin.math.abs

/**
 * Money and length presentation.
 *
 * **Currency rule:** amounts are stored as integer Toman. They are never stored
 * or computed as floating point, and never silently converted to/from Rial.
 * Grouping is done in plain Kotlin so the formatter is thread-safe and has no
 * locale surprises (DecimalFormat is not thread-safe and follows device locale).
 */
object NumberFormatter {

    const val TOMAN = "تومان"
    const val METER = "متر"
    const val EM_DASH = "—"

    /** "1,800,000" — ASCII, machine-readable. */
    fun formatPlain(value: Long): String {
        val sign = if (value < 0) "-" else ""
        val digits = abs(value).toString()
        return sign + digits.reversed().chunked(3).joinToString(",").reversed()
    }

    /** "۱٬۸۰۰٬۰۰۰" — Persian digits with Persian thousands separators. */
    fun formatPersian(value: Long): String =
        PersianDigits.toPersian(formatPlain(value).replace(',', '٬'))

    /** "۱٬۸۰۰٬۰۰۰ تومان" */
    fun formatToman(amountToman: Long): String = "${formatPersian(amountToman)} $TOMAN"

    /** "۵۲۰ متر" */
    fun formatMeters(meters: Int): String = "${formatPersian(meters.toLong())} $METER"

    /** "۵۲۰" (Persian digits, no unit). */
    fun formatMetersValue(meters: Int): String = formatPersian(meters.toLong())

    fun formatInteger(value: Int): String = formatPersian(value.toLong())

    /** Parses user input (Persian or ASCII digits) into a non-negative integer. */
    fun parseNonNegativeInt(input: String): Int? {
        if (input.contains('-')) return null
        val normalized = PersianDigits.normalizeNumberInput(input)
        if (normalized.isEmpty()) return null
        val value = normalized.toLongOrNull() ?: return null
        return if (value in 0..Int.MAX_VALUE) value.toInt() else null
    }

    /** Parses user input into an integer Toman amount. Never produces a fraction. */
    fun parseToman(input: String): Long? {
        if (input.contains('-')) return null
        val normalized = PersianDigits.normalizeNumberInput(input)
        if (normalized.isEmpty()) return null
        val value = normalized.toLongOrNull() ?: return null
        return if (value >= 0) value else null
    }
}
