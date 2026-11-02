package ir.hoboho.shooting.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.hoboho.shooting.R
import ir.hoboho.shooting.core.date.JalaliCalendar
import ir.hoboho.shooting.core.date.JalaliDate
import ir.hoboho.shooting.core.format.DateFormatter
import ir.hoboho.shooting.core.format.PersianDigits

/** A read-only field that opens a picker on tap (same shape as every text field). */
@Composable
fun ClickableField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        ShootingTextField(
            value = value,
            onValueChange = {},
            label = label,
            readOnly = true,
            leadingIcon = Icons.Filled.CalendarToday,
        )
        Box(modifier = Modifier.matchParentSize().clickable(onClick = onClick))
    }
}

/** Jalali month-grid date picker; Saturday is the first column. */
@Composable
fun JalaliDatePickerDialog(
    initialEpochDay: Long,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit,
) {
    val initial = JalaliCalendar.toJalali(initialEpochDay)
    var year by remember { mutableIntStateOf(initial.year) }
    var month by remember { mutableIntStateOf(initial.month) }
    val today = remember { JalaliCalendar.todayEpochDay() }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onDateSelected(today); onDismiss() }) {
                Text(stringResource(R.string.action_today))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        title = { Text(stringResource(R.string.date_picker_title)) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {
                        if (month == 1) { month = 12; year -= 1 } else month -= 1
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.calendar_month_previous))
                    }
                    Text(
                        text = PersianDigits.toPersian(JalaliDate.monthLabel(year, month)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    IconButton(onClick = {
                        if (month == 12) { month = 1; year += 1 } else month += 1
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.calendar_month_next))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    JalaliDate.shortWeekdayNames.forEach { name ->
                        Text(
                            text = name,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                val daysInMonth = JalaliCalendar.daysInMonth(year, month)
                val leading = JalaliCalendar.dayOfWeek(JalaliCalendar.toEpochDay(year, month, 1))
                val cells = ArrayList<Int?>()
                repeat(leading) { cells.add(null) }
                for (day in 1..daysInMonth) cells.add(day)

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(cells) { day ->
                        Box(modifier = Modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (day != null) {
                                val epoch = JalaliCalendar.toEpochDay(year, month, day)
                                val isSelected = epoch == initialEpochDay
                                val isToday = epoch == today
                                Surface(
                                    shape = CircleShape,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    modifier = Modifier.fillMaxSize().clickable {
                                        onDateSelected(epoch)
                                        onDismiss()
                                    },
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = PersianDigits.toPersian(day.toString()),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}

/** Single-select dropdown with the same field styling as the rest of the app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ShootingDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    onSelected: (T) -> Unit,
    labelOf: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected?.let(labelOf) ?: "",
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(labelOf(option)) },
                    onClick = { onSelected(option); expanded = false },
                )
            }
        }
    }
}

/** Compact date range selector used by reports and the dashboard. */
@Composable
fun DateRangeFields(
    startEpochDay: Long,
    endEpochDay: Long,
    onStartChanged: (Long) -> Unit,
    onEndChanged: (Long) -> Unit,
) {
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            ClickableField(
                label = stringResource(R.string.label_from_date),
                value = DateFormatter.format(startEpochDay),
                onClick = { pickingStart = true },
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            ClickableField(
                label = stringResource(R.string.label_to_date),
                value = DateFormatter.format(endEpochDay),
                onClick = { pickingEnd = true },
            )
        }
    }
    if (pickingStart) {
        JalaliDatePickerDialog(startEpochDay, { pickingStart = false }, onStartChanged)
    }
    if (pickingEnd) {
        JalaliDatePickerDialog(endEpochDay, { pickingEnd = false }, onEndChanged)
    }
}
