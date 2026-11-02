@file:OptIn(ExperimentalMaterial3Api::class)

package ir.hoboho.shooting.feature.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hoboho.shooting.R
import ir.hoboho.shooting.core.backup.BackupCounts
import ir.hoboho.shooting.core.format.NumberFormatter
import ir.hoboho.shooting.domain.backup.RestoreStrategy
import ir.hoboho.shooting.ui.components.ChoiceChips
import ir.hoboho.shooting.ui.components.ConfirmDialog
import ir.hoboho.shooting.ui.components.EmptyState
import ir.hoboho.shooting.ui.components.SectionCard
import ir.hoboho.shooting.ui.components.ShootingButton
import ir.hoboho.shooting.ui.components.StatRow
import ir.hoboho.shooting.ui.components.rememberFileSaveAction

@Composable
private fun countsText(counts: BackupCounts): String = stringResource(
    R.string.backup_counts,
    NumberFormatter.formatInteger(counts.projects),
    NumberFormatter.formatInteger(counts.workdays),
    NumberFormatter.formatInteger(counts.operations),
    NumberFormatter.formatInteger(counts.expenses),
    NumberFormatter.formatInteger(counts.settlements),
)

@Composable
fun BackupScreen(viewModel: BackupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val createAndShare = rememberFileSaveAction(viewModel::createAndShare)
    var confirmReplace by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.inspect(it) }
    }

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() }
    }

    if (confirmReplace) {
        ConfirmDialog(
            title = stringResource(R.string.backup_restore),
            message = stringResource(R.string.backup_restore_confirm),
            confirmLabel = stringResource(R.string.backup_restore),
            onConfirm = { confirmReplace = false; viewModel.restore() },
            onDismiss = { confirmReplace = false },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text(stringResource(R.string.backup_title)) }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard(title = stringResource(R.string.backup_create)) {
                    Text(stringResource(R.string.backup_magic_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ShootingButton(
                        text = stringResource(R.string.backup_create),
                        onClick = createAndShare,
                        enabled = !state.busy,
                    )
                }
            }
            item {
                SectionCard(title = stringResource(R.string.backup_preview)) {
                    ShootingButton(
                        text = stringResource(R.string.backup_pick),
                        onClick = { picker.launch(arrayOf("*/*")) },
                        level = ir.hoboho.shooting.ui.components.ButtonLevel.Outline,
                        enabled = !state.busy,
                    )
                    val inspection = state.inspection
                    if (inspection == null) {
                        EmptyState(stringResource(R.string.backup_preview_incoming))
                    } else {
                        val preview = inspection.preview
                        StatRow(stringResource(R.string.backup_format_version),
                            NumberFormatter.formatInteger(preview.formatVersion))
                        StatRow(stringResource(R.string.backup_exported_at), state.exportedAtText)
                        StatRow(stringResource(R.string.backup_preview_incoming),
                            countsText(preview.incoming))
                        StatRow(stringResource(R.string.backup_preview_existing),
                            countsText(preview.existing))
                        if (preview.warnings.isNotEmpty()) {
                            preview.warnings.forEach { warning ->
                                Text(warning,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            item {
                SectionCard(title = stringResource(R.string.backup_strategy)) {
                    ChoiceChips(
                        options = RestoreStrategy.entries,
                        selected = state.strategy,
                        onSelected = viewModel::setStrategy,
                        labelOf = { strategy ->
                            stringResource(
                                when (strategy) {
                                    RestoreStrategy.ADD_ONLY -> R.string.backup_strategy_add_only
                                    RestoreStrategy.MERGE -> R.string.backup_strategy_merge
                                    RestoreStrategy.REPLACE -> R.string.backup_strategy_replace
                                },
                            )
                        },
                    )
                    Text(stringResource(R.string.backup_strategy_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                ShootingButton(
                    text = stringResource(R.string.backup_restore),
                    onClick = {
                        if (state.strategy == RestoreStrategy.REPLACE) confirmReplace = true
                        else viewModel.restore()
                    },
                    enabled = !state.busy && state.inspection != null &&
                        state.inspection?.warnings?.isEmpty() == true,
                )
            }
        }
    }
}
