package ir.hoboho.shooting.domain.report

import ir.hoboho.shooting.core.date.DateRange
import ir.hoboho.shooting.core.format.DateFormatter

/** Deterministic, range-aware names for exported reports. */
object ReportFileNames {
    fun project(projectId: Long, range: DateRange): String {
        val start = DateFormatter.format(range.startEpochDay, false).replace('/', '-')
        val end = DateFormatter.format(range.endEpochDay, false).replace('/', '-')
        return "project-$projectId-$start-$end.pdf"
    }
}
