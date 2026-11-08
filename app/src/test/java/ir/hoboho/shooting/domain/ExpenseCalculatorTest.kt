package ir.hoboho.shooting.domain

import ir.hoboho.shooting.domain.model.Expense
import ir.hoboho.shooting.domain.model.Settlement
import ir.hoboho.shooting.domain.model.SettlementStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ExpenseCalculatorTest {

    private fun expense(amount: Long, settled: Long) = Expense(
        dateEpochDay = 20_000L, categoryId = 1L, amountToman = amount,
        settledAmountToman = settled,
        companyApproved = true,
    )

    @Test
    fun outstanding_isAmountMinusTotalSettlements() {
        assertEquals(1_000_000L, ExpenseCalculator.outstanding(1_000_000L, 0L))
        assertEquals(650_000L, ExpenseCalculator.outstanding(1_000_000L, 350_000L))
        assertEquals(0L, ExpenseCalculator.outstanding(1_000_000L, 1_000_000L))
    }

    @Test
    fun status_coversUnsettledPartialAndFull() {
        assertEquals(SettlementStatus.UNSETTLED, ExpenseCalculator.status(1000L, 0L))
        assertEquals(SettlementStatus.PARTIAL, ExpenseCalculator.status(1000L, 400L))
        assertEquals(SettlementStatus.SETTLED, ExpenseCalculator.status(1000L, 1000L))
        assertEquals(SettlementStatus.UNSETTLED, expense(1000L, 0L).status)
        assertEquals(SettlementStatus.PARTIAL, expense(1000L, 400L).status)
        assertEquals(SettlementStatus.SETTLED, expense(1000L, 1000L).status)
    }

    @Test
    fun multipleSettlements_neverExceedOutstanding() {
        val settlements = listOf(
            Settlement(expenseId = 1L, amountToman = 200_000L, dateEpochDay = 20_001L),
            Settlement(expenseId = 1L, amountToman = 300_000L, dateEpochDay = 20_002L),
            Settlement(expenseId = 1L, amountToman = 150_000L, dateEpochDay = 20_003L),
        )
        val settled = ExpenseCalculator.totalSettled(settlements)
        assertEquals(650_000L, settled)
        assertEquals(350_000L, ExpenseCalculator.outstanding(1_000_000L, settled))
        assertTrue(ExpenseCalculator.canAddSettlement(1_000_000L, settled, 350_000L))
        assertFalse(ExpenseCalculator.canAddSettlement(1_000_000L, settled, 350_001L))
        assertFalse(ExpenseCalculator.canAddSettlement(1_000_000L, settled, 0L))
        assertTrue(Validator.settlement(350_001L, 1_000_000L, settled).any {
            it.field == ValidationField.SETTLEMENT_EXCEEDS_OUTSTANDING
        })
    }

    @Test
    fun settlementsAboveAmount_areRejected() {
        try {
            ExpenseCalculator.outstanding(500L, 501L)
            fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("exceed"))
        }
    }

    @Test
    fun fifoAllocations_useOldestApprovedExpensesAndAllowPartialAllocation() {
        val expenses = listOf(
            Expense(id = 3L, dateEpochDay = 30L, categoryId = 1L, amountToman = 900L, companyApproved = true),
            Expense(id = 1L, dateEpochDay = 10L, categoryId = 1L, amountToman = 1_500L, companyApproved = true),
            Expense(id = 2L, dateEpochDay = 20L, categoryId = 1L, amountToman = 1_000L, companyApproved = false),
        )
        assertEquals(
            listOf(SettlementAllocation(1L, 1_500L), SettlementAllocation(3L, 500L)),
            ExpenseCalculator.fifoAllocations(expenses, 2_000L),
        )
    }

    @Test
    fun fifoAllocations_acceptPaymentAboveReceivablesAndAllocateOnlyAvailableBalance() {
        val expenses = listOf(
            Expense(id = 1L, dateEpochDay = 10L, categoryId = 1L, amountToman = 500L, companyApproved = true),
            Expense(id = 2L, dateEpochDay = 20L, categoryId = 1L, amountToman = 1_000L, companyApproved = false),
        )
        assertEquals(
            listOf(SettlementAllocation(1L, 500L)),
            ExpenseCalculator.fifoAllocations(expenses, 5_000L),
        )
    }

    @Test
    fun overview_aggregatesIntegersOnly() {
        val expenses = listOf(
            expense(1_000_000L, 0L),
            expense(500_000L, 200_000L),
            expense(250_000L, 250_000L),
        )
        val overview = ExpenseCalculator.overview(expenses)
        assertEquals(1_750_000L, overview.totalExpensesToman)
        assertEquals(450_000L, overview.totalReimbursedToman)
        assertEquals(1_300_000L, overview.outstandingToman)
        assertEquals(1, overview.unsettledCount)
        assertEquals(1, overview.partiallySettledCount)
        assertEquals(1, overview.fullySettledCount)
    }
}
