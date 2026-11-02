package ir.hoboho.shooting.feature.workdays

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hoboho.shooting.domain.model.Workday
import ir.hoboho.shooting.domain.repository.WorkdayRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WorkdaysViewModel @Inject constructor(
    repository: WorkdayRepository,
) : ViewModel() {
    val workdays: StateFlow<List<Workday>> = repository.observeWorkdays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
