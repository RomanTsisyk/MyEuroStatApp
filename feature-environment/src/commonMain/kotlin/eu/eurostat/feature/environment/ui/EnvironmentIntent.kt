package eu.eurostat.feature.environment.ui

import eu.eurostat.feature.environment.domain.EnvMetric
import eu.eurostat.feature.environment.domain.EnvSector

/**
 * User-driven mutations of the Environment screen. Sector and metric selection
 * are stored in [EnvironmentUiState.Content] via the component so they survive
 * recomposition and can be asserted in tests.
 */
sealed interface EnvironmentIntent {
    /**
     * Highlight a single country in the chart without changing the query countries.
     * The active country is persisted in the component and reflected in [EnvironmentUiState.Content].
     */
    data class SelectActiveCountry(val code: String) : EnvironmentIntent

    /** Replace the full set of queried countries and reload. */
    data class SelectCountries(val codes: List<String>) : EnvironmentIntent

    /** Change the active sector for GHG / Energy charts (no-op for SDG metric). */
    data class SelectSector(val sector: EnvSector) : EnvironmentIntent

    /** Change the hero chart metric. */
    data class SelectMetric(val metric: EnvMetric) : EnvironmentIntent

    /** Change the headline year without reloading chart data. */
    data class SelectYear(val year: Int) : EnvironmentIntent

    data class ChangeYearRange(val range: IntRange) : EnvironmentIntent
    data object Refresh : EnvironmentIntent
    data object Retry : EnvironmentIntent
}
