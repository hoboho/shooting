package ir.hoboho.shooting.domain

import ir.hoboho.shooting.domain.model.ShootingOperation
import ir.hoboho.shooting.domain.model.Workday
import kotlin.math.abs

/**
 * The core business rule of the application.
 *
 * `L = abs(endMeter - startMeter)`. A negative **raw** difference is perfectly
 * valid: cable reels can be read in reverse order. It must never be rejected.
 */
object ShootingCalculator {

    fun lengthMeters(startMeter: Int, endMeter: Int): Int {
        require(startMeter >= 0 && endMeter >= 0) { "Meter readings must be non-negative" }
        // Long arithmetic avoids Int overflow before abs() is applied.
        val difference: Long = endMeter.toLong() - startMeter.toLong()
        return abs(difference).toInt()
    }

    /** Recomputes the stored length from the two readings. */
    fun recompute(operation: ShootingOperation): ShootingOperation =
        operation.copy(lengthMeters = lengthMeters(operation.startMeter, operation.endMeter))

    fun recomputeAll(operations: List<ShootingOperation>): List<ShootingOperation> =
        operations.map(::recompute)

    /** Daily total = sum of operation lengths. */
    fun dailyTotal(operations: List<ShootingOperation>): Int =
        sumLengths(operations.map { it.lengthMeters })

    fun sumLengths(lengths: Iterable<Int>): Int {
        var total = 0L
        for (length in lengths) {
            require(length >= 0) { "Operation length must be non-negative" }
            total += length.toLong()
            require(total <= Int.MAX_VALUE) { "Total shooting length overflows" }
        }
        return total.toInt()
    }

    /** The cached workday total must equal the sum of its operations. */
    fun isTotalConsistent(workday: Workday): Boolean =
        workday.totalLengthMeters == dailyTotal(workday.operations)
}
