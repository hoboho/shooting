package ir.hoboho.shooting.domain

import ir.hoboho.shooting.core.date.DateRange
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

    fun build(
        range: DateRange,
        projectFilter: Long? = null,
        projectFilterName: String? = null,
        workdays: List<Workday>,
        expenses: List<Expense>,
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
            expenseCount = filteredExpenses.size,
            totalExpensesToman = overview.totalExpensesToman,
            settledToman = overview.totalReimbursedToman,
            outstandingToman = overview.outstandingToman,
        )
    }
}
