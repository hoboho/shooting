package ir.hoboho.shooting.domain

import ir.hoboho.shooting.domain.model.PointType
import ir.hoboho.shooting.domain.model.ShootingOperation
import ir.hoboho.shooting.domain.model.Workday
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OperationSearchTest {
    private val workday = Workday(
        projectId = 1L,
        projectName = "اراک",
        dateEpochDay = 20_759L,
        workArea = "بهار و رازی",
        notes = "یادداشت غیرمرتبط",
        operations = listOf(
            ShootingOperation(
                startType = PointType.ODC,
                startCode = "60903",
                startMeter = 2_758,
                endType = PointType.FAT,
                endCode = "6090303",
                endMeter = 2_578,
                cableTypeId = 1L,
                lengthMeters = 180,
            ),
        ),
    )

    @Test fun findsStartOrEndPointCodes() {
        assertTrue(workday.matchesOperationSearch("60903"))
        assertTrue(workday.matchesOperationSearch("6090303"))
    }

    @Test fun findsMeterValuesUsingPersianOrLatinDigits() {
        assertTrue(workday.matchesOperationSearch("۲۵۷۸"))
        assertTrue(workday.matchesOperationSearch("180"))
        assertTrue(workday.matchesOperationSearch("275"))
    }

    @Test fun doesNotSearchProjectMetadataNotesOrDate() {
        assertFalse(workday.matchesOperationSearch("اراک"))
        assertFalse(workday.matchesOperationSearch("بهار"))
        assertFalse(workday.matchesOperationSearch("یادداشت"))
        assertFalse(workday.matchesOperationSearch("۱۴۰۵"))
    }

    @Test fun blankQueryKeepsAllWorkdays() {
        assertTrue(workday.matchesOperationSearch(""))
        assertTrue(workday.matchesOperationSearch("   "))
    }
}
