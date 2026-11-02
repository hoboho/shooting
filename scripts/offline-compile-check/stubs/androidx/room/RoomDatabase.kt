package androidx.room
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase

abstract class RoomDatabase {
    abstract class Callback {
        open fun onCreate(db: SupportSQLiteDatabase) {}
        open fun onOpen(db: SupportSQLiteDatabase) {}
    }
    open class Builder<T : RoomDatabase> {
        fun addMigrations(vararg migrations: androidx.room.migration.Migration): Builder<T> = this
        fun addCallback(callback: Callback): Builder<T> = this
        fun allowMainThreadQueries(): Builder<T> = this
        fun build(): T = throw UnsupportedOperationException()
    }
}
object Room {
    fun <T : RoomDatabase> databaseBuilder(context: Context, klass: Class<T>, name: String): RoomDatabase.Builder<T> =
        RoomDatabase.Builder()
    fun <T : RoomDatabase> inMemoryDatabaseBuilder(context: Context, klass: Class<T>): RoomDatabase.Builder<T> =
        RoomDatabase.Builder()
}
suspend fun <R> RoomDatabase.withTransaction(block: suspend () -> R): R = block()
