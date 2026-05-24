package eu.eurostat.feature.science.ui

sealed interface ScienceIntent {
    /** Replace the full set of tracked countries and re-fetch. */
    data class SelectCountries(val codes: List<String>) : ScienceIntent
    /** Change which country's sparklines / headline are shown without re-fetching. */
    data class SelectActiveCountry(val code: String) : ScienceIntent
    data class ChangeYearRange(val range: IntRange) : ScienceIntent
    /** Change the displayed year for the headline and radar without re-fetching. */
    data class SelectYear(val year: Int) : ScienceIntent
    data object Refresh : ScienceIntent
    data object Retry : ScienceIntent
}
