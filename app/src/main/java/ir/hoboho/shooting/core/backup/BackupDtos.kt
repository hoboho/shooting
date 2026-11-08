package ir.hoboho.shooting.core.backup

import ir.hoboho.shooting.domain.model.AppSettings
import ir.hoboho.shooting.domain.model.CableType
import ir.hoboho.shooting.domain.model.CompanyPayment
import ir.hoboho.shooting.domain.model.Expense
import ir.hoboho.shooting.domain.model.ExpenseCategory
import ir.hoboho.shooting.domain.model.Project
import ir.hoboho.shooting.domain.model.Settlement
import ir.hoboho.shooting.domain.model.ShootingOperation
import ir.hoboho.shooting.domain.model.ThemeMode
import ir.hoboho.shooting.domain.model.Workday

/**
 * Fallbacks only: real v1 backups always carry their app version fields. These
 * two constants describe the first released format, not the current app version
 * (which comes from AppInfo / gradle.properties).
 */
private const val LEGACY_APP_VERSION_NAME = "1.0.0"
private const val LEGACY_APP_VERSION_CODE = 1

/**
 * Versioned on-disk shape of a Shooting backup.
 *
 * Local database ids are never trusted across installations. Every row carries a
 * stable `uuid`; relationships in the file are expressed by those uuids, and
 * [BackupMapper] resolves them back to local ids on restore.
 */
data class BackupPayload(
    val formatVersion: Int = BackupCodec.FORMAT_VERSION,
    val appVersionName: String = LEGACY_APP_VERSION_NAME,
    val appVersionCode: Int = LEGACY_APP_VERSION_CODE,
    val exportedAtEpochMilli: Long,
    val settings: SettingsDto,
    val projects: List<ProjectDto> = emptyList(),
    val workdays: List<WorkdayDto> = emptyList(),
    val operations: List<OperationDto> = emptyList(),
    val cableTypes: List<CableTypeDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val expenses: List<ExpenseDto> = emptyList(),
    val settlements: List<SettlementDto> = emptyList(),
    val companyPayments: List<CompanyPaymentDto> = emptyList(),
)

data class SettingsDto(
    val themeMode: String = ThemeMode.SYSTEM.name,
    val defaultProjectUuid: String = "",
    val lastBackupEpochMilli: Long = 0L,
)

data class ProjectDto(
    val uuid: String,
    val name: String,
    val workArea: String,
    val employer: String,
    val supervisor: String,
    val description: String,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

data class WorkdayDto(
    val uuid: String,
    val projectUuid: String,
    val dateEpochDay: Long,
    val workArea: String,
    val employer: String,
    val supervisor: String,
    val workerCount: Int,
    val notes: String,
    val totalLengthMeters: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

data class OperationDto(
    val uuid: String,
    val workdayUuid: String,
    val startType: String,
    val startCode: String,
    val startMeter: Int,
    val endType: String,
    val endCode: String,
    val endMeter: Int,
    val cableTypeCoreCount: Int,
    val lengthMeters: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

data class CableTypeDto(val coreCount: Int, val label: String, val active: Boolean, val sortOrder: Int)
data class CategoryDto(val name: String, val active: Boolean, val sortOrder: Int)

data class ExpenseDto(
    val uuid: String,
    val dateEpochDay: Long,
    val categoryName: String,
    val amountToman: Long,
    val description: String,
    val receiptReference: String,
    val projectUuid: String = "",
    val workdayUuid: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val companyApproved: Boolean = true,
)

data class CompanyPaymentDto(
    val uuid: String,
    val amountToman: Long,
    val dateEpochDay: Long,
    val receiptReference: String,
    val note: String,
    val createdAt: Long,
)

data class SettlementDto(
    val uuid: String,
    val expenseUuid: String,
    val amountToman: Long,
    val dateEpochDay: Long,
    val note: String,
    val createdAt: Long,
)

/** Everything in the local database that a backup must cover. */
data class BackupData(
    val settings: AppSettings = AppSettings(),
    val projects: List<Project> = emptyList(),
    val workdays: List<Workday> = emptyList(),
    val operations: List<ShootingOperation> = emptyList(),
    val cableTypes: List<CableType> = emptyList(),
    val categories: List<ExpenseCategory> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val settlements: List<Settlement> = emptyList(),
    val companyPayments: List<CompanyPayment> = emptyList(),
)

data class BackupCounts(
    val projects: Int = 0,
    val workdays: Int = 0,
    val operations: Int = 0,
    val cableTypes: Int = 0,
    val categories: Int = 0,
    val expenses: Int = 0,
    val settlements: Int = 0,
    val companyPayments: Int = 0,
) {
    val total: Int get() = projects + workdays + operations + cableTypes + categories + expenses + settlements + companyPayments

    companion object {
        fun from(payload: BackupPayload) = BackupCounts(
            projects = payload.projects.size,
            workdays = payload.workdays.size,
            operations = payload.operations.size,
            cableTypes = payload.cableTypes.size,
            categories = payload.categories.size,
            expenses = payload.expenses.size,
            settlements = payload.settlements.size,
            companyPayments = payload.companyPayments.size,
        )

        fun from(data: BackupData) = BackupCounts(
            projects = data.projects.size,
            workdays = data.workdays.size,
            operations = data.operations.size,
            cableTypes = data.cableTypes.size,
            categories = data.categories.size,
            expenses = data.expenses.size,
            settlements = data.settlements.size,
            companyPayments = data.companyPayments.size,
        )
    }
}
