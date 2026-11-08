package ir.hoboho.shooting.feature.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hoboho.shooting.core.common.Clock
import ir.hoboho.shooting.core.common.StringProvider
import ir.hoboho.shooting.core.common.ValidationException
import ir.hoboho.shooting.core.date.JalaliCalendar
import ir.hoboho.shooting.core.format.NumberFormatter
import ir.hoboho.shooting.domain.Validator
import ir.hoboho.shooting.domain.model.Expense
import ir.hoboho.shooting.domain.model.ExpenseCategory
import ir.hoboho.shooting.domain.model.Project
import ir.hoboho.shooting.domain.model.Settlement
import ir.hoboho.shooting.domain.model.Workday
import ir.hoboho.shooting.domain.repository.ExpenseRepository
import ir.hoboho.shooting.domain.repository.ProjectRepository
import ir.hoboho.shooting.domain.repository.ReferenceRepository
import ir.hoboho.shooting.domain.repository.WorkdayRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExpenseEditorState(
    val id: Long = 0L,
    val uuid: String = "",
    val createdAt: Long = 0L,
    val dateEpochDay: Long = JalaliCalendar.todayEpochDay(),
    val categoryId: Long? = null,
    val amount: String = "",
    val description: String = "",
    val receiptReference: String = "",
    val companyApproved: Boolean = false,
    val projectId: Long? = null,
    val workdayId: Long? = null,
    val categories: List<ExpenseCategory> = emptyList(),
    val projects: List<Project> = emptyList(),
    val workdays: List<Workday> = emptyList(),
    val settlements: List<Settlement> = emptyList(),
    val amountToman: Long = 0L,
    val settledAmountToman: Long = 0L,
    val settlementAmount: String = "",
    val settlementDate: Long = JalaliCalendar.todayEpochDay(),
    val settlementNote: String = "",
    val editingSettlementId: Long? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null,
) {
    val outstandingToman: Long get() = amountToman - settledAmountToman
}

