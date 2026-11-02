package ir.hoboho.shooting.domain.model

/**
 * Domain models. Plain Kotlin, no Android or Room types: the same objects are
 * used by the UI, the report builders, the backup mapper and the unit tests.
 */

/** Selectable start/end point types. */
enum class PointType(val label: String) {
    ODC("ODC"),
    FAT("FAT"),
    HANDHOLE("حوضچه"),
    MANHOLE("منهول"),
    ;

    companion object {
        fun fromName(name: String): PointType =
            entries.firstOrNull { it.name == name } ?: ODC
    }
}

data class Project(
    val id: Long = 0L,
    /** Stable identity across installs; used by backup/restore matching. */
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

/**
 * A manageable cable type. New core counts can be added at runtime without any
 * schema change — operations reference [id], never a hard-coded enum.
 */
data class CableType(
    val id: Long = 0L,
    val coreCount: Int,
    val label: String,
    val active: Boolean = true,
    val sortOrder: Int = 0,
)

data class ExpenseCategory(
    val id: Long = 0L,
    val name: String,
    val active: Boolean = true,
    val sortOrder: Int = 0,
)

/**
 * One shooting operation inside a workday.
 *
 * [lengthMeters] is always `abs(endMeter - startMeter)`. It is stored so a saved
 * report is stable even if the user later changes a meter reading on a draft,
 * and is recomputed by the repository on every write.
 */
data class ShootingOperation(
    val id: Long = 0L,
    val uuid: String = "",
    val workdayId: Long = 0L,
    val startType: PointType = PointType.ODC,
    val startCode: String = "",
    val startMeter: Int = 0,
    val endType: PointType = PointType.FAT,
    val endCode: String = "",
    val endMeter: Int = 0,
    val cableTypeId: Long = 0L,
    val lengthMeters: Int = kotlin.math.abs(endMeter.toLong() - startMeter.toLong()).toInt(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

/** A daily shooting report. [totalLengthMeters] is the sum of its operations. */
data class Workday(
    val id: Long = 0L,
    val uuid: String = "",
    val projectId: Long,
    val projectName: String = "",
    val dateEpochDay: Long,
    val workArea: String = "",
    val employer: String = "",
    val supervisor: String = "",
    val workerCount: Int = 0,
    val notes: String = "",
    val operations: List<ShootingOperation> = emptyList(),
    val totalLengthMeters: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

enum class SettlementStatus { UNSETTLED, PARTIAL, SETTLED }

/**
 * An expense paid on behalf of the employer/company. It is a company receivable,
 * never a personal expense and never a salary.
 */
data class Expense(
    val id: Long = 0L,
    val uuid: String = "",
    val dateEpochDay: Long,
    val categoryId: Long,
    val categoryName: String = "",
    val amountToman: Long,
    val description: String = "",
    val receiptReference: String = "",
    val projectId: Long? = null,
    val projectName: String? = null,
    val workdayId: Long? = null,
    val settledAmountToman: Long = 0L,
    val settlementCount: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val outstandingToman: Long
        get() = ir.hoboho.shooting.domain.ExpenseCalculator.outstanding(amountToman, settledAmountToman)

    val status: SettlementStatus
        get() = ir.hoboho.shooting.domain.ExpenseCalculator.status(amountToman, settledAmountToman)
}

/** A single reimbursement against an expense; multiple settlements are allowed. */
data class Settlement(
    val id: Long = 0L,
    val uuid: String = "",
    val expenseId: Long,
    val amountToman: Long,
    val dateEpochDay: Long,
    val note: String = "",
    val createdAt: Long = 0L,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** User configuration that is included in backups. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultProjectId: Long? = null,
    val lastBackupEpochMilli: Long = 0L,
)

/** Seed values for the two manageable reference tables. */
object ReferenceData {
    val DEFAULT_CABLE_CORES: List<Int> = listOf(6, 12, 24, 48, 96, 144)

    val DEFAULT_EXPENSE_CATEGORIES: List<String> = listOf(
        "سوخت خودرو",
        "سوخت دستگاه",
        "مخارج خودرو",
        "مخارج دستگاه",
        "اقلام مصرفی",
        "خورد و خوراک",
        "هزینههای بدون فاکتور",
    )

    /** "۲۴ کور" */
    fun cableLabel(coreCount: Int): String =
        "${ir.hoboho.shooting.core.format.PersianDigits.toPersian(coreCount.toString())} کور"
}
