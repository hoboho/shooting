package ir.hoboho.shooting.data.mapper

import ir.hoboho.shooting.data.local.AppSettingEntity
import ir.hoboho.shooting.data.local.CableTypeEntity
import ir.hoboho.shooting.data.local.ExpenseCategoryEntity
import ir.hoboho.shooting.data.local.ExpenseEntity
import ir.hoboho.shooting.data.local.ExpenseRow
import ir.hoboho.shooting.data.local.OperationEntity
import ir.hoboho.shooting.data.local.ProjectEntity
import ir.hoboho.shooting.data.local.SettlementEntity
import ir.hoboho.shooting.data.local.WorkdayDetailRow
import ir.hoboho.shooting.data.local.WorkdayEntity
import ir.hoboho.shooting.data.local.WorkdayRow
import ir.hoboho.shooting.domain.ShootingCalculator
import ir.hoboho.shooting.domain.model.AppSettings
import ir.hoboho.shooting.domain.model.CableType
import ir.hoboho.shooting.domain.model.Expense
import ir.hoboho.shooting.domain.model.ExpenseCategory
import ir.hoboho.shooting.domain.model.PointType
import ir.hoboho.shooting.domain.model.Project
import ir.hoboho.shooting.domain.model.Settlement
import ir.hoboho.shooting.domain.model.ShootingOperation
import ir.hoboho.shooting.domain.model.ThemeMode
import ir.hoboho.shooting.domain.model.Workday

// ------------------------------------------------------------------ projects

fun ProjectEntity.toDomain() = Project(
    id, uuid, name, workArea, employer, supervisor, description, active, createdAt, updatedAt,
)

fun Project.toEntity() = ProjectEntity(
    id, uuid, name, workArea, employer, supervisor, description, active, createdAt, updatedAt,
)

// ------------------------------------------------------------------ workdays

fun WorkdayRow.toDomain() = Workday(
    id = workday.id,
    uuid = workday.uuid,
    projectId = workday.projectId,
    projectName = projectName,
    dateEpochDay = workday.dateEpochDay,
    workArea = workday.workArea,
    employer = workday.employer,
    supervisor = workday.supervisor,
    workerCount = workday.workerCount,
    notes = workday.notes,
    totalLengthMeters = workday.totalLengthMeters,
    createdAt = workday.createdAt,
    updatedAt = workday.updatedAt,
)

fun WorkdayDetailRow.toDomain() = Workday(
    id = workday.id,
    uuid = workday.uuid,
    projectId = workday.projectId,
    projectName = projectName,
    dateEpochDay = workday.dateEpochDay,
    workArea = workday.workArea,
    employer = workday.employer,
    supervisor = workday.supervisor,
    workerCount = workday.workerCount,
    notes = workday.notes,
    operations = operations.map(OperationEntity::toDomain),
    totalLengthMeters = workday.totalLengthMeters,
    createdAt = workday.createdAt,
    updatedAt = workday.updatedAt,
)

fun Workday.toEntity() = WorkdayEntity(
    id = id,
    uuid = uuid,
    projectId = projectId,
    dateEpochDay = dateEpochDay,
    workArea = workArea,
    employer = employer,
    supervisor = supervisor,
    workerCount = workerCount,
    notes = notes,
    totalLengthMeters = ShootingCalculator.dailyTotal(operations).takeIf { operations.isNotEmpty() }
        ?: totalLengthMeters,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ---------------------------------------------------------------- operations

fun OperationEntity.toDomain() = ShootingOperation(
    id = id,
    uuid = uuid,
    workdayId = workdayId,
    startType = PointType.fromName(startType),
    startCode = startCode,
    startMeter = startMeter,
    endType = PointType.fromName(endType),
    endCode = endCode,
    endMeter = endMeter,
    cableTypeId = cableTypeId,
    lengthMeters = lengthMeters,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ShootingOperation.toEntity() = OperationEntity(
    id = id,
    uuid = uuid,
    workdayId = workdayId,
    startType = startType.name,
    startCode = startCode,
    startMeter = startMeter,
    endType = endType.name,
    endCode = endCode,
    endMeter = endMeter,
    cableTypeId = cableTypeId,
    lengthMeters = lengthMeters,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ------------------------------------------------------------- reference data

fun CableTypeEntity.toDomain() = CableType(id, coreCount, label, active, sortOrder)
fun CableType.toEntity() = CableTypeEntity(id, coreCount, label, active, sortOrder)
fun ExpenseCategoryEntity.toDomain() = ExpenseCategory(id, name, active, sortOrder)
fun ExpenseCategory.toEntity() = ExpenseCategoryEntity(id, name, active, sortOrder)

// ------------------------------------------------------------------- expenses

fun ExpenseRow.toDomain() = Expense(
    id = expense.id,
    uuid = expense.uuid,
    dateEpochDay = expense.dateEpochDay,
    categoryId = expense.categoryId,
    categoryName = categoryName,
    amountToman = expense.amountToman,
    description = expense.description,
    receiptReference = expense.receiptReference,
    projectId = expense.projectId,
    projectName = projectName,
    workdayId = expense.workdayId,
    companyApproved = expense.companyApproved,
    settledAmountToman = settledAmountToman,
    settlementCount = settlementCount,
    createdAt = expense.createdAt,
    updatedAt = expense.updatedAt,
)

fun Expense.toEntity() = ExpenseEntity(
    id = id,
    uuid = uuid,
    dateEpochDay = dateEpochDay,
    categoryId = categoryId,
    amountToman = amountToman,
    description = description,
    receiptReference = receiptReference,
    projectId = projectId,
    workdayId = workdayId,
    companyApproved = companyApproved,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun SettlementEntity.toDomain() = Settlement(
    id, uuid, expenseId, amountToman, dateEpochDay, note, createdAt,
)

fun Settlement.toEntity() = SettlementEntity(
    id, uuid, expenseId, amountToman, dateEpochDay, note, createdAt,
)

// -------------------------------------------------------------------- settings

object SettingKeys {
    const val THEME_MODE = "theme_mode"
    const val DEFAULT_PROJECT_ID = "default_project_id"
    const val LAST_BACKUP_EPOCH_MILLI = "last_backup_epoch_milli"
}

fun List<AppSettingEntity>.toDomain(): AppSettings {
    val map = associate { it.key to it.value }
    return AppSettings(
        themeMode = runCatching { ThemeMode.valueOf(map[SettingKeys.THEME_MODE] ?: "SYSTEM") }
            .getOrDefault(ThemeMode.SYSTEM),
        defaultProjectId = map[SettingKeys.DEFAULT_PROJECT_ID]?.toLongOrNull(),
        lastBackupEpochMilli = map[SettingKeys.LAST_BACKUP_EPOCH_MILLI]?.toLongOrNull() ?: 0L,
    )
}

fun AppSettings.toEntities(): List<AppSettingEntity> = listOf(
    AppSettingEntity(SettingKeys.THEME_MODE, themeMode.name),
    AppSettingEntity(SettingKeys.DEFAULT_PROJECT_ID, defaultProjectId?.toString() ?: ""),
    AppSettingEntity(SettingKeys.LAST_BACKUP_EPOCH_MILLI, lastBackupEpochMilli.toString()),
)
