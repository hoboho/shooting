package ir.hoboho.shooting.domain.report

import ir.hoboho.shooting.core.date.DateRange
import ir.hoboho.shooting.core.format.DateFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportFileNamesTest {
    @Test fun projectFilenameContainsBothSelectedRangeBoundaries() {
        val range = DateRange(20_759L, 20_789L)
        val expected = "project-42-" +
            DateFormatter.format(range.startEpochDay, false).replace('/', '-') + "-" +
            DateFormatter.format(range.endEpochDay, false).replace('/', '-') + ".pdf"
        assertEquals(expected, ReportFileNames.project(42L, range))
    }
}
