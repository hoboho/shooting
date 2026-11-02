package ir.hoboho.shooting.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hoboho.shooting.R
import ir.hoboho.shooting.core.backup.BackupException
import ir.hoboho.shooting.core.backup.BackupManager
import ir.hoboho.shooting.core.common.StringProvider
import ir.hoboho.shooting.core.date.JalaliCalendar
import ir.hoboho.shooting.core.format.DateFormatter
import ir.hoboho.shooting.core.share.FileSharer
import ir.hoboho.shooting.domain.backup.BackupInspection
import ir.hoboho.shooting.domain.backup.RestoreStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val strategy: RestoreStrategy = RestoreStrategy.ADD_ONLY,
    val inspection: BackupInspection? = null,
    val exportedAtText: String = "",
    val busy: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val manager: BackupManager,
    private val sharer: FileSharer,
    private val strings: StringProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()
    private var pendingText: String? = null

    fun createAndShare() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val file = manager.createBackup()
                sharer.shareBackup(file)
                show(R.string.backup_created)
            } catch (t: Throwable) {
                show(R.string.msg_backup_failed)
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun inspect(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val text = manager.read(uri)
                pendingText = text
                val inspection = manager.inspect(text, _state.value.strategy)
                _state.update {
                    it.copy(
                        inspection = inspection,
                        exportedAtText = formatExportTime(inspection.preview.exportedAtEpochMilli),
                    )
                }
            } catch (e: BackupException.UnsupportedVersion) {
                pendingText = null
                _state.update { it.copy(inspection = null) }
                show(R.string.backup_unsupported)
            } catch (t: Throwable) {
                pendingText = null
                _state.update { it.copy(inspection = null) }
                show(R.string.backup_invalid)
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun setStrategy(strategy: RestoreStrategy) {
        _state.update { it.copy(strategy = strategy) }
        val text = pendingText ?: return
        viewModelScope.launch {
            try {
                val inspection = manager.inspect(text, strategy)
                _state.update {
                    it.copy(
                        inspection = inspection,
                        exportedAtText = formatExportTime(inspection.preview.exportedAtEpochMilli),
                    )
                }
            } catch (t: Throwable) {
                show(R.string.backup_invalid)
            }
        }
    }

    fun restore() {
        val text = pendingText ?: return
        if (_state.value.inspection?.warnings?.isNotEmpty() == true) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                manager.restore(text, _state.value.strategy)
                show(R.string.backup_restored)
            } catch (t: Throwable) {
                show(R.string.msg_restore_failed)
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    private fun show(resId: Int) = _state.update { it.copy(message = strings.get(resId)) }

    private fun formatExportTime(epochMilli: Long): String {
        if (epochMilli <= 0L) return strings.get(R.string.value_not_set)
        val epochDay = JalaliCalendar.fromEpochMilli(epochMilli)
        return DateFormatter.format(epochDay)
    }
}
