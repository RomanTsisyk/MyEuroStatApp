package eu.eurostat.feature.compare.domain

/**
 * One (year, value) observation of a comparison series.
 *
 * [value] is nullable so that missing observations survive as gaps rather than
 * being dropped: the line chart breaks the line across a `null`, and the
 * "Indexed 100" rebasing preserves interior `null`s. The value is already
 * expressed in the indicator's display unit (e.g. GDP in billions of EUR, not
 * the raw millions returned by Eurostat) — see [CompareDataSource].
 *
 * @property year calendar year of the observation.
 * @property value value in the indicator's display unit, or `null` for a gap.
 */
data class CompareSeriesPoint(
    val year: Int,
    val value: Double?,
)

/**
 * A single country's line on the Compare chart: an ordered run of
 * [CompareSeriesPoint]s for one indicator.
 *
 * Points are ordered ascending by [CompareSeriesPoint.year] (the source domain
 * series are already year-sorted). The country's palette color is assigned by
 * the UI from the country's index in the current selection order — never stored
 * here — so any set of picked countries stays visually stable.
 *
 * @property countryCode Eurostat `geo` code (e.g. `"DE"`, `"PL"`).
 * @property points year-sorted observations; may contain `null` [CompareSeriesPoint.value] gaps.
 */
data class CompareSeries(
    val countryCode: String,
    val points: List<CompareSeriesPoint>,
)
