package ir.hoboho.shooting.feature.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hoboho.shooting.core.common.StringProvider
import ir.hoboho.shooting.core.common.ValidationException
import ir.hoboho.shooting.domain.ExpenseCalculator
import ir.hoboho.shooting.domain.ExpenseOverview
import ir.hoboho.shooting.core.format.NumberFormatter
import ir.hoboho.shooting.domain.model.Expense
import ir.hoboho.shooting.domain.model.CompanyPayment
import ir.hoboho.shooting.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val strings: StringProvider,
) : ViewModel() {

    val expenses: StateFlow<List<Expense>> = repository.observeExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val companyPayments: StateFlow<List<CompanyPayment>> = repository.observeCompanyPayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val overview: StateFlow<ExpenseOverview> = expenses
        .map { ExpenseCalculator.overview(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpenseOverview())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun allocateCompanyPayment(amountText: String, dateEpochDay: Long, receiptReference: String) = viewModelScope.launch {
        val amount = NumberFormatter.parseToman(amountText)
        if (amount == null || amount <= 0L) {
            _error.value = strings.get(ir.hoboho.shooting.R.string.msg_settlement_positive)
            return@launch
        }
        try {
            repository.allocateCompanyPayment(
                amountToman = amount,
                dateEpochDay = dateEpochDay,
                receiptReference = receiptReference.trim(),
                note = strings.get(ir.hoboho.shooting.R.string.expense_company_payment_note),
            )
        } catch (e: Exception) {
            _error.value = strings.get(ir.hoboho.shooting.R.string.error_generic)
        }
    }

    fun delete(id: Long) = viewModelScope.launch {
        try {
            repository.delete(id)
        } catch (e: ValidationException) {
            _error.value = strings.validationMessage(e.issues.first().field)
        } catch (e: Exception) {
            _error.value = strings.get(ir.hoboho.shooting.R.string.error_generic)
        }
    }

    fun consumeError() { _error.value = null }
}
