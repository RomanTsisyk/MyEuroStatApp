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
import androidx.compose.ui.unit.dp
import eu.eurostat.ui.theme.Euro
import myeurostatapp.core_ui.generated.resources.Res
import myeurostatapp.core_ui.generated.resources.ui_year_dropdown_label
import org.jetbrains.compose.resources.stringResource

/**
 * Pill-shaped dropdown for selecting a single year. Renders `"year: 2024 ▾"`
 * and opens a Material 3 dropdown menu listing [years] in descending order
 * (most recent first), with the currently [selectedYear] highlighted as the
 * default label.
 *
 * Default state: the screen's component pre-selects the latest available year
 * for the active country, so the dropdown opens already showing fresh data
 * without any user interaction.
 *
 * Tap target: 48dp minimum via [defaultMinSize], matching WCAG 2.1 AA and the
 * rest of the design system's interactive controls.
 *
 * @param selectedYear year currently displayed in the headline.
 * @param years selectable years; typically the union of observation years for
 *   the active country, sorted ascending.
 * @param onSelect invoked with the chosen year when the user taps an item.
 */
@Composable
fun YearDropdown(
    selectedYear: Int,
    years: List<Int>,
    onSelect: (Int) -> Unit,
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
            text = "${stringResource(Res.string.ui_year_dropdown_label)} ",
            style = Euro.typography.labelLarge,
            color = Euro.colors.muted,
        )
        Text(
            text = selectedYear.toString(),
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
            // Most-recent first so the default landing position in the menu
            // is the user's most likely target.
            years.sortedDescending().forEach { year ->
                DropdownMenuItem(
                    text = { Text(year.toString(), style = Euro.typography.bodyMedium) },
                    onClick = {
                        expanded = false
                        onSelect(year)
                    },
                )
            }
        }
    }
}
