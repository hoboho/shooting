package ir.hoboho.shooting.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** All tables store Gregorian epoch days and integer Toman; no floats, no date strings. */
@Entity(
    tableName = "projects",
    indices = [Index(value = ["uuid"], unique = true), Index(value = ["name"], unique = true)],
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val uuid: String = "",
    val name: String,
    val workArea: String = "",
    val employer: String = "",
    val supervisor: String = "",
    val description: String = "",
    val active: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

@Entity(
    tableName = "workdays",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["projectId"]),
        // DB-level second line of defence against duplicate daily reports.
        Index(value = ["projectId", "dateEpochDay"], unique = true),
    ],
)
data class WorkdayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val uuid: String = "",
    val projectId: Long,
    val dateEpochDay: Long,
    val workArea: String = "",
    val employer: String = "",
    val supervisor: String = "",
    val workerCount: Int = 0,
    val notes: String = "",
    /** Cached sum of operation lengths; always recomputed in one transaction. */
    val totalLengthMeters: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

@Entity(tableName = "cable_types", indices = [Index(value = ["coreCount"], unique = true)])
data class CableTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val coreCount: Int,
    val label: String,
    val active: Boolean = true,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "operations",
    foreignKeys = [
        ForeignKey(
            entity = WorkdayEntity::class,
            parentColumns = ["id"],
            childColumns = ["workdayId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CableTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["cableTypeId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["workdayId"]),
        Index(value = ["cableTypeId"]),
    ],
)
data class OperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val uuid: String = "",
    val workdayId: Long,
    val startType: String,
    val startCode: String,
    val startMeter: Int,
    val endType: String,
    val endCode: String,
    val endMeter: Int,
    val cableTypeId: Long,
    /** abs(endMeter - startMeter); never entered by the user. */
    val lengthMeters: Int,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

@Entity(tableName = "expense_categories", indices = [Index(value = ["name"], unique = true)])
data class ExpenseCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val active: Boolean = true,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = WorkdayEntity::class,
            parentColumns = ["id"],
            childColumns = ["workdayId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["dateEpochDay"]),
        Index(value = ["categoryId"]),
        Index(value = ["projectId"]),
        Index(value = ["workdayId"]),
    ],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val uuid: String = "",
    val dateEpochDay: Long,
    val categoryId: Long,
    /** Integer Toman. Never a floating-point value. */
    val amountToman: Long,
    val description: String = "",
    val receiptReference: String = "",
    val projectId: Long? = null,
    val workdayId: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val companyApproved: Boolean = false,
)

@Entity(
    tableName = "settlements",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["uuid"], unique = true), Index(value = ["expenseId"])],
)
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val uuid: String = "",
    val expenseId: Long,
    val amountToman: Long,
    val dateEpochDay: Long,
    val note: String = "",
    val createdAt: Long = 0L,
)

@Entity(tableName = "company_payments", indices = [Index(value = ["uuid"], unique = true), Index(value = ["dateEpochDay"])])
data class CompanyPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val uuid: String = "",
    val amountToman: Long,
    val dateEpochDay: Long,
    val receiptReference: String = "",
    val note: String = "",
    val createdAt: Long = 0L,
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String,
)
