package ir.hoboho.shooting.feature.workdays

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import ir.hoboho.shooting.core.format.DateFormatter
import ir.hoboho.shooting.core.format.NumberFormatter
import ir.hoboho.shooting.ui.components.EmptyState
import ir.hoboho.shooting.ui.components.SectionCard
import ir.hoboho.shooting.ui.components.ShootingButton
import ir.hoboho.shooting.ui.components.StatRow

@Composable
fun WorkdaysScreen(
    onNewWorkday: () -> Unit,
    onOpenWorkday: (Long) -> Unit,
    viewModel: WorkdaysViewModel = hiltViewModel(),
) {
    val workdays by viewModel.workdays.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.workdays_title)) }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ShootingButton(
                    text = stringResource(R.string.workday_new),
                    onClick = onNewWorkday,
                    icon = Icons.Filled.Add,
                )
            }
            if (workdays.isEmpty()) {
                item { EmptyState(stringResource(R.string.workdays_empty)) }
            }
            items(workdays, key = { it.id }) { workday ->
                SectionCard(
                    title = workday.projectName,
                    subtitle = DateFormatter.formatLong(workday.dateEpochDay),
                    modifier = Modifier.clickable { onOpenWorkday(workday.id) },
                ) {
                    StatRow(
                        label = stringResource(R.string.label_daily_total),
                        value = NumberFormatter.formatMeters(workday.totalLengthMeters),
                        emphasised = true,
                    )
                    StatRow(
                        label = stringResource(R.string.label_work_area),
                        value = workday.workArea.ifBlank { stringResource(R.string.value_not_set) },
                    )
                }
            }
        }
    }
}
