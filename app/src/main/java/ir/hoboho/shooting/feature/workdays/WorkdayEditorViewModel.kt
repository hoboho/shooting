package ir.hoboho.shooting.feature.workdays

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hoboho.shooting.core.common.Clock
import ir.hoboho.shooting.core.common.StringProvider
import ir.hoboho.shooting.core.common.ValidationException
import ir.hoboho.shooting.core.date.JalaliCalendar
import ir.hoboho.shooting.core.format.NumberFormatter
import ir.hoboho.shooting.domain.ShootingCalculator
import ir.hoboho.shooting.domain.ValidationIssue
import ir.hoboho.shooting.domain.Validator
import ir.hoboho.shooting.domain.model.CableType
import ir.hoboho.shooting.domain.model.OperationDraft
import ir.hoboho.shooting.domain.model.Project
import ir.hoboho.shooting.domain.model.Workday
import ir.hoboho.shooting.domain.repository.ProjectRepository
import ir.hoboho.shooting.domain.repository.ReferenceRepository
import ir.hoboho.shooting.domain.repository.WorkdayRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkdayEditorState(
    val workdayId: Long = 0L,
    val uuid: String = "",
    val createdAt: Long = 0L,
    val projectId: Long = 0L,
    val dateEpochDay: Long = JalaliCalendar.todayEpochDay(),
    val workArea: String = "",
    val employer: String = "",
    val supervisor: String = "",
    val workerCount: String = "0",
    val notes: String = "",
    val operations: List<OperationDraft> = emptyList(),
    val projects: List<Project> = emptyList(),
    val cableTypes: List<CableType> = emptyList(),
    val inherited: Boolean = false,
    val saved: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val invalidRows: Set<Long> = emptySet(),
) {
    /** Live sum; incomplete rows contribute nothing until they are valid. */
    val totalLengthMeters: Int
        get() = ShootingCalculator.sumLengths(operations.mapNotNull { it.lengthMeters })
}

