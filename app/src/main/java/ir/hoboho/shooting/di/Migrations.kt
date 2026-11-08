package ir.hoboho.shooting.di

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Additive migration; old expenses remain approved to preserve existing receivables. */
object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE expenses ADD COLUMN companyApproved INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE expenses SET companyApproved = 1")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
