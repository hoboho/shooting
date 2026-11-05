@file:OptIn(ExperimentalMaterial3Api::class)

package ir.hoboho.shooting.feature.reports

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hoboho.shooting.R
import ir.hoboho.shooting.core.format.DateFormatter
import ir.hoboho.shooting.core.format.NumberFormatter
import ir.hoboho.shooting.domain.PeriodAggregator
import ir.hoboho.shooting.domain.model.CableLengthRow
import ir.hoboho.shooting.domain.model.Project
import ir.hoboho.shooting.domain.model.Workday
import ir.hoboho.shooting.ui.components.ChoiceChips
import ir.hoboho.shooting.ui.components.DateRangeFields
import ir.hoboho.shooting.ui.components.EmptyState
import ir.hoboho.shooting.ui.components.SectionCard
import ir.hoboho.shooting.ui.components.ShootingButton
import ir.hoboho.shooting.ui.components.ShootingDropdown
import ir.hoboho.shooting.ui.components.StatRow
import ir.hoboho.shooting.ui.components.rememberFileSaveAction

private fun workdayLabel(workday: Workday): String =
    "${workday.projectName} — ${DateFormatter.format(workday.dateEpochDay)}"

@Composable
private fun CableBreakdownRows(rows: List<CableLengthRow>) {
    if (rows.isEmpty()) {
        EmptyState(stringResource(R.string.label_no_data))
        return
    }
    rows.forEach { row ->
        StatRow(
            label = row.label,
            value = NumberFormatter.formatMeters(row.totalLengthMeters),
            hint = "${stringResource(R.string.settings_core_count)}: ${NumberFormatter.formatInteger(row.coreCount)}" +
                " | ${stringResource(R.string.label_operation_count)}: ${NumberFormatter.formatInteger(row.operationCount)}",
        )
    }
}