@HiltViewModel
class WorkdayEditorViewModel @Inject constructor(
    private val workdayRepository: WorkdayRepository,
    private val projectRepository: ProjectRepository,
    private val referenceRepository: ReferenceRepository,
    private val strings: StringProvider,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkdayEditorState())
    val state: StateFlow<WorkdayEditorState> = _state.asStateFlow()
    private var nextKey = 1L

    init {
        viewModelScope.launch {
            combine(
                projectRepository.observeActiveProjects(),
                referenceRepository.observeActiveCableTypes(),
            ) { projects, cables -> projects to cables }
                .collect { (projects, cables) ->
                    _state.update { state ->
                        val projectId = state.projectId.takeIf { id -> projects.any { it.id == id } }
                            ?: projects.firstOrNull()?.id ?: 0L
                        state.copy(
                            projects = projects,
                            cableTypes = cables,
                            projectId = projectId,
                            operations = state.operations.map { draft ->
                                if (draft.cableTypeId == null) {
                                    draft.copy(cableTypeId = cables.firstOrNull()?.id)
                                } else draft
                            },
                        )
                    }
                }
        }
    }

    fun load(workdayId: Long, initialProjectId: Long) {
        viewModelScope.launch {
            if (workdayId > 0L) {
                workdayRepository.getWorkday(workdayId)?.let { workday ->
                    _state.update {
                        it.copy(
                            workdayId = workday.id, uuid = workday.uuid, createdAt = workday.createdAt,
                            projectId = workday.projectId, dateEpochDay = workday.dateEpochDay,
                            workArea = workday.workArea, employer = workday.employer,
                            supervisor = workday.supervisor,
                            workerCount = workday.workerCount.toString(), notes = workday.notes,
                            operations = workday.operations.map { op ->
                                OperationDraft.fromOperation(op, nextKey++)
                            },
                            inherited = false,
                        )
                    }
                }
            } else {
                val projectId = initialProjectId.takeIf { it > 0L }
                    ?: _state.value.projects.firstOrNull()?.id ?: 0L
                applyProjectDefaults(projectId)
            }
        }
    }

    /** Reuses the last workday of the project, falling back to project defaults. */
    private suspend fun applyProjectDefaults(projectId: Long) {
        val project = projectRepository.getProject(projectId) ?: return
        val last = workdayRepository.observeWorkdaysByProject(projectId).first().firstOrNull()
        _state.update {
            it.copy(
                projectId = projectId,
                workArea = last?.workArea?.takeIf(String::isNotBlank) ?: project.workArea,
                employer = last?.employer?.takeIf(String::isNotBlank) ?: project.employer,
                supervisor = last?.supervisor?.takeIf(String::isNotBlank) ?: project.supervisor,
                workerCount = last?.workerCount?.toString() ?: "0",
                inherited = true,
            )
        }
    }

    fun setProject(projectId: Long) {
        if (_state.value.workdayId > 0L) return // changing the project of a saved report is not allowed here
        viewModelScope.launch { applyProjectDefaults(projectId) }
    }

    fun setDate(epochDay: Long) = _state.update { if (it.saving) it else it.copy(dateEpochDay = epochDay, error = null) }
    fun setWorkArea(value: String) = _state.update { if (it.saving) it else it.copy(workArea = value, inherited = false) }
    fun setEmployer(value: String) = _state.update { if (it.saving) it else it.copy(employer = value, inherited = false) }
    fun setSupervisor(value: String) = _state.update { if (it.saving) it else it.copy(supervisor = value, inherited = false) }
    fun setWorkerCount(value: String) = _state.update { if (it.saving) it else it.copy(workerCount = value, inherited = false) }
    fun setNotes(value: String) = _state.update { if (it.saving) it else it.copy(notes = value) }

    fun addOperation() {
        _state.update { state ->
            if (state.saving) return@update state
            val draft = OperationDraft(
                key = nextKey++,
                cableTypeId = state.cableTypes.firstOrNull()?.id,
            )
            state.copy(operations = state.operations + draft)
        }
    }

    fun removeOperation(key: Long) =
        _state.update { if (it.saving) it else it.copy(operations = it.operations.filterNot { draft -> draft.key == key }) }

    fun updateOperation(key: Long, transform: (OperationDraft) -> OperationDraft) =
        _state.update { state ->
            if (state.saving) return@update state
            state.copy(
                operations = state.operations.map { draft ->
                    if (draft.key == key) transform(draft) else draft
                },
                invalidRows = state.invalidRows - key,
            )
        }

    fun save() {
        val current = _state.value
        if (current.saving || current.saved) return
        _state.update { if (it.saving || it.saved) it else it.copy(saving = true, error = null) }
        if (!_state.value.saving) return
        viewModelScope.launch {
            try {
            val cableIds = current.cableTypes.map { it.id }.toSet()
            val operationIssues = Validator.operations(current.operations, cableIds)
            val projectExists = projectRepository.getProject(current.projectId) != null
            val duplicate = workdayRepository.duplicateExists(
                current.projectId, current.dateEpochDay, current.workdayId,
            )
            val workerCount = NumberFormatter.parseNonNegativeInt(current.workerCount) ?: -1
            val workdayIssues = Validator.workday(
                projectId = current.projectId,
                projectExists = projectExists,
                dateEpochDay = current.dateEpochDay,
                workerCount = workerCount,
                operationCount = current.operations.size,
                duplicateExists = duplicate,
            )
            val issues: List<ValidationIssue> = workdayIssues + operationIssues
            if (issues.isNotEmpty()) {
                _state.update {
                    it.copy(
                        invalidRows = operationIssues.mapNotNull(ValidationIssue::rowKey).toSet(),
                        error = strings.validationMessage(issues.first().field),
                    )
                }
                return@launch
            }
                val now = clock.nowEpochMilli()
                val operations = current.operations.mapNotNull { it.toOperation(current.workdayId, now) }
                val workday = Workday(
                    id = current.workdayId,
                    uuid = current.uuid,
                    projectId = current.projectId,
                    dateEpochDay = current.dateEpochDay,
                    workArea = current.workArea.trim(),
                    employer = current.employer.trim(),
                    supervisor = current.supervisor.trim(),
                    workerCount = workerCount,
                    notes = current.notes.trim(),
                    operations = operations,
                    totalLengthMeters = ShootingCalculator.dailyTotal(operations),
                    createdAt = current.createdAt,
                )
                workdayRepository.save(workday, operations)
                _state.update { it.copy(saved = true) }
            } catch (e: ValidationException) {
                _state.update { it.copy(error = strings.validationMessage(e.issues.first().field)) }
            } catch (e: Exception) {
                _state.update { it.copy(error = strings.get(ir.hoboho.shooting.R.string.error_generic)) }
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }
}
