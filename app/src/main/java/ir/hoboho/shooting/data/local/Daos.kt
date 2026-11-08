package ir.hoboho.shooting.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Workday joined with its project name. Operations are loaded separately. */
data class WorkdayRow(
    @Embedded val workday: WorkdayEntity,
    val projectName: String,
)

/** A full daily report: workday + project name + operations. */
data class WorkdayDetailRow(
    @Embedded val workday: WorkdayEntity,
    val projectName: String,
    @Relation(parentColumn = "id", entityColumn = "workdayId")
    val operations: List<OperationEntity>,
)

/**
 * Expense joined with its category/project names and its settlement sums.
 * Settled amounts are derived by SQL, never cached in a column that could drift.
 */
data class ExpenseRow(
    @Embedded val expense: ExpenseEntity,
    val categoryName: String,
    val projectName: String?,
    val settledAmountToman: Long,
    val settlementCount: Int,
)

private const val EXPENSE_SELECT = """
    SELECT e.*, c.name AS categoryName, p.name AS projectName,
        COALESCE((SELECT SUM(s.amountToman) FROM settlements s WHERE s.expenseId = e.id), 0) AS settledAmountToman,
        (SELECT COUNT(*) FROM settlements s WHERE s.expenseId = e.id) AS settlementCount
    FROM expenses e
    INNER JOIN expense_categories c ON e.categoryId = c.id
    LEFT JOIN projects p ON e.projectId = p.id
"""

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY active DESC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE active = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects")
    suspend fun getAll(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): ProjectEntity?

    @Query("SELECT COUNT(*) FROM projects WHERE name = :name AND id != :excludeId")
    suspend fun countByName(name: String, excludeId: Long = -1L): Int

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(project: ProjectEntity): Long

    @Insert
    suspend fun insertAll(projects: List<ProjectEntity>)

    @Query("UPDATE projects SET active = :active, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean, updatedAt: Long)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM projects")
    suspend fun deleteAll()
}

@Dao
interface WorkdayDao {
    @Query(
        """SELECT w.*, p.name AS projectName FROM workdays w
           INNER JOIN projects p ON w.projectId = p.id
           ORDER BY w.dateEpochDay DESC, w.id DESC"""
    )
    fun observeAll(): Flow<List<WorkdayRow>>

    /** Same order as [observeAll], but every workday carries its operations. */
    @Transaction
    @Query(
        """SELECT w.*, p.name AS projectName FROM workdays w
           INNER JOIN projects p ON w.projectId = p.id
           ORDER BY w.dateEpochDay DESC, w.id DESC"""
    )
    fun observeAllDetailed(): Flow<List<WorkdayDetailRow>>

    @Query(
        """SELECT w.*, p.name AS projectName FROM workdays w
           INNER JOIN projects p ON w.projectId = p.id
           WHERE w.projectId = :projectId
           ORDER BY w.dateEpochDay DESC, w.id DESC"""
    )
    fun observeByProject(projectId: Long): Flow<List<WorkdayRow>>

    @Query(
        """SELECT w.*, p.name AS projectName FROM workdays w
           INNER JOIN projects p ON w.projectId = p.id
           WHERE w.dateEpochDay BETWEEN :start AND :end
           ORDER BY w.dateEpochDay DESC, w.id DESC"""
    )
    fun observeInRange(start: Long, end: Long): Flow<List<WorkdayRow>>

    @Transaction
    @Query(
        """SELECT w.*, p.name AS projectName FROM workdays w
           INNER JOIN projects p ON w.projectId = p.id WHERE w.id = :id LIMIT 1"""
    )
    fun observeDetail(id: Long): Flow<WorkdayDetailRow?>

    @Transaction
    @Query(
        """SELECT w.*, p.name AS projectName FROM workdays w
           INNER JOIN projects p ON w.projectId = p.id WHERE w.id = :id LIMIT 1"""
    )
    suspend fun getDetail(id: Long): WorkdayDetailRow?

    @Query("SELECT * FROM workdays")
    suspend fun getAll(): List<WorkdayEntity>

