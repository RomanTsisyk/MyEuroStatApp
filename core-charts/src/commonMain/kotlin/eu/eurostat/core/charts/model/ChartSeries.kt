package eu.eurostat.core.charts.model

import androidx.compose.ui.graphics.Color

/**
 * A named, colored ordered series of [ChartPoint]s.
 *
 * @property label Human-readable name for the series (e.g. country name).
 * @property color The encoding color for this series.
 * @property points Ordered list of data points; may contain null y for gaps.
 */
data class ChartSeries(
    val label: String,
    val color: Color,
    val points: List<ChartPoint>,
)
