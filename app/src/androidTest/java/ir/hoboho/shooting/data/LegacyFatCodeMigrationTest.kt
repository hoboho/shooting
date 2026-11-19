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

/** Verifies that v3's UI-injected ODC prefix is removed without touching unrelated codes. */
@RunWith(AndroidJUnit4::class)
class LegacyFatCodeMigrationTest {
    @Test
    fun migration_removesOnlyTheInjectedOdcPrefixFromFatCodes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "legacy-fat-migration-test.db"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE operations (id INTEGER PRIMARY KEY, startType TEXT NOT NULL, " +
                                "startCode TEXT NOT NULL, endType TEXT NOT NULL, endCode TEXT NOT NULL)",
                        )
                        db.execSQL("INSERT INTO operations VALUES (1, 'ODC', '60903', 'FAT', '609036090303')")
                        db.execSQL("INSERT INTO operations VALUES (2, 'ODC', '60904', 'FAT', '60904')")
                        db.execSQL("INSERT INTO operations VALUES (3, 'HH', '6018', 'FAT', '6090303')")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build(),
        )
        try {
            val db = helper.writableDatabase
            Migrations.MIGRATION_3_4.migrate(db)
            val cursor = db.query("SELECT id, endCode FROM operations ORDER BY id")
            cursor.use {
                assertThat(it.moveToNext()).isTrue()
                assertThat(it.getString(1)).isEqualTo("6090303")
                assertThat(it.moveToNext()).isTrue()
                assertThat(it.getString(1)).isEmpty()
                assertThat(it.moveToNext()).isTrue()
                assertThat(it.getString(1)).isEqualTo("6090303")
            }
        } finally {
            helper.close()
            context.deleteDatabase(name)
        }
    }
}
