package ir.hoboho.shooting.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * On Android 8/9, ask for the legacy storage permission before an export so the
 * file can land in public Downloads. If denied, PublicDownloadSaver still
 * stores a copy in the app's external files directory. Android 10+ needs no
 * permission because it writes through MediaStore.Downloads.
 */
@Composable
fun rememberFileSaveAction(onReady: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnReady = rememberUpdatedState(onReady)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { currentOnReady.value() }

    return remember(context, launcher) {
        {
            if (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                currentOnReady.value()
            }
        }
    }
}
