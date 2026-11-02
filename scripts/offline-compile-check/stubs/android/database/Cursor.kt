package android.database
interface Cursor : java.io.Closeable {
    fun moveToNext(): Boolean
    fun getString(index: Int): String
    fun getColumnIndexOrThrow(name: String): Int
    override fun close()
}
