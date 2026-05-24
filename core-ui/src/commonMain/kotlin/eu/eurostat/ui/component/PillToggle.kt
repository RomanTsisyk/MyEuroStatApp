package eu.eurostat.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.eurostat.ui.theme.Euro

/**
 * Inline pill toggle: a single fully-rounded capsule containing N text
 * segments. Active segment is filled with the active color; siblings are
 * transparent over the parent surface.
 *
 * @param options labels in source order.
 * @param selectedIndex index of the active option.
 * @param onSelect callback invoked with the new index when tapped.
 * @param activeColor fill color for the active segment.
 */
@Composable
fun PillToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Euro.colors.accent,
) {
    Row(
        modifier = modifier
            .clip(Euro.shapes.pill)
            .background(Euro.colors.surface2, Euro.shapes.pill)
            .padding(3.dp),
    ) {
        options.forEachIndexed { idx, label ->
            val selected = idx == selectedIndex
            val bg = if (selected) activeColor else Color.Transparent
            val fg = if (selected) Color.White else Euro.colors.ink
            Box(
                modifier = Modifier
                    .clip(Euro.shapes.pill)
                    .background(bg, Euro.shapes.pill)
                    .clickable { onSelect(idx) }
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(horizontal = Euro.spacing.m, vertical = Euro.spacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = Euro.typography.labelLarge, color = fg)
            }
        }
    }
}