@HiltViewModel
class ExpenseEditorViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val projectRepository: ProjectRepository,
    private val workdayRepository: WorkdayRepository,
    private val referenceRepository: ReferenceRepository,
    private val strings: StringProvider,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(ExpenseEditorState())
    val state: StateFlow<ExpenseEditorState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                referenceRepository.observeActiveCategories(),
                projectRepository.observeActiveProjects(),
                workdayRepository.observeWorkdays(),
            ) { categories, projects, workdays -> Triple(categories, projects, workdays) }
                .collect { (categories, projects, workdays) ->
                    _state.update { state ->
                        state.copy(
                            categories = categories,
                            projects = projects,
                            workdays = workdays,
                            categoryId = state.categoryId ?: categories.firstOrNull()?.id,
                        )
                    }
                }
        }
    }

    fun load(expenseId: Long) {
        if (expenseId <= 0L) return
        viewModelScope.launch {
            expenseRepository.getExpense(expenseId)?.let { expense ->
                _state.update {
                    it.copy(
                        id = expense.id, uuid = expense.uuid, createdAt = expense.createdAt,
                        dateEpochDay = expense.dateEpochDay, categoryId = expense.categoryId,
                        amount = expense.amountToman.toString(), description = expense.description,
                        receiptReference = expense.receiptReference,
                        companyApproved = expense.companyApproved,
                        projectId = expense.projectId, workdayId = expense.workdayId,
                        amountToman = expense.amountToman,
                        settledAmountToman = expense.settledAmountToman,
                    )
                }
            }
            expenseRepository.observeSettlements(expenseId).collect { settlements ->
                _state.update {
                    it.copy(
                        settlements = settlements,
                        settledAmountToman = settlements.sumOf { s -> s.amountToman },
                    )
                }
            }
        }
    }

    fun setDate(value: Long) = _state.update { it.copy(dateEpochDay = value) }
    fun setCategory(value: Long) = _state.update { it.copy(categoryId = value, error = null) }
    fun setAmount(value: String) = _state.update { it.copy(amount = value, error = null) }
    fun setDescription(value: String) = _state.update { it.copy(description = value) }
    fun setReceipt(value: String) = _state.update { it.copy(receiptReference = value) }
    fun setCompanyApproved(value: Boolean) = _state.update { state ->
        if (!value && state.settledAmountToman > 0L) state.copy(
            error = strings.get(ir.hoboho.shooting.R.string.msg_expense_has_settlements),
        ) else state.copy(companyApproved = value, error = null)
    }
    fun setProject(value: Long?) = _state.update { it.copy(projectId = value) }
    fun setWorkday(value: Long?) = _state.update { it.copy(workdayId = value) }
    fun setSettlementAmount(value: String) = _state.update { it.copy(settlementAmount = value, error = null) }
    fun setSettlementDate(value: Long) = _state.update { it.copy(settlementDate = value) }
    fun setSettlementNote(value: String) = _state.update { it.copy(settlementNote = value) }

    fun save() {
        val current = _state.value
        val amount = NumberFormatter.parseToman(current.amount)
        val issues = Validator.expense(amount, current.categoryId, current.amount)
        if (issues.isNotEmpty()) {
            _state.update { it.copy(error = strings.validationMessage(issues.first().field)) }
            return
        }
        viewModelScope.launch {
            try {
                val id = expenseRepository.save(
                    Expense(
                        id = current.id, uuid = current.uuid, dateEpochDay = current.dateEpochDay,
                        categoryId = current.categoryId!!, amountToman = amount!!,
                        description = current.description.trim(),
                        receiptReference = current.receiptReference.trim(),
                        companyApproved = current.companyApproved,
                        projectId = current.projectId, workdayId = current.workdayId,
                        createdAt = current.createdAt,
                    ),
                )
                _state.update {
                    it.copy(id = id, amountToman = amount, saved = true)
                }
            } catch (e: ValidationException) {
                _state.update { it.copy(error = strings.validationMessage(e.issues.first().field)) }
            } catch (e: Exception) {
                _state.update { it.copy(error = strings.get(ir.hoboho.shooting.R.string.error_generic)) }
            }
        }
    }

    fun editSettlement(id: Long) {
        val settlement = _state.value.settlements.firstOrNull { it.id == id } ?: return
        _state.update { it.copy(
            editingSettlementId = id,
            settlementAmount = settlement.amountToman.toString(),
            settlementDate = settlement.dateEpochDay,
            settlementNote = settlement.note,
            error = null,
        ) }
    }

    fun cancelSettlementEdit() = _state.update {
        it.copy(editingSettlementId = null, settlementAmount = "", settlementNote = "", error = null)
    }

    fun addSettlement() {
        val current = _state.value
        if (current.id == 0L) {
            _state.update { it.copy(error = strings.get(ir.hoboho.shooting.R.string.action_save)) }
            return
        }
        val amount = NumberFormatter.parseToman(current.settlementAmount)
        val oldSettlement = current.settlements.firstOrNull { it.id == current.editingSettlementId }
        val alreadySettled = current.settledAmountToman - (oldSettlement?.amountToman ?: 0L)
        val issues = Validator.settlement(amount, current.amountToman, alreadySettled)
        if (issues.isNotEmpty()) {
            _state.update { it.copy(error = strings.validationMessage(issues.first().field)) }
            return
        }
        viewModelScope.launch {
            try {
                val settlement = Settlement(
                    id = current.editingSettlementId ?: 0L,
                    expenseId = current.id,
                    amountToman = amount!!,
                    dateEpochDay = current.settlementDate,
                    note = current.settlementNote.trim(),
                    createdAt = clock.nowEpochMilli(),
                )
                if (current.editingSettlementId == null) expenseRepository.addSettlement(settlement)
                else expenseRepository.updateSettlement(settlement)
                _state.update { it.copy(
                    editingSettlementId = null, settlementAmount = "", settlementNote = "", error = null,
                ) }
            } catch (e: ValidationException) {
                _state.update { it.copy(error = strings.validationMessage(e.issues.first().field)) }
            } catch (e: Exception) {
                _state.update { it.copy(error = strings.get(ir.hoboho.shooting.R.string.error_generic)) }
            }
        }
    }

    fun deleteSettlement(id: Long) = viewModelScope.launch {
        expenseRepository.deleteSettlement(id)
        if (_state.value.editingSettlementId == id) cancelSettlementEdit()
    }

    fun consumeSaved() = _state.update { it.copy(saved = false) }

    fun delete() {
        val id = _state.value.id
        if (id == 0L) return
        viewModelScope.launch {
            try {
                expenseRepository.delete(id)
                _state.update { it.copy(deleted = true) }
            } catch (e: ValidationException) {
                _state.update { it.copy(error = strings.validationMessage(e.issues.first().field)) }
            }
        }
    }
}
