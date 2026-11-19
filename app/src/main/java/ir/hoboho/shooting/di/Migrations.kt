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

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS company_payments (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uuid TEXT NOT NULL, amountToman INTEGER NOT NULL, dateEpochDay INTEGER NOT NULL, receiptReference TEXT NOT NULL, note TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_company_payments_uuid ON company_payments(uuid)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_company_payments_dateEpochDay ON company_payments(dateEpochDay)")
        }
    }

    /**
     * Older editor builds stored ODC + the entered FAT code in one field.
     * Recover the exact user-entered FAT value by removing that injected prefix.
     * Equality is included: an empty FAT input was previously saved as the ODC.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """UPDATE operations
                   SET endCode = substr(endCode, length(startCode) + 1)
                   WHERE startType = 'ODC'
                     AND endType = 'FAT'
                     AND length(startCode) > 0
                     AND substr(endCode, 1, length(startCode)) = startCode""",
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}
