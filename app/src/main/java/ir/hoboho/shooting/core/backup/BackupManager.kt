package ir.hoboho.shooting.core.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.hoboho.shooting.core.common.Clock
import ir.hoboho.shooting.core.format.DateFormatter
import ir.hoboho.shooting.domain.backup.BackupInspection
import ir.hoboho.shooting.domain.backup.RestoreOutcome
import ir.hoboho.shooting.domain.backup.RestorePreview
import ir.hoboho.shooting.domain.backup.RestoreStrategy
import ir.hoboho.shooting.domain.repository.BackupRepository
import ir.hoboho.shooting.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class BackupException(message: String) : IllegalArgumentException(message) {
    class InvalidFile : BackupException("فایل پشتیبان معتبر نیست")
    class UnsupportedVersion(val found: Int) : BackupException("نسخه $found پشتیبانی نمیشود")
    class Integrity(val problems: List<String>) : BackupException(problems.joinToString("، "))
}

/**
 * Local backup/restore orchestration.
 *
 * Rules: versioned format + SHA-256 integrity check, explicit restore strategy,
 * preview before writing, a safety snapshot of current data before *every*
 * restore, and no silent overwrite.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: BackupRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {

    suspend fun read(uri: Uri): String = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw BackupException.InvalidFile()
    }

    /** Creates a versioned backup file in app-private storage. */
    suspend fun createBackup(): File = withContext(Dispatchers.IO) {
        val data = repository.snapshot()
        val now = clock.nowEpochMilli()
        val payload = BackupMapper.toPayload(data, AppInfo.VERSION_NAME, AppInfo.VERSION_CODE, now)
        val text = BackupCodec.encode(payload)
        val dir = File(context.filesDir, "backups")
        val label = DateFormatter.format(
            ir.hoboho.shooting.core.date.JalaliCalendar.todayEpochDay(),
            persianDigits = false,
        ).replace('/', '-')
        val file = File(dir, "shooting-backup-$label-$now${AppInfo.BACKUP_FILE_EXTENSION}")
        dir.mkdirs()
        file.writeText(text, Charsets.UTF_8)
        settings.setLastBackupEpochMilli(now)
        file
    }

    /** Validates and previews a file without writing a single row. */
    suspend fun inspect(text: String, strategy: RestoreStrategy): BackupInspection {
        val payload = requirePayload(text)
        val warnings = BackupMapper.validate(payload)
        val preview = RestorePreview(
            formatVersion = payload.formatVersion,
            appVersionName = payload.appVersionName,
            exportedAtEpochMilli = payload.exportedAtEpochMilli,
            incoming = BackupCounts.from(payload),
            existing = repository.counts(),
            strategy = strategy,
            formatSupported = payload.formatVersion == BackupCodec.FORMAT_VERSION,
            warnings = warnings,
        )
        return BackupInspection(preview, warnings)
    }

    suspend fun restore(text: String, strategy: RestoreStrategy): RestoreOutcome {
        val payload = requirePayload(text)
        val problems = BackupMapper.validate(payload)
        if (problems.isNotEmpty()) throw BackupException.Integrity(problems)
        val data = BackupMapper.toData(payload)
        // Safety snapshot first: even a failed restore leaves the old data intact
        // and gives the user a file to recover from.
        val safety = createSafetySnapshot()
        val imported = repository.restore(data, strategy)
        return RestoreOutcome(strategy, imported, safety.name)
    }

    private suspend fun createSafetySnapshot(): File = createBackup()

    private fun requirePayload(text: String): BackupPayload = when (val result = BackupCodec.decode(text)) {
        is BackupReadResult.Success -> result.payload
        is BackupReadResult.UnsupportedVersion -> throw BackupException.UnsupportedVersion(result.found)
        is BackupReadResult.ChecksumMismatch,
        is BackupReadResult.Malformed,
        is BackupReadResult.WrongMagic -> throw BackupException.InvalidFile()
    }
}
