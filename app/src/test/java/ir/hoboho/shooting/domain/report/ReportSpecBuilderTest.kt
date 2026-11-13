package ir.hoboho.shooting.domain.report

import ir.hoboho.shooting.core.date.DateRange
import ir.hoboho.shooting.core.pdf.PdfBlock
import ir.hoboho.shooting.domain.PeriodAggregator
import ir.hoboho.shooting.domain.model.CableType
import ir.hoboho.shooting.domain.model.Expense
import ir.hoboho.shooting.domain.model.PointType
import ir.hoboho.shooting.domain.model.Project
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

    private fun operation(cableId: Long, length: Int) = ShootingOperation(
        startType = PointType.ODC,
        startCode = "ODC-1",
        startMeter = length + 100,
        endType = PointType.FAT,
        endCode = "FAT-2",
        endMeter = 100,
        cableTypeId = cableId,
        lengthMeters = length,
    )

    @Test
    fun pointTypeLabels_useShortReportNames() {
        assertEquals("HH", PointType.HANDHOLE.label)
        assertEquals("MH", PointType.MANHOLE.label)
    }

    @Test
    fun dailyReport_containsProjectInfoTotalAndCableBreakdown() {
        val workday = Workday(
            projectId = 1L, projectName = "پروژه الف", dateEpochDay = 20_759L,
            workArea = "منطقه ۳", employer = "شرکت مخابرات", supervisor = "مهندس رضایی",
            workerCount = 4, notes = "هوای بارانی",
            operations = listOf(operation(cableId = 3L, length = 800)),
            totalLengthMeters = 800,
        )
        val spec = ReportSpecBuilder.daily(workday, cableTypes)
        assertEquals(ReportText.TITLE_DAILY, spec.title)
        assertTrue(spec.blocks.filterIsInstance<PdfBlock.KeyValueList>()
            .flatMap { it.entries }.any { it.first == ReportText.DAILY_TOTAL && it.second.contains("۸۰۰") })
        assertTrue(spec.blocks.filterIsInstance<PdfBlock.KeyValueList>()
            .flatMap { it.entries }.any { it.first == ReportText.PROJECT && it.second == "پروژه الف" })

        val tables = spec.blocks.filterIsInstance<PdfBlock.Table>()
        assertTrue(tables.any { it.caption == ReportText.CABLE_BREAKDOWN && it.rows.single()[3] == "۸۰۰" })
        val operationsTable = tables.single { it.header.size == 7 }
        assertEquals(listOf(
            ReportText.ROW, ReportText.START_POINT, ReportText.START_METER,
            ReportText.END_POINT, ReportText.END_METER, ReportText.CABLE, ReportText.LENGTH,
        ), operationsTable.header)
        assertEquals("ODC_ODC-1", operationsTable.rows.single()[1])
        assertEquals("FAT_FAT-2", operationsTable.rows.single()[3])
        assertEquals("۸۰۰", operationsTable.rows.single()[6])
        assertEquals("۶ کور", operationsTable.rows.single()[5])
    }

    @Test
    fun dailySummary_keepsTotalAndAddsSeparateCableBreakdown() {
        val spec = ReportSpecBuilder.daily(
            Workday(projectId = 1L, projectName = "الف", dateEpochDay = 20_759L, totalLengthMeters = 10),
            cableTypes,
        )
        val labels = spec.blocks.filterIsInstance<PdfBlock.KeyValueList>().flatMap { it.entries }.map { it.first }
        assertTrue(ReportText.DAILY_TOTAL in labels)
        assertTrue(ReportText.OPERATION_COUNT !in labels)
        assertTrue(ReportText.CABLE_BREAKDOWN !in labels)
        assertTrue(spec.blocks.filterIsInstance<PdfBlock.Table>()
            .none { it.caption == ReportText.CABLE_BREAKDOWN })
    }

    @Test
    fun projectOperations_mergesPointAndCodeAndOmitsRowColumn() {
        val workday = Workday(
            projectId = 1L,
            projectName = "پروژه الف",
            dateEpochDay = 20_759L,
            operations = listOf(
                operation(cableId = 3L, length = 800).copy(
                    startType = PointType.HANDHOLE,
                    startCode = "123",
                    endType = PointType.MANHOLE,
                    endCode = "456",
                ),
            ),
            totalLengthMeters = 800,
        )
        val spec = ReportSpecBuilder.projectOperations(
            workdays = listOf(workday),
            project = Project(name = "پروژه الف"),
            cableTypes = cableTypes,
            range = DateRange(20_759L, 20_759L),
        )
        val table = spec.blocks.filterIsInstance<PdfBlock.Table>().single()
        assertEquals(listOf(
            ReportText.DATE, ReportText.START_POINT, ReportText.START_METER,
            ReportText.END_POINT, ReportText.END_METER, ReportText.CABLE, ReportText.LENGTH,
        ), table.header)
        assertTrue(ReportText.ROW !in table.header)
        assertEquals("HH_123", table.rows.single()[1])
        assertEquals("MH_456", table.rows.single()[3])
        assertEquals(7, table.rows.single().size)
    }

    @Test
    fun periodReport_keepsLengthAndMoneyInSeparateBlocks() {
        val workdays = listOf(
            Workday(
                projectId = 1L, projectName = "الف", dateEpochDay = 20_759L,
                operations = listOf(operation(cableId = 3L, length = 520)),
                totalLengthMeters = 520,
            ),
        )
        val expenses = listOf(
            Expense(dateEpochDay = 20_759L, categoryId = 1L, amountToman = 1_000_000L, settledAmountToman = 250_000L, companyApproved = true),
        )
        val report = PeriodAggregator.build(
            DateRange(20_759L, 20_759L),
            workdays = workdays,
            expenses = expenses,
            cableTypes = cableTypes,
        )
        val spec = ReportSpecBuilder.period(report)
        assertEquals(ReportText.TITLE_PERIOD, spec.title)
        val tables = spec.blocks.filterIsInstance<PdfBlock.Table>()
        assertEquals(3, tables.size) // daily + project + cable breakdown
        assertTrue(tables.any { it.caption == ReportText.CABLE_BREAKDOWN && it.rows.single()[3] == "۵۲۰" })
        val kvLabels = spec.blocks.filterIsInstance<PdfBlock.KeyValueList>().flatMap { it.entries }
        assertTrue(kvLabels.any { it.first == ReportText.PERIOD_TOTAL && it.second.contains("۵۲۰") })
        assertTrue(kvLabels.any { it.first == ReportText.TOTAL_OUTSTANDING && it.second.contains("تومان") })
    }

    @Test
    fun expenseReport_listsSettlementsAndIntegerToman() {
        val expense = Expense(
            dateEpochDay = 20_759L, categoryId = 1L, categoryName = "سوخت خودرو",
            amountToman = 1_000_000L, settledAmountToman = 400_000L, description = "بنزین",
            companyApproved = true,
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
