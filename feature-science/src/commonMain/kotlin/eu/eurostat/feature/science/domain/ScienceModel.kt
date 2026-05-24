package eu.eurostat.feature.science.domain

/**
 * A single observation for one country in one year across the three Science
 * % metrics. Any field may be null when the corresponding Eurostat dataset
 * lacks a value for that (geo, time) pair.
 */
data class ScienceDataPoint(
    val countryCode: String,
    val year: Int,
    val rdSpendPctGdp: Double? = null,
    val internetUsagePct: Double? = null,
    val tertiaryEducPct: Double? = null,
)

/**
 * Time series of [ScienceDataPoint] values for a single country, ordered by
 * year ascending. [countryName] is the human-readable label; falls back to
 * [countryCode] when not provided by the source.
 */
data class ScienceTimeSeries(
    val countryCode: String,
    val countryName: String,
    val points: List<ScienceDataPoint>,
)
