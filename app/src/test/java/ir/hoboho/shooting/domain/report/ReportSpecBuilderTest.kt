package ir.hoboho.shooting.domain.report

import ir.hoboho.shooting.core.date.DateRange
import ir.hoboho.shooting.core.pdf.PdfBlock
import ir.hoboho.shooting.domain.PeriodAggregator
import ir.hoboho.shooting.domain.model.CableType
import ir.hoboho.shooting.domain.model.Expense
import ir.hoboho.shooting.domain.model.PointType
import ir.hoboho.shooting.domain.model.ReferenceData
import ir.hoboho.shooting.domain.model.ShootingOperation
import ir.hoboho.shooting.domain.model.Workday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportSpecBuilderTest {

    private val cableTypes = ReferenceData.DEFAULT_CABLE_CORES.mapIndexed { i, core ->
        CableType(id = i + 1L, coreCount = core, label = ReferenceData.cableLabel(core))
    }

    @Test
    fun dailyReport_containsProjectInfoAndTotal() {
        val operations = listOf(
            ShootingOperation(startType = PointType.ODC, startCode = "ODC-1", startMeter = 900,
                endType = PointType.FAT, endCode = "FAT-2", endMeter = 100, cableTypeId = 3L),
        )
        val workday = Workday(
            projectId = 1L, projectName = "پروژه الف", dateEpochDay = 20_759L,
            workArea = "منطقه ۳", employer = "شرکت مخابرات", supervisor = "مهندس رضایی",
            workerCount = 4, notes = "هوای بارانی", operations = operations, totalLengthMeters = 800,
        )
        val spec = ReportSpecBuilder.daily(workday, cableTypes)
        assertEquals(ReportText.TITLE_DAILY, spec.title)
        assertTrue(spec.blocks.filterIsInstance<PdfBlock.KeyValueList>()
            .flatMap { it.entries }.any { it.first == ReportText.DAILY_TOTAL && it.second.contains("۸۰۰") })
        assertTrue(spec.blocks.filterIsInstance<PdfBlock.KeyValueList>()
            .flatMap { it.entries }.any { it.first == ReportText.PROJECT && it.second == "پروژه الف" })
        val table = spec.blocks.filterIsInstance<PdfBlock.Table>().single()
        assertEquals(9, table.header.size)
        assertEquals("FAT", table.rows.single()[4])
        assertEquals("۸۰۰", table.rows.single()[8])
        assertEquals("۲۴ کور", table.rows.single()[7])
    }

    @Test
    fun dailySummary_hasNoOperationCountOrCableBreakdown() {
        val spec = ReportSpecBuilder.daily(
            Workday(projectId = 1L, projectName = "الف", dateEpochDay = 20_759L, totalLengthMeters = 10),
            cableTypes,
        )
        val labels = spec.blocks.filterIsInstance<PdfBlock.KeyValueList>().flatMap { it.entries }.map { it.first }
        assertTrue(ReportText.DAILY_TOTAL in labels)
        assertTrue("تعداد عملیات" !in labels)
        assertTrue("تفکیک نوع کابل" !in labels)
    }

    @Test
    fun periodReport_keepsLengthAndMoneyInSeparateBlocks() {
        val workdays = listOf(
            Workday(projectId = 1L, projectName = "الف", dateEpochDay = 20_759L, totalLengthMeters = 520),
        )
        val expenses = listOf(
            Expense(dateEpochDay = 20_759L, categoryId = 1L, amountToman = 1_000_000L, settledAmountToman = 250_000L),
        )
        val report = PeriodAggregator.build(DateRange(20_759L, 20_759L), workdays = workdays, expenses = expenses)
        val spec = ReportSpecBuilder.period(report)
        assertEquals(ReportText.TITLE_PERIOD, spec.title)
        val tables = spec.blocks.filterIsInstance<PdfBlock.Table>()
        assertEquals(2, tables.size) // daily + project breakdown
        val kvLabels = spec.blocks.filterIsInstance<PdfBlock.KeyValueList>().flatMap { it.entries }
        assertTrue(kvLabels.any { it.first == ReportText.PERIOD_TOTAL && it.second.contains("۵۲۰") })
        assertTrue(kvLabels.any { it.first == ReportText.TOTAL_OUTSTANDING && it.second.contains("تومان") })
    }

    @Test
    fun expenseReport_listsSettlementsAndIntegerToman() {
        val expense = Expense(
            dateEpochDay = 20_759L, categoryId = 1L, categoryName = "سوخت خودرو",
            amountToman = 1_000_000L, settledAmountToman = 400_000L, description = "بنزین",
        )
        val rows = listOf(ExpenseReportRow(expense, listOf(
            ir.hoboho.shooting.domain.model.Settlement(
                expenseId = 1L, amountToman = 400_000L, dateEpochDay = 20_760L, note = "واریز",
            ),
        )))
        val spec = ReportSpecBuilder.expenses(rows, DateRange(20_759L, 20_760L))
        assertEquals(ReportText.TITLE_EXPENSES, spec.title)
        val tables = spec.blocks.filterIsInstance<PdfBlock.Table>()
        assertEquals(2, tables.size)
        assertEquals("۱٬۰۰۰٬۰۰۰", tables[0].rows.single()[5])
        assertEquals("۶۰۰٬۰۰۰", tables[0].rows.single()[7])
        assertEquals(ReportText.PARTIAL, tables[0].rows.single()[8])
        assertEquals("۴۰۰٬۰۰۰", tables[1].rows.single()[2])
    }
}
