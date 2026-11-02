package androidx.room
import kotlin.reflect.KClass

annotation class Entity(
    val tableName: String = "",
    val indices: Array<Index> = [],
    val foreignKeys: Array<ForeignKey> = [],
    val primaryKeys: Array<String> = [],
)
annotation class Index(val value: Array<String>, val unique: Boolean = false)
annotation class ForeignKey(
    val entity: KClass<*>,
    val parentColumns: Array<String>,
    val childColumns: Array<String>,
    val onDelete: Int = 0,
) {
    companion object {
        const val CASCADE = 1
        const val RESTRICT = 2
        const val SET_NULL = 3
    }
}
annotation class PrimaryKey(val autoGenerate: Boolean = false)
annotation class Dao
annotation class Query(val value: String)
annotation class Insert(val onConflict: Int = 1)
annotation class Upsert
annotation class Delete
annotation class Transaction
annotation class Embedded(val prefix: String = "")
annotation class Relation(val parentColumn: String, val entityColumn: String, val entity: KClass<*> = Any::class)
annotation class Database(
    val entities: Array<KClass<*>>,
    val version: Int,
    val exportSchema: Boolean = true,
)
