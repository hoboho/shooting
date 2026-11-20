@file:OptIn(ExperimentalMaterial3Api::class)

package ir.hoboho.shooting.feature.workdays

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hoboho.shooting.R
import ir.hoboho.shooting.core.format.DateFormatter
import ir.hoboho.shooting.core.format.NumberFormatter
import ir.hoboho.shooting.domain.matchesOperationSearch
import ir.hoboho.shooting.ui.components.EmptyState
import ir.hoboho.shooting.ui.components.SectionCard
import ir.hoboho.shooting.ui.components.ShootingButton
import ir.hoboho.shooting.ui.components.ShootingTextField
import ir.hoboho.shooting.ui.components.StatRow



@Composable
fun WorkdaysScreen(
    onNewWorkday: () -> Unit,
    onOpenWorkday: (Long) -> Unit,
    viewModel: WorkdaysViewModel = hiltViewModel(),
) {
    val workdays by viewModel.workdays.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val filtered = workdays.filter { it.matchesOperationSearch(query) }

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
                SectionCard(title = stringResource(R.string.search_and_filter), icon = Icons.Filled.FilterList) {
                    ShootingTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = stringResource(R.string.search_reports_hint),
                    )
                    Text(stringResource(R.string.search_result_count, NumberFormatter.formatInteger(filtered.size)))
                }
            }
            if (filtered.isEmpty()) {
                item { EmptyState(stringResource(R.string.label_no_data)) }
            }
            itemsIndexed(filtered, key = { _, item -> item.id }) { index, workday ->
                val swipeOffset = remember(workday.id) { Animatable(0f) }
                val swipeScope = rememberCoroutineScope()
                SectionCard(
                    title = workday.projectName,
                    icon = Icons.Filled.CalendarMonth,
                    subtitle = DateFormatter.formatLong(workday.dateEpochDay),
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = swipeOffset.value
                            val progress = (abs(swipeOffset.value) / 320f).coerceIn(0f, 1f)
                            scaleX = 1f - progress * 0.025f
                            alpha = 1f - progress * 0.12f
                        }
                        .clickable { onOpenWorkday(workday.id) }
                        .pointerInput(workday.id, filtered.size, index) {
                            var drag = 0f
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, amount ->
                                    change.consume()
                                    drag += amount
                                    val atEdge = (drag > 0f && index == 0) ||
                                        (drag < 0f && index == filtered.lastIndex)
                                    val visualOffset = if (atEdge) {
                                        val limit = size.width * 0.22f
                                        sign(drag) * limit * (1f - exp(-abs(drag) / (size.width.coerceAtLeast(1) * 0.55f)))
                                    } else {
                                        drag.coerceIn(-size.width.toFloat(), size.width.toFloat())
                                    }
                                    swipeScope.launch { swipeOffset.snapTo(visualOffset) }
                                },
                                onDragEnd = {
                                    val target = when {
                                        drag < -size.width * 0.22f && index < filtered.lastIndex -> filtered[index + 1].id
                                        drag > size.width * 0.22f && index > 0 -> filtered[index - 1].id
                                        else -> null
                                    }
                                    if (target != null) {
                                        swipeScope.launch {
                                            swipeOffset.animateTo(
                                                targetValue = sign(drag) * size.width * 0.85f,
                                                animationSpec = spring(dampingRatio = 0.82f, stiffness = 360f),
                                            )
                                            onOpenWorkday(target)
                                        }
                                    } else {
                                        swipeScope.launch {
                                            swipeOffset.animateTo(0f, spring(dampingRatio = 0.68f, stiffness = 420f))
                                        }
                                    }
                                    drag = 0f
                                },
                                onDragCancel = {
                                    swipeScope.launch { swipeOffset.animateTo(0f, spring()) }
                                    drag = 0f
                                },
                            )
                        },
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
