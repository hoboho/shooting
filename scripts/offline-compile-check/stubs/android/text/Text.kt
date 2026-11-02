package android.text
import android.graphics.Canvas
import android.graphics.Paint

object Layout {
    enum class Alignment { ALIGN_NORMAL, ALIGN_OPPOSITE, ALIGN_CENTER }
}
class TextPaint : Paint {
    constructor()
    constructor(flags: Int)
}
class StaticLayout {
    class Builder private constructor() {
        companion object {
            fun obtain(source: CharSequence, start: Int, end: Int, paint: TextPaint, width: Int): Builder = Builder()
        }
        fun setAlignment(a: Layout.Alignment): Builder = this
        fun setTextDirection(d: Any): Builder = this
        fun setLineSpacing(add: Float, mult: Float): Builder = this
        fun setIncludePad(include: Boolean): Builder = this
        fun setEllipsize(e: TextUtils.TruncateAt?): Builder = this
        fun build(): StaticLayout = StaticLayout()
    }
    val height: Int get() = 0
    fun draw(canvas: Canvas) {}
}
object TextDirectionHeuristics { val FIRSTSTRONG_RTL: Any = Any() }
object TextUtils {
    enum class TruncateAt { START, MIDDLE, END, MARQUEE }
    fun ellipsize(text: CharSequence, paint: Paint, width: Float, where: TruncateAt): CharSequence = text
}
