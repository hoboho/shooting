package android.graphics
import java.io.File

class Typeface {
    companion object {
        val SANS_SERIF: Typeface = Typeface()
        const val BOLD = 1
        fun create(family: Typeface?, style: Int): Typeface = Typeface()
        fun createFromFile(file: File): Typeface = Typeface()
        fun createFromAsset(am: android.content.AssetManager, path: String): Typeface = Typeface()
    }
}
open class Paint {
    companion object {
        const val ANTI_ALIAS_FLAG = 1
        const val SUBPIXEL_TEXT_FLAG = 128
    }
    enum class Style { FILL, STROKE, FILL_AND_STROKE }
    enum class Align { LEFT, CENTER, RIGHT }
    constructor()
    constructor(flags: Int)
    constructor(other: Paint)
    var color: Int = 0
    var style: Style = Style.FILL
    var strokeWidth: Float = 0f
    var textSize: Float = 0f
    var textAlign: Align = Align.LEFT
    var isAntiAlias: Boolean = false
    var typeface: Typeface? = null
    open fun measureText(text: String): Float = 0f
}
open class Canvas {
    fun drawText(text: String, x: Float, y: Float, paint: Paint) {}
    fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, paint: Paint) {}
    fun drawRect(l: Float, t: Float, r: Float, b: Float, paint: Paint) {}
    fun save(): Int = 0
    fun restore() {}
    fun translate(dx: Float, dy: Float) {}
}
