@file:OptIn(ExperimentalMaterial3Api::class)

package ir.hoboho.shooting.feature.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard(title = stringResource(R.string.settings_appearance)) {
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
                SectionCard(title = stringResource(R.string.settings_reference_data)) {
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
                SectionCard(title = stringResource(R.string.settings_backup)) {
                    ShootingButton(
                        text = stringResource(R.string.settings_backup),
                        onClick = onOpenBackup,
                        level = ButtonLevel.Secondary,
                    )
                }
            }
            item {
                SectionCard(title = stringResource(R.string.settings_about)) {
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
