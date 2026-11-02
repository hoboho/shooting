@file:OptIn(ExperimentalMaterial3Api::class)

package ir.hoboho.shooting.feature.expenses

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hoboho.shooting.R
import ir.hoboho.shooting.core.format.DateFormatter
import ir.hoboho.shooting.core.format.NumberFormatter
import ir.hoboho.shooting.domain.model.Project
import ir.hoboho.shooting.domain.model.Workday
import ir.hoboho.shooting.ui.components.ButtonLevel
import ir.hoboho.shooting.ui.components.ClickableField
import ir.hoboho.shooting.ui.components.ConfirmDialog
import ir.hoboho.shooting.ui.components.JalaliDatePickerDialog
import ir.hoboho.shooting.ui.components.SectionCard
import ir.hoboho.shooting.ui.components.ShootingButton
import ir.hoboho.shooting.ui.components.ShootingDropdown
import ir.hoboho.shooting.ui.components.ShootingNumberField
import ir.hoboho.shooting.ui.components.ShootingTextField
import ir.hoboho.shooting.ui.components.StatRow

private fun workdayLabel(workday: Workday): String =
    "${workday.projectName} — ${DateFormatter.format(workday.dateEpochDay)}"

/** A non-null wrapper so "not set" is a real, selectable, labelled option. */
private data class OptionalRef<T>(val value: T?)

@Composable
fun ExpenseEditorScreen(
    expenseId: Long,
    onDone: () -> Unit,
    viewModel: ExpenseEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }
    var showSettlementDatePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val savedText = stringResource(R.string.msg_expense_saved)
    val notSetText = stringResource(R.string.value_not_set)

    LaunchedEffect(expenseId) { viewModel.load(expenseId) }
    LaunchedEffect(state.saved) {
        if (state.saved) {
            snackbar.showSnackbar(savedText)
            viewModel.consumeSaved()
        }
    }
    LaunchedEffect(state.deleted) { if (state.deleted) onDone() }
    LaunchedEffect(state.error) { state.error?.let { snackbar.showSnackbar(it) } }

    if (showDatePicker) {
        JalaliDatePickerDialog(state.dateEpochDay, { showDatePicker = false }, viewModel::setDate)
    }
    if (showSettlementDatePicker) {
        JalaliDatePickerDialog(state.settlementDate, { showSettlementDatePicker = false }, viewModel::setSettlementDate)
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_delete_title),
            message = stringResource(R.string.expense_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { viewModel.delete(); confirmDelete = false },
            onDismiss = { confirmDelete = false },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (expenseId == 0L) R.string.expense_new else R.string.expense_edit)) },
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
            Text(stringResource(R.string.expenses_hint),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)

            SectionCard(title = stringResource(if (expenseId == 0L) R.string.expense_new else R.string.expense_edit)) {
                ClickableField(
                    label = stringResource(R.string.expense_date),
                    value = DateFormatter.formatLong(state.dateEpochDay),
                    onClick = { showDatePicker = true },
                )
                ShootingDropdown(
                    label = stringResource(R.string.expense_category),
                    options = state.categories,
                    selected = state.categories.firstOrNull { it.id == state.categoryId },
                    onSelected = { viewModel.setCategory(it.id) },
                    labelOf = { it.name },
                )
                ShootingNumberField(
                    value = state.amount,
                    onValueChange = viewModel::setAmount,
                    label = stringResource(R.string.label_amount),
                    suffix = stringResource(R.string.toman),
                    isError = state.error != null,
                )
                ShootingTextField(state.description, viewModel::setDescription,
                    stringResource(R.string.expense_description), singleLine = false, minLines = 2)
                ShootingTextField(state.receiptReference, viewModel::setReceipt,
                    stringResource(R.string.expense_receipt))
                ShootingDropdown(
                    label = "${stringResource(R.string.expense_project)} (${stringResource(R.string.label_optional)})",
                    options = listOf(OptionalRef<Project>(null)) +
                        state.projects.map { OptionalRef<Project>(it) },
                    selected = OptionalRef(state.projects.firstOrNull { it.id == state.projectId }),
                    onSelected = { viewModel.setProject(it.value?.id) },
                    labelOf = { it.value?.name ?: notSetText },
                )
                ShootingDropdown(
                    label = "${stringResource(R.string.expense_workday)} (${stringResource(R.string.label_optional)})",
                    options = listOf(OptionalRef<Workday>(null)) +
                        state.workdays.map { OptionalRef<Workday>(it) },
                    selected = OptionalRef(state.workdays.firstOrNull { it.id == state.workdayId }),
                    onSelected = { viewModel.setWorkday(it.value?.id) },
                    labelOf = { it.value?.let(::workdayLabel) ?: notSetText },
                )
            }

            SectionCard(title = stringResource(R.string.expense_settlements)) {
                StatRow(stringResource(R.string.expense_amount),
                    NumberFormatter.formatToman(state.amountToman))
                StatRow(stringResource(R.string.expense_reimbursed),
                    NumberFormatter.formatToman(state.settledAmountToman))
                StatRow(stringResource(R.string.expense_outstanding),
                    NumberFormatter.formatToman(state.outstandingToman), emphasised = true)
                state.settlements.forEach { settlement ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Row {
                        Text(
                            "${DateFormatter.format(settlement.dateEpochDay)} — " +
                                NumberFormatter.formatToman(settlement.amountToman),
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { viewModel.deleteSettlement(settlement.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                    if (settlement.note.isNotBlank()) {
                        Text(settlement.note,
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                    }
                }
                ShootingNumberField(
                    value = state.settlementAmount,
                    onValueChange = viewModel::setSettlementAmount,
                    label = stringResource(R.string.label_amount),
                    suffix = stringResource(R.string.toman),
                )
                ClickableField(
                    label = stringResource(R.string.label_settlement_date),
                    value = DateFormatter.format(state.settlementDate),
                    onClick = { showSettlementDatePicker = true },
                )
                ShootingTextField(state.settlementNote, viewModel::setSettlementNote,
                    stringResource(R.string.label_settlement_note))
                ShootingButton(
                    text = stringResource(R.string.expense_settlement_new),
                    onClick = viewModel::addSettlement,
                    level = ButtonLevel.Secondary,
                    enabled = state.id != 0L,
                )
            }

            ShootingButton(
                text = stringResource(R.string.action_save),
                onClick = viewModel::save,
            )
            if (expenseId != 0L) {
                ShootingButton(
                    text = stringResource(R.string.expense_delete),
                    onClick = { confirmDelete = true },
                    level = ButtonLevel.Danger,
                    icon = Icons.Filled.Delete,
                )
            }
        }
    }
}
