package ir.hoboho.shooting.domain

import ir.hoboho.shooting.domain.model.Expense
import ir.hoboho.shooting.domain.model.Settlement
import ir.hoboho.shooting.domain.model.SettlementStatus

/** All money is integer Toman. No floating point anywhere in this file. */
object ExpenseCalculator {

    fun totalSettled(settlements: List<Settlement>): Long {
        var total = 0L
        for (settlement in settlements) {
            require(settlement.amountToman > 0) { "Settlement amount must be positive" }
            total = Math.addExact(total, settlement.amountToman)
        }
        return total
    }

    /** Outstanding balance = expense amount - total settlements. */
    fun outstanding(amountToman: Long, settledToman: Long): Long {
        require(amountToman >= 0) { "Expense amount must be non-negative" }
        require(settledToman >= 0) { "Settled amount must be non-negative" }
        require(settledToman <= amountToman) {
            "Settlements ($settledToman) exceed the expense ($amountToman)"
        }
        return amountToman - settledToman
    }

    fun status(amountToman: Long, settledToman: Long): SettlementStatus = when {
        settledToman <= 0L -> SettlementStatus.UNSETTLED
        settledToman >= amountToman -> SettlementStatus.SETTLED
        else -> SettlementStatus.PARTIAL
    }

    /** True only when the additional settlement fits inside the outstanding balance. */
    fun canAddSettlement(amountToman: Long, settledToman: Long, additionalToman: Long): Boolean =
        additionalToman > 0L && additionalToman <= outstanding(amountToman, settledToman)

    fun overview(expenses: List<Expense>): ExpenseOverview {
        var total = 0L
        var reimbursed = 0L
        var outstanding = 0L
        var unsettled = 0
        var partial = 0
        var settledCount = 0
        for (expense in expenses) {
            total = Math.addExact(total, expense.amountToman)
            reimbursed = Math.addExact(reimbursed, expense.settledAmountToman)
            outstanding = Math.addExact(outstanding, expense.outstandingToman)
            when (expense.status) {
                SettlementStatus.UNSETTLED -> unsettled++
                SettlementStatus.PARTIAL -> partial++
                SettlementStatus.SETTLED -> settledCount++
            }
        }
        return ExpenseOverview(
            totalExpensesToman = total,
            totalReimbursedToman = reimbursed,
            outstandingToman = outstanding,
            unsettledCount = unsettled,
            partiallySettledCount = partial,
            fullySettledCount = settledCount,
        )
    }
}

data class ExpenseOverview(
    val totalExpensesToman: Long = 0L,
    val totalReimbursedToman: Long = 0L,
    val outstandingToman: Long = 0L,
    val unsettledCount: Int = 0,
    val partiallySettledCount: Int = 0,
    val fullySettledCount: Int = 0,
)
