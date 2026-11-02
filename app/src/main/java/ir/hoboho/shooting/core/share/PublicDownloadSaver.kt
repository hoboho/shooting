package ir.hoboho.shooting.core.share

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists exported reports/backups in a user-visible Downloads folder in
 * addition to the app-private copy used by FileProvider for sharing.
 *
 * Android 10+ uses MediaStore.Downloads (no storage permission). Android 8/9
 * uses the legacy public Downloads directory when WRITE_EXTERNAL_STORAGE was
 * granted, otherwise falls back to the app's external files directory.
 */
@Singleton
class PublicDownloadSaver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class SavedFile(val displayPath: String)

    fun savePdf(file: File, displayName: String): SavedFile =
        save(file, displayName, "application/pdf")

    fun saveBackup(file: File, displayName: String): SavedFile =
        save(file, displayName, "application/octet-stream")

    fun save(file: File, displayName: String, mimeType: String): SavedFile {
        val name = uniqueName(displayName)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStore(file, name, mimeType)
        } else {
            saveLegacy(file, name)
        }
    }

    private fun saveWithMediaStore(file: File, name: String, mimeType: String): SavedFile {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/Shooting",
            )
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri: Uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Could not create Downloads entry")
        resolver.openOutputStream(uri)?.use { output ->
            file.inputStream().use { input -> input.copyTo(output) }
        } ?: throw IllegalStateException("Could not open Downloads output")
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return SavedFile("${Environment.DIRECTORY_DOWNLOADS}/Shooting/$name")
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(file: File, name: String): SavedFile {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
        val base = if (granted) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        } else {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        }
        val dir = File(base, "Shooting").apply { mkdirs() }
        val target = File(dir, name)
        file.copyTo(target, overwrite = true)
        val display = if (granted) {
            "${Environment.DIRECTORY_DOWNLOADS}/Shooting/$name"
        } else {
            target.absolutePath
        }
        return SavedFile(display)
    }

    private fun uniqueName(displayName: String): String {
        val safe = displayName.replace('/', '_').replace('\\', '_')
        val dot = safe.lastIndexOf('.')
        val base = if (dot > 0) safe.substring(0, dot) else safe
        val ext = if (dot > 0) safe.substring(dot) else ""
        return "$base-${System.currentTimeMillis()}$ext"
    }
}
