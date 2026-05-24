package eu.eurostat.feature.population.ui

sealed interface PopulationIntent {
    data class SelectCountries(val codes: List<String>) : PopulationIntent
    data class ChangeYearRange(val range: IntRange) : PopulationIntent
    /** User tapped a country chip — switches the focused country for the pyramid. */
    data class SelectActiveCountry(val code: String) : PopulationIntent
    /** User dragged the year slider — switches the focused year for the pyramid. */
    data class SelectYear(val year: Int) : PopulationIntent
    /** User tapped Total/Men/Women in the segmented control. */
    data class SelectMetric(val index: Int) : PopulationIntent
    data object Refresh : PopulationIntent
    data object Retry : PopulationIntent
}
