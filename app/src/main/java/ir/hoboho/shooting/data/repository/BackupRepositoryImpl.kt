package ir.hoboho.shooting.data.repository

import androidx.room.withTransaction
import ir.hoboho.shooting.core.backup.BackupCounts
import ir.hoboho.shooting.core.backup.BackupData
import ir.hoboho.shooting.core.backup.BackupIntegrityException
import ir.hoboho.shooting.data.local.AppSettingDao
import ir.hoboho.shooting.data.local.CableTypeDao
import ir.hoboho.shooting.data.local.ExpenseCategoryDao
import ir.hoboho.shooting.data.local.ExpenseDao
import ir.hoboho.shooting.data.local.OperationDao
import ir.hoboho.shooting.data.local.ProjectDao
import ir.hoboho.shooting.data.local.SettlementDao
import ir.hoboho.shooting.data.local.ShootingDatabase
import ir.hoboho.shooting.data.local.WorkdayDao
import ir.hoboho.shooting.data.mapper.toDomain
import ir.hoboho.shooting.data.mapper.toEntities
import ir.hoboho.shooting.data.mapper.toEntity
import ir.hoboho.shooting.domain.backup.RestoreStrategy
import ir.hoboho.shooting.domain.model.Expense
import ir.hoboho.shooting.domain.model.Workday
import ir.hoboho.shooting.domain.repository.BackupRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads/writes the whole database for backup and restore.
 *
 * Restoration is one transaction with an explicit strategy. Rows are matched by
 * stable uuid (or natural key for reference data); nothing is deleted except in
 * REPLACE, where the user has confirmed it and a safety snapshot already exists.
 */