    @Query("SELECT * FROM workdays WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WorkdayEntity?

    @Query("SELECT * FROM workdays WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): WorkdayEntity?

    @Query("SELECT COUNT(*) FROM workdays WHERE projectId = :projectId AND dateEpochDay = :day AND id != :excludeId")
    suspend fun countDuplicates(projectId: Long, day: Long, excludeId: Long): Int

    @Upsert
    suspend fun upsert(workday: WorkdayEntity): Long

    @Insert
    suspend fun insertAll(workdays: List<WorkdayEntity>): List<Long>

    /** Recomputes the cached daily total from the stored operation lengths. */
    @Query(
        """UPDATE workdays SET totalLengthMeters =
           (SELECT COALESCE(SUM(o.lengthMeters), 0) FROM operations o WHERE o.workdayId = :id)
           WHERE id = :id"""
    )
    suspend fun recomputeTotal(id: Long)

    @Query("DELETE FROM workdays WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM workdays")
    suspend fun deleteAll()
}

@Dao
interface OperationDao {
    @Query("SELECT * FROM operations WHERE workdayId = :workdayId ORDER BY id ASC")
    fun observeByWorkday(workdayId: Long): Flow<List<OperationEntity>>

    @Query("SELECT * FROM operations WHERE workdayId = :workdayId ORDER BY id ASC")
    suspend fun getByWorkday(workdayId: Long): List<OperationEntity>

    @Query("SELECT * FROM operations")
    suspend fun getAll(): List<OperationEntity>

    @Query("SELECT * FROM operations WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): OperationEntity?

    @Upsert
    suspend fun upsert(operation: OperationEntity): Long

    @Insert
    suspend fun insertAll(operations: List<OperationEntity>): List<Long>

    @Query("DELETE FROM operations WHERE workdayId = :workdayId")
    suspend fun deleteByWorkday(workdayId: Long)

    @Query("DELETE FROM operations")
    suspend fun deleteAll()
}

@Dao
interface CableTypeDao {
    @Query("SELECT * FROM cable_types ORDER BY sortOrder ASC, coreCount ASC")
    fun observeAll(): Flow<List<CableTypeEntity>>

    @Query("SELECT * FROM cable_types WHERE active = 1 ORDER BY sortOrder ASC, coreCount ASC")
    fun observeActive(): Flow<List<CableTypeEntity>>

    @Query("SELECT * FROM cable_types")
    suspend fun getAll(): List<CableTypeEntity>

    @Query("SELECT * FROM cable_types WHERE coreCount = :core LIMIT 1")
    suspend fun getByCoreCount(core: Int): CableTypeEntity?

    @Query("SELECT COUNT(*) FROM operations WHERE cableTypeId = :id")
    suspend fun usageCount(id: Long): Int

    @Upsert
    suspend fun upsert(type: CableTypeEntity): Long

    @Insert
    suspend fun insertAll(types: List<CableTypeEntity>)

    @Query("UPDATE cable_types SET active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("DELETE FROM cable_types")
    suspend fun deleteAll()
}

@Dao
interface ExpenseCategoryDao {
    @Query("SELECT * FROM expense_categories ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<ExpenseCategoryEntity>>

    @Query("SELECT * FROM expense_categories WHERE active = 1 ORDER BY sortOrder ASC, id ASC")
    fun observeActive(): Flow<List<ExpenseCategoryEntity>>

    @Query("SELECT * FROM expense_categories")
    suspend fun getAll(): List<ExpenseCategoryEntity>

    @Query("SELECT * FROM expense_categories WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): ExpenseCategoryEntity?

    @Query("SELECT COUNT(*) FROM expenses WHERE categoryId = :id")
    suspend fun usageCount(id: Long): Int

    @Upsert
    suspend fun upsert(category: ExpenseCategoryEntity): Long

    @Insert
    suspend fun insertAll(categories: List<ExpenseCategoryEntity>)

    @Query("UPDATE expense_categories SET active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("DELETE FROM expense_categories")
    suspend fun deleteAll()
}

@Dao
interface ExpenseDao {
    @Query("$EXPENSE_SELECT ORDER BY e.dateEpochDay DESC, e.id DESC")
    fun observeAll(): Flow<List<ExpenseRow>>

    @Query("$EXPENSE_SELECT WHERE e.dateEpochDay BETWEEN :start AND :end ORDER BY e.dateEpochDay DESC, e.id DESC")
    fun observeInRange(start: Long, end: Long): Flow<List<ExpenseRow>>

    @Query("$EXPENSE_SELECT WHERE e.id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ExpenseRow?>

    @Query("$EXPENSE_SELECT WHERE e.id = :id LIMIT 1")
    suspend fun getById(id: Long): ExpenseRow?

    @Query("SELECT * FROM expenses")
    suspend fun getAll(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): ExpenseEntity?

    @Query("$EXPENSE_SELECT WHERE e.companyApproved = 1 ORDER BY e.dateEpochDay ASC, e.id ASC")
    suspend fun getApprovedOldest(): List<ExpenseRow>

    @Upsert
    suspend fun upsert(expense: ExpenseEntity): Long

    @Insert
    suspend fun insertAll(expenses: List<ExpenseEntity>): List<Long>

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}

@Dao
interface SettlementDao {
    @Query("SELECT * FROM settlements WHERE expenseId = :expenseId ORDER BY dateEpochDay ASC, id ASC")
    fun observeByExpense(expenseId: Long): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE expenseId = :expenseId ORDER BY dateEpochDay ASC, id ASC")
    suspend fun getByExpense(expenseId: Long): List<SettlementEntity>

    @Query("SELECT * FROM settlements ORDER BY dateEpochDay ASC, id ASC")
    fun observeAll(): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements")
    suspend fun getAll(): List<SettlementEntity>

    @Query("SELECT * FROM settlements WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): SettlementEntity?

    @Query("SELECT * FROM settlements WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SettlementEntity?

    @Query("SELECT COALESCE(SUM(amountToman), 0) FROM settlements WHERE expenseId = :expenseId")
    suspend fun totalForExpense(expenseId: Long): Long

    @Upsert
    suspend fun upsert(settlement: SettlementEntity): Long

    @Insert
    suspend fun insertAll(settlements: List<SettlementEntity>): List<Long>

    @Query("DELETE FROM settlements WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM settlements")
    suspend fun deleteAll()
}

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    fun observe(key: String): Flow<AppSettingEntity?>

    @Query("SELECT * FROM app_settings")
    fun observeAll(): Flow<List<AppSettingEntity>>

    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): AppSettingEntity?

    @Query("SELECT * FROM app_settings")
    suspend fun getAll(): List<AppSettingEntity>

    @Upsert
    suspend fun upsert(setting: AppSettingEntity): Long

    @Query("DELETE FROM app_settings")
    suspend fun deleteAll()
}
