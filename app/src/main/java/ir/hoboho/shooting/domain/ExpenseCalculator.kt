package ir.hoboho.shooting.domain

import ir.hoboho.shooting.domain.model.Expense
import ir.hoboho.shooting.domain.model.Settlement
import ir.hoboho.shooting.domain.model.SettlementStatus

/** All money is integer Toman. */
object ExpenseCalculator {
    fun totalSettled(settlements: List<Settlement>): Long {
        var total = 0L
        for (settlement in settlements) {
            require(settlement.amountToman > 0) { "Settlement amount must be positive" }
            total = Math.addExact(total, settlement.amountToman)
        }
        return total
    }

    fun outstanding(amountToman: Long, settledToman: Long): Long {
        require(amountToman >= 0) { "Expense amount must be non-negative" }
        require(settledToman >= 0) { "Settled amount must be non-negative" }
        require(settledToman <= amountToman) { "Settlements exceed the expense" }
        return amountToman - settledToman
    }

    fun status(amountToman: Long, settledToman: Long): SettlementStatus = when {
        settledToman <= 0L -> SettlementStatus.UNSETTLED
        settledToman >= amountToman -> SettlementStatus.SETTLED
        else -> SettlementStatus.PARTIAL
    }

    fun canAddSettlement(amountToman: Long, settledToman: Long, additionalToman: Long): Boolean =
        additionalToman > 0L && additionalToman <= outstanding(amountToman, settledToman)

    /** FIFO: oldest approved expense first; the final allocation may be partial. */
    fun fifoAllocations(expenses: List<Expense>, paymentToman: Long): List<SettlementAllocation> {
        require(paymentToman > 0L) { "Company payment must be positive" }
        val eligible = expenses.asSequence()
            .filter { it.companyApproved && it.outstandingToman > 0L }
            .sortedWith(compareBy<Expense> { it.dateEpochDay }.thenBy { it.id })
            .toList()
        // The full cash receipt is recorded separately. Allocate only what is owed.
        var remaining = paymentToman
        return buildList {
            for (expense in eligible) {
                if (remaining == 0L) break
                val allocation = minOf(remaining, expense.outstandingToman)
                add(SettlementAllocation(expense.id, allocation))
                remaining -= allocation
            }
        }
    }

    fun overview(expenses: List<Expense>): ExpenseOverview {
        var total = 0L
        var reimbursed = 0L
        var outstanding = 0L
        var unsettled = 0
        var partial = 0
        var settledCount = 0
        for (expense in expenses) {
            total = Math.addExact(total, expense.amountToman)
            if (expense.companyApproved) {
                reimbursed = Math.addExact(reimbursed, expense.settledAmountToman)
                outstanding = Math.addExact(outstanding, expense.outstandingToman)
                when (expense.status) {
                    SettlementStatus.UNSETTLED -> unsettled++
                    SettlementStatus.PARTIAL -> partial++
                    SettlementStatus.SETTLED -> settledCount++
                }
            }
        }
        return ExpenseOverview(total, reimbursed, outstanding, unsettled, partial, settledCount)
    }
}

data class SettlementAllocation(val expenseId: Long, val amountToman: Long)

data class ExpenseOverview(
    val totalExpensesToman: Long = 0L,
    val totalReimbursedToman: Long = 0L,
    val outstandingToman: Long = 0L,
    val unsettledCount: Int = 0,
    val partiallySettledCount: Int = 0,
    val fullySettledCount: Int = 0,
)
