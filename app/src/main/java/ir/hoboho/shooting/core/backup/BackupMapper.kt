package ir.hoboho.shooting.core.backup

import ir.hoboho.shooting.domain.ExpenseCalculator
import ir.hoboho.shooting.domain.ShootingCalculator
import ir.hoboho.shooting.domain.Validator
import ir.hoboho.shooting.domain.model.AppSettings
import ir.hoboho.shooting.domain.model.CableType
import ir.hoboho.shooting.domain.model.CompanyPayment
import ir.hoboho.shooting.domain.model.Expense
import ir.hoboho.shooting.domain.model.ExpenseCategory
import ir.hoboho.shooting.domain.model.PointType
import ir.hoboho.shooting.domain.model.Project
import ir.hoboho.shooting.domain.model.Settlement
import ir.hoboho.shooting.domain.model.ShootingOperation
import ir.hoboho.shooting.domain.model.ThemeMode
import ir.hoboho.shooting.domain.model.Workday

/** Thrown when a backup cannot be mapped without losing data. */
class BackupIntegrityException(message: String) : IllegalArgumentException(message)

/**
 * Converts between domain data and the versioned backup payload.
 *
 * Local ids are replaced by stable uuids in the file and rebuilt deterministically
 * on import. Unresolvable references are an error, never a silently dropped row.
 */
object BackupMapper {

    fun toPayload(
        data: BackupData,
        appVersionName: String,
        appVersionCode: Int,
        exportedAtEpochMilli: Long,
        formatVersion: Int = BackupCodec.FORMAT_VERSION,
    ): BackupPayload {
        val projectUuid = data.projects.associate { it.id to it.uuid }
        val workdayUuid = data.workdays.associate { it.id to it.uuid }
        val expenseUuid = data.expenses.associate { it.id to it.uuid }
        val cableCore = data.cableTypes.associate { it.id to it.coreCount }
        val categoryName = data.categories.associate { it.id to it.name }
        return BackupPayload(
            formatVersion = formatVersion,
            appVersionName = appVersionName,
            appVersionCode = appVersionCode,
            exportedAtEpochMilli = exportedAtEpochMilli,
            settings = SettingsDto(
                themeMode = data.settings.themeMode.name,
                defaultProjectUuid = data.settings.defaultProjectId?.let { projectUuid[it] }.orEmpty(),
                lastBackupEpochMilli = data.settings.lastBackupEpochMilli,
            ),
            projects = data.projects.map {
                ProjectDto(it.uuid, it.name, it.workArea, it.employer, it.supervisor,
                    it.description, it.active, it.createdAt, it.updatedAt)
            },
            workdays = data.workdays.map {
                WorkdayDto(it.uuid, projectUuid[it.projectId].orEmpty(), it.dateEpochDay,
                    it.workArea, it.employer, it.supervisor, it.workerCount, it.notes,
                    it.totalLengthMeters, it.createdAt, it.updatedAt)
            },
            operations = data.operations.map {
                OperationDto(it.uuid, workdayUuid[it.workdayId].orEmpty(), it.startType.name,
                    it.startCode, it.startMeter, it.endType.name, it.endCode, it.endMeter,
                    cableCore[it.cableTypeId] ?: 0, it.lengthMeters, it.createdAt, it.updatedAt)
            },
            cableTypes = data.cableTypes.map { CableTypeDto(it.coreCount, it.label, it.active, it.sortOrder) },
            categories = data.categories.map { CategoryDto(it.name, it.active, it.sortOrder) },
            expenses = data.expenses.map {
                ExpenseDto(it.uuid, it.dateEpochDay, categoryName[it.categoryId].orEmpty(),
                    it.amountToman, it.description, it.receiptReference,
                    it.projectId?.let { id -> projectUuid[id] }.orEmpty(),
                    it.workdayId?.let { id -> workdayUuid[id] }.orEmpty(),
                    it.createdAt, it.updatedAt, it.companyApproved)
            },
            settlements = data.settlements.map {
                SettlementDto(it.uuid, expenseUuid[it.expenseId].orEmpty(), it.amountToman,
                    it.dateEpochDay, it.note, it.createdAt)
            },
            companyPayments = data.companyPayments.map {
                CompanyPaymentDto(it.uuid, it.amountToman, it.dateEpochDay,
                    it.receiptReference, it.note, it.createdAt)
            },
        )
    }

