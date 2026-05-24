package eu.eurostat.feature.tourism.ui

import eu.eurostat.feature.tourism.domain.TourismResidence

/**
 * All user actions the tourism screen can dispatch into [TourismComponent].
 *
 * [HighlightResidence] only changes which series is emphasised in the UI — it
 * does not retrigger a fetch (the repo always returns all three residences).
 */
sealed interface TourismIntent {
    data class SelectCountries(val codes: List<String>) : TourismIntent
    data class ChangeYearRange(val range: IntRange) : TourismIntent
    data class SelectActiveCountry(val code: String) : TourismIntent
    data class HighlightResidence(val residence: TourismResidence) : TourismIntent
    data class SelectYear(val year: Int) : TourismIntent
    data object Refresh : TourismIntent
    data object Retry : TourismIntent
}
