package eu.eurostat.feature.science.ui

import eu.eurostat.feature.science.domain.ScienceQuery
import eu.eurostat.feature.science.domain.ScienceTimeSeries

sealed interface ScienceUiState {
    data object Loading : ScienceUiState
    data class Content(
        val series: List<ScienceTimeSeries>,
        val isStale: Boolean,
        val query: ScienceQuery,
        /** The country whose sparklines / headline are displayed. */
        val activeCountry: String,
        /** Ordered list of all currently tracked country codes. */
        val availableCountries: List<String>,
        /** The year currently selected by the year picker. Null when the active country has no observations. */
        val selectedYear: Int?,
        /** All years with observations for the active country, sorted ascending. */
        val availableYears: List<Int>,
    ) : ScienceUiState
    data class Empty(val query: ScienceQuery) : ScienceUiState
    data class Error(val message: String, val canRetry: Boolean) : ScienceUiState
}
