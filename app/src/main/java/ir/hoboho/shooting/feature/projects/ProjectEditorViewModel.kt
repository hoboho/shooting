package ir.hoboho.shooting.feature.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hoboho.shooting.core.common.StringProvider
import ir.hoboho.shooting.core.common.ValidationException
import ir.hoboho.shooting.domain.model.Project
import ir.hoboho.shooting.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectEditorState(
    val id: Long = 0L,
    val uuid: String = "",
    val name: String = "",
    val workArea: String = "",
    val employer: String = "",
    val supervisor: String = "",
    val description: String = "",
    val active: Boolean = true,
    val createdAt: Long = 0L,
    val saved: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ProjectEditorViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val strings: StringProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectEditorState())
    val state: StateFlow<ProjectEditorState> = _state.asStateFlow()

    fun load(projectId: Long) {
        if (projectId <= 0L) return
        viewModelScope.launch {
            repository.getProject(projectId)?.let { project ->
                _state.update {
                    it.copy(
                        id = project.id, uuid = project.uuid, name = project.name,
                        workArea = project.workArea, employer = project.employer,
                        supervisor = project.supervisor, description = project.description,
                        active = project.active, createdAt = project.createdAt,
                    )
                }
            }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value, error = null) }
    fun setWorkArea(value: String) = _state.update { it.copy(workArea = value) }
    fun setEmployer(value: String) = _state.update { it.copy(employer = value) }
    fun setSupervisor(value: String) = _state.update { it.copy(supervisor = value) }
    fun setDescription(value: String) = _state.update { it.copy(description = value) }
    fun setActive(value: Boolean) = _state.update { it.copy(active = value) }

    fun save() {
        val current = _state.value
        viewModelScope.launch {
            try {
                repository.save(
                    Project(
                        id = current.id, uuid = current.uuid, name = current.name,
                        workArea = current.workArea, employer = current.employer,
                        supervisor = current.supervisor, description = current.description,
                        active = current.active, createdAt = current.createdAt,
                    ),
                )
                _state.update { it.copy(saved = true) }
            } catch (e: ValidationException) {
                _state.update { it.copy(error = strings.validationMessage(e.issues.first().field)) }
            } catch (e: Exception) {
                _state.update { it.copy(error = strings.get(ir.hoboho.shooting.R.string.error_generic)) }
            }
        }
    }
}
