package ir.hoboho.shooting.data.repository

import androidx.room.withTransaction
import ir.hoboho.shooting.core.common.Clock
import ir.hoboho.shooting.core.common.NotFoundException
import ir.hoboho.shooting.core.common.ValidationException
import ir.hoboho.shooting.data.local.CableTypeDao
import ir.hoboho.shooting.data.local.OperationDao
import ir.hoboho.shooting.data.local.ProjectDao
import ir.hoboho.shooting.data.local.ShootingDatabase
import ir.hoboho.shooting.data.local.WorkdayDao
import ir.hoboho.shooting.data.mapper.toDomain
import ir.hoboho.shooting.data.mapper.toEntity
import ir.hoboho.shooting.domain.ShootingCalculator
import ir.hoboho.shooting.domain.ValidationField
import ir.hoboho.shooting.domain.ValidationIssue
import ir.hoboho.shooting.domain.Validator
import ir.hoboho.shooting.domain.model.ShootingOperation
import ir.hoboho.shooting.domain.model.Workday
import ir.hoboho.shooting.domain.repository.WorkdayRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkdayRepositoryImpl @Inject constructor(
    private val db: ShootingDatabase,
    private val dao: WorkdayDao,
    private val operationDao: OperationDao,
    private val projectDao: ProjectDao,
    private val cableTypeDao: CableTypeDao,
    private val clock: Clock,
) : WorkdayRepository {

    override fun observeWorkdays(): Flow<List<Workday>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeWorkdaysInRange(start: Long, end: Long): Flow<List<Workday>> =
        dao.observeInRange(start, end).map { rows -> rows.map { it.toDomain() } }

    override fun observeWorkdaysByProject(projectId: Long): Flow<List<Workday>> =
        dao.observeByProject(projectId).map { rows -> rows.map { it.toDomain() } }

    override fun observeWorkday(id: Long): Flow<Workday?> =
        dao.observeDetail(id).map { row -> row?.toDomain() }

    override suspend fun getWorkday(id: Long): Workday? = dao.getDetail(id)?.toDomain()

    override suspend fun getAllWorkdaysWithOperations(): List<Workday> =
        dao.getAll().mapNotNull { row -> dao.getDetail(row.id)?.toDomain() }

    override suspend fun duplicateExists(projectId: Long, dateEpochDay: Long, excludeId: Long): Boolean =
        dao.countDuplicates(projectId, dateEpochDay, excludeId) > 0

    override suspend fun save(workday: Workday, operations: List<ShootingOperation>): Long =
        db.withTransaction {
            val projectExists = projectDao.getById(workday.projectId) != null
            val duplicate = dao.countDuplicates(workday.projectId, workday.dateEpochDay, workday.id) > 0
            val issues = Validator.workday(
                projectId = workday.projectId,
                projectExists = projectExists,
                dateEpochDay = workday.dateEpochDay,
                workerCount = workday.workerCount,
                operationCount = operations.size,
                duplicateExists = duplicate,
            ).toMutableList()

            val cableIds = cableTypeDao.getAll().map { it.id }.toSet()
            for (operation in operations) {
                if (operation.startCode.isBlank()) issues += ValidationIssue(ValidationField.START_CODE)
                if (operation.endCode.isBlank()) issues += ValidationIssue(ValidationField.END_CODE)
                if (operation.startMeter < 0) issues += ValidationIssue(ValidationField.START_METER)
                if (operation.endMeter < 0) issues += ValidationIssue(ValidationField.END_METER)
                if (operation.cableTypeId !in cableIds) issues += ValidationIssue(ValidationField.CABLE_TYPE)
            }
            if (issues.isNotEmpty()) throw ValidationException(issues)

            // Absolute difference is recomputed here, never trusted from the UI.
            val recomputed = ShootingCalculator.recomputeAll(operations)
            val total = ShootingCalculator.dailyTotal(recomputed)
            val now = clock.nowEpochMilli()
            val entity = workday.copy(
                uuid = workday.uuid.ifBlank { UUID.randomUUID().toString() },
                totalLengthMeters = total,
                createdAt = if (workday.id == 0L) now else workday.createdAt,
                updatedAt = now,
            ).toEntity()
            val insertedId = dao.upsert(entity)
            val workdayId = if (insertedId == -1L) workday.id else insertedId

            // Replace the operation rows but keep their stable uuids, so a
            // restored backup still recognises them. Nothing is silently lost:
            // every incoming operation is written below.
            operationDao.deleteByWorkday(workdayId)
            operationDao.insertAll(recomputed.map { op ->
                op.copy(
                    id = 0L,
                    uuid = op.uuid.ifBlank { UUID.randomUUID().toString() },
                    workdayId = workdayId,
                    createdAt = if (op.createdAt == 0L) now else op.createdAt,
                    updatedAt = now,
                ).toEntity()
            })
            // The cached total always matches the stored operations.
            dao.recomputeTotal(workdayId)
            workdayId
        }

    override suspend fun delete(id: Long) {
        if (dao.getById(id) == null) throw NotFoundException("workday $id")
        // Operations cascade; linked expenses keep their history (SET NULL).
        dao.deleteById(id)
    }
}
