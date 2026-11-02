package ir.hoboho.shooting.domain

import ir.hoboho.shooting.domain.model.PointType
import ir.hoboho.shooting.domain.model.ShootingOperation
import ir.hoboho.shooting.domain.model.Workday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ShootingCalculatorTest {

    @Test
    fun length_isAbsoluteDifference() {
        assertEquals(520, ShootingCalculator.lengthMeters(100, 620))
        assertEquals(520, ShootingCalculator.lengthMeters(620, 100))
        assertEquals(0, ShootingCalculator.lengthMeters(250, 250))
    }

    @Test
    fun negativeRawDifference_isValid_notAValidationError() {
        // A reverse cable-reel reading produces a negative raw difference.
        val raw = 120 - 980
        assertTrue(raw < 0)
        val operation = ShootingOperation(
            startMeter = 980, endMeter = 120, startType = PointType.ODC,
            endType = PointType.FAT, startCode = "ODC-1", endCode = "FAT-9",
        )
        assertEquals(860, operation.lengthMeters)
        assertEquals(860, ShootingCalculator.recompute(operation).lengthMeters)
        assertTrue(Validator.operations(
            listOf(ir.hoboho.shooting.domain.model.OperationDraft(
                key = 1L, startMeter = "980", endMeter = "120",
                startCode = "ODC-1", endCode = "FAT-9", cableTypeId = 1L,
            )),
            setOf(1L),
        ).isEmpty())
    }

    @Test
    fun length_updatesWhenEitherMeterChanges() {
        var draft = ir.hoboho.shooting.domain.model.OperationDraft(
            key = 1L, startMeter = "100", endMeter = "620", cableTypeId = 1L,
            startCode = "ODC-1", endCode = "FAT-2",
        )
        assertEquals(520, draft.lengthMeters)
        draft = draft.copy(startMeter = "700")
        assertEquals(80, draft.lengthMeters)
        draft = draft.copy(endMeter = "100")
        assertEquals(600, draft.lengthMeters)
        assertEquals(600, draft.toOperation(9L, 1234L)!!.lengthMeters)
    }

    @Test
    fun dailyTotal_isSumOfOperationLengths() {
        val operations = listOf(
            ShootingOperation(startMeter = 10, endMeter = 200),           // 190
            ShootingOperation(startMeter = 900, endMeter = 300),           // 600 (negative raw)
            ShootingOperation(startMeter = 0, endMeter = 0),               // 0
        ).map(ShootingCalculator::recompute)
        assertEquals(790, ShootingCalculator.dailyTotal(operations))
        assertEquals(790, ShootingCalculator.sumLengths(operations.map { it.lengthMeters }))
        val workday = Workday(projectId = 1L, dateEpochDay = 20_000L, operations = operations, totalLengthMeters = 790)
        assertTrue(ShootingCalculator.isTotalConsistent(workday))
        assertFalse(ShootingCalculator.isTotalConsistent(workday.copy(totalLengthMeters = 789)))
    }

    @Test
    fun hugeReadings_neverOverflow() {
        assertEquals(Int.MAX_VALUE, ShootingCalculator.lengthMeters(0, Int.MAX_VALUE))
        assertEquals(Int.MAX_VALUE - 7, ShootingCalculator.lengthMeters(Int.MAX_VALUE, 7))
    }

    @Test
    fun negativeMeterReading_isRejected() {
        try {
            ShootingCalculator.lengthMeters(-1, 10)
            fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("non-negative"))
        }
    }

    @Test
    fun incompleteDraft_cannotBecomeAnOperation() {
        val incomplete = ir.hoboho.shooting.domain.model.OperationDraft(
            key = 2L, startMeter = "100", endMeter = "500", cableTypeId = null,
        )
        assertEquals(null, incomplete.toOperation(1L, 0L))
        assertTrue(Validator.operation(incomplete, setOf(1L)).any {
            it.field == ValidationField.CABLE_TYPE
        })
    }
}
