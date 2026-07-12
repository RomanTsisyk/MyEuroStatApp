package eu.eurostat.feature.population.ui

import eu.eurostat.core.common.AppError
import eu.eurostat.feature.population.domain.PopulationQuery
import eu.eurostat.feature.population.domain.PopulationSnapshot
import eu.eurostat.feature.population.domain.PopulationTimeSeries

/** UI state for the Population module. */
sealed interface PopulationUiState {
    data object Loading : PopulationUiState

    /**
     * Content branch.
     *
     * @property timeSeries Per-country trend (sex=T, age=TOTAL).
     * @property snapshot Snapshot for the currently selected (country, year), or
     *   `null` if cohorts are unavailable (e.g. only cached data has been emitted
     *   yet — the snapshot will populate once the network response arrives).
     * @property selectedCountry Country code currently in focus.
     * @property selectedYear Year currently in focus.
     * @property availableYears Sorted list of years for the selected country.
     * @property availableCountries Country codes present in [timeSeries].
     * @property selectedMetric 0 = Total, 1 = Men, 2 = Women.
     */
    data class Content(
        val timeSeries: List<PopulationTimeSeries>,
        val snapshot: PopulationSnapshot?,
        val selectedCountry: String,
        val selectedYear: Int,
        val availableYears: List<Int>,
        val availableCountries: List<String>,
        val selectedMetric: Int,
        val isStale: Boolean,
        val query: PopulationQuery,
    ) : PopulationUiState

    data class Empty(val query: PopulationQuery) : PopulationUiState

    /**
     * Error branch.
     *
     * @property error Raw [AppError] cause — the screen resolves user-facing
     *   text via [eu.eurostat.ui.component.states.localizedMessage], so the
     *   copy follows the app locale (including runtime language switches).
     * @property canRetry Whether the [PopulationIntent.Retry] action should be offered.
     */
    data class Error(val error: AppError, val canRetry: Boolean) : PopulationUiState
}
