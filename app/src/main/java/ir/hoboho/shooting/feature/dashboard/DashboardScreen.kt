package ir.hoboho.shooting.feature.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hoboho.shooting.R
import ir.hoboho.shooting.core.format.DateFormatter
import ir.hoboho.shooting.core.format.NumberFormatter
import ir.hoboho.shooting.ui.components.ButtonLevel
import ir.hoboho.shooting.ui.components.ChoiceChips
import ir.hoboho.shooting.ui.components.EmptyState
import ir.hoboho.shooting.ui.components.SectionCard
import ir.hoboho.shooting.ui.components.ShootingButton
import ir.hoboho.shooting.ui.components.StatRow
import ir.hoboho.shooting.ui.components.TagChip

@Composable
fun DashboardScreen(
    onNewWorkday: () -> Unit,
    onOpenWorkday: (Long) -> Unit,
    onNewExpense: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenProjects: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(stringResource(R.string.dashboard_title), style = MaterialTheme.typography.headlineMedium)
            Text(state.todayLabel, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShootingButton(
                    text = stringResource(R.string.dashboard_quick_workday),
                    onClick = onNewWorkday,
                    icon = Icons.Filled.Add,
                    fullWidth = false,
                    modifier = Modifier.weight(1f),
                )
                ShootingButton(
                    text = stringResource(R.string.dashboard_quick_report),
                    onClick = onOpenReports,
                    icon = Icons.Filled.Assessment,
                    level = ButtonLevel.Secondary,
                    fullWidth = false,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            SectionCard(
                title = stringResource(R.string.dashboard_period_length),
                subtitle = state.periodLabel,
            ) {
                ChoiceChips(
                    options = DashboardPeriod.entries,
                    selected = state.period,
                    onSelected = viewModel::setPeriod,
                    labelOf = { period ->
                        stringResource(
                            when (period) {
                                DashboardPeriod.MONTH -> R.string.dashboard_this_month
                                DashboardPeriod.YEAR -> R.string.dashboard_this_year
                                DashboardPeriod.ALL -> R.string.dashboard_all_time
                            },
                        )
                    },
                )
                StatRow(
                    label = stringResource(R.string.label_total_length),
                    value = NumberFormatter.formatMeters(state.periodLengthMeters),
                    emphasised = true,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                StatRow(
                    label = stringResource(R.string.dashboard_outstanding),
                    value = NumberFormatter.formatToman(state.outstandingToman),
                )
                StatRow(
                    label = stringResource(R.string.dashboard_active_projects),
                    value = NumberFormatter.formatInteger(state.activeProjects),
                )
            }
        }

        item {
            ShootingButton(
                text = stringResource(R.string.dashboard_quick_expense),
                onClick = onNewExpense,
                icon = Icons.Filled.Receipt,
                level = ButtonLevel.Outline,
            )
        }

        item {
            SectionCard(
                title = stringResource(R.string.dashboard_recent_workdays),
                trailing = { TagChip(stringResource(R.string.action_open)) },
            ) {
                if (state.recentWorkdays.isEmpty()) {
                    EmptyState(stringResource(R.string.workdays_empty))
                } else {
                    state.recentWorkdays.forEach { workday ->
                        StatRow(
                            label = "${workday.projectName} — ${DateFormatter.format(workday.dateEpochDay)}",
                            value = NumberFormatter.formatMeters(workday.totalLengthMeters),
                            modifier = Modifier.clickable { onOpenWorkday(workday.id) },
                        )
                    }
                    ShootingButton(
                        text = stringResource(R.string.nav_projects),
                        onClick = onOpenProjects,
                        level = ButtonLevel.Text,
                    )
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.dashboard_recent_operations)) {
                if (state.recentOperations.isEmpty()) {
                    EmptyState(stringResource(R.string.label_no_data))
                } else {
                    state.recentOperations.forEach { operation ->
                        StatRow(
                            label = "${operation.startCode} → ${operation.endCode}",
                            value = NumberFormatter.formatMeters(operation.lengthMeters),
                        )
                    }
                }
            }
        }
    }
}
