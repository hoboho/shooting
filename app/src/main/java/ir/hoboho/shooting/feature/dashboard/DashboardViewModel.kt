package ir.hoboho.shooting.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hoboho.shooting.core.date.DateRange
import ir.hoboho.shooting.core.date.JalaliCalendar
import ir.hoboho.shooting.core.format.DateFormatter
import ir.hoboho.shooting.domain.ExpenseCalculator
import ir.hoboho.shooting.domain.PeriodAggregator
import ir.hoboho.shooting.domain.model.ShootingOperation
import ir.hoboho.shooting.domain.model.Workday
import ir.hoboho.shooting.domain.repository.ExpenseRepository
import ir.hoboho.shooting.domain.repository.ProjectRepository
import ir.hoboho.shooting.domain.repository.WorkdayRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DashboardPeriod { MONTH, YEAR, ALL }

data class DashboardUiState(
    val period: DashboardPeriod = DashboardPeriod.MONTH,
    val todayLabel: String = "",
    val periodLabel: String = "",
    val activeProjects: Int = 0,
    val recentWorkdays: List<Workday> = emptyList(),
    val recentOperations: List<ShootingOperation> = emptyList(),
    val periodLengthMeters: Int = 0,
    val outstandingToman: Long = 0L,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val workdayRepository: WorkdayRepository,
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {

    private val period = MutableStateFlow(DashboardPeriod.MONTH)
    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                projectRepository.observeActiveProjects(),
                workdayRepository.observeWorkdays(),
                expenseRepository.observeExpenses(),
                period,
            ) { projects, workdays, expenses, selectedPeriod ->
                val today = JalaliCalendar.todayEpochDay()
                val range = when (selectedPeriod) {
                    DashboardPeriod.MONTH -> DateRange.thisMonth(today)
                    DashboardPeriod.YEAR -> DateRange.thisYear(today)
                    DashboardPeriod.ALL -> DateRange.all()
                }
                val overview = ExpenseCalculator.overview(expenses)
                DashboardUiState(
                    period = selectedPeriod,
                    todayLabel = DateFormatter.formatLongWithWeekday(today),
                    periodLabel = DateFormatter.formatRange(range),
                    activeProjects = projects.size,
                    recentWorkdays = workdays.take(5),
                    periodLengthMeters = PeriodAggregator.build(
                        range, workdays = workdays, expenses = expenses,
                    ).totalLengthMeters,
                    outstandingToman = overview.outstandingToman,
                )
            }.collect { combined ->
                _state.update { current -> combined.copy(recentOperations = current.recentOperations) }
            }
        }
        // Reload recent operations whenever any workday changes, so the dashboard
        // never shows a stale list after saving or editing a daily report.
        viewModelScope.launch {
            workdayRepository.observeWorkdays().collect {
                val operations = workdayRepository.getAllWorkdaysWithOperations()
                    .flatMap { workday -> workday.operations }
                    .takeLast(5)
                    .reversed()
                _state.update { current -> current.copy(recentOperations = operations) }
            }
        }
    }

    fun setPeriod(value: DashboardPeriod) { period.value = value }

}
