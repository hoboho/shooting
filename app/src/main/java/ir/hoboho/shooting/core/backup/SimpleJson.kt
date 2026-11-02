package ir.hoboho.shooting.core.backup

/**
 * A small, strict JSON reader/writer for the backup format.
 *
 * The backup schema is fixed and versioned, so a dependency-free codec is
 * preferable to a reflection-based serializer: it is deterministic (stable key
 * order makes checksums reproducible), fully unit-testable on the JVM and never
 * breaks when a Kotlin data class is renamed.
 */
sealed interface JsonValue {
    data class Obj(val values: Map<String, JsonValue>) : JsonValue
    data class Arr(val values: List<JsonValue>) : JsonValue
    data class Str(val value: String) : JsonValue
    data class Num(val raw: String) : JsonValue
    data class Bool(val value: Boolean) : JsonValue
    data object Null : JsonValue
}

object SimpleJson {

    fun parse(text: String): JsonValue {
        val parser = Parser(text)
        val value = parser.parseValue()
        parser.skipWhitespace()
        require(parser.atEnd()) { "Trailing characters in JSON" }
        return value
    }

    fun stringify(value: JsonValue): String {
        val out = StringBuilder()
        write(value, out)
        return out.toString()
    }

    private fun write(value: JsonValue, out: StringBuilder) {
        when (value) {
            is JsonValue.Obj -> {
                out.append('{')
                value.values.entries.forEachIndexed { index, (key, child) ->
                    if (index > 0) out.append(',')
                    writeString(key, out)
                    out.append(':')
                    write(child, out)
                }
                out.append('}')
            }
            is JsonValue.Arr -> {
                out.append('[')
                value.values.forEachIndexed { index, child ->
                    if (index > 0) out.append(',')
                    write(child, out)
                }
                out.append(']')
            }
            is JsonValue.Str -> writeString(value.value, out)
            is JsonValue.Num -> out.append(value.raw)
            is JsonValue.Bool -> out.append(if (value.value) "true" else "false")
            JsonValue.Null -> out.append("null")
        }
    }

    private fun writeString(text: String, out: StringBuilder) {
        out.append('"')
        for (char in text) {
            when (char) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                '\b' -> out.append("\\b")
                '\u000C' -> out.append("\\f")
                else -> if (char < ' ') {
                    out.append("\\u")
                    out.append(char.code.toString(16).padStart(4, '0'))
                } else {
                    out.append(char)
                }
            }
        }
        out.append('"')
    }

    private class Parser(private val text: String) {
        private var pos = 0

        fun atEnd(): Boolean = pos >= text.length

        fun skipWhitespace() {
            while (pos < text.length && text[pos].isWhitespace()) pos++
        }

        fun parseValue(): JsonValue {
            skipWhitespace()
            require(pos < text.length) { "Unexpected end of JSON" }
            return when (text[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> JsonValue.Str(parseString())
                't' -> { expect("true"); JsonValue.Bool(true) }
                'f' -> { expect("false"); JsonValue.Bool(false) }
                'n' -> { expect("null"); JsonValue.Null }
                else -> parseNumber()
            }
        }

        private fun expect(literal: String) {
            require(text.startsWith(literal, pos)) { "Expected $literal at $pos" }
            pos += literal.length
        }

        private fun parseObject(): JsonValue.Obj {
            pos++ // {
            val values = LinkedHashMap<String, JsonValue>()
            skipWhitespace()
            if (pos < text.length && text[pos] == '}') { pos++; return JsonValue.Obj(values) }
            while (true) {
                skipWhitespace()
                require(pos < text.length && text[pos] == '"') { "Expected object key at $pos" }
                val key = parseString()
                skipWhitespace()
                require(pos < text.length && text[pos] == ':') { "Expected ':' at $pos" }
                pos++
                values[key] = parseValue()
                skipWhitespace()
                require(pos < text.length) { "Unterminated object" }
                when (text[pos]) {
                    ',' -> pos++
                    '}' -> { pos++; return JsonValue.Obj(values) }
                    else -> error("Expected ',' or '}' at $pos")
                }
            }
        }

        private fun parseArray(): JsonValue.Arr {
            pos++ // [
            val values = ArrayList<JsonValue>()
            skipWhitespace()
            if (pos < text.length && text[pos] == ']') { pos++; return JsonValue.Arr(values) }
            while (true) {
                values.add(parseValue())
                skipWhitespace()
                require(pos < text.length) { "Unterminated array" }
                when (text[pos]) {
                    ',' -> pos++
                    ']' -> { pos++; return JsonValue.Arr(values) }
                    else -> error("Expected ',' or ']' at $pos")
                }
            }
        }

        private fun parseString(): String {
            require(text[pos] == '"') { "Expected string at $pos" }
            pos++
            val out = StringBuilder()
            while (pos < text.length) {
                val char = text[pos++]
                when (char) {
                    '"' -> return out.toString()
                    '\\' -> {
                        require(pos < text.length) { "Unterminated escape" }
                        when (val esc = text[pos++]) {
                            '"' -> out.append('"')
                            '\\' -> out.append('\\')
                            '/' -> out.append('/')
                            'b' -> out.append('\b')
                            'f' -> out.append('\u000C')
                            'n' -> out.append('\n')
                            'r' -> out.append('\r')
                            't' -> out.append('\t')
                            'u' -> {
                                require(pos + 4 <= text.length) { "Invalid unicode escape" }
                                val hex = text.substring(pos, pos + 4)
                                out.append(hex.toInt(16).toChar())
                                pos += 4
                            }
                            else -> error("Invalid escape \\$esc")
                        }
                    }
                    else -> out.append(char)
                }
            }
            error("Unterminated string")
        }

        private fun parseNumber(): JsonValue.Num {
            val start = pos
            if (text[pos] == '-') pos++
            while (pos < text.length && text[pos] in '0'..'9') pos++
            if (pos < text.length && text[pos] == '.') {
                pos++
                while (pos < text.length && text[pos] in '0'..'9') pos++
            }
            if (pos < text.length && (text[pos] == 'e' || text[pos] == 'E')) {
                pos++
                if (pos < text.length && (text[pos] == '+' || text[pos] == '-')) pos++
                while (pos < text.length && text[pos] in '0'..'9') pos++
            }
            require(pos > start) { "Invalid number at $start" }
            return JsonValue.Num(text.substring(start, pos))
        }
    }
}

// ------------------------------------------------------------------- accessors

fun JsonValue.asObj(): JsonValue.Obj? = this as? JsonValue.Obj
fun JsonValue.asArr(): JsonValue.Arr? = this as? JsonValue.Arr
fun JsonValue.asString(): String? = (this as? JsonValue.Str)?.value
fun JsonValue.asLong(): Long? = (this as? JsonValue.Num)?.raw?.toLongOrNull()
fun JsonValue.asInt(): Int? = asLong()?.toInt()
fun JsonValue.asBoolean(): Boolean? = (this as? JsonValue.Bool)?.value
fun JsonValue.Obj.field(name: String): JsonValue = values[name] ?: JsonValue.Null
fun JsonValue.Obj.string(name: String): String = field(name).asString() ?: ""
fun JsonValue.Obj.long(name: String): Long = field(name).asLong() ?: 0L
fun JsonValue.Obj.int(name: String): Int = field(name).asInt() ?: 0
fun JsonValue.Obj.boolean(name: String): Boolean = field(name).asBoolean() ?: false
