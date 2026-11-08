package ir.hoboho.shooting.domain

import ir.hoboho.shooting.core.date.DateRange
import ir.hoboho.shooting.domain.model.CableType
import ir.hoboho.shooting.domain.model.Expense
import ir.hoboho.shooting.domain.model.ReferenceData
import ir.hoboho.shooting.domain.model.ShootingOperation
import ir.hoboho.shooting.domain.model.Workday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodAggregatorTest {

    private fun workday(
        id: Long,
        projectId: Long,
        name: String,
        day: Long,
        meters: Int,
        operations: List<ShootingOperation> = emptyList(),
    ) = Workday(
        id = id, projectId = projectId, projectName = name, dateEpochDay = day,
        operations = operations, totalLengthMeters = meters,
    )

    private fun expense(id: Long, day: Long, projectId: Long?, amount: Long, settled: Long) = Expense(
        id = id, dateEpochDay = day, categoryId = 1L, amountToman = amount,
        settledAmountToman = settled, projectId = projectId, companyApproved = true,
    )

    private val workdays = listOf(
        workday(1L, 10L, "پروژه الف", 20_001L, 500),
        workday(2L, 10L, "پروژه الف", 20_002L, 300),
        workday(3L, 20L, "پروژه ب", 20_002L, 700),
        workday(4L, 10L, "پروژه الف", 20_050L, 900),
    )
    private val expenses = listOf(
        expense(1L, 20_001L, 10L, 1_000_000L, 200_000L),
        expense(2L, 20_002L, 20L, 500_000L, 500_000L),
        expense(3L, 20_060L, null, 250_000L, 0L),
    )

    @Test
    fun dateRangeFiltering_isInclusive() {
        val range = DateRange(20_001L, 20_002L)
        assertEquals(3, PeriodAggregator.filterWorkdays(workdays, range, null).size)
        assertEquals(2, PeriodAggregator.filterExpenses(expenses, range, null).size)
        assertTrue(PeriodAggregator.filterWorkdays(workdays, range, null).none { it.dateEpochDay == 20_050L })
    }

    @Test
    fun projectFiltering_appliesToWorkdaysAndLinkedExpenses() {
        val range = DateRange(20_000L, 20_100L)
        assertEquals(3, PeriodAggregator.filterWorkdays(workdays, range, 10L).size)
        assertEquals(1, PeriodAggregator.filterWorkdays(workdays, range, 20L).size)
        assertEquals(1, PeriodAggregator.filterExpenses(expenses, range, 10L).size)
    }

    @Test
    fun breakdowns_aggregateLengthsOnly() {
        val range = DateRange(20_000L, 20_100L)
        val report = PeriodAggregator.build(range, workdays = workdays, expenses = expenses)
        assertEquals(2400, report.totalLengthMeters)
        assertEquals(4, report.workdayCount)
        assertEquals(3, report.dailyBreakdown.size)
        assertEquals(1000, report.dailyBreakdown.first { it.dateEpochDay == 20_002L }.totalLengthMeters)
        assertEquals(2, report.projectBreakdown.size)
        assertEquals(1700, report.projectBreakdown.first { it.projectId == 10L }.totalLengthMeters)
        assertEquals(700, report.projectBreakdown.first { it.projectId == 20L }.totalLengthMeters)
    }

    @Test
    fun cableBreakdown_groupsByStandardCoreCounts() {
        val cables = ReferenceData.DEFAULT_CABLE_CORES.mapIndexed { i, core ->
            CableType(id = i + 1L, coreCount = core, label = ReferenceData.cableLabel(core))
        }
        fun op(id: Long, length: Int) = ShootingOperation(
            cableTypeId = id, startMeter = 0, endMeter = length, lengthMeters = length,
        )
        val days = listOf(
            workday(1L, 10L, "پروژه الف", 20_001L, 300, listOf(op(1L, 100), op(1L, 200))),
            workday(2L, 10L, "پروژه الف", 20_002L, 500, listOf(op(4L, 500))),
        )
        val range = DateRange(20_001L, 20_002L)
        val report = PeriodAggregator.build(
            range, workdays = days, expenses = expenses, cableTypes = cables,
        )
        val twoCore = report.cableBreakdown.single { it.coreCount == 2 }
        val twelveCore = report.cableBreakdown.single { it.coreCount == 12 }
        assertEquals(300, twoCore.totalLengthMeters)
        assertEquals(2, twoCore.operationCount)
        assertEquals(500, twelveCore.totalLengthMeters)
        assertEquals(800, report.totalLengthMeters)
    }

    @Test
    fun moneyAndLengthAreNeverCombined() {
        val range = DateRange(20_000L, 20_100L)
        val report = PeriodAggregator.build(range, workdays = workdays, expenses = expenses)
        assertEquals(1_750_000L, report.totalExpensesToman)
        assertEquals(700_000L, report.settledToman)
        assertEquals(1_050_000L, report.outstandingToman)
        // Length is a separate Int field, money separate Long fields.
        assertEquals(2400, report.totalLengthMeters)
        assertEquals(3, report.expenseCount)
    }

    @Test
    fun filteredReport_excludesOtherProjects() {
        val report = PeriodAggregator.build(
            DateRange(20_000L, 20_100L), projectFilter = 20L, projectFilterName = "پروژه ب",
            workdays = workdays, expenses = expenses,
        )
        assertEquals(700, report.totalLengthMeters)
        assertEquals(1, report.workdayCount)
        assertEquals(500_000L, report.totalExpensesToman)
        assertEquals("پروژه ب", report.projectFilterName)
    }
}
