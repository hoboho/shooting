package androidx.core.graphics
import android.graphics.Canvas

fun String.toColorInt(): Int = 0
inline fun Canvas.withTranslation(x: Float = 0f, y: Float = 0f, block: Canvas.() -> Unit) {
    save()
    translate(x, y)
    try { block() } finally { restore() }
}
