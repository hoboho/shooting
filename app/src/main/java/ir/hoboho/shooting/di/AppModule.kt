package ir.hoboho.shooting.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.hoboho.shooting.core.common.Clock
import ir.hoboho.shooting.core.common.SystemClock
import ir.hoboho.shooting.data.local.AppSettingDao
import ir.hoboho.shooting.data.local.CableTypeDao
import ir.hoboho.shooting.data.local.ExpenseCategoryDao
import ir.hoboho.shooting.data.local.ExpenseDao
import ir.hoboho.shooting.data.local.OperationDao
import ir.hoboho.shooting.data.local.ProjectDao
import ir.hoboho.shooting.data.local.SettlementDao
import ir.hoboho.shooting.data.local.ShootingDatabase
import ir.hoboho.shooting.data.local.WorkdayDao
import ir.hoboho.shooting.domain.model.ReferenceData
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = SystemClock

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ShootingDatabase =
        Room.databaseBuilder(context, ShootingDatabase::class.java, ShootingDatabase.NAME)
            .addMigrations(*Migrations.ALL)
            .addCallback(SeedCallback)
            .build()

    @Provides fun provideProjectDao(db: ShootingDatabase): ProjectDao = db.projectDao()
    @Provides fun provideWorkdayDao(db: ShootingDatabase): WorkdayDao = db.workdayDao()
    @Provides fun provideOperationDao(db: ShootingDatabase): OperationDao = db.operationDao()
    @Provides fun provideCableTypeDao(db: ShootingDatabase): CableTypeDao = db.cableTypeDao()
    @Provides fun provideCategoryDao(db: ShootingDatabase): ExpenseCategoryDao = db.expenseCategoryDao()
    @Provides fun provideExpenseDao(db: ShootingDatabase): ExpenseDao = db.expenseDao()
    @Provides fun provideSettlementDao(db: ShootingDatabase): SettlementDao = db.settlementDao()
    @Provides fun provideAppSettingDao(db: ShootingDatabase): AppSettingDao = db.appSettingDao()
}

/**
 * Seeds the two manageable reference tables on first install. Extra cable core
 * counts and expense categories are ordinary rows added later by the user, so
 * no schema change is needed to extend them.
 */
object SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        ReferenceData.DEFAULT_CABLE_CORES.forEachIndexed { index, core ->
            db.execSQL(
                "INSERT INTO cable_types (coreCount, label, active, sortOrder) VALUES (?, ?, 1, ?)",
                arrayOf<Any>(core, ReferenceData.cableLabel(core), index),
            )
        }
        ReferenceData.DEFAULT_EXPENSE_CATEGORIES.forEachIndexed { index, name ->
            db.execSQL(
                "INSERT INTO expense_categories (name, active, sortOrder) VALUES (?, 1, ?)",
                arrayOf<Any>(name, index),
            )
        }
    }
}
