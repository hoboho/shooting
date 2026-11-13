package ir.hoboho.shooting.domain.report

import ir.hoboho.shooting.core.date.DateRange
import ir.hoboho.shooting.core.format.DateFormatter
import ir.hoboho.shooting.core.format.NumberFormatter
import ir.hoboho.shooting.core.pdf.PdfAlign
import ir.hoboho.shooting.core.pdf.PdfBlock
import ir.hoboho.shooting.core.pdf.PdfColumn
import ir.hoboho.shooting.core.pdf.PdfDocumentSpec
import ir.hoboho.shooting.core.pdf.PdfTextStyle
import ir.hoboho.shooting.domain.model.CableLengthRow
import ir.hoboho.shooting.domain.model.CableType
import ir.hoboho.shooting.domain.model.Expense
import ir.hoboho.shooting.domain.model.PeriodReport
import ir.hoboho.shooting.domain.model.Project
import ir.hoboho.shooting.domain.model.Settlement
import ir.hoboho.shooting.domain.model.SettlementStatus
import ir.hoboho.shooting.domain.model.ShootingOperation
import ir.hoboho.shooting.domain.model.Workday

/** One expense with its settlement lines, for the expense PDF. */
data class ExpenseReportRow(
    val expense: Expense,
    val settlements: List<Settlement> = emptyList(),
)

/**
 * Builds PDF document specs from domain data. Pure Kotlin: every number is
 * formatted here with Persian digits, Jalali dates and Toman, so the Android PDF
 * engine only has to *draw* text, never interpret business values.
 */
object ReportSpecBuilder {

    private fun header() = listOf("${ReportText.BRAND} | ${ReportText.BRAND_LATIN}", ReportText.SUBTITLE)

    private fun persianInt(value: Int) = NumberFormatter.formatInteger(value)
    private fun persianLong(value: Long) = NumberFormatter.formatPersian(value)
    private fun meters(value: Int) = NumberFormatter.formatMeters(value)
    private fun toman(value: Long) = NumberFormatter.formatToman(value)

    private data class CableGroup(
        val id: Long,
        val operations: List<ShootingOperation>,
        val totalMeters: Int,
    )

    /** Rows for a daily report's cable-type (core-count) breakdown. */
    private fun cableRowsFromOperations(
        operations: List<ShootingOperation>,
        cableTypes: List<CableType>,
    ): List<List<String>> {
        val labels = cableTypes.associateBy { it.id }
        return operations.groupBy { it.cableTypeId }
            .map { (id, ops) -> CableGroup(id, ops, ops.sumOf { it.lengthMeters }) }
            .sortedByDescending { it.totalMeters }
            .map { group ->
                val type = labels[group.id]
                listOf(
                    type?.label ?: persianInt(group.id.toInt()),
                    type?.coreCount?.let { persianInt(it) } ?: NumberFormatter.EM_DASH,
                    persianInt(group.operations.size),
                    NumberFormatter.formatMetersValue(group.totalMeters),
                )
            }
    }

    /** Rows for a period report's pre-aggregated cable breakdown. */
    private fun cableRowsFromReport(breakdown: List<CableLengthRow>): List<List<String>> =
        breakdown.map { row ->
            listOf(
                row.label,
                if (row.coreCount > 0) persianInt(row.coreCount) else NumberFormatter.EM_DASH,
                persianInt(row.operationCount),
                NumberFormatter.formatMetersValue(row.totalLengthMeters),
            )
        }

    private fun cableTable(rows: List<List<String>>): PdfBlock.Table = PdfBlock.Table(
        columns = listOf(
            PdfColumn(ReportText.CABLE, 1.6f),
            PdfColumn(ReportText.CORE_COUNT, 1f),
            PdfColumn(ReportText.OPERATION_COUNT, 1f),
            PdfColumn(ReportText.LENGTH, 1f),
        ),
        header = listOf(
            ReportText.CABLE,
            ReportText.CORE_COUNT,
            ReportText.OPERATION_COUNT,
            ReportText.LENGTH,
        ),
        rows = rows,
        caption = ReportText.CABLE_BREAKDOWN,
        aligns = listOf(PdfAlign.START, PdfAlign.CENTER, PdfAlign.CENTER, PdfAlign.CENTER),
    )

    // ------------------------------------------------------------- daily report

