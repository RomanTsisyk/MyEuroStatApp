package eu.eurostat.feature.economy.domain

/**
 * The unit dimension supplied as the `unit` filter on the GDP dataset
 * (`nama_10_gdp`). The Economy screen always renders the value as
 * "current prices in million EUR", but the enum is preserved so the
 * query layer can request an alternative aggregate (chain-linked volume
 * percent change) when needed.
 */
enum class EconomyUnit(val code: String) {
    CP_MEUR("CP_MEUR"),
    CLV_PCH_PRE("CLV_PCH_PRE"),
}

/**
 * The three Economy metrics rendered by the segmented switcher.
 * Each metric corresponds to one Eurostat dataset:
 *
 * - [Gdp]       → `nama_10_gdp`     (`gdpEur`, million EUR)
 * - [Inflation] → `prc_hicp_aind`   (`hicpIndex`, 2015 = 100)
 * - [Deficit]   → `gov_10dd_edpt1`  (`deficitPctGdp`, % of GDP)
 */
enum class EconomyMetric { Gdp, Inflation, Deficit }

/**
 * A single country-year economy observation merging all three metric
 * datasets. Any field may be null when the underlying dataset has no
 * value for that country-year (sparse coverage is common for the
 * deficit series in particular).
 *
 * @property countryCode Eurostat geo code (e.g. `DE`, `EU27_2020`).
 * @property year Calendar year of the observation.
 * @property gdpEur GDP at current prices, in million EUR.
 * @property hicpIndex Harmonised consumer-price index (2015 = 100).
 * @property deficitPctGdp Government net lending/borrowing as % of GDP.
 */
data class EconomyDataPoint(
    val countryCode: String,
    val year: Int,
    val gdpEur: Long? = null,
    val hicpIndex: Double? = null,
    val deficitPctGdp: Double? = null,
) {
    /** Returns the value of the requested [metric], or null if absent. */
    fun fieldFor(metric: EconomyMetric): Double? = when (metric) {
        EconomyMetric.Gdp -> gdpEur?.toDouble()
        EconomyMetric.Inflation -> hicpIndex
        EconomyMetric.Deficit -> deficitPctGdp
    }
}

/**
 * Per-country time series of merged economy observations, sorted by [EconomyDataPoint.year].
 *
 * @property countryCode Eurostat geo code.
 * @property countryName Resolved label from `dimensionLabels["geo"]`, falls back to the code.
 * @property points Year-sorted list of merged observations.
 */
data class EconomyTimeSeries(
    val countryCode: String,
    val countryName: String,
    val points: List<EconomyDataPoint>,
)
