package ir.hoboho.shooting.core.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.hoboho.shooting.core.pdf.PdfDocumentSpec
import ir.hoboho.shooting.core.pdf.PdfReportEngine
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Writes PDFs to app-private storage and shares them through a FileProvider. */
@Singleton
class ReportFileWriter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: PdfReportEngine,
) {
    fun writePdf(spec: PdfDocumentSpec, fileName: String): File {
        val dir = File(context.filesDir, "exports")
        val file = File(dir, fileName)
        return engine.render(spec, file)
    }
}

@Singleton
class FileSharer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun share(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun sharePdf(file: File) = share(file, "application/pdf")

    fun shareBackup(file: File) = share(file, "application/octet-stream")
}
