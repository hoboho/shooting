package ir.hoboho.shooting.domain

import ir.hoboho.shooting.core.date.DateRange
import ir.hoboho.shooting.core.format.NumberFormatter
import ir.hoboho.shooting.domain.model.CableLengthRow
import ir.hoboho.shooting.domain.model.CableType
import ir.hoboho.shooting.domain.model.DailyLengthRow
import ir.hoboho.shooting.domain.model.Expense
import ir.hoboho.shooting.domain.model.PeriodReport
import ir.hoboho.shooting.domain.model.ProjectLengthRow
import ir.hoboho.shooting.domain.model.Workday

/** Pure aggregation used by reports, the dashboard and the PDF builders. */
object PeriodAggregator {

    fun filterWorkdays(workdays: List<Workday>, range: DateRange, projectId: Long?): List<Workday> =
        workdays.filter { workday ->
            workday.dateEpochDay in range && (projectId == null || workday.projectId == projectId)
        }

    /** Project filter applies to expenses that carry a project association. */
    fun filterExpenses(expenses: List<Expense>, range: DateRange, projectId: Long?): List<Expense> =
        expenses.filter { expense ->
            expense.dateEpochDay in range && (projectId == null || expense.projectId == projectId)
        }

    fun dailyBreakdown(workdays: List<Workday>): List<DailyLengthRow> =
        workdays.groupBy { it.dateEpochDay }
            .map { (day, rows) ->
                DailyLengthRow(day, ShootingCalculator.sumLengths(rows.map { it.totalLengthMeters }))
            }
            .sortedBy { it.dateEpochDay }

    fun projectBreakdown(workdays: List<Workday>): List<ProjectLengthRow> =
        workdays.groupBy { it.projectId }
            .map { (projectId, rows) ->
                ProjectLengthRow(
                    projectId = projectId,
                    projectName = rows.first().projectName,
                    totalLengthMeters = ShootingCalculator.sumLengths(rows.map { it.totalLengthMeters }),
                    workdayCount = rows.size,
                )
            }
            .sortedByDescending { it.totalLengthMeters }

    /** Length by cable core count; unknown legacy ids still get a readable row. */
    fun cableBreakdown(
        workdays: List<Workday>,
        cableTypes: List<CableType> = emptyList(),
    ): List<CableLengthRow> {
        val labels = cableTypes.associateBy { it.id }
        return workdays.flatMap { it.operations }
            .groupBy { it.cableTypeId }
            .map { (id, operations) ->
                val type = labels[id]
                CableLengthRow(
                    cableTypeId = id,
                    label = type?.label
                        ?: "نوع کابل ${NumberFormatter.formatInteger(id.toInt())}",
                    coreCount = type?.coreCount ?: 0,
                    totalLengthMeters = ShootingCalculator.sumLengths(
                        operations.map { it.lengthMeters },
                    ),
                    operationCount = operations.size,
                )
            }
            .sortedByDescending { it.totalLengthMeters }
    }

    fun build(
        range: DateRange,
        projectFilter: Long? = null,
        projectFilterName: String? = null,
        workdays: List<Workday>,
        expenses: List<Expense>,
        cableTypes: List<CableType> = emptyList(),
    ): PeriodReport {
        val filteredWorkdays = filterWorkdays(workdays, range, projectFilter)
        val filteredExpenses = filterExpenses(expenses, range, projectFilter)
        val overview = ExpenseCalculator.overview(filteredExpenses)
        return PeriodReport(
            range = range,
            projectFilter = projectFilter,
            projectFilterName = projectFilterName,
            workdayCount = filteredWorkdays.size,
            totalLengthMeters = ShootingCalculator.sumLengths(
                filteredWorkdays.map { it.totalLengthMeters },
            ),
            dailyBreakdown = dailyBreakdown(filteredWorkdays),
            projectBreakdown = projectBreakdown(filteredWorkdays),
            cableBreakdown = cableBreakdown(filteredWorkdays, cableTypes),
            expenseCount = filteredExpenses.size,
            totalExpensesToman = overview.totalExpensesToman,
            settledToman = overview.totalReimbursedToman,
            outstandingToman = overview.outstandingToman,
        )
    }
}
