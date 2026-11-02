@file:OptIn(ExperimentalMaterial3Api::class)

package ir.hoboho.shooting.feature.projects

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hoboho.shooting.R
import ir.hoboho.shooting.ui.components.ConfirmDialog
import ir.hoboho.shooting.ui.components.EmptyState
import ir.hoboho.shooting.ui.components.SectionCard
import ir.hoboho.shooting.ui.components.ShootingButton
import ir.hoboho.shooting.ui.components.TagChip

@Composable
fun ProjectsScreen(
    onNewProject: () -> Unit,
    onEditProject: (Long) -> Unit,
    onOpenProjectWorkdays: () -> Unit,
    viewModel: ProjectsViewModel = hiltViewModel(),
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(error) {
        error?.let { snackbar.showSnackbar(it); viewModel.consumeError() }
    }
    if (pendingDelete != null) {
        ConfirmDialog(
            title = stringResource(R.string.action_delete),
            message = stringResource(R.string.project_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { viewModel.delete(pendingDelete!!); pendingDelete = null },
            onDismiss = { pendingDelete = null },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text(stringResource(R.string.projects_title)) }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ShootingButton(
                    text = stringResource(R.string.project_new),
                    onClick = onNewProject,
                    icon = Icons.Filled.Add,
                )
            }
            if (projects.isEmpty()) {
                item { EmptyState(stringResource(R.string.projects_empty)) }
            }
            items(projects, key = { it.id }) { project ->
                SectionCard(
                    title = project.name,
                    subtitle = listOf(project.workArea, project.employer)
                        .filter { it.isNotBlank() }.joinToString(" — "),
                    trailing = { TagChip(stringResource(if (project.active) R.string.label_active else R.string.label_inactive)) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { onEditProject(project.id) }) {
                            Text(stringResource(R.string.action_edit))
                        }
                        TextButton(onClick = {
                            viewModel.setActive(project.id, !project.active)
                        }) {
                            Text(stringResource(if (project.active) R.string.action_archive else R.string.action_activate))
                        }
                        TextButton(onClick = onOpenProjectWorkdays) {
                            Text(stringResource(R.string.project_workdays))
                        }
                        IconButton(onClick = { pendingDelete = project.id }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
