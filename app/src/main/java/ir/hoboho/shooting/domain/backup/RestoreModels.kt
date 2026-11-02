package ir.hoboho.shooting.domain.backup

import ir.hoboho.shooting.core.backup.BackupCounts

/**
 * How a restore merges a backup into the current database. The UI must show
 * this choice explicitly: a restore is never silent and a safety snapshot is
 * always written first.
 */
enum class RestoreStrategy {
    /** Insert only rows that are not already present; never touch existing data. */
    ADD_ONLY,

    /** Insert missing rows and update rows that match on their stable uuid. */
    MERGE,

    /** Replace all current data with the file. Requires an explicit confirmation. */
    REPLACE,
}

/** What a restore will do, computed *before* anything is written. */
data class RestorePreview(
    val formatVersion: Int,
    val appVersionName: String,
    val exportedAtEpochMilli: Long,
    val incoming: BackupCounts,
    val existing: BackupCounts,
    val strategy: RestoreStrategy,
    val formatSupported: Boolean,
    val warnings: List<String> = emptyList(),
) {
    val willReplaceExistingData: Boolean get() = strategy == RestoreStrategy.REPLACE
    val willOverwriteExistingData: Boolean get() = strategy == RestoreStrategy.MERGE
}

/** Outcome of a completed restore. */
data class RestoreOutcome(
    val strategy: RestoreStrategy,
    val imported: BackupCounts,
    val safetySnapshotFile: String?,
)

/** Chosen for a file before the user confirms a restore. */
data class BackupInspection(
    val preview: RestorePreview,
    val warnings: List<String>,
)
