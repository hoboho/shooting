package ir.hoboho.shooting.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import ir.hoboho.shooting.di.Migrations
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the 5→6 data transform, not a full Room historical-schema upgrade. */
@RunWith(AndroidJUnit4::class)
class CashLedgerMigrationTest {
    @Test
    fun migration5to6_seedsReceiptsAndApprovedExpensesOnly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "ledger-migration-test.db"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE company_payments (id INTEGER PRIMARY KEY, uuid TEXT NOT NULL, amountToman INTEGER NOT NULL, dateEpochDay INTEGER NOT NULL, note TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE expenses (id INTEGER PRIMARY KEY, uuid TEXT NOT NULL, amountToman INTEGER NOT NULL, dateEpochDay INTEGER NOT NULL, description TEXT NOT NULL, createdAt INTEGER NOT NULL, companyApproved INTEGER NOT NULL)")
                        db.execSQL("INSERT INTO company_payments VALUES (1, 'p1', 500, 20000, 'receipt', 100)")
                        db.execSQL("INSERT INTO expenses VALUES (1, 'e1', 300, 20001, 'approved', 101, 1)")
                        db.execSQL("INSERT INTO expenses VALUES (2, 'e2', 200, 20002, 'pending', 102, 0)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build(),
        )
        try {
            val db = helper.writableDatabase
            Migrations.MIGRATION_5_6.migrate(db)
            // The seeding SQL is deliberately idempotent.
            Migrations.MIGRATION_5_6.migrate(db)
            db.query("SELECT idempotencyKey, originType, originId, amountToman FROM cash_ledger ORDER BY idempotencyKey").use { cursor ->
                assertThat(cursor.count).isEqualTo(2)
                assertThat(cursor.moveToNext()).isTrue()
                assertThat(cursor.getString(0)).isEqualTo("expense:e1")
                assertThat(cursor.getString(1)).isEqualTo("EXPENSE")
                assertThat(cursor.getLong(2)).isEqualTo(1L)
                assertThat(cursor.getLong(3)).isEqualTo(-300L)
                assertThat(cursor.moveToNext()).isTrue()
                assertThat(cursor.getString(0)).isEqualTo("receipt:p1")
                assertThat(cursor.getLong(3)).isEqualTo(500L)
            }
        } finally {
            helper.close()
            context.deleteDatabase(name)
        }
    }
}
