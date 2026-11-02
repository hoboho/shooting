package ir.hoboho.shooting.domain.repository

import ir.hoboho.shooting.core.backup.BackupCounts
import ir.hoboho.shooting.core.backup.BackupData
import ir.hoboho.shooting.domain.backup.RestoreStrategy
import ir.hoboho.shooting.domain.model.AppSettings
import ir.hoboho.shooting.domain.model.CableType
import ir.hoboho.shooting.domain.model.Expense
import ir.hoboho.shooting.domain.model.ExpenseCategory
import ir.hoboho.shooting.domain.model.Project
import ir.hoboho.shooting.domain.model.Settlement
import ir.hoboho.shooting.domain.model.ShootingOperation
import ir.hoboho.shooting.domain.model.ThemeMode
import ir.hoboho.shooting.domain.model.Workday
import kotlinx.coroutines.flow.Flow

/** UI depends on these interfaces only; Room/Hilt stay behind the boundary. */
interface ProjectRepository {
    fun observeProjects(): Flow<List<Project>>
    fun observeActiveProjects(): Flow<List<Project>>
    suspend fun getProjects(): List<Project>
    suspend fun getProject(id: Long): Project?
    suspend fun save(project: Project): Long
    suspend fun setActive(id: Long, active: Boolean)
    suspend fun delete(id: Long)
}

interface WorkdayRepository {
    fun observeWorkdays(): Flow<List<Workday>>
    fun observeWorkdaysInRange(start: Long, end: Long): Flow<List<Workday>>
    fun observeWorkdaysByProject(projectId: Long): Flow<List<Workday>>
    fun observeWorkday(id: Long): Flow<Workday?>
    suspend fun getWorkday(id: Long): Workday?
    suspend fun getAllWorkdaysWithOperations(): List<Workday>
    suspend fun duplicateExists(projectId: Long, dateEpochDay: Long, excludeId: Long): Boolean
    /** Saves the workday and its operations atomically; recomputes all lengths/totals. */
    suspend fun save(workday: Workday, operations: List<ShootingOperation>): Long
    suspend fun delete(id: Long)
}

interface ExpenseRepository {
    fun observeExpenses(): Flow<List<Expense>>
    fun observeExpensesInRange(start: Long, end: Long): Flow<List<Expense>>
    fun observeExpense(id: Long): Flow<Expense?>
    suspend fun getExpense(id: Long): Expense?
    suspend fun getAllExpenses(): List<Expense>
    suspend fun save(expense: Expense): Long
    suspend fun delete(id: Long)
    fun observeSettlements(expenseId: Long): Flow<List<Settlement>>
    fun observeAllSettlements(): Flow<List<Settlement>>
    suspend fun getAllSettlements(): List<Settlement>
    /** Validates against the outstanding balance inside a transaction. */
    suspend fun addSettlement(settlement: Settlement): Long
    suspend fun deleteSettlement(id: Long)
}

interface ReferenceRepository {
    fun observeCableTypes(): Flow<List<CableType>>
    fun observeActiveCableTypes(): Flow<List<CableType>>
    fun observeCategories(): Flow<List<ExpenseCategory>>
    fun observeActiveCategories(): Flow<List<ExpenseCategory>>
    suspend fun getAllCableTypes(): List<CableType>
    suspend fun getAllCategories(): List<ExpenseCategory>
    suspend fun addCableType(coreCount: Int): Long
    suspend fun setCableTypeActive(id: Long, active: Boolean)
    suspend fun addCategory(name: String): Long
    suspend fun setCategoryActive(id: Long, active: Boolean)
}

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun getSettings(): AppSettings
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDefaultProjectId(id: Long?)
    suspend fun setLastBackupEpochMilli(value: Long)
}

interface BackupRepository {
    suspend fun snapshot(): BackupData
    suspend fun counts(): BackupCounts
    /** Never overwrites silently; strategy is explicit and runs in one transaction. */
    suspend fun restore(data: BackupData, strategy: RestoreStrategy): BackupCounts
}
