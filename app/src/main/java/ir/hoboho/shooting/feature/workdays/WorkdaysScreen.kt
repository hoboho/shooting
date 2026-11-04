@file:OptIn(ExperimentalMaterial3Api::class)

package ir.hoboho.shooting.feature.workdays

import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hoboho.shooting.R
import ir.hoboho.shooting.core.date.JalaliCalendar
import ir.hoboho.shooting.core.format.DateFormatter
import ir.hoboho.shooting.core.format.NumberFormatter
import ir.hoboho.shooting.domain.model.Workday
import ir.hoboho.shooting.ui.components.ChoiceChips
import ir.hoboho.shooting.ui.components.DateRangeFields
import ir.hoboho.shooting.ui.components.EmptyState
import ir.hoboho.shooting.ui.components.SectionCard
import ir.hoboho.shooting.ui.components.ShootingButton
import ir.hoboho.shooting.ui.components.ShootingTextField
import ir.hoboho.shooting.ui.components.StatRow

private fun Workday.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return true
    return listOf(projectName, workArea, employer, supervisor, notes)
        .any { it.contains(query.trim(), ignoreCase = true) }
}

@Composable
fun WorkdaysScreen(
    onNewWorkday: () -> Unit,
    onOpenWorkday: (Long) -> Unit,
    viewModel: WorkdaysViewModel = hiltViewModel(),
) {
    val workdays by viewModel.workdays.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var projectFilter by remember { mutableStateOf<String?>(null) }
    val minDate = workdays.minOfOrNull { it.dateEpochDay } ?: JalaliCalendar.todayEpochDay()
    val maxDate = workdays.maxOfOrNull { it.dateEpochDay } ?: JalaliCalendar.todayEpochDay()
    var startDate by remember(workdays) { mutableStateOf(minDate) }
    var endDate by remember(workdays) { mutableStateOf(maxDate) }
    val projects = workdays.map { it.projectName }.distinct().sorted()
    val filtered = workdays.filter { day ->
        day.matchesQuery(query) &&
            (projectFilter == null || day.projectName == projectFilter) &&
            day.dateEpochDay in startDate..endDate
    }

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
            item {
                SectionCard(title = stringResource(R.string.search_and_filter)) {
                    ShootingTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = stringResource(R.string.search_reports_hint),
                    )
                    Text(stringResource(R.string.label_project_filter))
                    ChoiceChips(
                        options = listOf<String?>(null) + projects,
                        selected = projectFilter,
                        onSelected = { projectFilter = it },
                        labelOf = { it ?: stringResource(R.string.label_all_projects) },
                    )
                    DateRangeFields(
                        startEpochDay = startDate,
                        endEpochDay = endDate,
                        onStartChanged = { startDate = it.coerceAtMost(endDate) },
                        onEndChanged = { endDate = it.coerceAtLeast(startDate) },
                    )
                    Text(stringResource(R.string.search_result_count, NumberFormatter.formatInteger(filtered.size)))
                }
            }
            if (filtered.isEmpty()) {
                item { EmptyState(stringResource(R.string.label_no_data)) }
            }
            items(filtered, key = { it.id }) { workday ->
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
