@file:OptIn(ExperimentalMaterial3Api::class)

package ir.hoboho.shooting.feature.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hoboho.shooting.R
import ir.hoboho.shooting.domain.model.ThemeMode
import ir.hoboho.shooting.ui.components.ButtonLevel
import ir.hoboho.shooting.ui.components.ChoiceChips
import ir.hoboho.shooting.ui.components.SectionCard
import ir.hoboho.shooting.ui.components.ShootingButton

@Composable
fun SettingsScreen(
    onOpenBackup: () -> Unit,
    onOpenReferences: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    var operatorName by rememberSaveable { mutableStateOf(settings.operatorName) }
    LaunchedEffect(settings.operatorName) { if (operatorName != settings.operatorName) operatorName = settings.operatorName }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard(title = "اطلاعات اپراتور", icon = Icons.Filled.Badge) {
                    Text("نامی که در گزارش‌های PDF درج می‌شود", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = operatorName,
                        onValueChange = { value -> operatorName = value },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("نام و نام خانوادگی") },
                        placeholder = { Text("نام اپراتور") },
                    )
                    ShootingButton(
                        text = "ذخیره نام اپراتور",
                        onClick = { viewModel.setOperatorName(operatorName.trim()) },
                        level = ButtonLevel.Secondary,
                        enabled = operatorName.trim() != settings.operatorName,
                    )
                }
            }
            item {
                SectionCard(title = stringResource(R.string.settings_appearance), icon = Icons.Filled.Palette) {
                    ChoiceChips(
                        options = ThemeMode.entries,
                        selected = settings.themeMode,
                        onSelected = viewModel::setThemeMode,
                        labelOf = { mode ->
                            stringResource(
                                when (mode) {
                                    ThemeMode.SYSTEM -> R.string.theme_system
                                    ThemeMode.LIGHT -> R.string.theme_light
                                    ThemeMode.DARK -> R.string.theme_dark
                                },
                            )
                        },
                    )
                }
            }
            item {
                SectionCard(title = stringResource(R.string.settings_reference_data), icon = Icons.Filled.Tune) {
                    ShootingButton(
                        text = stringResource(R.string.settings_cable_types),
                        onClick = onOpenReferences,
                        level = ButtonLevel.Outline,
                    )
                    ShootingButton(
                        text = stringResource(R.string.settings_expense_categories),
                        onClick = onOpenReferences,
                        level = ButtonLevel.Outline,
                    )
                }
            }
            item {
                SectionCard(title = stringResource(R.string.settings_backup), icon = Icons.Filled.Backup) {
                    ShootingButton(
                        text = stringResource(R.string.settings_backup),
                        onClick = onOpenBackup,
                        level = ButtonLevel.Secondary,
                    )
                }
            }
            item {
                SectionCard(title = stringResource(R.string.settings_updates), icon = Icons.Filled.SystemUpdate) {
                    Text(stringResource(R.string.settings_current_version, ir.hoboho.shooting.BuildConfig.APP_VERSION_NAME),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                    ShootingButton(
                        text = stringResource(R.string.settings_check_updates),
                        onClick = viewModel::checkForUpdates,
                        level = ButtonLevel.Secondary,
                        loading = updateState.isChecking,
                        enabled = !updateState.isChecking,
                    )
                    if (updateState.message != null) {
                        Text(updateState.message!!,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (updateState.updateAvailable) {
                        ShootingButton(
                            text = stringResource(R.string.settings_install_update),
                            onClick = viewModel::installUpdate,
                            level = ButtonLevel.Primary,
                        )
                    }
                }
            }
            item {
                SectionCard(title = stringResource(R.string.settings_about), icon = Icons.Filled.Info) {
                    Text(stringResource(R.string.settings_about_text),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                    Text("${stringResource(R.string.app_name)} ${ir.hoboho.shooting.core.backup.AppInfo.VERSION_NAME}",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
