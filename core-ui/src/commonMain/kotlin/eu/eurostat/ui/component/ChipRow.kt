package eu.eurostat.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import eu.eurostat.ui.theme.Euro

/**
 * Wrap-friendly row of selectable pill chips. Multiple chips can be active
 * simultaneously; the caller maintains the [selected] set.
 *
 * @param options labels rendered in source order.
 * @param selected current set of selected labels.
 * @param onToggle invoked with the tapped option's label.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRow(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Euro.spacing.s),
        verticalArrangement = Arrangement.spacedBy(Euro.spacing.s),
    ) {
        options.forEach { label ->
            val isSelected = label in selected
            val bg = if (isSelected) Euro.colors.ink else Euro.colors.paperAlt
            val fg = if (isSelected) Euro.colors.paper else Euro.colors.ink
            val borderColor = if (isSelected) Euro.colors.ink else Euro.colors.border
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(Euro.shapes.pill)
                    .background(bg, Euro.shapes.pill)
                    .border(1.dp, borderColor, Euro.shapes.pill)
                    .clickable { onToggle(label) }
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(horizontal = Euro.spacing.m, vertical = Euro.spacing.xs),
            ) {
                Text(
                    text = label,
                    style = Euro.typography.labelLarge,
                    color = fg,
                )
            }
        }
    }
}
