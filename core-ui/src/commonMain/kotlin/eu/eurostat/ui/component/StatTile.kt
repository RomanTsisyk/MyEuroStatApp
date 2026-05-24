package eu.eurostat.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.eurostat.ui.theme.Euro

/**
 * Small statistic tile composed of a label, a tabular value and an optional
 * delta string.
 *
 * @param label the field name (e.g. `"Δ vs prev year"`).
 * @param value the tabular value (e.g. `"+1.2%"`).
 * @param delta optional secondary line (e.g. `"vs 2023"`); omitted when blank.
 * @param bordered when true, draws a 1dp translucent border around the tile.
 */
@Composable
fun StatTile(
    label: String,
    value: String,
    delta: String = "",
    bordered: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val border = if (bordered) {
        Modifier.border(1.dp, Euro.colors.border, Euro.shapes.medium)
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .then(border)
            .padding(Euro.spacing.m),
    ) {
        Text(
            text = label.uppercase(),
            style = Euro.typography.labelSmall,
            color = Euro.colors.muted,
        )
        Text(
            text = value,
            style = Euro.typography.tabularNumLarge,
            color = Euro.colors.ink,
        )
        if (delta.isNotBlank()) {
            Text(
                text = delta,
                style = Euro.typography.bodySmall,
                color = Euro.colors.muted,
            )
        }
    }
}
