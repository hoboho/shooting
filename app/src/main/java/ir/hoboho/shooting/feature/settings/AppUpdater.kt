package ir.hoboho.shooting.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.hoboho.shooting.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateState(
    val isChecking: Boolean = false,
    val latestVersion: String? = null,
    val message: String? = null,
    val updateAvailable: Boolean = false,
)

class AppUpdater @javax.inject.Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun checkAndPrepare(): AppUpdateState = withContext(Dispatchers.IO) {
        val releases = requestJson("https://api.github.com/repos/hoboho/shooting/releases?per_page=100")
        val release = (0 until releases.length())
            .asSequence()
            .mapNotNull { releases.optJSONObject(it) }
            .firstOrNull { item ->
                !item.optBoolean("draft", true) &&
                    !item.optBoolean("prerelease", true) &&
                    item.optString("tag_name").endsWith("-release") &&
                    item.optJSONArray("assets")?.hasAsset("Shooting-release.apk") == true
            } ?: return@withContext AppUpdateState(
                message = "هنوز نسخهٔ انتشار امضاشده‌ای برای به‌روزرسانی منتشر نشده است.",
            )

        val tag = release.optString("tag_name")
        val latestVersion = tag.removePrefix("v").removeSuffix("-release")
        if (compareAppVersions(latestVersion, BuildConfig.APP_VERSION_NAME) <= 0) {
            return@withContext AppUpdateState(
                latestVersion = latestVersion,
                message = "برنامه به‌روز است. نسخهٔ نصب‌شده: ${BuildConfig.APP_VERSION_NAME}",
            )
        }

        val asset = release.getJSONArray("assets").let { assets ->
            (0 until assets.length()).asSequence()
                .mapNotNull { assets.optJSONObject(it) }
                .first { it.optString("name") == "Shooting-release.apk" }
        }
        val downloadUrl = asset.getString("browser_download_url")
        val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val apk = File(updateDir, "Shooting-release.apk")
        download(downloadUrl, apk)

        val packageInfo = context.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
            ?: throw IllegalStateException("فایل APK دریافت‌شده معتبر نیست.")
        if (packageInfo.applicationInfo?.packageName != context.packageName) {
            apk.delete()
            throw IllegalStateException("این فایل متعلق به برنامهٔ شوتینگ نیست.")
        }
        @Suppress("DEPRECATION")
        val downloadedVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
        if (downloadedVersionCode <= BuildConfig.APP_VERSION_CODE.toLong()) {
            apk.delete()
            throw IllegalStateException("نسخهٔ APK با نسخهٔ اعلام‌شده در انتشار GitHub مطابقت ندارد.")
        }

        AppUpdateState(
            latestVersion = latestVersion,
            message = "نسخهٔ جدید $latestVersion آمادهٔ نصب است.",
            updateAvailable = true,
        )
    }

    fun installPreparedUpdate(): String? {
        val apk = File(context.cacheDir, "updates/Shooting-release.apk")
        if (!apk.isFile) return "فایل به‌روزرسانی پیدا نشد؛ ابتدا بررسی به‌روزرسانی را انجام دهید."

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val permissionIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(permissionIntent)
            return "اجازهٔ نصب برنامه از این منبع را فعال کنید؛ سپس دکمهٔ نصب را دوباره بزنید."
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk,
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
        return null
    }

    private fun JSONArray.hasAsset(name: String): Boolean =
        (0 until length()).any { optJSONObject(it)?.optString("name") == name }

    private fun requestJson(url: String): JSONArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connection.setRequestProperty("User-Agent", "Shooting-Android-App")
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("دریافت اطلاعات انتشار از GitHub ناموفق بود (HTTP ${connection.responseCode}).")
            }
            return JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun download(url: String, destination: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 20_000
            connection.readTimeout = 60_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "Shooting-Android-App")
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("دریافت فایل APK ناموفق بود (HTTP ${connection.responseCode}).")
            }
            val temp = File(destination.parentFile, "${destination.name}.part")
            connection.inputStream.use { input ->
                temp.outputStream().buffered().use { output -> input.copyTo(output) }
            }
            if (temp.length() < 1_000_000L) {
                temp.delete()
                throw IllegalStateException("فایل APK دریافتی ناقص است.")
            }
            if (destination.exists()) destination.delete()
            if (!temp.renameTo(destination)) {
                temp.copyTo(destination, overwrite = true)
                temp.delete()
            }
        } finally {
            connection.disconnect()
        }
    }

}

internal fun compareAppVersions(left: String, right: String): Int {
    fun parts(value: String): List<Int> {
        val parsed = value.trim().removePrefix("v").split(".").map { it.toIntOrNull() }
        require(parsed.size == 3 && parsed.all { it != null }) {
            "شمارهٔ نسخه معتبر نیست: $value"
        }
        return parsed.filterNotNull()
    }
    val a = parts(left)
    val b = parts(right)
    for (index in 0..2) {
        val compared = a[index].compareTo(b[index])
        if (compared != 0) return compared
    }
    return 0
}
