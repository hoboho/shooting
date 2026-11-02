package ir.hoboho.shooting.core.common

import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.hoboho.shooting.R
import ir.hoboho.shooting.domain.ValidationField
import javax.inject.Inject
import javax.inject.Singleton

/** Resolves Persian UI text; keeps Android resources out of domain code. */
@Singleton
class StringProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun get(@StringRes id: Int): String = context.getString(id)
    fun get(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)

    /** Maps machine-readable validation issues to a single readable message. */
    fun validationMessage(field: ValidationField): String = get(
        when (field) {
            ValidationField.PROJECT_NAME -> R.string.msg_project_name_required
            ValidationField.PROJECT_DUPLICATE -> R.string.msg_project_duplicate
            ValidationField.WORKDAY_PROJECT -> R.string.msg_workday_no_project
            ValidationField.WORKDAY_DATE -> R.string.msg_workday_date_required
            ValidationField.WORKDAY_DUPLICATE -> R.string.msg_workday_duplicate
            ValidationField.WORKDAY_NO_OPERATIONS -> R.string.msg_workday_operations_empty
            ValidationField.WORKER_COUNT -> R.string.msg_workday_date_required
            ValidationField.START_CODE, ValidationField.END_CODE -> R.string.msg_point_code_required
            ValidationField.START_METER, ValidationField.END_METER -> R.string.msg_meter_required
            ValidationField.CABLE_TYPE -> R.string.msg_cable_type_required
            ValidationField.EXPENSE_AMOUNT -> R.string.msg_expense_amount_required
            ValidationField.EXPENSE_CATEGORY -> R.string.msg_expense_category_required
            ValidationField.EXPENSE_HAS_SETTLEMENTS -> R.string.msg_expense_has_settlements
            ValidationField.SETTLEMENT_AMOUNT -> R.string.msg_settlement_positive
            ValidationField.SETTLEMENT_EXCEEDS_OUTSTANDING -> R.string.msg_settlement_exceeds
        },
    )
}
