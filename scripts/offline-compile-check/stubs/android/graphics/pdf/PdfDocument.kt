package android.graphics.pdf
import android.graphics.Canvas
import java.io.OutputStream

class PdfDocument {
    class PageInfo {
        class Builder(private val w: Int, private val h: Int, private val n: Int) {
            fun create(): PageInfo = PageInfo()
        }
    }
    class Page { val canvas: Canvas = Canvas() }
    fun startPage(info: PageInfo): Page = Page()
    fun finishPage(page: Page) {}
    fun writeTo(stream: OutputStream) {}
    fun close() {}
}
