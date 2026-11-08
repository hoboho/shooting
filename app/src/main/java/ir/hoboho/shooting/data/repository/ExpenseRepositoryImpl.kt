package ir.hoboho.shooting.data.repository

import androidx.room.withTransaction
import ir.hoboho.shooting.core.common.Clock
import ir.hoboho.shooting.core.common.NotFoundException
import ir.hoboho.shooting.core.common.ValidationException
import ir.hoboho.shooting.data.local.ExpenseCategoryDao
import ir.hoboho.shooting.data.local.ExpenseDao
import ir.hoboho.shooting.data.local.SettlementDao
import ir.hoboho.shooting.data.local.ShootingDatabase
import ir.hoboho.shooting.data.mapper.toDomain
import ir.hoboho.shooting.data.mapper.toEntity
import ir.hoboho.shooting.domain.ExpenseCalculator
import ir.hoboho.shooting.domain.ValidationField
import ir.hoboho.shooting.domain.ValidationIssue
import ir.hoboho.shooting.domain.Validator
import ir.hoboho.shooting.domain.model.Expense
import ir.hoboho.shooting.domain.model.Settlement
import ir.hoboho.shooting.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val db: ShootingDatabase,
    private val dao: ExpenseDao,
    private val settlementDao: SettlementDao,
    private val categoryDao: ExpenseCategoryDao,
    private val clock: Clock,
) : ExpenseRepository {

    override fun observeExpenses(): Flow<List<Expense>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeExpensesInRange(start: Long, end: Long): Flow<List<Expense>> =
        dao.observeInRange(start, end).map { rows -> rows.map { it.toDomain() } }

    override fun observeExpense(id: Long): Flow<Expense?> =
        dao.observeById(id).map { row -> row?.toDomain() }

    override suspend fun getExpense(id: Long): Expense? = dao.getById(id)?.toDomain()

    override suspend fun getAllExpenses(): List<Expense> =
        dao.getAll().map { entity ->
            dao.getById(entity.id)?.toDomain()
                ?: Expense(id = entity.id, dateEpochDay = entity.dateEpochDay,
                    categoryId = entity.categoryId, amountToman = entity.amountToman)
        }

    override suspend fun save(expense: Expense): Long {
        val issues = Validator.expense(
            amountToman = expense.amountToman,
            categoryId = expense.categoryId,
            amountText = expense.amountToman.toString(),
        )
        if (expense.amountToman > 0L && categoryDao.getAll().none { it.id == expense.categoryId }) {
            // Category must exist; the UI only offers active ones.
            throw ValidationException(listOf(ValidationIssue(ValidationField.EXPENSE_CATEGORY)))
        }
        if (issues.isNotEmpty()) throw ValidationException(issues)
        if (expense.id > 0L) {
            val settled = settlementDao.totalForExpense(expense.id)
            if (settled > 0L && !expense.companyApproved) {
                throw ValidationException(listOf(ValidationIssue(ValidationField.EXPENSE_HAS_SETTLEMENTS)))
            }
            if (settled > expense.amountToman) {
                throw ValidationException(listOf(ValidationIssue(ValidationField.SETTLEMENT_EXCEEDS_OUTSTANDING)))
            }
        }
        val now = clock.nowEpochMilli()
        val entity = expense.copy(
            uuid = expense.uuid.ifBlank { UUID.randomUUID().toString() },
            createdAt = if (expense.id == 0L) now else expense.createdAt,
            updatedAt = now,
        ).toEntity()
        val id = dao.upsert(entity)
        return if (id == -1L) expense.id else id
    }

    override suspend fun delete(id: Long) {
        if (dao.getById(id) == null) throw NotFoundException("expense $id")
        if (settlementDao.getByExpense(id).isNotEmpty()) {
            // Never silently discard settlement history.
            throw ValidationException(listOf(ValidationIssue(ValidationField.EXPENSE_HAS_SETTLEMENTS)))
        }
        dao.deleteById(id)
    }

    override fun observeSettlements(expenseId: Long): Flow<List<Settlement>> =
        settlementDao.observeByExpense(expenseId).map { rows -> rows.map { it.toDomain() } }

    override fun observeAllSettlements(): Flow<List<Settlement>> =
        settlementDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getAllSettlements(): List<Settlement> =
        settlementDao.getAll().map { it.toDomain() }

    override suspend fun addSettlement(settlement: Settlement): Long = db.withTransaction {
        val expense = dao.getById(settlement.expenseId)
            ?: throw NotFoundException("expense ${settlement.expenseId}")
        if (!expense.expense.companyApproved) {
            throw ValidationException(listOf(ValidationIssue(ValidationField.EXPENSE_HAS_SETTLEMENTS)))
        }
        val alreadySettled = settlementDao.totalForExpense(settlement.expenseId)
        val issues = Validator.settlement(
            additionalToman = settlement.amountToman,
            amountToman = expense.expense.amountToman,
            alreadySettledToman = alreadySettled,
        )
        if (issues.isNotEmpty()) throw ValidationException(issues)
        val entity = settlement.copy(
            id = 0L,
            uuid = settlement.uuid.ifBlank { UUID.randomUUID().toString() },
            createdAt = if (settlement.createdAt == 0L) clock.nowEpochMilli() else settlement.createdAt,
        ).toEntity()
        settlementDao.upsert(entity)
    }

    override suspend fun updateSettlement(settlement: Settlement) = db.withTransaction {
        val existing = settlementDao.getById(settlement.id)
            ?: throw NotFoundException("settlement " + settlement.id)
        val expense = dao.getById(existing.expenseId)
            ?: throw NotFoundException("expense " + existing.expenseId)
        if (!expense.expense.companyApproved) {
            throw ValidationException(listOf(ValidationIssue(ValidationField.EXPENSE_HAS_SETTLEMENTS)))
        }
        val alreadySettled = settlementDao.totalForExpense(existing.expenseId) - existing.amountToman
        val issues = Validator.settlement(
            additionalToman = settlement.amountToman,
            amountToman = expense.expense.amountToman,
            alreadySettledToman = alreadySettled,
        )
        if (issues.isNotEmpty()) throw ValidationException(issues)
        settlementDao.upsert(
            settlement.copy(expenseId = existing.expenseId, uuid = existing.uuid, createdAt = existing.createdAt)
                .toEntity(),
        )
        Unit
    }

    override suspend fun allocateCompanyPayment(
        amountToman: Long,
        dateEpochDay: Long,
        note: String,
    ): Long = db.withTransaction {
        val approved = dao.getApprovedOldest().map { it.toDomain() }
        val allocations = try {
            ExpenseCalculator.fifoAllocations(approved, amountToman)
        } catch (error: IllegalArgumentException) {
            throw ValidationException(listOf(ValidationIssue(ValidationField.SETTLEMENT_EXCEEDS_OUTSTANDING)))
        }
        val now = clock.nowEpochMilli()
        allocations.forEach { allocation ->
            settlementDao.upsert(
                Settlement(
                    uuid = UUID.randomUUID().toString(),
                    expenseId = allocation.expenseId,
                    amountToman = allocation.amountToman,
                    dateEpochDay = dateEpochDay,
                    note = note,
                    createdAt = now,
                ).toEntity(),
            )
        }
        amountToman
    }

    override suspend fun deleteSettlement(id: Long) = settlementDao.deleteById(id)
}