    fun daily(workday: Workday, cableTypes: List<CableType>): PdfDocumentSpec {
        val labels = cableTypes.associateBy { it.id }
        val rows = workday.operations.mapIndexed { index, op ->
            listOf(
                persianInt(index + 1),
                "${op.startType.label}_${op.startCode}",
                persianInt(op.startMeter),
                "${op.endType.label}_${op.endCode}",
                persianInt(op.endMeter),
                labels[op.cableTypeId]?.label ?: persianInt(op.cableTypeId.toInt()),
                NumberFormatter.formatMetersValue(op.lengthMeters),
            )
        }
        val cableRows = cableRowsFromOperations(workday.operations, cableTypes)
        val info = listOf(
            ReportText.PROJECT to workday.projectName,
            ReportText.DATE to DateFormatter.formatLong(workday.dateEpochDay),
            ReportText.WORK_AREA to workday.workArea,
            ReportText.EMPLOYER to workday.employer,
            ReportText.SUPERVISOR to workday.supervisor,
            ReportText.WORKER_COUNT to persianInt(workday.workerCount),
            ReportText.NOTES to workday.notes.ifBlank { NumberFormatter.EM_DASH },
        )
        return PdfDocumentSpec(
            title = ReportText.TITLE_DAILY,
            subtitle = "${workday.projectName} — ${DateFormatter.format(workday.dateEpochDay)}",
            headerLines = header(),
            blocks = buildList {
                add(PdfBlock.KeyValueList(info))
                add(PdfBlock.Divider())
                // The daily summary still focuses on the total length; cable
                // breakdown is a separate, explicitly labelled table.
                add(
                    PdfBlock.KeyValueList(
                        listOf(ReportText.DAILY_TOTAL to meters(workday.totalLengthMeters)),
                        emphasisedLabels = setOf(ReportText.DAILY_TOTAL),
                    ),
                )
                if (cableRows.isNotEmpty()) add(cableTable(cableRows))
                add(PdfBlock.Paragraph(ReportText.OPERATIONS, style = PdfTextStyle.SectionHeader))
                add(
                    PdfBlock.Table(
                        columns = listOf(
                            PdfColumn(ReportText.ROW, 0.5f, 26f),
                            PdfColumn(ReportText.START_POINT, 1.5f),
                            PdfColumn(ReportText.START_METER, 1f),
                            PdfColumn(ReportText.END_POINT, 1.5f),
                            PdfColumn(ReportText.END_METER, 1f),
                            PdfColumn(ReportText.CABLE, 1f),
                            PdfColumn(ReportText.LENGTH, 1f),
                        ),
                        header = listOf(
                            ReportText.ROW, ReportText.START_POINT, ReportText.START_METER,
                            ReportText.END_POINT, ReportText.END_METER, ReportText.CABLE, ReportText.LENGTH,
                        ),
                        rows = rows,
                        aligns = List(7) { PdfAlign.CENTER },
                    ),
                )
                add(PdfBlock.Paragraph(ReportText.LENGTH_NOTE, style = PdfTextStyle.Small))
            },
        )
    }

    // ------------------------------------------------------------ period report

