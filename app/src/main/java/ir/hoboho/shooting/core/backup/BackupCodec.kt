package ir.hoboho.shooting.core.backup

import java.security.MessageDigest

/** Result of reading a backup file, with explicit, user-facing failure reasons. */
sealed interface BackupReadResult {
    data class Success(val payload: BackupPayload, val formatVersion: Int) : BackupReadResult
    data object Malformed : BackupReadResult
    data object WrongMagic : BackupReadResult
    data object ChecksumMismatch : BackupReadResult
    data class UnsupportedVersion(val found: Int) : BackupReadResult
}

/**
 * Envelope + integrity validation for backups.
 *
 * A file is a JSON object with a magic marker, a format version, a SHA-256
 * checksum over the *exact* payload string, and the payload itself. Corrupt or
 * tampered files are rejected before a single row is written.
 */
object BackupCodec {

    const val MAGIC = "SHOOTING-BACKUP"
    const val FORMAT_VERSION = 1

    fun encode(payload: BackupPayload): String {
        val canonicalPayload = SimpleJson.stringify(payloadToJson(payload))
        val checksum = sha256Hex(canonicalPayload)
        val envelope = JsonValue.Obj(
            linkedMapOf(
                "magic" to JsonValue.Str(MAGIC),
                "formatVersion" to JsonValue.Num(payload.formatVersion.toString()),
                "sha256" to JsonValue.Str(checksum),
                "payload" to payloadToJson(payload),
            ),
        )
        return SimpleJson.stringify(envelope)
    }

    fun decode(text: String): BackupReadResult {
        val root = try {
            SimpleJson.parse(text).asObj()
        } catch (t: Throwable) {
            return BackupReadResult.Malformed
        } ?: return BackupReadResult.Malformed

        if (root.string("magic") != MAGIC) return BackupReadResult.WrongMagic
        val version = root.int("formatVersion")
        if (version > FORMAT_VERSION) return BackupReadResult.UnsupportedVersion(version)
        if (version < 1) return BackupReadResult.UnsupportedVersion(version)

        val payloadNode = root.field("payload").asObj() ?: return BackupReadResult.Malformed
        val checksum = root.string("sha256")
        if (checksum.isBlank() || checksum != sha256Hex(SimpleJson.stringify(payloadNode))) {
            return BackupReadResult.ChecksumMismatch
        }
        return try {
            BackupReadResult.Success(payloadFromJson(payloadNode), version)
        } catch (t: Throwable) {
            BackupReadResult.Malformed
        }
    }

    fun sha256Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    // -------------------------------------------------------------- conversion

    private fun objOf(vararg pairs: Pair<String, JsonValue>): JsonValue.Obj =
        JsonValue.Obj(linkedMapOf(*pairs))

    private fun arrOf(items: List<JsonValue>): JsonValue.Arr = JsonValue.Arr(items)

    private fun payloadToJson(p: BackupPayload): JsonValue.Obj = objOf(
        "formatVersion" to JsonValue.Num(p.formatVersion.toString()),
        "appVersionName" to JsonValue.Str(p.appVersionName),
        "appVersionCode" to JsonValue.Num(p.appVersionCode.toString()),
        "exportedAtEpochMilli" to JsonValue.Num(p.exportedAtEpochMilli.toString()),
        "settings" to settingsToJson(p.settings),
        "projects" to arrOf(p.projects.map(::projectToJson)),
        "workdays" to arrOf(p.workdays.map(::workdayToJson)),
        "operations" to arrOf(p.operations.map(::operationToJson)),
        "cableTypes" to arrOf(p.cableTypes.map(::cableTypeToJson)),
        "categories" to arrOf(p.categories.map(::categoryToJson)),
        "expenses" to arrOf(p.expenses.map(::expenseToJson)),
        "settlements" to arrOf(p.settlements.map(::settlementToJson)),
    )

    private fun settingsToJson(s: SettingsDto) = objOf(
        "themeMode" to JsonValue.Str(s.themeMode),
        "defaultProjectUuid" to JsonValue.Str(s.defaultProjectUuid),
        "lastBackupEpochMilli" to JsonValue.Num(s.lastBackupEpochMilli.toString()),
    )

    private fun projectToJson(p: ProjectDto) = objOf(
        "uuid" to JsonValue.Str(p.uuid), "name" to JsonValue.Str(p.name),
        "workArea" to JsonValue.Str(p.workArea), "employer" to JsonValue.Str(p.employer),
        "supervisor" to JsonValue.Str(p.supervisor), "description" to JsonValue.Str(p.description),
        "active" to JsonValue.Bool(p.active), "createdAt" to JsonValue.Num(p.createdAt.toString()),
        "updatedAt" to JsonValue.Num(p.updatedAt.toString()),
    )

    private fun workdayToJson(w: WorkdayDto) = objOf(
        "uuid" to JsonValue.Str(w.uuid), "projectUuid" to JsonValue.Str(w.projectUuid),
        "dateEpochDay" to JsonValue.Num(w.dateEpochDay.toString()),
        "workArea" to JsonValue.Str(w.workArea), "employer" to JsonValue.Str(w.employer),
        "supervisor" to JsonValue.Str(w.supervisor),
        "workerCount" to JsonValue.Num(w.workerCount.toString()),
        "notes" to JsonValue.Str(w.notes),
        "totalLengthMeters" to JsonValue.Num(w.totalLengthMeters.toString()),
        "createdAt" to JsonValue.Num(w.createdAt.toString()),
        "updatedAt" to JsonValue.Num(w.updatedAt.toString()),
    )

