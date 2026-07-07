package eu.eurostat.feature.economy.ui

import eu.eurostat.feature.economy.domain.EconomyMetric
import eu.eurostat.feature.economy.domain.EconomyQuery
import eu.eurostat.feature.economy.domain.EconomyTimeSeries

/**
 * Sealed UI state for the Economy feature screen.
 *
 * - [Loading] initial fetch with no cache.
 * - [Content] merged time series (GDP + HICP + deficit) ready to render;
 *   the screen picks which metric to plot from a local selection.
 * - [Empty]   no data returned for the active query.
 * - [Error]   terminal failure with a user-facing message and retry hint.
 */
sealed interface EconomyUiState {
    data object Loading : EconomyUiState

    /**
     * @property timeSeries merged per-country series; each point carries
     *   nullable values for the three metrics.
     * @property isStale true when the data was served from cache past TTL.
     * @property query the query that produced this state (kept for diagnostics).
     * @property activeCountry code of the currently highlighted country (headline + chart focus).
     * @property availableCountries ordered list of country codes present in [timeSeries].
     * @property selectedMetric the currently selected metric tab; survives rotation.
     * @property displayYearRange the client-side year filter applied to the chart; survives rotation.
     * @property selectedYear the year shown in the headline and stat tiles; defaults to the
     *   latest year available for the active country.
     * @property availableYears sorted ascending list of observation years for the active country.
     * @property normalized true when the chart is rebased to an index (first visible
     *   year = 100) for cross-country comparison; survives rotation.
     */
    data class Content(
        val timeSeries: List<EconomyTimeSeries>,
        val isStale: Boolean,
        val query: EconomyQuery,
        val activeCountry: String,
        val availableCountries: List<String>,
        val selectedMetric: EconomyMetric = EconomyMetric.Gdp,
        val displayYearRange: IntRange? = null,
        val selectedYear: Int = 0,
        val availableYears: List<Int> = emptyList(),
        val normalized: Boolean = false,
    ) : EconomyUiState

    data class Empty(val query: EconomyQuery) : EconomyUiState
    data class Error(val message: String, val canRetry: Boolean) : EconomyUiState
}
