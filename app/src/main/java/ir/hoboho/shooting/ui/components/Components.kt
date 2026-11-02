package ir.hoboho.shooting.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ir.hoboho.shooting.R
import ir.hoboho.shooting.core.format.PersianDigits

/** One button hierarchy for the whole app. */
enum class ButtonLevel { Primary, Secondary, Outline, Text, Danger }

@Composable
fun ShootingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    level: ButtonLevel = ButtonLevel.Primary,
    icon: ImageVector? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
    fullWidth: Boolean = true,
) {
    val effective = if (fullWidth) modifier.fillMaxWidth() else modifier
    val active = enabled && !loading
    val content: @Composable RowScope.() -> Unit = {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = when (level) {
                    ButtonLevel.Primary, ButtonLevel.Danger -> MaterialTheme.colorScheme.onPrimary
                    ButtonLevel.Secondary -> MaterialTheme.colorScheme.onSecondaryContainer
                    ButtonLevel.Outline, ButtonLevel.Text -> MaterialTheme.colorScheme.primary
                },
            )
            if (text.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Text(text)
            }
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text)
        }
    }
    when (level) {
        ButtonLevel.Primary -> Button(onClick, enabled = active, modifier = effective, content = content)
        ButtonLevel.Secondary -> FilledTonalButton(onClick, enabled = active, modifier = effective, content = content)
        ButtonLevel.Outline -> OutlinedButton(onClick, enabled = active, modifier = effective, content = content)
        ButtonLevel.Text -> TextButton(onClick, enabled = active, modifier = effective, content = content)
        ButtonLevel.Danger -> Button(
            onClick, enabled = active, modifier = effective, content = content,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        )
    }
}

private val fieldColors @Composable get() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
)

@Composable
fun ShootingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    placeholder: String = "",
    suffix: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        suffix = suffix?.let { { Text(it) } },
        isError = isError,
        supportingText = errorMessage?.let { { Text(it) } },
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        shape = MaterialTheme.shapes.medium,
        colors = fieldColors,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Integer numeric field. The callback always receives ASCII digits; the user
 * always sees Persian digits with thousands separators.
 */
@Composable
fun ShootingNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    grouped: Boolean = true,
) {
    val display = if (grouped) PersianDigits.toPersianGrouped(value) else PersianDigits.toPersian(value)
    OutlinedTextField(
        value = display,
        onValueChange = { raw -> onValueChange(PersianDigits.normalizeNumberInput(raw)) },
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        isError = isError,
        supportingText = errorMessage?.let { { Text(it) } },
        enabled = enabled,
        readOnly = readOnly,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = MaterialTheme.shapes.medium,
        colors = fieldColors,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun StatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasised: Boolean = false,
    hint: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = if (emphasised) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                fontWeight = if (emphasised) FontWeight.Bold else FontWeight.Normal,
                color = if (emphasised) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            if (hint != null) {
                Text(text = hint, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = if (emphasised) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasised) FontWeight.Bold else FontWeight.Medium,
            color = if (emphasised) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** The only grouped container used across forms, summaries and reports. */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                trailing?.invoke()
            }
            Spacer(Modifier.size(10.dp))
            content()
        }
    }
}

@Composable
fun TagChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(
    options: List<T>,
    selected: T?,
    onSelected: (T) -> Unit,
    labelOf: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelected(option) },
                label = { Text(labelOf(option)) },
            )
        }
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Used only for destructive actions and full-replace restores. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
