package eu.eurostat.feature.transport.ui

import eu.eurostat.feature.transport.domain.TransportMode

sealed interface TransportIntent {
    /** User tapped a country chip — switches the focused country for charts. */
    data class SelectActiveCountry(val code: String) : TransportIntent
    /** User confirmed a country picker selection — replaces fetched country set and re-fetches. */
    data class SelectCountries(val codes: List<String>) : TransportIntent
    data class ChangeYearRange(val range: IntRange) : TransportIntent
    data class ChangeMode(val mode: TransportMode) : TransportIntent
    /** User selected a panel visibility filter (ROAD / AIR / ALL). */
    data class SelectPanelMode(val mode: TransportPanelMode) : TransportIntent
    /** User toggled the logarithmic y-axis scale. */
    data object ToggleLogScale : TransportIntent
    /** User selected a year from the year dropdown. */
    data class SelectYear(val year: Int) : TransportIntent
    data object Refresh : TransportIntent
    data object Retry : TransportIntent
}
