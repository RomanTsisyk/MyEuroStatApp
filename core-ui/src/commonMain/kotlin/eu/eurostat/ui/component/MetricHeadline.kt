package eu.eurostat.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.eurostat.ui.theme.Euro

/**
 * Large headline metric paired with a unit, a subtitle and a year pill.
 *
 * Layout:
 *   [value tabular] [unit muted]                       [● year]
 *   [subtitle muted]
 *
 * @param value the numeric value rendered in tabular monospace.
 * @param unit the unit suffix rendered smaller and muted (e.g. `"M"`, `"%"`).
 * @param subtitle short caption rendered below the value.
 * @param year the year string rendered inside the trailing pill.
 * @param accent dot color inside the year pill (typically the module accent).
 */
@Composable
fun MetricHeadline(
    value: String,
    unit: String,
    subtitle: String,
    year: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = Euro.typography.displayMedium.copy(
                        fontFeatureSettings = "tnum",
                    ),
                    color = Euro.colors.ink,
                )
                Spacer(Modifier.width(Euro.spacing.s))
                Text(
                    text = unit,
                    style = Euro.typography.headlineSmall,
                    color = Euro.colors.muted,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Text(
                text = subtitle,
                style = Euro.typography.bodySmall,
                color = Euro.colors.muted,
            )
        }
        YearPill(year = year, accent = accent)
    }
}

@Composable
private fun YearPill(year: String, accent: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Euro.colors.surface2, Euro.shapes.pill)
            .padding(horizontal = Euro.spacing.m, vertical = Euro.spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(accent, CircleShape),
        )
        Spacer(Modifier.width(Euro.spacing.s))
        Text(
            text = year,
            style = Euro.typography.tabularNumSmall,
            color = Euro.colors.muted,
        )
    }
}
