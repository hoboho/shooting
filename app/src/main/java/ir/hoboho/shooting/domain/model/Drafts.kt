package ir.hoboho.shooting.domain.model

import ir.hoboho.shooting.core.format.NumberFormatter
import ir.hoboho.shooting.domain.ShootingCalculator

/**
 * Mutable UI draft of one operation. Meter readings stay as strings while the
 * user types; the derived [lengthMeters] recomputes on every edit, and
 * [toOperation] refuses to produce an incomplete/invalid operation.
 */
data class OperationDraft(
    /** Stable local key for LazyColumn items (not a database id). */
    val key: Long = 0L,
    val id: Long = 0L,
    val uuid: String = "",
    val startType: PointType = PointType.ODC,
    val startCode: String = "",
    val startMeter: String = "",
    val endType: PointType = PointType.FAT,
    val endCode: String = "",
    val endMeter: String = "",
    val cableTypeId: Long? = null,
    val createdAt: Long = 0L,
) {
    val parsedStartMeter: Int? get() = NumberFormatter.parseNonNegativeInt(startMeter)
    val parsedEndMeter: Int? get() = NumberFormatter.parseNonNegativeInt(endMeter)

    /** Absolute difference, or null while either reading is incomplete. */
    val lengthMeters: Int?
        get() {
            val start = parsedStartMeter ?: return null
            val end = parsedEndMeter ?: return null
            return ShootingCalculator.lengthMeters(start, end)
        }

    /** Builds a storable operation, or null when any required field is missing. */
    fun toOperation(workdayId: Long, now: Long): ShootingOperation? {
        val start = parsedStartMeter ?: return null
        val end = parsedEndMeter ?: return null
        val cable = cableTypeId ?: return null
        if (startCode.isBlank() || endCode.isBlank()) return null
        return ShootingOperation(
            id = id,
            uuid = uuid,
            workdayId = workdayId,
            startType = startType,
            startCode = startCode.trim(),
            startMeter = start,
            endType = endType,
            endCode = endCode.trim(),
            endMeter = end,
            cableTypeId = cable,
            lengthMeters = ShootingCalculator.lengthMeters(start, end),
            createdAt = if (id == 0L) now else createdAt,
            updatedAt = now,
        )
    }

    companion object {
        fun fromOperation(op: ShootingOperation, key: Long): OperationDraft = OperationDraft(
            key = key,
            id = op.id,
            uuid = op.uuid,
            startType = op.startType,
            startCode = op.startCode,
            startMeter = op.startMeter.toString(),
            endType = op.endType,
            endCode = op.endCode,
            endMeter = op.endMeter.toString(),
            cableTypeId = op.cableTypeId,
            createdAt = op.createdAt,
        )
    }
}