    fun period(report: PeriodReport): PdfDocumentSpec {
        val dailyRows = report.dailyBreakdown.map { row ->
            listOf(DateFormatter.format(row.dateEpochDay), NumberFormatter.formatMetersValue(row.totalLengthMeters))
        }
        val projectRows = report.projectBreakdown.map { row ->
            listOf(row.projectName, NumberFormatter.formatMetersValue(row.totalLengthMeters))
        }
        val cableRows = cableRowsFromReport(report.cableBreakdown)
        return PdfDocumentSpec(
            title = ReportText.TITLE_PERIOD,
            subtitle = "${DateFormatter.formatRange(report.range)} — ${report.projectFilterName ?: ReportText.ALL_PROJECTS}",
            headerLines = header(),
            blocks = buildList {
                add(
                    PdfBlock.KeyValueList(
                        listOf(
                            ReportText.RANGE to DateFormatter.formatRange(report.range),
                            ReportText.PROJECT to (report.projectFilterName ?: ReportText.ALL_PROJECTS),
                            ReportText.PERIOD_TOTAL to meters(report.totalLengthMeters),
                        ),
                        emphasisedLabels = setOf(ReportText.PERIOD_TOTAL),
                    ),
                )
                add(
                    PdfBlock.Table(
                        columns = listOf(PdfColumn(ReportText.DATE, 1f), PdfColumn(ReportText.LENGTH, 1f)),
                        header = listOf(ReportText.DATE, ReportText.LENGTH),
                        rows = dailyRows,
                        caption = ReportText.DAILY_BREAKDOWN,
                        aligns = listOf(PdfAlign.START, PdfAlign.CENTER),
                    ),
                )
                add(
                    PdfBlock.Table(
                        columns = listOf(PdfColumn(ReportText.PROJECT, 1.6f), PdfColumn(ReportText.LENGTH, 1f)),
                        header = listOf(ReportText.PROJECT, ReportText.LENGTH),
                        rows = projectRows,
                        caption = ReportText.PROJECT_BREAKDOWN,
                        aligns = listOf(PdfAlign.START, PdfAlign.CENTER),
                    ),
                )
                if (cableRows.isNotEmpty()) add(cableTable(cableRows))
                add(PdfBlock.Paragraph(ReportText.EXPENSE_SUMMARY, style = PdfTextStyle.SectionHeader))
                // Money is a separate block, never combined with length values.
                add(
                    PdfBlock.KeyValueList(
                        listOf(
                            ReportText.TOTAL_EXPENSES to toman(report.totalExpensesToman),
                            ReportText.TOTAL_SETTLED to toman(report.settledToman),
                            ReportText.TOTAL_OUTSTANDING to toman(report.outstandingToman),
                        ),
                        emphasisedLabels = setOf(ReportText.TOTAL_OUTSTANDING),
                    ),
                )
                add(PdfBlock.Paragraph(ReportText.LENGTH_NOTE, style = PdfTextStyle.Small))
            },
        )
    }

    /** Detailed project-wide operation ledger; each operation carries its own work date. */
    fun projectOperations(
        workdays: List<Workday>,
        project: Project,
        cableTypes: List<CableType>,
        range: DateRange,
    ): PdfDocumentSpec {
        val cableLabels = cableTypes.associateBy { it.id }
        val rows = workdays.sortedBy { it.dateEpochDay }.flatMap { workday ->
            workday.operations.mapIndexed { index, operation ->
                listOf(
                    DateFormatter.format(workday.dateEpochDay),
                    "${operation.startType.label}_${operation.startCode}",
                    persianInt(operation.startMeter),
                    "${operation.endType.label}_${operation.endCode}",
                    persianInt(operation.endMeter),
                    cableLabels[operation.cableTypeId]?.label ?: persianInt(operation.cableTypeId.toInt()),
                    NumberFormatter.formatMetersValue(operation.lengthMeters),
                )
            }
        }
        val first = workdays.minByOrNull { it.dateEpochDay }
        return PdfDocumentSpec(
            title = "ریز کارکرد پروژه",
            subtitle = "${project.name} — ${DateFormatter.formatRange(range)}",
            headerLines = header(),
            blocks = buildList {
                add(PdfBlock.KeyValueList(listOf(
                    ReportText.PROJECT to project.name,
                    ReportText.WORK_AREA to (first?.workArea?.takeIf { it.isNotBlank() } ?: NumberFormatter.EM_DASH),
                    ReportText.EMPLOYER to (project.employer.ifBlank { first?.employer ?: NumberFormatter.EM_DASH }),
                    ReportText.SUPERVISOR to (project.supervisor.ifBlank { first?.supervisor ?: NumberFormatter.EM_DASH }),
                    ReportText.RANGE to DateFormatter.formatRange(range),
                )))
                add(PdfBlock.Table(
                    columns = listOf(
                        PdfColumn(ReportText.DATE, 1.1f),
                        PdfColumn(ReportText.START_POINT, 1.25f),
                        PdfColumn(ReportText.START_METER, 0.7f),
                        PdfColumn(ReportText.END_POINT, 1.25f),
                        PdfColumn(ReportText.END_METER, 0.7f),
                        PdfColumn(ReportText.CABLE, 0.9f),
                        PdfColumn(ReportText.LENGTH, 0.8f),
                    ),
                    header = listOf(ReportText.DATE, ReportText.START_POINT, ReportText.START_METER, ReportText.END_POINT, ReportText.END_METER, ReportText.CABLE, ReportText.LENGTH),
                    rows = rows,
                    caption = ReportText.OPERATIONS,
                    aligns = List(7) { PdfAlign.CENTER },
                ))
                add(PdfBlock.Paragraph(ReportText.LENGTH_NOTE, style = PdfTextStyle.Small))
            },
        )
    }

