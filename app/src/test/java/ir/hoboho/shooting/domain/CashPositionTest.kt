package ir.hoboho.shooting.domain

import com.google.common.truth.Truth.assertThat
import ir.hoboho.shooting.domain.model.CashPosition
import org.junit.Test

class CashPositionTest {
    @Test
    fun deficitIsKeptAsNegativeCashAndPositiveCompanyReceivable() {
        val afterThirteenMillionExpenseAndTenMillionAdvance = CashPosition(-3_000_000L)
        assertThat(afterThirteenMillionExpenseAndTenMillionAdvance.cashboxBalanceToman).isEqualTo(-3_000_000L)
        assertThat(afterThirteenMillionExpenseAndTenMillionAdvance.companyReceivableToman).isEqualTo(3_000_000L)

        val afterTwoMillionAdditionalReceipt = CashPosition(-1_000_000L)
        assertThat(afterTwoMillionAdditionalReceipt.cashboxBalanceToman).isEqualTo(-1_000_000L)
        assertThat(afterTwoMillionAdditionalReceipt.companyReceivableToman).isEqualTo(1_000_000L)
    }

    @Test
    fun positiveCashBalanceDoesNotCreateCompanyReceivable() {
        val position = CashPosition(7_000_000L)
        assertThat(position.cashboxBalanceToman).isEqualTo(7_000_000L)
        assertThat(position.companyReceivableToman).isEqualTo(0L)
    }
}
