package ir.hoboho.shooting.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The single local source of truth.
 *
 * **Migration policy:** `fallbackToDestructiveMigration` is deliberately never
 * used. Every future schema change must ship an explicit, additive `Migration`
 * registered in `di/Migrations.kt`; `room.schemaLocation` exports the current
 * JSON schema. Historical schemas and full upgrade validation still need to be
 * recovered and tested. Migrations 1–6 are explicit; no destructive fallback
 * is configured.
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
        CashLedgerEntryEntity::class,
        AppSettingEntity::class,
    ],
    version = 6,
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
    abstract fun cashLedgerDao(): CashLedgerDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        const val NAME = "shooting.db"
    }
}
