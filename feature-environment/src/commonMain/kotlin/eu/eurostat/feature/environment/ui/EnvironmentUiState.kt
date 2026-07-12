package eu.eurostat.feature.environment.ui

import eu.eurostat.core.common.AppError
import eu.eurostat.feature.environment.domain.EnvMetric
import eu.eurostat.feature.environment.domain.EnvSector
import eu.eurostat.feature.environment.domain.EnvironmentQuery
import eu.eurostat.feature.environment.domain.EnvironmentTimeSeries

/**
 * UI state for the Environment screen. The same Content payload backs every
 * metric/sector toggle — the screen filters [timeSeries] client-side.
 */
sealed interface EnvironmentUiState {
    data object Loading : EnvironmentUiState

    data class Content(
        val timeSeries: List<EnvironmentTimeSeries>,
        val isStale: Boolean,
        val query: EnvironmentQuery,
        /** The currently highlighted country code. */
        val activeCountry: String,
        /** Ordered list of country codes present in [timeSeries]. */
        val availableCountries: List<String>,
        /** Currently selected sector for GHG/Energy charts. */
        val activeSector: EnvSector = EnvSector.Total,
        /** Currently selected metric shown in the hero chart. */
        val activeMetric: EnvMetric = EnvMetric.Ghg,
        /** Year shown in the headline stat (does not affect the chart). */
        val selectedYear: Int = 0,
        /** All years available for the active country's series, ascending. */
        val availableYears: List<Int> = emptyList(),
    ) : EnvironmentUiState

    data class Empty(val query: EnvironmentQuery) : EnvironmentUiState

    /**
     * The screen resolves user-facing text via [AppError.localizedMessage]
     * (`eu.eurostat.ui.component.states`), so the copy localizes and follows
     * runtime language switches instead of being fixed at emission time.
     */
    data class Error(val error: AppError, val canRetry: Boolean) : EnvironmentUiState
}
