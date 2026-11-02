package ir.hoboho.shooting.core.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withTranslation
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.hoboho.shooting.domain.report.ReportText
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Renders a [PdfDocumentSpec] to an A4 PDF.
 *
 * Two decisions matter for Persian output:
 *
 * 1. **RTL text** is laid out by [StaticLayout] with `FIRSTSTRONG_RTL` (for
 *    paragraphs) and right-aligned canvas text for table cells. Android's text
 *    engine runs bidi + Arabic shaping, so Persian words join correctly.
 * 2. **The font is embedded** from `assets/fonts` (Vazirmatn, SIL OFL), so the
 *    PDF looks the same in any viewer, including desktop readers without a
 *    Persian font installed.
 *
 * Table headers are repeated on *every* page a table spans; columns run right to
 * left, which is what a Persian reader expects.
 */
@Singleton
class PdfReportEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun render(spec: PdfDocumentSpec, outputFile: File): File {
        val fonts = loadFonts()
        val document = PdfDocument()
        try {
            val painter = Painter(document, fonts, spec)
            while (painter.hasMoreContent()) painter.renderNextPage()
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { stream -> document.writeTo(stream) }
            return outputFile
        } finally {
            document.close()
        }
    }

    private data class Fonts(val regular: Typeface, val medium: Typeface, val bold: Typeface)

    private fun loadFonts(): Fonts = try {
        Fonts(
            typefaceFromAsset("fonts/Vazirmatn-Regular.ttf"),
            typefaceFromAsset("fonts/Vazirmatn-Medium.ttf"),
            typefaceFromAsset("fonts/Vazirmatn-Bold.ttf"),
        )
    } catch (t: Throwable) {
        Fonts(
            Typeface.SANS_SERIF,
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD),
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD),
        )
    }

    private fun typefaceFromAsset(path: String): Typeface {
        context.assets.open(path).use { input ->
            val temp = File.createTempFile("shooting-font", ".ttf", context.cacheDir)
            temp.outputStream().use { output -> input.copyTo(output) }
            val face = Typeface.createFromFile(temp)
            temp.delete()
            return face
        }
    }

    private inner class Painter(
        private val document: PdfDocument,
        private val fonts: Fonts,
        private val spec: PdfDocumentSpec,
    ) {
        private var blockIndex = 0
        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var cursorY = 0f
        private var pendingTableRow = 0
        /** A KeyValueList can also span pages; track progress to avoid a loop. */
        private var pendingKeyValueRow = 0

        /** A table must print its header again when it continues on a new page. */
        private var tableNeedsHeader = true

        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#D1D9D3".toColorInt()
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
        }
        private val headerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#D8F2E9".toColorInt() }
        private val zebraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#F6FAF8".toColorInt() }
        private val emphasisedBandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#DCEDE5".toColorInt() }
        /** Title, subtitle and rules are ordinary blocks so pagination handles them. */
        private val blocks: List<PdfBlock> = buildList {
            add(PdfBlock.Paragraph(spec.title, PdfTextStyle.Title, spaceAfterPt = 4f))
            spec.subtitle?.let { add(PdfBlock.Paragraph(it, PdfTextStyle.Subtitle, spaceAfterPt = 6f)) }
            spec.headerLines.forEach { line ->
                add(PdfBlock.Paragraph(line, PdfTextStyle.Small, spaceAfterPt = 1f))
            }
            add(PdfBlock.Divider(spaceBeforePt = 4f, spaceAfterPt = 8f))
            addAll(spec.blocks)
        }

        fun hasMoreContent(): Boolean = blockIndex < blocks.size || page != null

        fun renderNextPage() {
            pageNumber++
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val newPage = document.startPage(info)
            page = newPage
            canvas = newPage.canvas
            cursorY = MARGIN_TOP
            tableNeedsHeader = true
            drawPageHeader()
            while (blockIndex < blocks.size) {
                val consumed = drawBlock(blocks[blockIndex])
                if (!consumed) break // needs a fresh page
                blockIndex++
                pendingTableRow = 0
                pendingKeyValueRow = 0
                tableNeedsHeader = true
            }
            drawFooter()
            document.finishPage(newPage)
            if (blockIndex >= blocks.size) {
                page = null
                canvas = null
            }
        }

        private fun drawPageHeader() {
            val latin = textPaint(PdfWeight.BOLD, 11f)
            canvas?.drawText(ReportText.BRAND_LATIN, MARGIN_LEFT, cursorY + latin.textSize, latin)
            val persian = textPaint(PdfWeight.BOLD, 13f)
            val brand = ReportText.BRAND
            canvas?.drawText(brand, CONTENT_RIGHT - persian.measureText(brand), cursorY + persian.textSize, persian)
            cursorY += max(latin.textSize, persian.textSize) + 6f
            canvas?.drawLine(MARGIN_LEFT, cursorY, CONTENT_RIGHT, cursorY, Paint(linePaint).apply {
                color = ACCENT
                strokeWidth = 1.4f
            })
            cursorY += 12f
        }

        private fun drawFooter() {
            val paint = textPaint(PdfWeight.REGULAR, 8.5f).apply { color = MUTED }
            val footerY = PAGE_HEIGHT - MARGIN_BOTTOM + 18f
            canvas?.drawLine(MARGIN_LEFT, footerY - 12f, CONTENT_RIGHT, footerY - 12f, linePaint)
            canvas?.drawText(
                spec.footerText,
                CONTENT_RIGHT - paint.measureText(spec.footerText), footerY, paint,
            )
            if (spec.pageNumbers) {
                val label = ir.hoboho.shooting.core.format.PersianDigits.toPersian(pageNumber.toString())
                canvas?.drawText(label, MARGIN_LEFT, footerY, paint)
            }
        }

        /** @return true when fully drawn, false when a new page is needed. */
        private fun drawBlock(block: PdfBlock): Boolean {
            val bottom = PAGE_HEIGHT - MARGIN_BOTTOM
            return when (block) {
                is PdfBlock.Spacer -> {
                    if (cursorY + block.heightPt > bottom) return false
                    cursorY += block.heightPt
                    true
                }
                is PdfBlock.Divider -> {
                    if (cursorY + block.spaceBeforePt + block.spaceAfterPt + 2f > bottom) return false
                    cursorY += block.spaceBeforePt
                    canvas?.drawLine(MARGIN_LEFT, cursorY, CONTENT_RIGHT, cursorY, linePaint)
                    cursorY += block.spaceAfterPt
                    true
                }
                is PdfBlock.Paragraph -> {
                    val layout = layoutFor(block.text, block.style, CONTENT_WIDTH)
                    val height = layout.height + block.spaceBeforePt + block.spaceAfterPt
                    if (cursorY + height > bottom) return false
                    cursorY += block.spaceBeforePt
                    canvas?.withTranslation(MARGIN_LEFT, cursorY) { layout.draw(this) }
                    cursorY += layout.height + block.spaceAfterPt
                    true
                }
                is PdfBlock.KeyValueList -> drawKeyValueList(block, bottom)
                is PdfBlock.Table -> drawTable(block, bottom)
            }
        }

        private fun drawKeyValueList(block: PdfBlock.KeyValueList, bottom: Float): Boolean {
            val labelWidth = CONTENT_WIDTH * 0.45f
            val valueWidth = CONTENT_WIDTH - labelWidth
            val paint = textPaint(PdfWeight.REGULAR, 10f)
            val rowHeight = paint.textSize * 1.8f
            for ((index, entry) in block.entries.withIndex()) {
                if (index < pendingKeyValueRow) continue
                val (label, value) = entry
                if (cursorY + rowHeight > bottom) return false
                val emphasised = label in block.emphasisedLabels
                if (emphasised) {
                    canvas?.drawRect(
                        MARGIN_LEFT - 4f, cursorY - 4f,
                        CONTENT_RIGHT + 4f, cursorY + rowHeight - 4f, emphasisedBandPaint,
                    )
                }
                val valuePaint = textPaint(
                    if (emphasised) PdfWeight.BOLD else PdfWeight.REGULAR,
                    if (emphasised) 10.5f else 10f,
                ).apply {
                    if (emphasised) color = ACCENT_DARK
                    textAlign = Paint.Align.RIGHT
                }
                val labelLayout = layoutFor(label, PdfTextStyle.Body, labelWidth)
                canvas?.withTranslation(CONTENT_RIGHT - labelWidth, cursorY - 2f) { labelLayout.draw(this) }
                val clipped = ellipsize(value, valuePaint, valueWidth - 8f)
                canvas?.drawText(clipped, CONTENT_RIGHT - labelWidth - 12f, cursorY + paint.textSize, valuePaint)
                cursorY += rowHeight
                canvas?.drawLine(MARGIN_LEFT, cursorY - 4f, CONTENT_RIGHT, cursorY - 4f, linePaint)
                pendingKeyValueRow++
            }
            return true
        }

        private fun drawTable(block: PdfBlock.Table, bottom: Float): Boolean {
            val widths = resolveWidths(block.columns)
            val headerPaint = textPaint(PdfWeight.BOLD, 8.6f)
            val cellPaint = textPaint(PdfWeight.REGULAR, 8.6f)
            val headerHeight = headerPaint.textSize * 2.1f
            val rowHeight = cellPaint.textSize * 1.95f

            if (pendingTableRow == 0) block.caption?.let { caption ->
                val layout = layoutFor(caption, PdfTextStyle.SectionHeader, CONTENT_WIDTH)
                if (cursorY + layout.height + 4f > bottom) return false
                canvas?.withTranslation(MARGIN_LEFT, cursorY) { layout.draw(this) }
                cursorY += layout.height + 4f
            }

            if (tableNeedsHeader) {
                if (cursorY + headerHeight > bottom) return false
                canvas?.drawRect(MARGIN_LEFT, cursorY - 2f, CONTENT_RIGHT, cursorY + headerHeight - 2f, headerFillPaint)
                drawRow(block.header, widths, block.aligns, headerPaint, headerHeight, header = true)
                cursorY += headerHeight
                tableNeedsHeader = false
            }

            while (pendingTableRow < block.rows.size) {
                if (cursorY + rowHeight > bottom) {
                    // Continues on the next page, where the header is repeated.
                    return false
                }
                val index = pendingTableRow
                if (index % 2 == 1) {
                    canvas?.drawRect(
                        MARGIN_LEFT, cursorY - 1f,
                        CONTENT_RIGHT, cursorY + rowHeight - 1f, zebraPaint,
                    )
                }
                drawRow(block.rows[index], widths, block.aligns, cellPaint, rowHeight, header = false)
                canvas?.drawLine(MARGIN_LEFT, cursorY + rowHeight - 1f, CONTENT_RIGHT, cursorY + rowHeight - 1f, linePaint)
                cursorY += rowHeight
                pendingTableRow++
            }
            return true
        }

        /** Columns are laid out right-to-left: the first column is rightmost. */
        private fun drawRow(
            cells: List<String>,
            widths: List<Float>,
            aligns: List<PdfAlign>,
            paint: TextPaint,
            rowHeight: Float,
            header: Boolean,
        ) {
            var x = CONTENT_RIGHT
            for (index in cells.indices) {
                val width = widths.getOrElse(index) { CONTENT_WIDTH / cells.size }
                val align = aligns.getOrElse(index) { PdfAlign.START }
                val cellLeft = x - width
                val clipped = ellipsize(cells[index], paint, width - CELL_PADDING * 2)
                val textWidth = paint.measureText(clipped)
                val textX = when (align) {
                    PdfAlign.START -> x - CELL_PADDING - textWidth // right side of an RTL cell
                    PdfAlign.END -> cellLeft + CELL_PADDING
                    PdfAlign.CENTER -> cellLeft + (width - textWidth) / 2f
                }
                canvas?.drawText(clipped, textX, cursorY + rowHeight * 0.72f, paint)
                if (header) {
                    canvas?.drawLine(cellLeft, cursorY - 2f, cellLeft, cursorY + rowHeight - 2f, linePaint)
                }
                x = cellLeft
            }
        }

        private fun resolveWidths(columns: List<PdfColumn>): List<Float> {
            if (columns.isEmpty()) return emptyList()
            val totalWeight = columns.sumOf { it.weight.toDouble() }.toFloat()
            if (totalWeight <= 0f) return List(columns.size) { CONTENT_WIDTH / columns.size }
            val scaled = columns.map { max(it.minWidthPt, CONTENT_WIDTH * (it.weight / totalWeight)) }
            val sum = scaled.sum()
            return scaled.map { it * (CONTENT_WIDTH / sum) }
        }

        private fun textPaint(weight: PdfWeight, sizePt: Float): TextPaint = TextPaint(
            Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG,
        ).apply {
            typeface = when (weight) {
                PdfWeight.REGULAR -> fonts.regular
                PdfWeight.MEDIUM -> fonts.medium
                PdfWeight.BOLD -> fonts.bold
            }
            textSize = sizePt // PDF units are points already
            color = if (weight == PdfWeight.BOLD) TEXT_STRONG else TEXT_PRIMARY
            isAntiAlias = true
        }

        private fun stylePaint(style: PdfTextStyle): TextPaint {
            val (weight, size, color) = when (style) {
                PdfTextStyle.Title -> Triple(PdfWeight.BOLD, 17f, TEXT_STRONG)
                PdfTextStyle.Subtitle -> Triple(PdfWeight.MEDIUM, 11.5f, MUTED)
                PdfTextStyle.SectionHeader -> Triple(PdfWeight.BOLD, 12f, ACCENT_DARK)
                PdfTextStyle.Body -> Triple(PdfWeight.REGULAR, 10f, TEXT_PRIMARY)
                PdfTextStyle.Small -> Triple(PdfWeight.REGULAR, 8.6f, MUTED)
                PdfTextStyle.TableHeader -> Triple(PdfWeight.BOLD, 8.6f, TEXT_STRONG)
                PdfTextStyle.TableCell -> Triple(PdfWeight.REGULAR, 8.6f, TEXT_PRIMARY)
            }
            return textPaint(weight, size).apply { this.color = color }
        }

        private fun layoutFor(text: String, style: PdfTextStyle, width: Float): StaticLayout =
            StaticLayout.Builder
                .obtain(text, 0, text.length, stylePaint(style), width.toInt().coerceAtLeast(1))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
                .setLineSpacing(2f, 1f)
                .setIncludePad(false)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()

        private fun ellipsize(text: String, paint: TextPaint, maxWidth: Float): String {
            if (maxWidth <= 0f) return ""
            if (paint.measureText(text) <= maxWidth) return text
            return TextUtils.ellipsize(text, paint, maxWidth, TextUtils.TruncateAt.END).toString()
        }
    }

    companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        private const val MARGIN_LEFT = 34f
        private const val MARGIN_TOP = 40f
        private const val MARGIN_BOTTOM = 46f
        private const val CONTENT_RIGHT = PAGE_WIDTH - MARGIN_LEFT
        private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT * 2
        private const val CELL_PADDING = 4f

        private val TEXT_PRIMARY = "#17201D".toColorInt()
        private val TEXT_STRONG = "#0B1220".toColorInt()
        private val MUTED = "#5C6875".toColorInt()
        private val ACCENT = "#176B5B".toColorInt()
        private val ACCENT_DARK = "#0B3A2C".toColorInt()
    }
}
