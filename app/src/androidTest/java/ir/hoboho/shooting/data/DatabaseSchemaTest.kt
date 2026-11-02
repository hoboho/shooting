package ir.hoboho.shooting.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import ir.hoboho.shooting.data.local.ShootingDatabase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the exported version-1 schema shape on a real SQLite database.
 * Version 1 has no migrations to replay; from version 2 onwards every change
 * must add a `MigrationTestHelper` case using the committed schema JSON.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseSchemaTest {

    private lateinit var db: ShootingDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ShootingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        // Force creation so PRAGMA queries see the real tables.
        db.openHelper.writableDatabase.query("SELECT 1").close()
    }

    @After
    fun tearDown() = db.close()

    private fun sqlite() = db.openHelper.writableDatabase

    private fun tableNames(): Set<String> {
        val cursor = sqlite().query(
            "SELECT name FROM sqlite_master WHERE type='table' " +
                "AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%' " +
                "AND name != 'android_metadata'",
        )
        return cursor.use {
            val names = LinkedHashSet<String>()
            val index = it.getColumnIndexOrThrow("name")
            while (it.moveToNext()) names += it.getString(index)
            names
        }
    }

    private fun columnNames(table: String): Set<String> {
        val cursor = sqlite().query("PRAGMA table_info(`$table`)")
        return cursor.use {
            val names = LinkedHashSet<String>()
            val index = it.getColumnIndexOrThrow("name")
            while (it.moveToNext()) names += it.getString(index)
            names
        }
    }

    @Test
    fun version1_createsEveryTable() {
        assertThat(db.openHelper.writableDatabase.version).isEqualTo(1)
        assertThat(tableNames()).containsExactly(
            "projects", "workdays", "operations", "cable_types",
            "expense_categories", "expenses", "settlements", "app_settings",
        )
    }

    @Test
    fun moneyAndDates_areStoredAsIntegers() {
        assertThat(columnNames("expenses")).containsAtLeast(
            "dateEpochDay", "amountToman", "categoryId", "projectId", "workdayId",
        )
        assertThat(columnNames("settlements")).containsAtLeast("amountToman", "dateEpochDay")
        assertThat(columnNames("operations")).containsAtLeast(
            "startMeter", "endMeter", "lengthMeters", "cableTypeId",
        )
    }

    @Test
    fun uniqueIndex_preventsAccidentalDuplicateProjectDate() {
        sqlite().execSQL(
            "INSERT INTO projects (uuid, name, workArea, employer, supervisor, description, " +
                "active, createdAt, updatedAt) VALUES ('p1', 'پروژه', '', '', '', '', 1, 0, 0)",
        )
        val workdayValues = "(uuid, projectId, dateEpochDay, workArea, employer, supervisor, " +
            "workerCount, notes, totalLengthMeters, createdAt, updatedAt) VALUES "
        sqlite().execSQL(
            "INSERT INTO workdays $workdayValues ('w1', 1, 20000, '', '', '', 0, '', 0, 0, 0)",
        )
        var rejected = false
        try {
            sqlite().execSQL(
                "INSERT INTO workdays $workdayValues ('w2', 1, 20000, '', '', '', 0, '', 0, 0, 0)",
            )
        } catch (e: SQLiteConstraintException) {
            rejected = true
        }
        assertThat(rejected).isTrue()
    }

    @Test
    fun foreignKeys_areEnforced() {
        sqlite().execSQL("PRAGMA foreign_keys=ON")
        // A valid cable type exists, so the only violated FK is workdayId.
        sqlite().execSQL(
            "INSERT INTO cable_types (coreCount, label, active, sortOrder) VALUES (24, '۲۴ کور', 1, 0)",
        )
        var rejected = false
        try {
            sqlite().execSQL(
                "INSERT INTO operations (uuid, workdayId, startType, startCode, startMeter, " +
                    "endType, endCode, endMeter, cableTypeId, lengthMeters, createdAt, updatedAt) " +
                    "VALUES ('o1', 999, 'ODC', 'A', 0, 'FAT', 'B', 10, 1, 10, 0, 0)",
            )
        } catch (e: SQLiteConstraintException) {
            rejected = true
        }
        assertThat(rejected).isTrue()
    }
}
