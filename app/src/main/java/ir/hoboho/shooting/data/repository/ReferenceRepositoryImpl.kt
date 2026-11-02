package ir.hoboho.shooting.data.repository

import ir.hoboho.shooting.core.common.ValidationException
import ir.hoboho.shooting.data.local.CableTypeDao
import ir.hoboho.shooting.data.local.CableTypeEntity
import ir.hoboho.shooting.data.local.ExpenseCategoryDao
import ir.hoboho.shooting.data.local.ExpenseCategoryEntity
import ir.hoboho.shooting.data.mapper.toDomain
import ir.hoboho.shooting.domain.ValidationField
import ir.hoboho.shooting.domain.ValidationIssue
import ir.hoboho.shooting.domain.model.CableType
import ir.hoboho.shooting.domain.model.ExpenseCategory
import ir.hoboho.shooting.domain.model.ReferenceData
import ir.hoboho.shooting.domain.repository.ReferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferenceRepositoryImpl @Inject constructor(
    private val cableTypeDao: CableTypeDao,
    private val categoryDao: ExpenseCategoryDao,
) : ReferenceRepository {

    override fun observeCableTypes(): Flow<List<CableType>> =
        cableTypeDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeActiveCableTypes(): Flow<List<CableType>> =
        cableTypeDao.observeActive().map { rows -> rows.map { it.toDomain() } }

    override fun observeCategories(): Flow<List<ExpenseCategory>> =
        categoryDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeActiveCategories(): Flow<List<ExpenseCategory>> =
        categoryDao.observeActive().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getAllCableTypes(): List<CableType> =
        cableTypeDao.getAll().map { it.toDomain() }

    override suspend fun getAllCategories(): List<ExpenseCategory> =
        categoryDao.getAll().map { it.toDomain() }

    /** New core counts are rows, not enum values: no schema change needed. */
    override suspend fun addCableType(coreCount: Int): Long {
        if (coreCount <= 0 || cableTypeDao.getByCoreCount(coreCount) != null) {
            throw ValidationException(listOf(ValidationIssue(ValidationField.CABLE_TYPE)))
        }
        val nextSort = cableTypeDao.getAll().maxOfOrNull { it.sortOrder + 1 } ?: 0
        return cableTypeDao.upsert(
            CableTypeEntity(
                coreCount = coreCount,
                label = ReferenceData.cableLabel(coreCount),
                sortOrder = nextSort,
            ),
        )
    }

    override suspend fun setCableTypeActive(id: Long, active: Boolean) {
        // Rows in use are only deactivated, never deleted (FK RESTRICT).
        if (!active && cableTypeDao.usageCount(id) > 0) {
            cableTypeDao.setActive(id, false)
            return
        }
        cableTypeDao.setActive(id, active)
    }

    override suspend fun addCategory(name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || categoryDao.getByName(trimmed) != null) {
            throw ValidationException(listOf(ValidationIssue(ValidationField.EXPENSE_CATEGORY)))
        }
        val nextSort = categoryDao.getAll().maxOfOrNull { it.sortOrder + 1 } ?: 0
        return categoryDao.upsert(ExpenseCategoryEntity(name = trimmed, sortOrder = nextSort))
    }

    override suspend fun setCategoryActive(id: Long, active: Boolean) {
        // Referenced categories are deactivated instead of removed.
        categoryDao.setActive(id, active)
    }
}
