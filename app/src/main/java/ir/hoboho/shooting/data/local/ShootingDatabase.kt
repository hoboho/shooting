package ir.hoboho.shooting.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The single local source of truth.
 *
 * **Migration policy:** `fallbackToDestructiveMigration` is deliberately never
 * used. Every future schema change must ship an explicit, additive `Migration`
 * registered in `di/Migrations.kt`; `room.schemaLocation` exports the versioned
 * JSON schema so CI can validate it. Versions 1–3 are upgraded through explicit
 * migrations; v4 also repairs legacy ODC-prefixed FAT point codes. No destructive
 * fallback is allowed, so existing project and operation history is preserved.
 */
@Database(
    entities = [
        ProjectEntity::class,
        WorkdayEntity::class,
        CableTypeEntity::class,
        OperationEntity::class,
        ExpenseCategoryEntity::class,
        ExpenseEntity::class,
        SettlementEntity::class,
        CompanyPaymentEntity::class,
        AppSettingEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class ShootingDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun workdayDao(): WorkdayDao
    abstract fun cableTypeDao(): CableTypeDao
    abstract fun operationDao(): OperationDao
    abstract fun expenseCategoryDao(): ExpenseCategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun settlementDao(): SettlementDao
    abstract fun companyPaymentDao(): CompanyPaymentDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        const val NAME = "shooting.db"
    }
}
