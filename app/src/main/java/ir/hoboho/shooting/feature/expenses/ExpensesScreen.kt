@file:OptIn(ExperimentalMaterial3Api::class)

package ir.hoboho.shooting.feature.expenses

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.foundation.isSystemInDarkTheme
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
import ir.hoboho.shooting.ui.components.ConfirmDialog
import ir.hoboho.shooting.ui.components.EmptyState
import ir.hoboho.shooting.ui.components.SectionCard
import ir.hoboho.shooting.ui.components.ShootingButton
import ir.hoboho.shooting.ui.components.StatRow
import ir.hoboho.shooting.ui.components.TagChip

@Composable
fun ExpensesScreen(
    onNewExpense: () -> Unit,
    onEditExpense: (Long) -> Unit,
    viewModel: ExpensesViewModel = hiltViewModel(),
) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val companyPayments by viewModel.companyPayments.collectAsStateWithLifecycle()
    val cashPosition by viewModel.cashPosition.collectAsStateWithLifecycle()
    val cashLedger by viewModel.cashLedger.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val paymentSaving by viewModel.paymentSaving.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<Long?>(null) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var paymentAmount by remember { mutableStateOf("") }
    var paymentReceiptReference by remember { mutableStateOf("") }
    var paymentDateEpochDay by remember { mutableStateOf(JalaliCalendar.todayEpochDay()) }
    var showPaymentDatePicker by remember { mutableStateOf(false) }
    var companyPaymentsExpanded by remember { mutableStateOf(false) }
    var cashLedgerExpanded by remember { mutableStateOf(false) }

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
                TextButton(enabled = !paymentSaving, onClick = {
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
                val totalReceived = companyPayments.sumOf { it.amountToman }
                SectionCard(
                    title = stringResource(R.string.expense_overview),
                    icon = Icons.Filled.AccountBalanceWallet,
                    subtitle = "نمای کلی وضعیت مالی پروژه",
                ) {
                    StatRow(stringResource(R.string.label_total_expenses),
                        NumberFormatter.formatToman(overview.totalExpensesToman))
                    CompanyIncomeRow(
                        label = stringResource(R.string.label_total_company_payments),
                        value = NumberFormatter.formatToman(totalReceived),
                    )
                    StatRow("موجودی صندوق پروژه",
                        NumberFormatter.formatToman(cashPosition.cashboxBalanceToman), emphasised = true)
                    StatRow("خالص گردش مالی",
                        NumberFormatter.formatToman(cashPosition.cashboxBalanceToman), emphasised = true)
                }
            }
            if (companyPayments.isNotEmpty()) {
                item {
                    SectionCard(
                        title = stringResource(R.string.expense_company_payments),
                        icon = Icons.Filled.Payments,
                        subtitle = "${NumberFormatter.formatInteger(companyPayments.size)} دریافتی ثبت‌شده",
                        trailing = {
                            IconButton(onClick = { companyPaymentsExpanded = !companyPaymentsExpanded }) {
                                Icon(
                                    if (companyPaymentsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (companyPaymentsExpanded) "بستن سوابق دریافتی" else "نمایش سوابق دریافتی",
                                )
                            }
                        },
                    ) {
                        AnimatedVisibility(visible = companyPaymentsExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                companyPayments.forEach { payment ->
                                    Surface(
                                        color = (if (isSystemInDarkTheme()) Color(0xFF69F0AE) else Color(0xFF00A844))
                                            .copy(alpha = 0.09f),
                                        shape = RoundedCornerShape(14.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    DateFormatter.format(payment.dateEpochDay),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                )
                                                if (payment.receiptReference.isNotBlank()) {
                                                    Text(
                                                        payment.receiptReference,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                            }
                                            Text(
                                                NumberFormatter.formatToman(payment.amountToman),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSystemInDarkTheme()) Color(0xFF69F0AE) else Color(0xFF00A844),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (cashLedger.isNotEmpty()) {
                item {
                    SectionCard(
                        title = "دفتر گردش مالی",
                        icon = Icons.Filled.ReceiptLong,
                        subtitle = "${NumberFormatter.formatInteger(cashLedger.size)} تراکنش در دفتر",
                        trailing = {
                            IconButton(onClick = { cashLedgerExpanded = !cashLedgerExpanded }) {
                                Icon(
                                    if (cashLedgerExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (cashLedgerExpanded) "بستن دفتر گردش مالی" else "باز کردن دفتر گردش مالی",
                                )
                            }
                        },
                    ) {
                        AnimatedVisibility(visible = cashLedgerExpanded) {
                            Column {
                                cashLedger.forEach { entry ->
                                    StatRow(
                                        label = "${DateFormatter.format(entry.dateEpochDay)} — ${entry.description.ifBlank { entry.originType }}",
                                        value = (if (entry.amountToman > 0L) "+" else "") + NumberFormatter.formatToman(entry.amountToman),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (expenses.isEmpty()) {
                item { EmptyState(stringResource(R.string.expenses_empty)) }
            }
            itemsIndexed(expenses, key = { _, item -> item.id }) { index, expense ->
                var expanded by remember(expense.id) { mutableStateOf(false) }
                SectionCard(
                    title = expense.categoryName,
                    icon = Icons.Filled.ReceiptLong,
                    subtitle = "${DateFormatter.format(expense.dateEpochDay)} — ${expense.description}",
                    modifier = Modifier.pointerInput(expense.id, expenses) {
                        var drag = 0f
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, amount -> change.consume(); drag += amount },
                            onDragEnd = {
                                if (drag < -80f && index < expenses.lastIndex) onEditExpense(expenses[index + 1].id)
                                else if (drag > 80f && index > 0) onEditExpense(expenses[index - 1].id)
                                drag = 0f
                            },
                            onDragCancel = { drag = 0f },
                        )
                    },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                NumberFormatter.formatToman(expense.amountToman),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            TagChip(stringResource(if (expense.companyApproved) R.string.expense_approved else R.string.expense_not_approved))
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(
                                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (expanded) "بستن جزئیات هزینه" else "نمایش جزئیات هزینه",
                                )
                            }
                        }
                    },
                ) {
                    AnimatedVisibility(visible = expanded) {
                        Column {
                            StatRow(stringResource(R.string.expense_amount),
                                NumberFormatter.formatToman(expense.amountToman))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { onEditExpense(expense.id) }) {
                                    Text(stringResource(R.string.action_edit))
                                }
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
    }
}

@Composable
private fun CompanyIncomeRow(label: String, value: String) {
    val incomeColor = if (isSystemInDarkTheme()) Color(0xFF69F0AE) else Color(0xFF00A844)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = incomeColor)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = incomeColor)
    }
}
