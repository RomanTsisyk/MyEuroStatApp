package eu.eurostat.feature.economy.ui

import eu.eurostat.core.charts.model.ChartSeries

/** Index-rebasing base: the first visible observation of each series scales to 100. */
private const val INDEX_BASE: Double = 100.0

/**
 * Rebases every series to a classic index where each series' FIRST point
 * equals [INDEX_BASE] (`value / first * 100`), making growth paths
 * comparable across countries with very different absolute levels.
 *
 * The caller passes series already filtered to the visible year range and
 * sorted ascending by x (year) — exactly what `buildChartSeries` produces —
 * so "first point" means "first observation inside the visible range".
 *
 * Series that cannot be rebased are dropped from the result rather than
 * rendered on the wrong (absolute) scale:
 * - empty series (no visible points),
 * - series whose first visible value is missing (a `null` gap),
 * - series whose first visible value is exactly zero (division by zero).
 *
 * `null` gaps after the first point are preserved as gaps. Labels and colors
 * are carried over unchanged.
 */
internal fun rebaseToIndex(series: List<ChartSeries>): List<ChartSeries> =
    series.mapNotNull { s ->
        val base = s.points.firstOrNull()?.y
        if (base == null || base == 0.0) {
            null
        } else {
            s.copy(points = s.points.map { p -> p.copy(y = p.y?.let { (it / base) * INDEX_BASE }) })
        }
    }
