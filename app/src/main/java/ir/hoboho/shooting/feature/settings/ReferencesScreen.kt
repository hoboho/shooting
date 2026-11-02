@file:OptIn(ExperimentalMaterial3Api::class)

package ir.hoboho.shooting.feature.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import ir.hoboho.shooting.ui.components.SectionCard
import ir.hoboho.shooting.ui.components.ShootingButton
import ir.hoboho.shooting.ui.components.ShootingNumberField
import ir.hoboho.shooting.ui.components.ShootingTextField

@Composable
fun ReferencesScreen(viewModel: ReferencesViewModel = hiltViewModel()) {
    val cableTypes by viewModel.cableTypes.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var newCore by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("") }

    LaunchedEffect(error) { error?.let { snackbar.showSnackbar(it); viewModel.consumeError() } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.settings_reference_data)) })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard(title = stringResource(R.string.settings_cable_types)) {
                    cableTypes.forEach { type ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(type.label, modifier = Modifier.weight(1f))
                            Switch(checked = type.active,
                                onCheckedChange = { viewModel.toggleCableType(type.id, it) })
                        }
                    }
                    ShootingNumberField(
                        value = newCore,
                        onValueChange = { newCore = it },
                        label = stringResource(R.string.settings_core_count),
                        grouped = false,
                    )
                    ShootingButton(
                        text = stringResource(R.string.settings_add_cable),
                        onClick = { viewModel.addCableType(newCore); newCore = "" },
                    )
                }
            }
            item {
                SectionCard(title = stringResource(R.string.settings_expense_categories)) {
                    categories.forEach { category ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(category.name, modifier = Modifier.weight(1f))
                            Switch(checked = category.active,
                                onCheckedChange = { viewModel.toggleCategory(category.id, it) })
                        }
                    }
                    ShootingTextField(newCategory, { newCategory = it },
                        stringResource(R.string.settings_category_name))
                    ShootingButton(
                        text = stringResource(R.string.settings_add_category),
                        onClick = { viewModel.addCategory(newCategory); newCategory = "" },
                    )
                }
            }
        }
    }
}