/** Keeps the "all projects" option as a real, labelled, non-null selection. */
private data class ProjectFilterChoice(val project: Project?)

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val allProjectsText = stringResource(R.string.label_all_projects)
    val exportDaily = rememberFileSaveAction(viewModel::exportDailyPdf)
    val exportPeriod = rememberFileSaveAction(viewModel::exportPeriodPdf)
    val exportExpenses = rememberFileSaveAction(viewModel::exportExpensesPdf)
    val exportProject = rememberFileSaveAction(viewModel::exportProjectPdf)

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text(stringResource(R.string.reports_title)) }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ChoiceChips(
                    options = ReportTab.entries,
                    selected = state.tab,
                    onSelected = viewModel::setTab,
                    labelOf = { tab ->
                        stringResource(
                            when (tab) {
                                ReportTab.DAILY -> R.string.reports_daily
                                ReportTab.PERIOD -> R.string.reports_period
                                ReportTab.PROJECT -> R.string.reports_project_detail
                                ReportTab.EXPENSES -> R.string.reports_expenses
                            },
                        )
                    },
                )
            }
            item {
                SectionCard(title = stringResource(R.string.label_date_range)) {
                    DateRangeFields(
                        startEpochDay = state.range.startEpochDay,
                        endEpochDay = state.range.endEpochDay,
                        onStartChanged = { viewModel.setRange(it, state.range.endEpochDay) },
                        onEndChanged = { viewModel.setRange(state.range.startEpochDay, it) },
                    )
                    ShootingDropdown(
                        label = stringResource(R.string.label_project_filter),
                        options = listOf(ProjectFilterChoice(null)) +
                            state.projects.map { ProjectFilterChoice(it) },
                        selected = ProjectFilterChoice(state.projects.firstOrNull { it.id == state.projectFilter }),
                        onSelected = { viewModel.setProjectFilter(it.project?.id) },
                        labelOf = { it.project?.name ?: allProjectsText },
                    )
                }
            }
            when (state.tab) {
                ReportTab.DAILY -> {
                    item {
                        SectionCard(title = stringResource(R.string.reports_daily)) {
                            if (state.workdays.isEmpty()) {
                                EmptyState(stringResource(R.string.label_no_data))
                            } else {
                                ShootingDropdown(
                                    label = stringResource(R.string.reports_select_day),
                                    options = state.workdays,
                                    selected = state.workdays.firstOrNull { it.id == state.selectedWorkdayId },
                                    onSelected = { viewModel.setSelectedWorkday(it.id) },
                                    labelOf = ::workdayLabel,
                                )
                                val selected = state.workdays.firstOrNull { it.id == state.selectedWorkdayId }
                                if (selected != null) {
                                    StatRow(stringResource(R.string.label_work_area),
                                        selected.workArea.ifBlank { stringResource(R.string.value_not_set) })
                                    StatRow(stringResource(R.string.label_employer),
                                        selected.employer.ifBlank { stringResource(R.string.value_not_set) })
                                    StatRow(stringResource(R.string.label_supervisor),
                                        selected.supervisor.ifBlank { stringResource(R.string.value_not_set) })
                                    StatRow(stringResource(R.string.label_worker_count),
                                        NumberFormatter.formatInteger(selected.workerCount))
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                    // Daily summary focuses on the total length only.
                                    StatRow(
                                        label = stringResource(R.string.label_daily_total),
                                        value = NumberFormatter.formatMeters(selected.totalLengthMeters),
                                        emphasised = true,
                                    )
                                }
                            }
                        }
                    }
                    item {
                        SectionCard(title = stringResource(R.string.label_cable_breakdown)) {
                            val selected = state.workdays.firstOrNull { it.id == state.selectedWorkdayId }
                            val rows = selected?.let {
                                PeriodAggregator.cableBreakdown(listOf(it), state.cableTypes)
                            } ?: emptyList()
                            CableBreakdownRows(rows)
                        }
                    }
                    item {
                        ShootingButton(
                            text = stringResource(R.string.reports_pdf_daily),
                            onClick = exportDaily,
                            icon = Icons.Filled.PictureAsPdf,
                        )
                    }
                }
                ReportTab.PERIOD -> {
                    item {
                        SectionCard(
                            title = stringResource(R.string.label_period_total),
                            subtitle = DateFormatter.formatRange(state.range),
                        ) {
                            StatRow(
                                label = stringResource(R.string.reports_total_length),
                                value = NumberFormatter.formatMeters(state.periodReport.totalLengthMeters),
                                emphasised = true,
                            )
                        }
                    }
                    item {
                        SectionCard(title = stringResource(R.string.label_daily_breakdown)) {
                            if (state.periodReport.dailyBreakdown.isEmpty()) {
                                EmptyState(stringResource(R.string.label_no_data))
                            } else {
                                state.periodReport.dailyBreakdown.forEach { row ->
                                    StatRow(
                                        label = DateFormatter.format(row.dateEpochDay),
                                        value = NumberFormatter.formatMeters(row.totalLengthMeters),
                                    )
                                }
                            }
                        }
                    }
                    item {
                        SectionCard(title = stringResource(R.string.label_project_breakdown)) {
                            if (state.periodReport.projectBreakdown.isEmpty()) {
                                EmptyState(stringResource(R.string.label_no_data))
                            } else {
                                state.periodReport.projectBreakdown.forEach { row ->
                                    StatRow(
                                        label = row.projectName,
                                        value = NumberFormatter.formatMeters(row.totalLengthMeters),
                                    )
                                }
                            }
                        }
                    }
                    item {
                        SectionCard(title = stringResource(R.string.label_cable_breakdown)) {
                            CableBreakdownRows(state.periodReport.cableBreakdown)
                        }
                    }
                    item {
                        // Money is a separate block; lengths and amounts are never combined.
                        SectionCard(title = stringResource(R.string.label_expense_summary)) {
                            StatRow(stringResource(R.string.reports_expense_totals),
                                NumberFormatter.formatToman(state.periodReport.totalExpensesToman))
                            StatRow(stringResource(R.string.reports_settled),
                                NumberFormatter.formatToman(state.periodReport.settledToman))
                            StatRow(stringResource(R.string.reports_outstanding),
                                NumberFormatter.formatToman(state.periodReport.outstandingToman), emphasised = true)
                            Text(stringResource(R.string.reports_note_length_money),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item {
                        ShootingButton(
                            text = stringResource(R.string.reports_pdf_period),
                            onClick = exportPeriod,
                            icon = Icons.Filled.PictureAsPdf,
                        )
                    }
                }
                ReportTab.PROJECT -> {
                    item {
                        SectionCard(title = stringResource(R.string.reports_project_detail)) {
                            if (state.projectFilter == null) {
                                Text(stringResource(R.string.reports_select_project_detail), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else if (state.workdays.isEmpty()) {
                                EmptyState(stringResource(R.string.label_no_data))
                            } else {
                                state.workdays.sortedBy { it.dateEpochDay }.forEach { workday ->
                                    workday.operations.forEachIndexed { index, operation ->
                                        StatRow(
                                            label = "${DateFormatter.format(workday.dateEpochDay)} | ${index + 1}. ${operation.startType.label} ${operation.startCode} ← ${operation.endType.label} ${operation.endCode}",
                                            value = NumberFormatter.formatMeters(operation.lengthMeters),
                                            hint = "${operation.startMeter} → ${operation.endMeter}",
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        ShootingButton(
                            text = stringResource(R.string.reports_pdf_project_detail),
                            onClick = exportProject,
                            icon = Icons.Filled.PictureAsPdf,
                            enabled = state.projectFilter != null && state.workdays.isNotEmpty(),
                        )
                    }
                }
                ReportTab.EXPENSES -> {
                    item {
                        SectionCard(title = stringResource(R.string.label_expense_summary)) {
                            StatRow(stringResource(R.string.reports_expense_totals),
                                NumberFormatter.formatToman(state.periodReport.totalExpensesToman))
                            StatRow(stringResource(R.string.reports_settled),
                                NumberFormatter.formatToman(state.periodReport.settledToman))
                            StatRow(stringResource(R.string.reports_outstanding),
                                NumberFormatter.formatToman(state.periodReport.outstandingToman), emphasised = true)
                        }
                    }
                    item {
                        SectionCard(title = stringResource(R.string.reports_expenses)) {
                            if (state.expenseRows.isEmpty()) {
                                EmptyState(stringResource(R.string.label_no_data))
                            } else {
                                state.expenseRows.forEach { row ->
                                    StatRow(
                                        label = "${DateFormatter.format(row.expense.dateEpochDay)} — ${row.expense.categoryName}",
                                        value = NumberFormatter.formatToman(row.expense.outstandingToman),
                                    )
                                }
                            }
                        }
                    }
                    item {
                        ShootingButton(
                            text = stringResource(R.string.reports_pdf_expenses),
                            onClick = exportExpenses,
                            icon = Icons.Filled.PictureAsPdf,
                        )
                    }
                }
            }
        }
    }
}
