package eu.eurostat.core.charts

/**
 * Back-compatibility aliases that preserve the original top-level chart model types
 * after the model classes were moved into the [eu.eurostat.core.charts.model] subpackage.
 *
 * Existing feature modules import [ChartPoint] / [ChartSeries] / [ChartAxis] / [ColorScale]
 * from this package; the aliases let them keep working without churn.
 */
typealias ChartPoint = eu.eurostat.core.charts.model.ChartPoint

/** Alias preserved for callers importing [ChartSeries] from the top-level package. */
typealias ChartSeries = eu.eurostat.core.charts.model.ChartSeries

/** Alias preserved for callers importing [ChartAxis] from the top-level package. */
typealias ChartAxis = eu.eurostat.core.charts.model.ChartAxis

/** Alias preserved for callers importing [ColorScale] from the top-level package. */
typealias ColorScale = eu.eurostat.core.charts.model.ColorScale
