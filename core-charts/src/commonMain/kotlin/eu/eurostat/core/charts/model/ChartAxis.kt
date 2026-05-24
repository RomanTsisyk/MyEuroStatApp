package eu.eurostat.core.charts.model

import eu.eurostat.core.common.formatCompactNumber

/**
 * Axis description used by line / area charts.
 *
 * @property label Title for the axis (rendered by callers, not by charts themselves).
 * @property range Optional explicit value range. When null, charts auto-fit to the data.
 * @property formatter Converts axis values to display strings.
 *   Defaults to [formatCompactNumber] which produces compact K/M/B suffixes
 *   (e.g. `850000` → `"850K"`, `1234567` → `"1.2M"`).
 */
data class ChartAxis(
    val label: String,
    val range: ClosedFloatingPointRange<Float>? = null,
    val formatter: (Float) -> String = { formatCompactNumber(it.toDouble()) },
)
