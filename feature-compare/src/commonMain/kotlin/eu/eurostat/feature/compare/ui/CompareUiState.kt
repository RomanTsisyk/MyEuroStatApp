package eu.eurostat.feature.compare.ui

import eu.eurostat.core.common.AppError
import eu.eurostat.feature.compare.domain.CompareIndicator
import eu.eurostat.feature.compare.domain.CompareSeries

/**
 * Sealed UI state for the cross-module Compare screen.
 *
 * - [Loading] first fetch with no data yet.
 * - [Content] one or more country series ready to overlay on the chart.
 * - [Empty]   the query succeeded but no country returned usable data.
 * - [Error]   terminal failure with no data, carrying the raw cause.
 */
sealed interface CompareUiState {

    /**
     * Initial load before any data (or between indicator/country changes).
     *
     * @property indicator the metric being (re)fetched — kept so the header
     *   accent and footer dataset don't flash back to a fallback mid-switch.
     */
    data class Loading(val indicator: CompareIndicator) : CompareUiState

    /**
     * Renderable comparison.
     *
     * @property indicator the metric being compared across countries.
     * @property countries selected country codes, in selection order (drives palette colours).
     * @property yearRange the year span actually present in [series] (chart x-domain).
     * @property normalized true when the chart rebases each series to 100 at its
     *   first visible point ("Indexed 100" mode); a pure view transform that
     *   never triggers a re-fetch.
     * @property series one [CompareSeries] per country that returned data.
     * @property isStale true when the data was served from cache past its TTL.
     */
    data class Content(
        val indicator: CompareIndicator,
        val countries: List<String>,
        val yearRange: IntRange,
        val normalized: Boolean,
        val series: List<CompareSeries>,
        val isStale: Boolean,
    ) : CompareUiState

    /**
     * Query succeeded but produced nothing to chart.
     *
     * @property indicator the metric that returned no data (kept for the footer / accent).
     * @property countries the countries that were queried.
     */
    data class Empty(
        val indicator: CompareIndicator,
        val countries: List<String>,
    ) : CompareUiState

    /**
     * Error branch.
     *
     * @property error raw [AppError] cause; the screen resolves user-facing text
     *   via [eu.eurostat.ui.component.states.localizedMessage] so copy follows
     *   the app locale.
     * @property canRetry whether the [CompareIntent.Refresh] retry action is offered.
     */
    data class Error(
        val error: AppError,
        val canRetry: Boolean,
    ) : CompareUiState
}
