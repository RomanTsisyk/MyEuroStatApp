package eu.eurostat.feature.tourism.ui

import eu.eurostat.feature.tourism.domain.TourismResidence
import eu.eurostat.feature.tourism.domain.TourismTimeSeries

/**
 * UI state for the tourism screen — a flat sealed hierarchy that maps 1:1
 * onto the four render paths in [TourismScreen].
 */
sealed interface TourismUiState {
    data object Loading : TourismUiState

    /**
     * @param timeSeries all countries' wide-row time series, sorted by code.
     * @param isStale true if the data was served from cache older than the TTL.
     * @param activeCountry the country whose chart + headline is currently shown.
     * @param highlightedResidence which residence series the user has highlighted
     *        via the chip row (drives the headline value).
     * @param heatmapCells row-major seasonality matrix (months × years) for the
     *        heatmap hero. Empty list means the chart falls back to placeholder data.
     * @param selectedYear the year currently displayed in the headline. Reflects the
     *        user's choice from [YearDropdown]; defaults to the latest year with any
     *        data for the active country.
     * @param availableYears years for which at least one residence has data for the
     *        active country. Sorted ascending so the dropdown shows them consistently.
     */
    data class Content(
        val timeSeries: List<TourismTimeSeries>,
        val isStale: Boolean,
        val activeCountry: String,
        val highlightedResidence: TourismResidence,
        val heatmapCells: List<List<Float>> = emptyList(),
        val selectedYear: Int = 0,
        val availableYears: List<Int> = emptyList(),
    ) : TourismUiState

    data object Empty : TourismUiState
    data class Error(val message: String, val canRetry: Boolean) : TourismUiState
}