    private fun operationToJson(o: OperationDto) = objOf(
        "uuid" to JsonValue.Str(o.uuid), "workdayUuid" to JsonValue.Str(o.workdayUuid),
        "startType" to JsonValue.Str(o.startType), "startCode" to JsonValue.Str(o.startCode),
        "startMeter" to JsonValue.Num(o.startMeter.toString()),
        "endType" to JsonValue.Str(o.endType), "endCode" to JsonValue.Str(o.endCode),
        "endMeter" to JsonValue.Num(o.endMeter.toString()),
        "cableTypeCoreCount" to JsonValue.Num(o.cableTypeCoreCount.toString()),
        "lengthMeters" to JsonValue.Num(o.lengthMeters.toString()),
        "createdAt" to JsonValue.Num(o.createdAt.toString()),
        "updatedAt" to JsonValue.Num(o.updatedAt.toString()),
    )

    private fun cableTypeToJson(c: CableTypeDto) = objOf(
        "coreCount" to JsonValue.Num(c.coreCount.toString()), "label" to JsonValue.Str(c.label),
        "active" to JsonValue.Bool(c.active), "sortOrder" to JsonValue.Num(c.sortOrder.toString()),
    )

    private fun categoryToJson(c: CategoryDto) = objOf(
        "name" to JsonValue.Str(c.name), "active" to JsonValue.Bool(c.active),
        "sortOrder" to JsonValue.Num(c.sortOrder.toString()),
    )

    private fun expenseToJson(e: ExpenseDto) = objOf(
        "uuid" to JsonValue.Str(e.uuid), "dateEpochDay" to JsonValue.Num(e.dateEpochDay.toString()),
        "categoryName" to JsonValue.Str(e.categoryName),
        "amountToman" to JsonValue.Num(e.amountToman.toString()),
        "description" to JsonValue.Str(e.description), "receiptReference" to JsonValue.Str(e.receiptReference),
        "projectUuid" to JsonValue.Str(e.projectUuid), "workdayUuid" to JsonValue.Str(e.workdayUuid),
        "createdAt" to JsonValue.Num(e.createdAt.toString()),
        "updatedAt" to JsonValue.Num(e.updatedAt.toString()),
    )

    private fun settlementToJson(s: SettlementDto) = objOf(
        "uuid" to JsonValue.Str(s.uuid), "expenseUuid" to JsonValue.Str(s.expenseUuid),
        "amountToman" to JsonValue.Num(s.amountToman.toString()),
        "dateEpochDay" to JsonValue.Num(s.dateEpochDay.toString()),
        "note" to JsonValue.Str(s.note), "createdAt" to JsonValue.Num(s.createdAt.toString()),
    )

    private fun payloadFromJson(o: JsonValue.Obj): BackupPayload = BackupPayload(
        formatVersion = o.int("formatVersion"),
        appVersionName = o.string("appVersionName"),
        appVersionCode = o.int("appVersionCode"),
        exportedAtEpochMilli = o.long("exportedAtEpochMilli"),
        settings = o.field("settings").asObj()?.let {
            SettingsDto(it.string("themeMode"), it.string("defaultProjectUuid"), it.long("lastBackupEpochMilli"))
        } ?: SettingsDto(),
        projects = o.field("projects").asArr()?.values?.map { v ->
            val x = v.asObj()!!
            ProjectDto(x.string("uuid"), x.string("name"), x.string("workArea"), x.string("employer"),
                x.string("supervisor"), x.string("description"), x.boolean("active"), x.long("createdAt"), x.long("updatedAt"))
        } ?: emptyList(),
        workdays = o.field("workdays").asArr()?.values?.map { v ->
            val x = v.asObj()!!
            WorkdayDto(x.string("uuid"), x.string("projectUuid"), x.long("dateEpochDay"),
                x.string("workArea"), x.string("employer"), x.string("supervisor"), x.int("workerCount"),
                x.string("notes"), x.int("totalLengthMeters"), x.long("createdAt"), x.long("updatedAt"))
        } ?: emptyList(),
        operations = o.field("operations").asArr()?.values?.map { v ->
            val x = v.asObj()!!
            OperationDto(x.string("uuid"), x.string("workdayUuid"), x.string("startType"),
                x.string("startCode"), x.int("startMeter"), x.string("endType"), x.string("endCode"),
                x.int("endMeter"), x.int("cableTypeCoreCount"), x.int("lengthMeters"),
                x.long("createdAt"), x.long("updatedAt"))
        } ?: emptyList(),
        cableTypes = o.field("cableTypes").asArr()?.values?.map { v ->
            val x = v.asObj()!!
            CableTypeDto(x.int("coreCount"), x.string("label"), x.boolean("active"), x.int("sortOrder"))
        } ?: emptyList(),
        categories = o.field("categories").asArr()?.values?.map { v ->
            val x = v.asObj()!!
            CategoryDto(x.string("name"), x.boolean("active"), x.int("sortOrder"))
        } ?: emptyList(),
        expenses = o.field("expenses").asArr()?.values?.map { v ->
            val x = v.asObj()!!
            ExpenseDto(x.string("uuid"), x.long("dateEpochDay"), x.string("categoryName"),
                x.long("amountToman"), x.string("description"), x.string("receiptReference"),
                x.string("projectUuid"), x.string("workdayUuid"), x.long("createdAt"), x.long("updatedAt"))
        } ?: emptyList(),
        settlements = o.field("settlements").asArr()?.values?.map { v ->
            val x = v.asObj()!!
            SettlementDto(x.string("uuid"), x.string("expenseUuid"), x.long("amountToman"),
                x.long("dateEpochDay"), x.string("note"), x.long("createdAt"))
        } ?: emptyList(),
    )
}
