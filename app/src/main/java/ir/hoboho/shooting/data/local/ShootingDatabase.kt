package ir.hoboho.shooting.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The single local source of truth.
 *
 * **Migration policy:** `fallbackToDestructiveMigration` is deliberately never
 * used. Every future schema change must ship an explicit, additive `Migration`
 * registered in `di/Migrations.kt`; `room.schemaLocation` exports the versioned
 * JSON schema so CI can validate it. Version 1 is the initial release, so there
 * are no migrations yet — but the infrastructure and the test harness are in
 * place from day one.
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
        AppSettingEntity::class,
    ],
    version = 1,
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
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        const val NAME = "shooting.db"
    }
}
