package eu.eurostat.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.eurostat.ui.theme.Euro

/**
 * A single KPI rendered inside [KpiTileSelector]. Carries identifying [key]
 * (used by selection) plus the three visible bits of text.
 *
 * @property key stable identifier (e.g. `"gdp"`).
 * @property label small caption shown at the top (e.g. `"GDP"`).
 * @property value tabular metric value (e.g. `"24.6T"`).
 * @property unit short unit string (e.g. `"€"`, `"%"`).
 */
data class KpiTile(
    val key: String,
    val label: String,
    val value: String,
    val unit: String,
)

/**
 * Three-up grid of selectable KPI tiles. The active tile gains a 1.5dp
 * border in the module accent and a small accent dot in the top-right
 * corner.
 *
 * @param tiles tiles to render, normally three per row.
 * @param selectedKey [KpiTile.key] of the currently selected tile.
 * @param onSelect invoked with the tapped tile's key.
 */
@Composable
fun KpiTileSelector(
    tiles: List<KpiTile>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    accent: androidx.compose.ui.graphics.Color = Euro.colors.accent,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Euro.spacing.s),
    ) {
        tiles.forEach { tile ->
            val selected = tile.key == selectedKey
            val borderColor = if (selected) accent else Euro.colors.border
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(if (selected) 1.5.dp else 1.dp, borderColor, Euro.shapes.large)
                    .background(Euro.colors.paperAlt, Euro.shapes.large)
                    .clickable { onSelect(tile.key) }
                    .padding(Euro.spacing.m),
            ) {
                Column {
                    Text(tile.label.uppercase(), style = Euro.typography.labelSmall, color = Euro.colors.muted)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            tile.value,
                            style = Euro.typography.tabularNumLarge,
                            color = Euro.colors.ink,
                        )
                        Text(
                            text = " " + tile.unit,
                            style = Euro.typography.bodySmall,
                            color = Euro.colors.muted,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
                if (selected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(6.dp)
                            .background(accent, CircleShape),
                    )
                }
            }
        }
    }
}
