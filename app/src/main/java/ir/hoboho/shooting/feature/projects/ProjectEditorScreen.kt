@file:OptIn(ExperimentalMaterial3Api::class)

package ir.hoboho.shooting.feature.projects

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hoboho.shooting.R
import ir.hoboho.shooting.ui.components.ShootingButton
import ir.hoboho.shooting.ui.components.ShootingTextField

@Composable
fun ProjectEditorScreen(
    projectId: Long,
    onDone: () -> Unit,
    viewModel: ProjectEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(projectId) { viewModel.load(projectId) }
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    LaunchedEffect(state.error) { state.error?.let { snackbar.showSnackbar(it) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (projectId == 0L) R.string.project_new else R.string.project_edit)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ShootingTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = stringResource(R.string.label_project_name),
                isError = state.error != null,
                errorMessage = state.error,
            )
            ShootingTextField(state.workArea, viewModel::setWorkArea, stringResource(R.string.label_work_area))
            ShootingTextField(state.employer, viewModel::setEmployer, stringResource(R.string.label_employer))
            ShootingTextField(state.supervisor, viewModel::setSupervisor, stringResource(R.string.label_supervisor))
            ShootingTextField(
                state.description, viewModel::setDescription,
                stringResource(R.string.label_description), singleLine = false, minLines = 3,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.label_active), modifier = Modifier.weight(1f))
                Switch(checked = state.active, onCheckedChange = viewModel::setActive)
            }
            ShootingButton(
                text = stringResource(R.string.action_save),
                onClick = viewModel::save,
            )
        }
    }
}
