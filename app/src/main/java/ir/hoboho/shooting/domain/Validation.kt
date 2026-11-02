package ir.hoboho.shooting.domain

import ir.hoboho.shooting.domain.model.Expense
import ir.hoboho.shooting.domain.model.OperationDraft
import ir.hoboho.shooting.domain.model.Workday

/** Machine-readable validation issues; the UI maps these to Persian messages. */
enum class ValidationField {
    PROJECT_NAME,
    PROJECT_DUPLICATE,
    WORKDAY_PROJECT,
    WORKDAY_DATE,
    WORKDAY_DUPLICATE,
    WORKDAY_NO_OPERATIONS,
    WORKER_COUNT,
    START_CODE,
    END_CODE,
    START_METER,
    END_METER,
    CABLE_TYPE,
    EXPENSE_AMOUNT,
    EXPENSE_CATEGORY,
    EXPENSE_HAS_SETTLEMENTS,
    SETTLEMENT_AMOUNT,
    SETTLEMENT_EXCEEDS_OUTSTANDING,
}

data class ValidationIssue(val field: ValidationField, val rowKey: Long? = null)

/**
 * Single place for every business validation. Nothing is saved when a list is
 * non-empty: repositories call these before writing and the UI shows the result
 * inline, so invalid or incomplete operations can never be silently persisted.
 */
object Validator {

    fun project(name: String, duplicateExists: Boolean): List<ValidationIssue> = buildList {
        if (name.isBlank()) add(ValidationIssue(ValidationField.PROJECT_NAME))
        if (duplicateExists) add(ValidationIssue(ValidationField.PROJECT_DUPLICATE))
    }

    fun workday(
        projectId: Long,
        projectExists: Boolean,
        dateEpochDay: Long?,
        workerCount: Int,
        operationCount: Int,
        duplicateExists: Boolean,
    ): List<ValidationIssue> = buildList {
        if (projectId <= 0L || !projectExists) add(ValidationIssue(ValidationField.WORKDAY_PROJECT))
        if (dateEpochDay == null) add(ValidationIssue(ValidationField.WORKDAY_DATE))
        if (workerCount < 0) add(ValidationIssue(ValidationField.WORKER_COUNT))
        if (operationCount == 0) add(ValidationIssue(ValidationField.WORKDAY_NO_OPERATIONS))
        if (duplicateExists) add(ValidationIssue(ValidationField.WORKDAY_DUPLICATE))
    }

    /** Draft-level validation: reports the offending row key for inline errors. */
    fun operation(draft: OperationDraft, validCableTypeIds: Set<Long>): List<ValidationIssue> = buildList {
        if (draft.startCode.isBlank()) add(ValidationIssue(ValidationField.START_CODE, draft.key))
        if (draft.endCode.isBlank()) add(ValidationIssue(ValidationField.END_CODE, draft.key))
        if (draft.parsedStartMeter == null) add(ValidationIssue(ValidationField.START_METER, draft.key))
        if (draft.parsedEndMeter == null) add(ValidationIssue(ValidationField.END_METER, draft.key))
        val cable = draft.cableTypeId
        if (cable == null || cable <= 0L || cable !in validCableTypeIds) {
            add(ValidationIssue(ValidationField.CABLE_TYPE, draft.key))
        }
    }

    fun operations(drafts: List<OperationDraft>, validCableTypeIds: Set<Long>): List<ValidationIssue> =
        drafts.flatMap { operation(it, validCableTypeIds) }

    fun expense(amountToman: Long?, categoryId: Long?, amountText: String): List<ValidationIssue> = buildList {
        if (amountText.isBlank() || amountToman == null || amountToman <= 0L) {
            add(ValidationIssue(ValidationField.EXPENSE_AMOUNT))
        }
        if (categoryId == null || categoryId <= 0L) {
            add(ValidationIssue(ValidationField.EXPENSE_CATEGORY))
        }
    }

    fun settlement(additionalToman: Long?, amountToman: Long, alreadySettledToman: Long): List<ValidationIssue> = buildList {
        if (additionalToman == null || additionalToman <= 0L) {
            add(ValidationIssue(ValidationField.SETTLEMENT_AMOUNT))
        } else if (!ExpenseCalculator.canAddSettlement(amountToman, alreadySettledToman, additionalToman)) {
            add(ValidationIssue(ValidationField.SETTLEMENT_EXCEEDS_OUTSTANDING))
        }
    }

    /**
     * Duplicate prevention: one report per project and date. [excludeId] lets
     * an existing report be edited without flagging itself.
     */
    fun isDuplicateWorkday(
        existing: List<Workday>,
        projectId: Long,
        dateEpochDay: Long,
        excludeId: Long = 0L,
    ): Boolean = existing.any {
        it.id != excludeId && it.projectId == projectId && it.dateEpochDay == dateEpochDay
    }

    /** Guard used by the backup importer so broken data is never written. */
    fun expensesAreConsistent(expenses: List<Expense>): Boolean = expenses.all {
        it.amountToman >= 0L && it.settledAmountToman >= 0L && it.settledAmountToman <= it.amountToman
    }
}
