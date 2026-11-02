package ir.hoboho.shooting.feature.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hoboho.shooting.core.common.StringProvider
import ir.hoboho.shooting.domain.model.Project
import ir.hoboho.shooting.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val strings: StringProvider,
) : ViewModel() {

    val projects: StateFlow<List<Project>> = repository.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setActive(id: Long, active: Boolean) = viewModelScope.launch {
        runCatching { repository.setActive(id, active) }
            .onFailure { _error.value = strings.get(ir.hoboho.shooting.R.string.error_generic) }
    }

    fun delete(id: Long) = viewModelScope.launch {
        runCatching { repository.delete(id) }
            .onFailure { _error.value = strings.get(ir.hoboho.shooting.R.string.error_generic) }
    }

    fun consumeError() { _error.value = null }
}
