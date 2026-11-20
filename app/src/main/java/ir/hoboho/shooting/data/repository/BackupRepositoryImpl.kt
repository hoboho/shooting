package ir.hoboho.shooting.data.repository

import androidx.room.withTransaction
import ir.hoboho.shooting.core.backup.BackupCounts
import ir.hoboho.shooting.core.backup.BackupData
import ir.hoboho.shooting.core.backup.BackupIntegrityException
import ir.hoboho.shooting.core.backup.CashLedgerDto
import ir.hoboho.shooting.data.local.CashLedgerEntryEntity
import ir.hoboho.shooting.data.local.AppSettingDao
import ir.hoboho.shooting.data.local.CableTypeDao
import ir.hoboho.shooting.data.local.CompanyPaymentDao
import ir.hoboho.shooting.data.local.CashLedgerDao
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
    private val companyPaymentDao: CompanyPaymentDao,
    private val cashLedgerDao: CashLedgerDao,
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
                companyApproved = entity.companyApproved,
            )
        }
        val payments = companyPaymentDao.getAll().map { it.toDomain() }
        val ledger = ledgerSnapshot(expenses, payments, cashLedgerDao.getAll())
        BackupData(
            settings = settingDao.getAll().toDomain(),
            projects = projects,
            workdays = workdays,
            operations = operationDao.getAll().map { it.toDomain() },
            cableTypes = cableTypeDao.getAll().map { it.toDomain() },
            categories = categories,
            expenses = expenses,
            settlements = settlements,
            companyPayments = payments,
            cashLedger = ledger,
        )
    }

    private fun ledgerSnapshot(
        expenses: List<Expense>,
        payments: List<ir.hoboho.shooting.domain.model.CompanyPayment>,
        rows: List<CashLedgerEntryEntity>,
    ): List<CashLedgerDto> {
        val byId = rows.associateBy { it.id }
        val expenseById = expenses.associate { it.id to it.uuid }
        val paymentById = payments.associate { it.id to it.uuid }
        return rows.map { row ->
            val originUuid = if (row.originType == "RECEIPT") {
                paymentById[row.originId] ?: row.idempotencyKey.removePrefix("receipt:")
            } else {
                expenseById[row.originId] ?: rows.lastOrNull {
                    it.originType == "EXPENSE" && it.originId == row.originId && it.idempotencyKey.startsWith("expense:")
                }?.idempotencyKey?.removePrefix("expense:")?.substringBefore(':')
            } ?: throw BackupIntegrityException("Ledger origin is missing: ${row.uuid}")
            if (originUuid.isBlank()) throw BackupIntegrityException("Ledger origin is blank: ${row.uuid}")
            val reversalUuid = row.reversalOfEntryId?.let { id ->
                byId[id]?.uuid ?: throw BackupIntegrityException("Ledger reversal is missing: ${row.uuid}")
            }.orEmpty()
            CashLedgerDto(row.uuid, row.idempotencyKey, row.originType, originUuid,
                row.amountToman, row.dateEpochDay, row.description, row.createdAt, reversalUuid)
        }
    }

    override suspend fun counts(): BackupCounts = BackupCounts.from(snapshot())

    override suspend fun restore(data: BackupData, strategy: RestoreStrategy): BackupCounts =
        db.withTransaction {
            val existingData = if (strategy == RestoreStrategy.REPLACE) null else snapshot()
            val existingLedgerRows = if (existingData == null) emptyList() else cashLedgerDao.getAll()
            if (strategy == RestoreStrategy.REPLACE) {
                // Child-first order keeps every FK satisfied.
                settlementDao.deleteAll()
                companyPaymentDao.deleteAll()
                cashLedgerDao.deleteAll()
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
                if (existing != null && overwrite && data.cashLedger == null &&
                    (existing.amountToman != expense.amountToman || existing.companyApproved != expense.companyApproved)
                ) throw BackupIntegrityException("Legacy backup would change an existing ledger expense: ${expense.uuid}")
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

            var importedPayments = 0
            val paymentIds = HashMap<String, Long>()
            for (payment in data.companyPayments) {
                val existing = companyPaymentDao.getByUuid(payment.uuid)
                if (existing != null && overwrite && data.cashLedger == null && existing.amountToman != payment.amountToman)
                    throw BackupIntegrityException("Legacy backup would change an existing receipt: ${payment.uuid}")
                if (existing != null && !overwrite) {
                    paymentIds[payment.uuid] = existing.id
                    continue
                }
                val inserted = companyPaymentDao.upsert(payment.copy(id = existing?.id ?: 0L).toEntity())
                paymentIds[payment.uuid] = existing?.id ?: inserted
                importedPayments++
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
                val expense = expenseDao.getById(id)?.expense
                    ?: throw BackupIntegrityException("Missing expense $uuid")
                val settled = settlementDao.totalForExpense(id)
                if (settled > expense.amountToman || (settled > 0L && !expense.companyApproved)) {
                    throw BackupIntegrityException("Settlements exceed expense $uuid")
                }
            }

            data.cashLedger?.let { incoming ->
                val oldLedger = existingData?.cashLedger.orEmpty()
                val oldByUuid = oldLedger.associateBy { it.uuid }
                val oldEntities = existingLedgerRows.associateBy { it.uuid }
                val existingExpenseUuids = existingData?.expenses.orEmpty().map { it.uuid }.toSet()
                val existingPaymentUuids = existingData?.companyPayments.orEmpty().map { it.uuid }.toSet()
                fun group(entry: CashLedgerDto) = (if (entry.originType == "RECEIPT") "receipt:" else "expense:") + entry.originUuid
                val existingGroups = oldLedger.groupBy(::group)
                val incomingGroups = incoming.groupBy(::group)
                val insertedIds = oldEntities.mapValues { it.value.id }.toMutableMap()
                var syntheticId = cashLedgerDao.minimumSyntheticOriginId()
                for ((groupKey, entries) in incomingGroups) {
                    val isReceipt = entries.first().originType == "RECEIPT"
                    val originUuid = entries.first().originUuid
                    val local = existingGroups[groupKey].orEmpty()
                    val existingRecord = if (isReceipt) originUuid in existingPaymentUuids else originUuid in existingExpenseUuids
                    if (strategy == RestoreStrategy.ADD_ONLY && (existingRecord || local.isNotEmpty())) continue
                    if (strategy == RestoreStrategy.MERGE && local.any { old -> entries.none { it == old } }) {
                        throw BackupIntegrityException("Conflicting ledger history for $groupKey")
                    }
                    entries.forEach { entry ->
                        val old = oldByUuid[entry.uuid]
                        if (old != null && old != entry) throw BackupIntegrityException("Conflicting ledger entry: ${entry.uuid}")
                    }
                    val priorId = local.firstOrNull()?.let { oldEntities[it.uuid]?.originId }
                    val originId = if (isReceipt) paymentIds[originUuid] ?: priorId
                        else expenseIds[originUuid] ?: priorId
                    val mappedOriginId = originId ?: (--syntheticId)
                    val pending = entries.filter { it.uuid !in insertedIds }.toMutableList()
                    while (pending.isNotEmpty()) {
                        val ready = pending.filter { it.reversalOfUuid.isBlank() || it.reversalOfUuid in insertedIds }
                        if (ready.isEmpty()) throw BackupIntegrityException("Unresolved ledger reversals: $groupKey")
                        for (entry in ready) {
                            val id = cashLedgerDao.insert(CashLedgerEntryEntity(
                                uuid = entry.uuid, idempotencyKey = entry.idempotencyKey,
                                originType = entry.originType, originId = mappedOriginId,
                                amountToman = entry.amountToman, dateEpochDay = entry.dateEpochDay,
                                description = entry.description, createdAt = entry.createdAt,
                                reversalOfEntryId = entry.reversalOfUuid.takeIf { it.isNotBlank() }?.let { insertedIds[it] },
                            ))
                            if (id == -1L) throw BackupIntegrityException("Conflicting ledger key: ${entry.idempotencyKey}")
                            insertedIds[entry.uuid] = id
                        }
                        pending.removeAll(ready.toSet())
                    }
                }
            }
            if (data.cashLedger != null) {
                val expenseUuids = existingData?.expenses.orEmpty().map { it.uuid }.toSet()
                val paymentUuids = existingData?.companyPayments.orEmpty().map { it.uuid }.toSet()
                for (expense in data.expenses) {
                    if (expense.companyApproved && expense.uuid !in expenseUuids &&
                        data.cashLedger.none { it.originType == "EXPENSE" && it.originUuid == expense.uuid }
                    ) throw BackupIntegrityException("Missing ledger for expense ${expense.uuid}")
                }
                for (payment in data.companyPayments) {
                    if (payment.uuid !in paymentUuids &&
                        data.cashLedger.none { it.originType == "RECEIPT" && it.originUuid == payment.uuid }
                    ) throw BackupIntegrityException("Missing ledger for payment ${payment.uuid}")
                }
            }
            cashLedgerDao.seedReceipts()
            cashLedgerDao.seedApprovedExpenses()
            // The imported state and immutable ledger must agree; reject divergent merges.
            val ledgerRows = cashLedgerDao.getAll()
            for ((uuid, id) in expenseIds) {
                val expense = expenseDao.getById(id)?.expense ?: throw BackupIntegrityException("Missing expense $uuid")
                val actual = ledgerRows.filter { it.originId == id && it.originType in setOf("EXPENSE", "REVERSAL") }
                    .fold(0L) { total, row -> Math.addExact(total, row.amountToman) }
                val expected = if (expense.companyApproved) -expense.amountToman else 0L
                if (actual != expected) throw BackupIntegrityException("Ledger conflict for expense $uuid")
            }
            for ((uuid, id) in paymentIds) {
                val payment = companyPaymentDao.getByUuid(uuid) ?: throw BackupIntegrityException("Missing payment $uuid")
                val actual = ledgerRows.filter { it.originId == id && it.originType == "RECEIPT" }
                    .fold(0L) { total, row -> Math.addExact(total, row.amountToman) }
                if (actual != payment.amountToman) throw BackupIntegrityException("Ledger conflict for payment $uuid")
            }

            if (overwrite) {
                settingDao.deleteAll()
                val remappedDefaultProjectId = data.settings.defaultProjectId?.let { incomingId ->
                    val uuid = projectUuidById[incomingId]
                        ?: throw BackupIntegrityException("Unknown default project id: $incomingId")
                    projectIds[uuid] ?: throw BackupIntegrityException("Unknown default project: $uuid")
                }
                data.settings.copy(defaultProjectId = remappedDefaultProjectId)
                    .toEntities().forEach { settingDao.upsert(it) }
            }
            BackupCounts(
                projects = data.projects.size,
                workdays = data.workdays.size,
                operations = importedOperations,
                cableTypes = data.cableTypes.size,
                categories = data.categories.size,
                expenses = data.expenses.size,
                settlements = importedSettlements,
                companyPayments = importedPayments,
            )
        }
}
