package ir.hoboho.shooting.feature.workdays

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hoboho.shooting.core.common.StringProvider
import ir.hoboho.shooting.core.format.DateFormatter
import ir.hoboho.shooting.core.share.FileSharer
import ir.hoboho.shooting.core.share.ReportFileWriter
import ir.hoboho.shooting.domain.model.Workday
import ir.hoboho.shooting.domain.report.ReportSpecBuilder
import ir.hoboho.shooting.domain.repository.ReferenceRepository
import ir.hoboho.shooting.domain.repository.WorkdayRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class WorkdayDetailViewModel @Inject constructor(
    private val workdayRepository: WorkdayRepository,
    private val referenceRepository: ReferenceRepository,
    private val writer: ReportFileWriter,
    private val sharer: FileSharer,
    private val strings: StringProvider,
) : ViewModel() {

    private val _workday = MutableStateFlow<Workday?>(null)
    val workday: StateFlow<Workday?> = _workday.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch { _workday.value = workdayRepository.getWorkday(id) }
    }

    fun exportPdf() {
        val report = _workday.value ?: return
        viewModelScope.launch {
            try {
                val cables = referenceRepository.getAllCableTypes()
                val spec = ReportSpecBuilder.daily(report, cables)
                val name = "daily-${DateFormatter.format(report.dateEpochDay, false).replace('/', '-')}-${report.id}.pdf"
                val file = withContext(Dispatchers.IO) { writer.writePdf(spec, name) }
                sharer.sharePdf(file)
                _message.value = strings.get(ir.hoboho.shooting.R.string.msg_pdf_saved)
            } catch (t: Throwable) {
                _message.value = strings.get(ir.hoboho.shooting.R.string.msg_pdf_failed)
            }
        }
    }

    fun consumeMessage() { _message.value = null }
}
