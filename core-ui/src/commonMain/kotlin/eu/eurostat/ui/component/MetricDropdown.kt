package eu.eurostat.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import eu.eurostat.ui.theme.Euro
import androidx.compose.ui.unit.dp

/**
 * Pill-shaped dropdown button used to switch the active metric in a chart.
 * Renders `"{label}: {value} ▾"` and opens a Material 3 dropdown menu.
 *
 * @param label leading field name (e.g. `"metric"`).
 * @param value currently selected value rendered inline.
 * @param options selectable values.
 * @param onSelect invoked with the chosen option.
 */
@Composable
fun MetricDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(Euro.shapes.pill)
            .background(Euro.colors.paperAlt, Euro.shapes.pill)
            .border(1.dp, Euro.colors.border, Euro.shapes.pill)
            .clickable { expanded = true }
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = Euro.spacing.m, vertical = Euro.spacing.xs),
    ) {
        Text(
            text = "$label: ",
            style = Euro.typography.labelLarge,
            color = Euro.colors.muted,
        )
        Text(
            text = value,
            style = Euro.typography.labelLarge,
            color = Euro.colors.ink,
        )
        Spacer(Modifier.width(Euro.spacing.xs))
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = Euro.colors.ink,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, style = Euro.typography.bodyMedium) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}
