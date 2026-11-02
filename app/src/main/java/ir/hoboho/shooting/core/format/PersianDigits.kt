package ir.hoboho.shooting.core.format

/**
 * Persian digit conversion. Digits are converted **only at the presentation
 * boundary**: database values, JSON backups and calculations stay in ASCII.
 */
object PersianDigits {

    private val PERSIAN = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    private const val PERSIAN_THOUSANDS_SEPARATOR = '٬' // U+066C
    private const val PERSIAN_DECIMAL_SEPARATOR = '٫'  // U+066B

    fun toPersian(input: String): String {
        if (input.isEmpty()) return input
        val builder = StringBuilder(input.length)
        for (char in input) {
            builder.append(if (char in '0'..'9') PERSIAN[char - '0'] else char)
        }
        return builder.toString()
    }

    /** Converts Persian and Arabic-Indic digits back to ASCII. */
    fun toLatin(input: String): String {
        if (input.isEmpty()) return input
        val builder = StringBuilder(input.length)
        for (char in input) {
            val ascii = when (char) {
                in '۰'..'۹' -> '0' + (char - '۰')
                in '٠'..'٩' -> '0' + (char - '٠')
                PERSIAN_THOUSANDS_SEPARATOR -> ','
                PERSIAN_DECIMAL_SEPARATOR -> '.'
                else -> char
            }
            builder.append(ascii)
        }
        return builder.toString()
    }

    /**
     * Removes only grouping separators and whitespace. Every other character is
     * deliberately preserved so that a pasted fraction, decimal point or letter
     * is **rejected by the parser** instead of being silently stripped away.
     * All numeric fields in the app are integers.
     */
    fun normalizeNumberInput(input: String): String =
        toLatin(input).filter { it != ',' && !it.isWhitespace() }

    /** "۱٬۵۰۰" — Persian digits with Persian thousands separators. */
    fun toPersianGrouped(input: String): String {
        val latin = toLatin(input)
        val digitsOnly = latin.filter(Char::isDigit)
        // If anything non-numeric slipped in (e.g. a pasted decimal point), show
        // it verbatim instead of hiding it behind a formatted number.
        if (digitsOnly.isEmpty() || latin.any { !it.isDigit() && it != ',' && !it.isWhitespace() }) {
            return toPersian(input)
        }
        val grouped = digitsOnly.reversed().chunked(3).joinToString("٬").reversed()
        return toPersian(grouped)
    }
}
