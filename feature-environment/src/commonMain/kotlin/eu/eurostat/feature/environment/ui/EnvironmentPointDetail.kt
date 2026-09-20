package eu.eurostat.feature.environment.ui

import eu.eurostat.core.charts.model.ChartPoint
import eu.eurostat.feature.environment.domain.EnvMetric
import kotlin.math.roundToInt

/**
 * Raw (unformatted, unlocalized) content of the chart-point detail sheet for a
 * tapped Environment hero-chart observation.
 *
 * @property countryCode Eurostat country/area code of the tapped series
 *   (the hero chart labels each series with its country code).
 * @property year Calendar year of the tapped observation.
 * @property value Metric value at that point in the chart's own unit, or `null`
 *   when the point carries no observation (rendered as an em dash by the screen).
 */
internal data class EnvironmentPointDetail(
    val countryCode: String,
    val year: Int,
    val value: Double?,
)

/**
 * Maps a tapped chart point to the sheet's raw fields. The hero chart plots the
 * metric value directly on the y axis (no indexed/rebased mode), so the point's
 * `y` already is the value to display and the x axis is the year.
 *
 * @param seriesLabel [eu.eurostat.core.charts.model.ChartSeries.label] of the
 *   tapped series — the country code, as set by `buildChartSeries`.
 * @param point The tapped point (`x` = year, `y` = metric value).
 */
internal fun tappedEnvironmentPoint(seriesLabel: String, point: ChartPoint): EnvironmentPointDetail =
    EnvironmentPointDetail(
        countryCode = seriesLabel,
        year = point.x.roundToInt(),
        value = point.y,
    )

/** Eurostat dataset code the hero-chart values of this metric are sourced from. */
internal fun EnvMetric.datasetCode(): String = when (this) {
    EnvMetric.Ghg -> "env_air_gge"
    EnvMetric.Energy -> "nrg_bal_c"
    EnvMetric.Sdg -> "sdg_13_10"
}
