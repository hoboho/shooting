package ir.hoboho.shooting.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hoboho.shooting.domain.model.AppSettings
import ir.hoboho.shooting.domain.model.ThemeMode
import ir.hoboho.shooting.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val appUpdater: AppUpdater,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = repository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _updateState = MutableStateFlow(AppUpdateState())
    val updateState: StateFlow<AppUpdateState> = _updateState.asStateFlow()

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }

    fun checkForUpdates() {
        if (_updateState.value.isChecking) return
        viewModelScope.launch {
            _updateState.value = AppUpdateState(isChecking = true)
            _updateState.value = try {
                appUpdater.checkAndPrepare()
            } catch (error: Exception) {
                AppUpdateState(message = error.message ?: "بررسی به‌روزرسانی ناموفق بود.")
            }
        }
    }

    fun installUpdate() {
        val message = runCatching { appUpdater.installPreparedUpdate() }
            .getOrElse { it.message ?: "اجرای نصب‌کننده ناموفق بود." }
        if (message != null) {
            _updateState.value = _updateState.value.copy(message = message)
        }
    }
}
