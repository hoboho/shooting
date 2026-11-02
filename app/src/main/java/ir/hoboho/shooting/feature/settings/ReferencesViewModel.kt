package ir.hoboho.shooting.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hoboho.shooting.core.common.StringProvider
import ir.hoboho.shooting.core.common.ValidationException
import ir.hoboho.shooting.core.format.NumberFormatter
import ir.hoboho.shooting.domain.model.CableType
import ir.hoboho.shooting.domain.model.ExpenseCategory
import ir.hoboho.shooting.domain.repository.ReferenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReferencesViewModel @Inject constructor(
    private val repository: ReferenceRepository,
    private val strings: StringProvider,
) : ViewModel() {

    val cableTypes: StateFlow<List<CableType>> = repository.observeCableTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val categories: StateFlow<List<ExpenseCategory>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun addCableType(input: String) {
        val core = NumberFormatter.parseNonNegativeInt(input)
        if (core == null || core == 0) {
            _error.value = strings.get(ir.hoboho.shooting.R.string.msg_cable_type_required)
            return
        }
        viewModelScope.launch {
            try {
                repository.addCableType(core)
            } catch (e: ValidationException) {
                _error.value = strings.get(ir.hoboho.shooting.R.string.msg_reference_duplicate)
            }
        }
    }

    fun toggleCableType(id: Long, active: Boolean) = viewModelScope.launch {
        repository.setCableTypeActive(id, active)
    }

    fun addCategory(name: String) = viewModelScope.launch {
        try {
            repository.addCategory(name)
        } catch (e: ValidationException) {
            _error.value = strings.get(ir.hoboho.shooting.R.string.msg_reference_duplicate)
        }
    }

    fun toggleCategory(id: Long, active: Boolean) = viewModelScope.launch {
        repository.setCategoryActive(id, active)
    }

    fun consumeError() { _error.value = null }
}
