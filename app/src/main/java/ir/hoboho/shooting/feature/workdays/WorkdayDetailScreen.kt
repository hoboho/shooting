package ir.hoboho.shooting.feature.workdays

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import ir.hoboho.shooting.ui.components.ButtonLevel
import ir.hoboho.shooting.ui.components.EmptyState
import ir.hoboho.shooting.ui.components.SectionCard
import ir.hoboho.shooting.ui.components.ShootingButton
import ir.hoboho.shooting.ui.components.StatRow

@Composable
fun WorkdayDetailScreen(
    workdayId: Long,
    onEdit: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: WorkdayDetailViewModel = hiltViewModel(),
) {
    val workday by viewModel.workday.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(workdayId) { viewModel.load(workdayId) }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.workday_detail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        val report = workday
        if (report == null) {
            EmptyState(stringResource(R.string.error_not_found))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard(
                    title = report.projectName,
                    subtitle = DateFormatter.formatLongWithWeekday(report.dateEpochDay),
                ) {
                    StatRow(stringResource(R.string.label_work_area),
                        report.workArea.ifBlank { stringResource(R.string.value_not_set) })
                    StatRow(stringResource(R.string.label_employer),
                        report.employer.ifBlank { stringResource(R.string.value_not_set) })
                    StatRow(stringResource(R.string.label_supervisor),
                        report.supervisor.ifBlank { stringResource(R.string.value_not_set) })
                    StatRow(stringResource(R.string.label_worker_count),
                        NumberFormatter.formatInteger(report.workerCount))
                    if (report.notes.isNotBlank()) {
                        StatRow(stringResource(R.string.label_notes), report.notes)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    StatRow(
                        label = stringResource(R.string.label_daily_total),
                        value = NumberFormatter.formatMeters(report.totalLengthMeters),
                        emphasised = true,
                    )
                }
            }
            item {
                SectionCard(title = stringResource(R.string.label_operations)) {
                    report.operations.forEachIndexed { index, op ->
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row {
                                Text(
                                    stringResource(R.string.operation_title, NumberFormatter.formatInteger(index + 1)),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                "${stringResource(R.string.label_start_point)}: ${op.startType.label} / ${op.startCode} / " +
                                    "${stringResource(R.string.label_start_meter)}: ${NumberFormatter.formatMetersValue(op.startMeter)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                "${stringResource(R.string.label_end_point)}: ${op.endType.label} / ${op.endCode} / " +
                                    "${stringResource(R.string.label_end_meter)}: ${NumberFormatter.formatMetersValue(op.endMeter)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            StatRow(
                                label = stringResource(R.string.operation_length),
                                value = NumberFormatter.formatMeters(op.lengthMeters),
                                emphasised = true,
                            )
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShootingButton(
                        text = stringResource(R.string.action_edit),
                        onClick = { onEdit(report.id) },
                        fullWidth = false,
                        modifier = Modifier.weight(1f),
                    )
                    ShootingButton(
                        text = stringResource(R.string.action_export_pdf),
                        onClick = viewModel::exportPdf,
                        icon = Icons.Filled.PictureAsPdf,
                        level = ButtonLevel.Secondary,
                        fullWidth = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
