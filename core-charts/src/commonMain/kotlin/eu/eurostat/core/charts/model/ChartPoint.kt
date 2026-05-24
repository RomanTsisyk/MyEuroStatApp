package eu.eurostat.core.charts.model

/**
 * A single data point in a chart series.
 *
 * @property x The horizontal value (e.g. year as a Double).
 * @property y The vertical value (e.g. population count). Nullable to support
 *             missing observations (rendered as gaps in line and area charts).
 */
data class ChartPoint(val x: Double, val y: Double?)