@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val db: ShootingDatabase,
    private val projectDao: ProjectDao,
    private val workdayDao: WorkdayDao,
    private val operationDao: OperationDao,
    private val cableTypeDao: CableTypeDao,
    private val categoryDao: ExpenseCategoryDao,
    private val expenseDao: ExpenseDao,
    private val settlementDao: SettlementDao,
    private val settingDao: AppSettingDao,
) : BackupRepository {

    override suspend fun snapshot(): BackupData = db.withTransaction {
        val projects = projectDao.getAll().map { it.toDomain() }
        val projectNames = projects.associate { it.id to it.name }
        val categories = categoryDao.getAll().map { it.toDomain() }
        val categoryNames = categories.associate { it.id to it.name }
        val settlements = settlementDao.getAll().map { it.toDomain() }
        val settledByExpense = settlements.groupBy { it.expenseId }
        val workdays = workdayDao.getAll().map { entity ->
            Workday(
                id = entity.id, uuid = entity.uuid, projectId = entity.projectId,
                projectName = projectNames[entity.projectId].orEmpty(),
                dateEpochDay = entity.dateEpochDay, workArea = entity.workArea,
                employer = entity.employer, supervisor = entity.supervisor,
                workerCount = entity.workerCount, notes = entity.notes,
                totalLengthMeters = entity.totalLengthMeters,
                createdAt = entity.createdAt, updatedAt = entity.updatedAt,
            )
        }
        val expenses = expenseDao.getAll().map { entity ->
            Expense(
                id = entity.id, uuid = entity.uuid, dateEpochDay = entity.dateEpochDay,
                categoryId = entity.categoryId,
                categoryName = categoryNames[entity.categoryId].orEmpty(),
                amountToman = entity.amountToman, description = entity.description,
                receiptReference = entity.receiptReference,
                projectId = entity.projectId,
                projectName = entity.projectId?.let { projectNames[it] },
                workdayId = entity.workdayId,
                settledAmountToman = settledByExpense[entity.id]?.sumOf { it.amountToman } ?: 0L,
                settlementCount = settledByExpense[entity.id]?.size ?: 0,
                createdAt = entity.createdAt, updatedAt = entity.updatedAt,
            )
        }
        BackupData(
            settings = settingDao.getAll().toDomain(),
            projects = projects,
            workdays = workdays,
            operations = operationDao.getAll().map { it.toDomain() },
            cableTypes = cableTypeDao.getAll().map { it.toDomain() },
            categories = categories,
            expenses = expenses,
            settlements = settlements,
        )
    }

    override suspend fun counts(): BackupCounts = BackupCounts.from(snapshot())

    override suspend fun restore(data: BackupData, strategy: RestoreStrategy): BackupCounts =
        db.withTransaction {
            if (strategy == RestoreStrategy.REPLACE) {
                // Child-first order keeps every FK satisfied.
                settlementDao.deleteAll()
                operationDao.deleteAll()
                expenseDao.deleteAll()
                workdayDao.deleteAll()
                projectDao.deleteAll()
                cableTypeDao.deleteAll()
                categoryDao.deleteAll()
                settingDao.deleteAll()
            }
            val overwrite = strategy != RestoreStrategy.ADD_ONLY

            val projectUuidById = data.projects.associate { it.id to it.uuid }
            val workdayUuidById = data.workdays.associate { it.id to it.uuid }
            val expenseUuidById = data.expenses.associate { it.id to it.uuid }
            val cableCoreById = data.cableTypes.associate { it.id to it.coreCount }
            val categoryNameById = data.categories.associate { it.id to it.name }

            val cableIds = HashMap<Int, Long>()
            for (type in data.cableTypes) {
                val existing = cableTypeDao.getByCoreCount(type.coreCount)
                if (existing == null) {
                    cableIds[type.coreCount] = cableTypeDao.upsert(type.copy(id = 0L).toEntity())
                } else {
                    if (overwrite) {
                        cableTypeDao.upsert(type.copy(id = existing.id).toEntity())
                    }
                    cableIds[type.coreCount] = existing.id
                }
            }

            val categoryIds = HashMap<String, Long>()
            for (category in data.categories) {
                val existing = categoryDao.getByName(category.name)
                if (existing == null) {
                    categoryIds[category.name] = categoryDao.upsert(category.copy(id = 0L).toEntity())
                } else {
                    if (overwrite) {
                        categoryDao.upsert(category.copy(id = existing.id).toEntity())
                    }
                    categoryIds[category.name] = existing.id
                }
            }

            val projectIds = HashMap<String, Long>()
            for (project in data.projects) {
                val existing = projectDao.getByUuid(project.uuid)
                if (existing == null) {
                    projectIds[project.uuid] = projectDao.upsert(project.copy(id = 0L).toEntity())
                } else {
                    if (overwrite) projectDao.upsert(project.copy(id = existing.id).toEntity())
                    projectIds[project.uuid] = existing.id
                }
            }

            val workdayIds = HashMap<String, Long>()
            for (workday in data.workdays) {
                val projectUuid = projectUuidById[workday.projectId]
                    ?: throw BackupIntegrityException("Workday has no project: ${workday.uuid}")
                val projectId = projectIds[projectUuid]
                    ?: throw BackupIntegrityException("Unknown project: $projectUuid")
                val existing = workdayDao.getByUuid(workday.uuid)
                if (existing == null) {
                    workdayIds[workday.uuid] = workdayDao.upsert(
                        workday.copy(id = 0L, projectId = projectId).toEntity(),
                    )
                } else {
                    if (overwrite) {
                        workdayDao.upsert(workday.copy(id = existing.id, projectId = projectId).toEntity())
                    }
                    workdayIds[workday.uuid] = existing.id
                }
            }

            var importedOperations = 0
            for (operation in data.operations) {
                val workdayUuid = workdayUuidById[operation.workdayId]
                    ?: throw BackupIntegrityException("Operation has no workday: ${operation.uuid}")
                val workdayId = workdayIds[workdayUuid]
                    ?: throw BackupIntegrityException("Unknown workday: $workdayUuid")
                val core = cableCoreById[operation.cableTypeId]
                    ?: throw BackupIntegrityException("Operation has no cable type: ${operation.uuid}")
                val cableTypeId = cableIds[core]
                    ?: throw BackupIntegrityException("Unknown cable type: $core")
                val existing = operationDao.getByUuid(operation.uuid)
                if (existing != null && !overwrite) continue
                operationDao.upsert(
                    operation.copy(
                        id = existing?.id ?: 0L,
                        workdayId = workdayId,
                        cableTypeId = cableTypeId,
                    ).toEntity(),
                )
                importedOperations++
            }
            // Totals are derived from stored rows, never from the file.
            workdayIds.values.forEach { workdayDao.recomputeTotal(it) }

            val expenseIds = HashMap<String, Long>()
            for (expense in data.expenses) {
                val categoryName = categoryNameById[expense.categoryId]
                    ?: throw BackupIntegrityException("Expense has no category: ${expense.uuid}")
                val categoryId = categoryIds[categoryName]
                    ?: throw BackupIntegrityException("Unknown category: $categoryName")
                val projectId = expense.projectId?.let { id ->
                    projectUuidById[id]?.let { uuid -> projectIds[uuid] }
                }
                val workdayId = expense.workdayId?.let { id ->
                    workdayUuidById[id]?.let { uuid -> workdayIds[uuid] }
                }
                val existing = expenseDao.getByUuid(expense.uuid)
                if (existing == null) {
                    expenseIds[expense.uuid] = expenseDao.upsert(
                        expense.copy(
                            id = 0L, categoryId = categoryId,
                            projectId = projectId, workdayId = workdayId,
                        ).toEntity(),
                    )
                } else {
                    if (overwrite) {
                        expenseDao.upsert(
                            expense.copy(
                                id = existing.id, categoryId = categoryId,
                                projectId = projectId, workdayId = workdayId,
                            ).toEntity(),
                        )
                    }
                    expenseIds[expense.uuid] = existing.id
                }
            }

            var importedSettlements = 0
            for (settlement in data.settlements) {
                val expenseUuid = expenseUuidById[settlement.expenseId]
                    ?: throw BackupIntegrityException("Settlement has no expense: ${settlement.uuid}")
                val expenseId = expenseIds[expenseUuid]
                    ?: throw BackupIntegrityException("Unknown expense: $expenseUuid")
                val existing = settlementDao.getByUuid(settlement.uuid)
                if (existing != null && !overwrite) continue
                settlementDao.upsert(
                    settlement.copy(id = existing?.id ?: 0L, expenseId = expenseId).toEntity(),
                )
                importedSettlements++
            }
            // Final invariant: settlements never exceed the expense amount.
            for ((uuid, id) in expenseIds) {
                val amount = expenseDao.getById(id)?.expense?.amountToman ?: 0L
                val settled = settlementDao.totalForExpense(id)
                if (settled > amount) {
                    throw BackupIntegrityException("Settlements exceed expense $uuid")
                }
            }

            if (overwrite) {
                settingDao.deleteAll()
                data.settings.toEntities().forEach { settingDao.upsert(it) }
            }
            BackupCounts(
                projects = data.projects.size,
                workdays = data.workdays.size,
                operations = importedOperations,
                cableTypes = data.cableTypes.size,
                categories = data.categories.size,
                expenses = data.expenses.size,
                settlements = importedSettlements,
            )
        }
}
