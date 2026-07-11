package eu.eurostat.feature.science.ui

import eu.eurostat.core.common.AppError
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
    /**
     * Carries the raw [AppError] rather than a pre-resolved message string so the
     * screen can resolve user-facing text via `AppError.localizedMessage()`,
     * keeping error copy localized and reactive to runtime language switches.
     */
    data class Error(val error: AppError, val canRetry: Boolean) : ScienceUiState
}
