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

    /** Permit multiple independent reports for one project/date (e.g. separate work areas). */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP INDEX IF EXISTS index_workdays_projectId_dateEpochDay")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS cash_ledger (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL, idempotencyKey TEXT NOT NULL, originType TEXT NOT NULL,
                originId INTEGER NOT NULL, amountToman INTEGER NOT NULL, dateEpochDay INTEGER NOT NULL,
                description TEXT NOT NULL, createdAt INTEGER NOT NULL, reversalOfEntryId INTEGER
            )""")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_cash_ledger_idempotencyKey ON cash_ledger(idempotencyKey)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_cash_ledger_originType_originId ON cash_ledger(originType, originId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_cash_ledger_dateEpochDay ON cash_ledger(dateEpochDay)")
            db.execSQL("""INSERT OR IGNORE INTO cash_ledger (uuid,idempotencyKey,originType,originId,amountToman,dateEpochDay,description,createdAt,reversalOfEntryId)
                SELECT lower(hex(randomblob(16))), 'receipt:' || uuid, 'RECEIPT', id, amountToman, dateEpochDay, note, createdAt, NULL FROM company_payments""")
            db.execSQL("""INSERT OR IGNORE INTO cash_ledger (uuid,idempotencyKey,originType,originId,amountToman,dateEpochDay,description,createdAt,reversalOfEntryId)
                SELECT lower(hex(randomblob(16))), 'expense:' || uuid, 'EXPENSE', id, -amountToman, dateEpochDay, description, createdAt, NULL FROM expenses WHERE companyApproved = 1""")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
}