    // ----------------------------------------------------------- expense report

    fun expenses(rows: List<ExpenseReportRow>, range: DateRange): PdfDocumentSpec {
        val expenseRows = rows.mapIndexed { index, row ->
            val e = row.expense
            listOf(
                persianInt(index + 1),
                DateFormatter.format(e.dateEpochDay),
                e.categoryName,
                e.description.ifBlank { NumberFormatter.EM_DASH },
                e.projectName ?: NumberFormatter.EM_DASH,
                persianLong(e.amountToman),
                persianLong(e.settledAmountToman),
                persianLong(e.outstandingToman),
                statusLabel(e.status),
            )
        }
        val settlementRows = rows.flatMap { row ->
            row.settlements.map { s ->
                listOf(
                    DateFormatter.format(s.dateEpochDay),
                    row.expense.description.ifBlank { row.expense.categoryName },
                    persianLong(s.amountToman),
                    s.note.ifBlank { NumberFormatter.EM_DASH },
                )
            }
        }
        val total = rows.sumOf { it.expense.amountToman }
        val settled = rows.sumOf { it.expense.settledAmountToman }
        val outstanding = rows.sumOf { it.expense.outstandingToman }
        return PdfDocumentSpec(
            title = ReportText.TITLE_EXPENSES,
            subtitle = DateFormatter.formatRange(range),
            headerLines = header(),
            blocks = listOf(
                PdfBlock.KeyValueList(
                    listOf(
                        ReportText.TOTAL_EXPENSES to toman(total),
                        ReportText.TOTAL_SETTLED to toman(settled),
                        ReportText.TOTAL_OUTSTANDING to toman(outstanding),
                    ),
                    emphasisedLabels = setOf(ReportText.TOTAL_OUTSTANDING),
                ),
                PdfBlock.Table(
                    columns = listOf(
                        PdfColumn(ReportText.ROW, 0.5f, 26f),
                        PdfColumn(ReportText.DATE, 1f),
                        PdfColumn(ReportText.CATEGORY, 1.2f),
                        PdfColumn(ReportText.DESCRIPTION, 1.5f),
                        PdfColumn(ReportText.PROJECT, 1.1f),
                        PdfColumn(ReportText.AMOUNT, 1.1f),
                        PdfColumn(ReportText.REIMBURSED, 1.1f),
                        PdfColumn(ReportText.OUTSTANDING, 1.1f),
                        PdfColumn(ReportText.STATUS, 1f),
                    ),
                    header = listOf(
                        ReportText.ROW, ReportText.DATE, ReportText.CATEGORY,
                        ReportText.DESCRIPTION, ReportText.PROJECT, ReportText.AMOUNT,
                        ReportText.REIMBURSED, ReportText.OUTSTANDING, ReportText.STATUS,
                    ),
                    rows = expenseRows,
                    caption = ReportText.EXPENSE_LIST,
                    aligns = List(9) { PdfAlign.CENTER },
                ),
                PdfBlock.Table(
                    columns = listOf(
                        PdfColumn(ReportText.DATE, 1f),
                        PdfColumn(ReportText.DESCRIPTION, 2f),
                        PdfColumn(ReportText.AMOUNT, 1f),
                        PdfColumn(ReportText.NOTES, 1.5f),
                    ),
                    header = listOf(ReportText.DATE, ReportText.DESCRIPTION, ReportText.AMOUNT, ReportText.NOTES),
                    rows = settlementRows,
                    caption = ReportText.SETTLEMENT_LIST,
                    aligns = listOf(PdfAlign.CENTER, PdfAlign.START, PdfAlign.CENTER, PdfAlign.START),
                ),
                PdfBlock.Paragraph(ReportText.LENGTH_NOTE, style = PdfTextStyle.Small),
            ),
        )
    }

    fun statusLabel(status: SettlementStatus): String = when (status) {
        SettlementStatus.UNSETTLED -> ReportText.UNSETTLED
        SettlementStatus.PARTIAL -> ReportText.PARTIAL
        SettlementStatus.SETTLED -> ReportText.SETTLED
    }
}
