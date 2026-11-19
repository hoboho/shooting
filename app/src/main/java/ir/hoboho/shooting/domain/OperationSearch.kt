package ir.hoboho.shooting.domain

import ir.hoboho.shooting.core.format.PersianDigits
import ir.hoboho.shooting.domain.model.Workday

/**
 * Daily-list search is intentionally limited to operation point codes and
 * meter values. It does not search project metadata, notes, or dates.
 */
fun Workday.matchesOperationSearch(query: String): Boolean {
    val term = PersianDigits.toLatin(query).trim()
    if (term.isBlank()) return true
    val codeMatches = operations.any { operation ->
        operation.startCode.contains(term, ignoreCase = true) ||
            operation.endCode.contains(term, ignoreCase = true)
    }
    val meterMatches = term.all { it.isDigit() } && operations.any { operation ->
        listOf(operation.startMeter, operation.endMeter, operation.lengthMeters)
            .any { it.toString().contains(term) }
    }
    return codeMatches || meterMatches
}
