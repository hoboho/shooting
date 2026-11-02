package ir.hoboho.shooting.core.common

import ir.hoboho.shooting.domain.ValidationIssue

/** Raised instead of silently saving invalid or incomplete data. */
class ValidationException(val issues: List<ValidationIssue>) :
    IllegalArgumentException("Validation failed: ${issues.joinToString { it.field.name }}")

class NotFoundException(message: String) : IllegalArgumentException(message)
