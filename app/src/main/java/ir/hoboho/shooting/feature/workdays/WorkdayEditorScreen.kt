@file:OptIn(ExperimentalMaterial3Api::class)

package ir.hoboho.shooting.feature.workdays

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hoboho.shooting.R
import ir.hoboho.shooting.core.format.DateFormatter
import ir.hoboho.shooting.core.format.NumberFormatter
import ir.hoboho.shooting.domain.model.CableType
import ir.hoboho.shooting.domain.model.OperationDraft
import ir.hoboho.shooting.domain.model.PointType
import ir.hoboho.shooting.ui.components.ButtonLevel
import ir.hoboho.shooting.ui.components.ChoiceChips
import ir.hoboho.shooting.ui.components.ClickableField
import ir.hoboho.shooting.ui.components.EmptyState
import ir.hoboho.shooting.ui.components.JalaliDatePickerDialog
import ir.hoboho.shooting.ui.components.SectionCard
import ir.hoboho.shooting.ui.components.ShootingButton
import ir.hoboho.shooting.ui.components.ShootingDropdown
import ir.hoboho.shooting.ui.components.ShootingNumberField
import ir.hoboho.shooting.ui.components.ShootingTextField
import ir.hoboho.shooting.ui.components.StatRow

@Composable
private fun pointTypeLabel(type: PointType): String = stringResource(
    when (type) {
        PointType.ODC -> R.string.point_odc
        PointType.FAT -> R.string.point_fat
        PointType.HANDHOLE -> R.string.point_handhole
        PointType.MANHOLE -> R.string.point_manhole
    },
)

@Composable
fun WorkdayEditorScreen(
    workdayId: Long,
    initialProjectId: Long,
    onDone: () -> Unit,
    viewModel: WorkdayEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }
    var operationQuery by remember { mutableStateOf("") }
    var operationCableFilter by remember { mutableStateOf<Long?>(null) }
    var operationPointFilter by remember { mutableStateOf<PointType?>(null) }

    LaunchedEffect(workdayId, initialProjectId) { viewModel.load(workdayId, initialProjectId) }
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    LaunchedEffect(state.error) { state.error?.let { snackbar.showSnackbar(it) } }

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initialEpochDay = state.dateEpochDay,
            onDismiss = { showDatePicker = false },
            onDateSelected = viewModel::setDate,
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (workdayId == 0L) R.string.workday_new else R.string.workday_edit)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionCard(
                title = stringResource(R.string.workday_detail),
                subtitle = if (state.inherited) stringResource(R.string.workday_inherited_hint)
                else stringResource(R.string.workday_reopen_hint),
            ) {
                ShootingDropdown(
                    label = stringResource(R.string.label_project),
                    options = state.projects,
                    selected = state.projects.firstOrNull { it.id == state.projectId },
                    onSelected = { viewModel.setProject(it.id) },
                    labelOf = { it.name },
                    enabled = workdayId == 0L,
                )
                ClickableField(
                    label = stringResource(R.string.workday_date),
                    value = DateFormatter.formatLong(state.dateEpochDay),
                    onClick = { showDatePicker = true },
                )
                ShootingTextField(state.workArea, viewModel::setWorkArea, stringResource(R.string.label_work_area))
                ShootingTextField(state.employer, viewModel::setEmployer, stringResource(R.string.label_employer))
                ShootingTextField(state.supervisor, viewModel::setSupervisor, stringResource(R.string.label_supervisor))
                ShootingNumberField(
                    value = state.workerCount,
                    onValueChange = viewModel::setWorkerCount,
                    label = stringResource(R.string.label_worker_count),
                    grouped = false,
                )
                ShootingTextField(
                    state.notes, viewModel::setNotes, stringResource(R.string.label_notes),
                    singleLine = false, minLines = 2,
                )
            }

            SectionCard(
                title = stringResource(R.string.label_operations),
                trailing = {
                    IconButton(onClick = viewModel::addOperation) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.operation_new))
                    }
                },
            ) {
                if (state.operations.isEmpty()) {
                    Text(stringResource(R.string.msg_workday_operations_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    ShootingTextField(
                        value = operationQuery,
                        onValueChange = { operationQuery = it },
                        label = stringResource(R.string.search_operations_hint),
                    )
                    Text(stringResource(R.string.label_cable_type), style = MaterialTheme.typography.labelMedium)
                    ChoiceChips(
                        options = listOf<Long?>(null) + state.cableTypes.map { it.id },
                        selected = operationCableFilter,
                        onSelected = { operationCableFilter = it },
                        labelOf = { id -> id?.let { selectedId -> state.cableTypes.firstOrNull { it.id == selectedId }?.label }
                            ?: stringResource(R.string.filter_all) },
                    )
                    Text(stringResource(R.string.label_start_type), style = MaterialTheme.typography.labelMedium)
                    ChoiceChips(
                        options = listOf<PointType?>(null) + PointType.entries,
                        selected = operationPointFilter,
                        onSelected = { operationPointFilter = it },
                        labelOf = { it?.let { type -> pointTypeLabel(type) } ?: stringResource(R.string.filter_all) },
                    )
                }
                val visibleOperations = state.operations.withIndex().filter { (_, draft) ->
                    val queryMatches = operationQuery.isBlank() ||
                        draft.startCode.contains(operationQuery.trim(), ignoreCase = true) ||
                        draft.endCode.contains(operationQuery.trim(), ignoreCase = true)
                    queryMatches &&
                        (operationCableFilter == null || draft.cableTypeId == operationCableFilter) &&
                        (operationPointFilter == null ||
                            draft.startType == operationPointFilter || draft.endType == operationPointFilter)
                }
                if (state.operations.isNotEmpty() && visibleOperations.isEmpty()) {
                    EmptyState(stringResource(R.string.search_no_results))
                }
                visibleOperations.forEach { (index, draft) ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                    OperationEditor(
                        index = index + 1,
                        draft = draft,
                        cableTypes = state.cableTypes,
                        invalid = draft.key in state.invalidRows,
                        onChange = { updated -> viewModel.updateOperation(draft.key) { updated } },
                        onRemove = { viewModel.removeOperation(draft.key) },
                    )
                }
                ShootingButton(
                    text = stringResource(R.string.operation_new),
                    onClick = viewModel::addOperation,
                    icon = Icons.Filled.Add,
                    level = ButtonLevel.Secondary,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                StatRow(
                    label = stringResource(R.string.label_daily_total),
                    value = NumberFormatter.formatMeters(state.totalLengthMeters),
                    emphasised = true,
                    hint = stringResource(R.string.operation_length_hint),
                )
            }

            ShootingButton(
                text = stringResource(R.string.action_save),
                onClick = viewModel::save,
                enabled = !state.saving && !state.saved,
            )
        }
    }
}

