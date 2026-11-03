package ir.hoboho.shooting.core.backup

import ir.hoboho.shooting.BuildConfig

/**
 * Build metadata written into backups and shown in settings.
 *
 * The values come from the generated [BuildConfig], whose fields are fed by the
 * single version definition in `gradle.properties` (see `app/build.gradle.kts`).
 * Nothing here is kept in sync by hand, so the APK, the UI and backup files can
 * never disagree about the running version.
 */
object AppInfo {
    val VERSION_NAME: String = BuildConfig.APP_VERSION_NAME
    val VERSION_CODE: Int = BuildConfig.APP_VERSION_CODE
    const val BACKUP_FILE_EXTENSION = ".shooting-backup"
}