    /** Rebuilds domain rows with fresh sequential local ids. */
    fun toData(payload: BackupPayload): BackupData {
        val projectIds = payload.projects.mapIndexed { i, p -> p.uuid to (i + 1).toLong() }.toMap()
        val workdayIds = payload.workdays.mapIndexed { i, w -> w.uuid to (i + 1).toLong() }.toMap()
        val expenseIds = payload.expenses.mapIndexed { i, e -> e.uuid to (i + 1).toLong() }.toMap()
        val cableIds = payload.cableTypes.mapIndexed { i, c -> c.coreCount to (i + 1).toLong() }.toMap()
        val categoryIds = payload.categories.mapIndexed { i, c -> c.name to (i + 1).toLong() }.toMap()
        val projectNames = payload.projects.associate { it.uuid to it.name }

        fun resolve(map: Map<String, Long>, key: String, what: String, allowBlank: Boolean = false): Long? {
            if (key.isBlank()) {
                if (allowBlank) return null
                throw BackupIntegrityException("Missing $what reference")
            }
            return map[key] ?: throw BackupIntegrityException("Unknown $what reference: $key")
        }

        val projects = payload.projects.mapIndexed { i, p ->
            Project(i + 1L, p.uuid, p.name, p.workArea, p.employer, p.supervisor,
                p.description, p.active, p.createdAt, p.updatedAt)
        }
        val workdays = payload.workdays.mapIndexed { i, w ->
            Workday(
                id = i + 1L, uuid = w.uuid,
                projectId = resolve(projectIds, w.projectUuid, "project")!!,
                projectName = projectNames[w.projectUuid].orEmpty(),
                dateEpochDay = w.dateEpochDay, workArea = w.workArea, employer = w.employer,
                supervisor = w.supervisor, workerCount = w.workerCount, notes = w.notes,
                totalLengthMeters = w.totalLengthMeters, createdAt = w.createdAt, updatedAt = w.updatedAt,
            )
        }
        val operations = payload.operations.mapIndexed { i, o ->
            ShootingOperation(
                id = i + 1L, uuid = o.uuid,
                workdayId = resolve(workdayIds, o.workdayUuid, "workday")!!,
                startType = PointType.fromName(o.startType), startCode = o.startCode,
                startMeter = o.startMeter, endType = PointType.fromName(o.endType),
                endCode = o.endCode, endMeter = o.endMeter,
                cableTypeId = cableIds[o.cableTypeCoreCount]
                    ?: throw BackupIntegrityException("Unknown cable type: ${o.cableTypeCoreCount}"),
                // Never trust a stored calculation: recompute from the readings.
                lengthMeters = ShootingCalculator.lengthMeters(o.startMeter, o.endMeter),
                createdAt = o.createdAt, updatedAt = o.updatedAt,
            )
        }
        val expenses = payload.expenses.mapIndexed { i, e ->
            Expense(
                id = i + 1L, uuid = e.uuid, dateEpochDay = e.dateEpochDay,
                categoryId = categoryIds[e.categoryName]
                    ?: throw BackupIntegrityException("Unknown expense category: ${e.categoryName}"),
                categoryName = e.categoryName, amountToman = e.amountToman,
                description = e.description, receiptReference = e.receiptReference,
                projectId = resolve(projectIds, e.projectUuid, "project", allowBlank = true),
                projectName = projectNames[e.projectUuid],
                workdayId = resolve(workdayIds, e.workdayUuid, "workday", allowBlank = true),
                companyApproved = e.companyApproved,
                createdAt = e.createdAt, updatedAt = e.updatedAt,
            )
        }
        val settlements = payload.settlements.mapIndexed { i, s ->
            Settlement(
                id = i + 1L, uuid = s.uuid,
                expenseId = resolve(expenseIds, s.expenseUuid, "expense")!!,
                amountToman = s.amountToman, dateEpochDay = s.dateEpochDay,
                note = s.note, createdAt = s.createdAt,
            )
        }
        // Settled amounts are derived from settlements, never stored.
        val settledByExpense = settlements.groupBy { it.expenseId }
            .mapValues { (_, list) -> ExpenseCalculator.totalSettled(list) }
        val rebuiltExpenses = expenses.map { expense ->
            expense.copy(
                settledAmountToman = settledByExpense[expense.id] ?: 0L,
                settlementCount = payload.settlements.count { it.expenseUuid == expense.uuid },
            )
        }

        val companyPayments = payload.companyPayments.mapIndexed { i, payment ->
            CompanyPayment(
                id = i + 1L, uuid = payment.uuid, amountToman = payment.amountToman,
                dateEpochDay = payment.dateEpochDay, receiptReference = payment.receiptReference,
                note = payment.note, createdAt = payment.createdAt,
            )
        }
        val settings = AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(payload.settings.themeMode) }
                .getOrDefault(ThemeMode.SYSTEM),
            defaultProjectId = payload.settings.defaultProjectUuid.takeIf { it.isNotBlank() }
                ?.let { projectIds[it] },
            lastBackupEpochMilli = payload.settings.lastBackupEpochMilli,
        )
        return BackupData(
            settings = settings,
            projects = projects,
            workdays = workdays,
            operations = operations,
            cableTypes = payload.cableTypes.mapIndexed { i, c ->
                CableType(i + 1L, c.coreCount, c.label, c.active, c.sortOrder)
            },
            categories = payload.categories.mapIndexed { i, c ->
                ExpenseCategory(i + 1L, c.name, c.active, c.sortOrder)
            },
            expenses = rebuiltExpenses,
            settlements = settlements,
            companyPayments = companyPayments,
        )
    }

    /** Structural + business validation performed before any write. */
    fun validate(payload: BackupPayload): List<String> = buildList {
        if (payload.formatVersion != BackupCodec.FORMAT_VERSION) {
            add("نسخه قالب پشتیبانی نمی شود: ${payload.formatVersion}")
        }
        val projectUuids = payload.projects.map { it.uuid }.toSet()
        val workdayUuids = payload.workdays.map { it.uuid }.toSet()
        val expenseUuids = payload.expenses.map { it.uuid }.toSet()
        val operationUuids = payload.operations.map { it.uuid }.toSet()
        val settlementUuids = payload.settlements.map { it.uuid }.toSet()
        if (projectUuids.size != payload.projects.size) add("شناسه پروژه تکراری است")
        if (workdayUuids.size != payload.workdays.size) add("شناسه گزارش تکراری است")
        if (expenseUuids.size != payload.expenses.size) add("شناسه هزینه تکراری است")
        if (operationUuids.size != payload.operations.size) add("شناسه عملیات تکراری است")
        if (settlementUuids.size != payload.settlements.size) add("شناسه تسویه تکراری است")
        if (payload.projects.map { it.name }.toSet().size != payload.projects.size) add("نام پروژه در فایل پشتیبان تکراری است")
        if (payload.cableTypes.map { it.coreCount }.toSet().size != payload.cableTypes.size) add("تعداد کور کابل در فایل پشتیبان تکراری است")
        if (payload.categories.map { it.name }.toSet().size != payload.categories.size) add("نام دسته هزینه در فایل پشتیبان تکراری است")
        val workdayKeys = payload.workdays.map { "${it.projectUuid}:${it.dateEpochDay}" }
        if (workdayKeys.toSet().size != workdayKeys.size) add("برای یک پروژه و تاریخ، بیش از یک گزارش در فایل وجود دارد")
        payload.workdays.filter { it.projectUuid !in projectUuids }.forEach { add("گزارش بدون پروژه: ${it.uuid}") }
        payload.operations.filter { it.workdayUuid !in workdayUuids }.forEach { add("عملیات بدون گزارش: ${it.uuid}") }
        payload.expenses.filter { it.projectUuid.isNotBlank() && it.projectUuid !in projectUuids }
            .forEach { add("هزینه با پروژه نامعتبر: ${it.uuid}") }
        payload.expenses.filter { it.workdayUuid.isNotBlank() && it.workdayUuid !in workdayUuids }
            .forEach { add("هزینه با گزارش نامعتبر: ${it.uuid}") }
        payload.settlements.filter { it.expenseUuid !in expenseUuids }
            .forEach { add("تسویه بدون هزینه: ${it.uuid}") }
        val approvedExpenseUuids = payload.expenses.filter { it.companyApproved }.map { it.uuid }.toSet()
        payload.settlements.filter { it.expenseUuid !in approvedExpenseUuids }
            .forEach { add("تسویه برای هزینه تأییدنشده: ${it.uuid}") }
        payload.settlements.filter { it.amountToman <= 0 }.forEach { add("مبلغ تسویه نامعتبر: ${it.uuid}") }
        payload.expenses.filter { it.amountToman < 0 }.forEach { add("مبلغ هزینه نامعتبر: ${it.uuid}") }
        val settledByExpense = payload.settlements.groupBy { it.expenseUuid }
            .mapValues { (_, list) -> list.sumOf { s -> s.amountToman } }
        payload.expenses.filter { (settledByExpense[it.uuid] ?: 0L) > it.amountToman }
            .forEach { add("تسویه بیشتر از مبلغ هزینه: ${it.uuid}") }
        payload.operations.forEach { op ->
            if (op.startMeter < 0 || op.endMeter < 0) {
                add("کنتور نامعتبر: ${op.uuid}")
            } else if (op.lengthMeters != ShootingCalculator.lengthMeters(op.startMeter, op.endMeter)) {
                add("طول ذخیره شده عملیات ناسازگار است: ${op.uuid}")
            }
        }
        if (!Validator.expensesAreConsistent(toExpenseShapesOrEmpty(payload))) {
            add("داده های هزینه ناسازگار است")
        }
    }

    /** Lightweight shape used only for the consistency guard above. */
    private fun toExpenseShapesOrEmpty(payload: BackupPayload): List<Expense> {
        val settled = payload.settlements.groupBy { it.expenseUuid }
            .mapValues { (_, list) -> list.sumOf { s -> s.amountToman } }
        return payload.expenses.map {
            Expense(
                amountToman = it.amountToman,
                companyApproved = it.companyApproved,
                settledAmountToman = settled[it.uuid] ?: 0L,
                dateEpochDay = it.dateEpochDay,
                categoryId = 0L,
            )
        }
    }
}