@Composable
private fun OperationEditor(
    index: Int,
    draft: OperationDraft,
    cableTypes: List<CableType>,
    invalid: Boolean,
    onChange: (OperationDraft) -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.operation_title, NumberFormatter.formatInteger(index)),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.operation_delete),
                    tint = MaterialTheme.colorScheme.error)
            }
        }

        Text(stringResource(R.string.operation_section_start), style = MaterialTheme.typography.labelLarge)
        ChoiceChips(
            options = PointType.entries,
            selected = draft.startType,
            onSelected = { onChange(draft.copy(startType = it)) },
            labelOf = { pointTypeLabel(it) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                ShootingTextField(
                    draft.startCode, { onChange(draft.copy(startCode = it)) },
                    stringResource(R.string.label_point_code), isError = invalid,
                    keyboardType = KeyboardType.Number,
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                ShootingNumberField(
                    draft.startMeter, { onChange(draft.copy(startMeter = it)) },
                    stringResource(R.string.label_start_meter), suffix = stringResource(R.string.meter),
                    isError = invalid, grouped = false,
                )
            }
        }

        Text(stringResource(R.string.operation_section_end), style = MaterialTheme.typography.labelLarge)
        ChoiceChips(
            options = PointType.entries,
            selected = draft.endType,
            onSelected = { onChange(draft.copy(endType = it)) },
            labelOf = { pointTypeLabel(it) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                ShootingTextField(
                    value = if (draft.startType == PointType.ODC && draft.endType == PointType.FAT)
                        draft.endCode.removePrefix(draft.startCode) else draft.endCode,
                    onValueChange = {
                        val fullCode = if (draft.startType == PointType.ODC && draft.endType == PointType.FAT)
                            draft.startCode + it else it
                        onChange(draft.copy(endCode = fullCode))
                    },
                    label = if (draft.startType == PointType.ODC && draft.endType == PointType.FAT)
                        stringResource(R.string.label_fat_suffix) else stringResource(R.string.label_point_code),
                    isError = invalid,
                    keyboardType = KeyboardType.Number,
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                ShootingNumberField(
                    draft.endMeter, { onChange(draft.copy(endMeter = it)) },
                    stringResource(R.string.label_end_meter), suffix = stringResource(R.string.meter),
                    isError = invalid, grouped = false,
                )
            }
        }

        ShootingDropdown(
            label = stringResource(R.string.label_cable_type),
            options = cableTypes,
            selected = cableTypes.firstOrNull { it.id == draft.cableTypeId },
            onSelected = { onChange(draft.copy(cableTypeId = it.id)) },
            labelOf = { it.label },
        )
        StatRow(
            label = stringResource(R.string.operation_length),
            value = draft.lengthMeters?.let { NumberFormatter.formatMeters(it) }
                ?: NumberFormatter.EM_DASH,
            emphasised = true,
        )
    }
}
