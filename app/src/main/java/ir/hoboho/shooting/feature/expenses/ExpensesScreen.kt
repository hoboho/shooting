@file:OptIn(ExperimentalMaterial3Api::class)

package ir.hoboho.shooting.feature.expenses

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hoboho.shooting.R
import ir.hoboho.shooting.core.format.DateFormatter
import ir.hoboho.shooting.core.format.NumberFormatter
import ir.hoboho.shooting.core.date.JalaliCalendar
import ir.hoboho.shooting.ui.components.ClickableField
import ir.hoboho.shooting.ui.components.JalaliDatePickerDialog
import ir.hoboho.shooting.domain.model.SettlementStatus
import ir.hoboho.shooting.ui.components.ConfirmDialog
import ir.hoboho.shooting.ui.components.EmptyState
import ir.hoboho.shooting.ui.components.SectionCard
import ir.hoboho.shooting.ui.components.ShootingButton
import ir.hoboho.shooting.ui.components.StatRow
import ir.hoboho.shooting.ui.components.TagChip

@Composable
private fun statusLabel(status: SettlementStatus): String = stringResource(
    when (status) {
        SettlementStatus.UNSETTLED -> R.string.status_unsettled
        SettlementStatus.PARTIAL -> R.string.status_partial
        SettlementStatus.SETTLED -> R.string.status_settled
    },
)

@Composable
fun ExpensesScreen(
    onNewExpense: () -> Unit,
    onEditExpense: (Long) -> Unit,
    viewModel: ExpensesViewModel = hiltViewModel(),
) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val companyPayments by viewModel.companyPayments.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<Long?>(null) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var paymentAmount by remember { mutableStateOf("") }
    var paymentReceiptReference by remember { mutableStateOf("") }
    var paymentDateEpochDay by remember { mutableStateOf(JalaliCalendar.todayEpochDay()) }
    var showPaymentDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(error) { error?.let { snackbar.showSnackbar(it); viewModel.consumeError() } }
    if (showPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text(stringResource(R.string.expense_company_payment)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = paymentAmount,
                        onValueChange = { paymentAmount = it },
                        label = { Text(stringResource(R.string.label_amount)) },
                        suffix = { Text(stringResource(R.string.toman)) },
                        singleLine = true,
                    )
                    ClickableField(
                        label = stringResource(R.string.label_date),
                        value = DateFormatter.format(paymentDateEpochDay),
                        onClick = { showPaymentDatePicker = true },
                    )
                    OutlinedTextField(
                        value = paymentReceiptReference,
                        onValueChange = { paymentReceiptReference = it },
                        label = { Text(stringResource(R.string.label_receipt_reference)) },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.allocateCompanyPayment(paymentAmount, paymentDateEpochDay, paymentReceiptReference)
                    paymentAmount = ""
                    paymentReceiptReference = ""
                    paymentDateEpochDay = JalaliCalendar.todayEpochDay()
                    showPaymentDialog = false
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
    if (showPaymentDatePicker) {
        JalaliDatePickerDialog(
            initialEpochDay = paymentDateEpochDay,
            onDismiss = { showPaymentDatePicker = false },
            onDateSelected = { paymentDateEpochDay = it },
        )
    }
    if (pendingDelete != null) {
        ConfirmDialog(
            title = stringResource(R.string.action_delete),
            message = stringResource(R.string.expenses_hint),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { viewModel.delete(pendingDelete!!); pendingDelete = null },
            onDismiss = { pendingDelete = null },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text(stringResource(R.string.expenses_title)) }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ShootingButton(
                    text = stringResource(R.string.expense_new),
                    onClick = onNewExpense,
                    icon = Icons.Filled.Add,
                )
            }
            item {
                ShootingButton(
                    text = stringResource(R.string.expense_company_payment),
                    onClick = { showPaymentDialog = true },
                    level = ir.hoboho.shooting.ui.components.ButtonLevel.Secondary,
                )
            }
            item {
                SectionCard(
                    title = stringResource(R.string.expense_overview),
                    subtitle = stringResource(R.string.expenses_hint),
                ) {
                    StatRow(stringResource(R.string.label_total_expenses),
                        NumberFormatter.formatToman(overview.totalExpensesToman))
                    StatRow(stringResource(R.string.label_total_reimbursed),
                        NumberFormatter.formatToman(overview.totalReimbursedToman))
                    StatRow(stringResource(R.string.label_outstanding_receivables),
                        NumberFormatter.formatToman(overview.outstandingToman), emphasised = true)
                }
            }
            if (companyPayments.isNotEmpty()) {
                item {
                    SectionCard(title = stringResource(R.string.expense_company_payments)) {
                        companyPayments.forEach { payment ->
                            StatRow(
                                DateFormatter.format(payment.dateEpochDay) +
                                    if (payment.receiptReference.isBlank()) "" else " — ${payment.receiptReference}",
                                NumberFormatter.formatToman(payment.amountToman),
                            )
                        }
                    }
                }
            }
            if (expenses.isEmpty()) {
                item { EmptyState(stringResource(R.string.expenses_empty)) }
            }
            items(expenses, key = { it.id }) { expense ->
                SectionCard(
                    title = expense.categoryName,
                    subtitle = "${DateFormatter.format(expense.dateEpochDay)} — ${expense.description}",
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TagChip(stringResource(if (expense.companyApproved) R.string.expense_approved else R.string.expense_not_approved))
                            TagChip(statusLabel(expense.status))
                        }
                    },
                ) {
                    StatRow(stringResource(R.string.expense_amount),
                        NumberFormatter.formatToman(expense.amountToman))
                    StatRow(stringResource(R.string.expense_reimbursed),
                        NumberFormatter.formatToman(expense.settledAmountToman))
                    StatRow(stringResource(R.string.expense_outstanding),
                        NumberFormatter.formatToman(expense.outstandingToman), emphasised = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onEditExpense(expense.id) }) {
                            Text(stringResource(R.string.action_edit))
                        }
                        Text(stringResource(R.string.expense_settlements) + ": " +
                            NumberFormatter.formatInteger(expense.settlementCount),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f))
                        IconButton(onClick = { pendingDelete = expense.id }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
