package ir.hoboho.shooting.domain

import ir.hoboho.shooting.domain.model.OperationDraft
import ir.hoboho.shooting.domain.model.Workday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorTest {

    @Test
    fun project_requiresNameAndUniqueName() {
        assertTrue(Validator.project("", false).any { it.field == ValidationField.PROJECT_NAME })
        assertTrue(Validator.project("پروژه الف", true).any { it.field == ValidationField.PROJECT_DUPLICATE })
        assertTrue(Validator.project("پروژه الف", false).isEmpty())
    }

    @Test
    fun workday_preventsDuplicateForSameProjectAndDate() {
        val existing = listOf(
            Workday(id = 1L, projectId = 7L, dateEpochDay = 20_100L),
            Workday(id = 2L, projectId = 8L, dateEpochDay = 20_100L),
        )
        assertTrue(Validator.isDuplicateWorkday(existing, 7L, 20_100L))
        assertTrue(Validator.isDuplicateWorkday(existing, 7L, 20_100L, excludeId = 0L))
        assertFalse(Validator.isDuplicateWorkday(existing, 7L, 20_100L, excludeId = 1L))
        assertFalse(Validator.isDuplicateWorkday(existing, 9L, 20_100L))
        assertFalse(Validator.isDuplicateWorkday(existing, 7L, 20_101L))
        assertTrue(Validator.workday(
            projectId = 7L, projectExists = true, dateEpochDay = 20_100L,
            workerCount = 0, operationCount = 2, duplicateExists = true,
        ).any { it.field == ValidationField.WORKDAY_DUPLICATE })
    }

    @Test
    fun workday_requiresProjectDateAndOperations() {
        val issues = Validator.workday(0L, false, null, -1, 0, false)
        assertEquals(4, issues.size)
        assertTrue(issues.any { it.field == ValidationField.WORKDAY_PROJECT })
        assertTrue(issues.any { it.field == ValidationField.WORKDAY_DATE })
        assertTrue(issues.any { it.field == ValidationField.WORKDAY_NO_OPERATIONS })
        assertTrue(issues.any { it.field == ValidationField.WORKER_COUNT })
    }

    @Test
    fun invalidOperations_areReportedPerRow() {
        val drafts = listOf(
            OperationDraft(key = 10L, startMeter = "", endMeter = "500", cableTypeId = 1L),
            OperationDraft(key = 11L, startMeter = "100", endMeter = "500", cableTypeId = 999L),
        )
        val issues = Validator.operations(drafts, setOf(1L))
        assertTrue(issues.any { it.rowKey == 10L && it.field == ValidationField.START_METER })
        assertTrue(issues.any { it.rowKey == 10L && it.field == ValidationField.START_CODE })
        assertTrue(issues.any { it.rowKey == 11L && it.field == ValidationField.CABLE_TYPE })
    }

    @Test
    fun reverseMeterOrder_isNotAnError() {
        val draft = OperationDraft(
            key = 3L, startCode = "ODC", endCode = "FAT",
            startMeter = "980", endMeter = "120", cableTypeId = 1L,
        )
        assertTrue(Validator.operation(draft, setOf(1L)).isEmpty())
        assertEquals(860, draft.toOperation(1L, 0L)!!.lengthMeters)
    }

    @Test
    fun workerCountDefaultsToZero() {
        val workday = Workday(projectId = 1L, dateEpochDay = 20_000L)
        assertEquals(0, workday.workerCount)
    }
}
