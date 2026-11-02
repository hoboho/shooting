package androidx.sqlite.db
interface SupportSQLiteDatabase {
    fun execSQL(sql: String)
    fun execSQL(sql: String, bindArgs: Array<Any>)
    var version: Int
    fun query(sql: String): android.database.Cursor
}
