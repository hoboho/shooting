package android.content
import android.net.Uri
import java.io.File
import java.io.InputStream

class ContentResolver {
    fun openInputStream(uri: Uri): InputStream? = null
}
class AssetManager {
    fun open(path: String): InputStream = java.io.ByteArrayInputStream(ByteArray(0))
}
open class Context {
    open val filesDir: File get() = File("/tmp")
    open val cacheDir: File get() = File("/tmp")
    open val assets: AssetManager get() = AssetManager()
    open val packageName: String get() = "ir.hoboho.shooting"
    open val contentResolver: ContentResolver get() = ContentResolver()
    open fun getString(id: Int): String = ""
    open fun getString(id: Int, vararg args: Any): String = ""
    open fun startActivity(intent: Intent) {}
}
class Intent(action: String = "") {
    companion object {
        const val ACTION_SEND = "android.intent.action.SEND"
        const val EXTRA_STREAM = "android.intent.extra.STREAM"
        const val FLAG_GRANT_READ_URI_PERMISSION = 1
        const val FLAG_ACTIVITY_NEW_TASK = 268435456
        fun createChooser(target: Intent, title: CharSequence?): Intent = target
    }
    var type: String? = null
    fun putExtra(name: String, value: Any?): Intent = this
    fun addFlags(flags: Int): Intent = this
    fun setPackage(pkg: String?): Intent = this
}
