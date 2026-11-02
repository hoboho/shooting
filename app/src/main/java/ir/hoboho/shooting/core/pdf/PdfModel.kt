package ir.hoboho.shooting.core.pdf

/**
 * The report *document model*. Deliberately free of Android types: business
 * code describes a report as data and [PdfReportEngine] is the only place that
 * knows how to draw it. This keeps report content unit-testable on the JVM.
 */
enum class PdfAlign { START, CENTER, END }

enum class PdfWeight { REGULAR, MEDIUM, BOLD }

sealed interface PdfBlock {
    data class Paragraph(
        val text: String,
        val style: PdfTextStyle = PdfTextStyle.Body,
        val align: PdfAlign = PdfAlign.START,
        val spaceBeforePt: Float = 0f,
        val spaceAfterPt: Float = 4f,
    ) : PdfBlock

    data class Divider(val spaceBeforePt: Float = 6f, val spaceAfterPt: Float = 6f) : PdfBlock

    data class Spacer(val heightPt: Float) : PdfBlock

    /** A table whose header row is repeated on every page it spans. */
    data class Table(
        val columns: List<PdfColumn>,
        val header: List<String>,
        val rows: List<List<String>>,
        val aligns: List<PdfAlign> = emptyList(),
        val caption: String? = null,
    ) : PdfBlock

    /** Two-column label/value list, right-aligned for Persian. */
    data class KeyValueList(
        val entries: List<Pair<String, String>>,
        val emphasisedLabels: Set<String> = emptySet(),
    ) : PdfBlock
}

data class PdfColumn(
    val title: String,
    /** Relative width; the engine scales the set to fit the content width. */
    val weight: Float = 1f,
    val minWidthPt: Float = 0f,
)

enum class PdfTextStyle { Title, Subtitle, SectionHeader, Body, Small, TableHeader, TableCell }

data class PdfDocumentSpec(
    val title: String,
    val subtitle: String? = null,
    val headerLines: List<String> = emptyList(),
    val blocks: List<PdfBlock>,
    val footerText: String = "تهیهشده با شوتینگ",
    val pageNumbers: Boolean = true,
    val documentLanguageTag: String = "fa",
)
