package ir.hoboho.shooting.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hoboho.shooting.R
import ir.hoboho.shooting.core.common.StringProvider
import ir.hoboho.shooting.core.date.DateRange
import ir.hoboho.shooting.core.format.DateFormatter
import ir.hoboho.shooting.core.share.PublicDownloadSaver
import ir.hoboho.shooting.core.share.ReportFileWriter
import ir.hoboho.shooting.domain.PeriodAggregator
import ir.hoboho.shooting.domain.model.CableType
import ir.hoboho.shooting.domain.model.PeriodReport
import ir.hoboho.shooting.domain.model.Project
import ir.hoboho.shooting.domain.model.Workday
import ir.hoboho.shooting.domain.report.ExpenseReportRow
import ir.hoboho.shooting.domain.report.ReportSpecBuilder
import ir.hoboho.shooting.domain.repository.ExpenseRepository
import ir.hoboho.shooting.domain.repository.ProjectRepository
import ir.hoboho.shooting.domain.repository.ReferenceRepository
import ir.hoboho.shooting.domain.repository.WorkdayRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

enum class ReportTab { DAILY, PERIOD, EXPENSES }

data class ReportsUiState(
    val tab: ReportTab = ReportTab.DAILY,
    val range: DateRange = DateRange.thisMonth(),
    val projectFilter: Long? = null,
    val projects: List<Project> = emptyList(),
    val workdays: List<Workday> = emptyList(),
    val selectedWorkdayId: Long? = null,
    val cableTypes: List<CableType> = emptyList(),
    val periodReport: PeriodReport = PeriodReport(
        range = DateRange.thisMonth(), projectFilter = null, projectFilterName = null,
        workdayCount = 0, totalLengthMeters = 0, dailyBreakdown = emptyList(),
        projectBreakdown = emptyList(), expenseCount = 0, totalExpensesToman = 0L,
        settledToman = 0L, outstandingToman = 0L,
    ),
    val expenseRows: List<ExpenseReportRow> = emptyList(),
    val message: String? = null,
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val workdayRepository: WorkdayRepository,
    private val expenseRepository: ExpenseRepository,
    private val projectRepository: ProjectRepository,
    private val referenceRepository: ReferenceRepository,
    private val writer: ReportFileWriter,
    private val saver: PublicDownloadSaver,
    private val strings: StringProvider,
) : ViewModel() {

    private val tab = MutableStateFlow(ReportTab.DAILY)
    private val range = MutableStateFlow(DateRange.thisMonth())
    private val projectFilter = MutableStateFlow<Long?>(null)
    private val selectedWorkdayId = MutableStateFlow<Long?>(null)
    private val _state = MutableStateFlow(ReportsUiState())
    val state: StateFlow<ReportsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                projectRepository.observeProjects(),
                workdayRepository.observeWorkdays(),
                expenseRepository.observeExpenses(),
                expenseRepository.observeAllSettlements(),
                combine(
                    tab,
                    range,
                    projectFilter,
                    selectedWorkdayId,
                    referenceRepository.observeCableTypes(),
                ) { t, r, p, s, cables -> ReportsInputs(t, r, p, s, cables) },
            ) { projects, workdays, expenses, settlements, inputs ->
                val filteredWorkdays = PeriodAggregator.filterWorkdays(workdays, inputs.range, inputs.projectId)
                val filteredExpenses = PeriodAggregator.filterExpenses(expenses, inputs.range, inputs.projectId)
                val settlementsByExpense = settlements.groupBy { it.expenseId }
                val projectName = inputs.projectId?.let { id -> projects.firstOrNull { it.id == id }?.name }
                val chosenId = inputs.selectedId ?: filteredWorkdays.firstOrNull()?.id
                ReportsUiState(
                    tab = inputs.tab,
                    range = inputs.range,
                    projectFilter = inputs.projectId,
                    projects = projects,
                    workdays = filteredWorkdays,
                    selectedWorkdayId = chosenId,
                    cableTypes = inputs.cableTypes,
                    periodReport = PeriodAggregator.build(
                        range = inputs.range,
                        projectFilter = inputs.projectId,
                        projectFilterName = projectName,
                        workdays = workdays,
                        expenses = expenses,
                        cableTypes = inputs.cableTypes,
                    ),
                    expenseRows = filteredExpenses.map { expense ->
                        ExpenseReportRow(expense, settlementsByExpense[expense.id].orEmpty())
                    },
                )
            }.collect { combined ->
                _state.value = combined.copy(message = _state.value.message)
            }
        }
    }

    fun setTab(value: ReportTab) { tab.value = value }
    fun setRange(start: Long, end: Long) {
        if (end < start) {
            showMessage(strings.get(R.string.msg_invalid_range))
            return
        }
        range.value = DateRange(start, end)
    }
    fun setProjectFilter(id: Long?) { projectFilter.value = id }
    fun setSelectedWorkday(id: Long) { selectedWorkdayId.value = id }

    fun exportDailyPdf() {
        val id = _state.value.selectedWorkdayId ?: run {
            showMessage(strings.get(R.string.msg_no_data_for_pdf)); return
        }
        viewModelScope.launch {
            try {
                val workday = workdayRepository.getWorkday(id)
                    ?: throw IllegalStateException("missing workday")
                val cables = referenceRepository.getAllCableTypes()
                val spec = ReportSpecBuilder.daily(workday, cables)
                val name = "daily-${DateFormatter.format(workday.dateEpochDay, false).replace('/', '-')}-${workday.id}.pdf"
                val file = withContext(Dispatchers.IO) { writer.writePdf(spec, name) }
                val saved = persistPdf(file, name)
                showMessage(pdfSavedMessage(saved))
            } catch (t: Throwable) {
                showMessage(strings.get(R.string.msg_pdf_failed))
            }
        }
    }

    fun exportPeriodPdf() {
        val report = _state.value.periodReport
        if (report.workdayCount == 0 && report.expenseCount == 0) {
            showMessage(strings.get(R.string.msg_no_data_for_pdf)); return
        }
        viewModelScope.launch {
            try {
                val spec = ReportSpecBuilder.period(report)
                val name = "period-${DateFormatter.format(report.range.startEpochDay, false).replace('/', '-')}.pdf"
                val file = withContext(Dispatchers.IO) { writer.writePdf(spec, name) }
                val saved = persistPdf(file, name)
                showMessage(pdfSavedMessage(saved))
            } catch (t: Throwable) {
                showMessage(strings.get(R.string.msg_pdf_failed))
            }
        }
    }

    fun exportExpensesPdf() {
        val rows = _state.value.expenseRows
        if (rows.isEmpty()) {
            showMessage(strings.get(R.string.msg_no_data_for_pdf)); return
        }
        viewModelScope.launch {
            try {
                val spec = ReportSpecBuilder.expenses(rows, _state.value.range)
                val name = "expenses-${DateFormatter.format(_state.value.range.startEpochDay, false).replace('/', '-')}.pdf"
                val file = withContext(Dispatchers.IO) { writer.writePdf(spec, name) }
                val saved = persistPdf(file, name)
                showMessage(pdfSavedMessage(saved))
            } catch (t: Throwable) {
                showMessage(strings.get(R.string.msg_pdf_failed))
            }
        }
    }

    fun consumeMessage() { _state.value = _state.value.copy(message = null) }

    private suspend fun persistPdf(file: File, name: String): PublicDownloadSaver.SavedFile? =
        withContext(Dispatchers.IO) {
            runCatching { saver.save(file, name, "application/pdf") }.getOrNull()
        }

    private fun pdfSavedMessage(saved: PublicDownloadSaver.SavedFile?): String =
        saved?.let { strings.get(R.string.msg_pdf_saved, it.displayPath) }
            ?: strings.get(R.string.msg_save_failed)

    private fun showMessage(text: String) { _state.value = _state.value.copy(message = text) }

    private data class ReportsInputs(
        val tab: ReportTab,
        val range: DateRange,
        val projectId: Long?,
        val selectedId: Long?,
        val cableTypes: List<CableType>,
    )
}
