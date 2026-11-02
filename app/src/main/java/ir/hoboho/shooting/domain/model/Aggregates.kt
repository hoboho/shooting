package ir.hoboho.shooting.domain.model

import ir.hoboho.shooting.core.date.DateRange

/** One row of the daily breakdown of a period report. */
data class DailyLengthRow(
    val dateEpochDay: Long,
    val totalLengthMeters: Int,
)

/** One row of the project breakdown of a period report. */
data class ProjectLengthRow(
    val projectId: Long,
    val projectName: String,
    val totalLengthMeters: Int,
    val workdayCount: Int,
)

/** Length aggregated by the extensible cable-type (core-count) reference row. */
data class CableLengthRow(
    val cableTypeId: Long,
    val label: String,
    val coreCount: Int,
    val totalLengthMeters: Int,
    val operationCount: Int,
)

/**
 * A period report keeps lengths (meters) and money (Toman) in separate fields:
 * they are different quantities and are never summed into a combined total.
 */
data class PeriodReport(
    val range: DateRange,
    val projectFilter: Long?,
    val projectFilterName: String?,
    val workdayCount: Int,
    val totalLengthMeters: Int,
    val dailyBreakdown: List<DailyLengthRow>,
    val projectBreakdown: List<ProjectLengthRow>,
    val cableBreakdown: List<CableLengthRow> = emptyList(),
    val expenseCount: Int,
    val totalExpensesToman: Long,
    val settledToman: Long,
    val outstandingToman: Long,
)

/** Everything the dashboard needs, already aggregated. */
data class DashboardSnapshot(
    val todayEpochDay: Long,
    val activeProjectCount: Int,
    val recentWorkdays: List<Workday>,
    val recentOperations: List<ShootingOperation>,
    val periodRange: DateRange,
    val periodLengthMeters: Int,
    val outstandingToman: Long,
)
