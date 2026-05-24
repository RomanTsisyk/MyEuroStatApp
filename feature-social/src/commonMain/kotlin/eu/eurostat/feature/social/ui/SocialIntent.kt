package eu.eurostat.feature.social.ui

sealed interface SocialIntent {
    data class SelectCountries(val codes: List<String>) : SocialIntent
    data class SelectActiveCountry(val code: String) : SocialIntent
    data class ChangeYearRange(val range: IntRange) : SocialIntent
    /** Fired when the user taps a KPI tile to change the highlighted metric. */
    data class SelectKpiTile(val key: SocialKpiKey) : SocialIntent
    /** Fired when the user selects a year from the YearDropdown in the header. */
    data class SelectYear(val year: Int) : SocialIntent
    data object Refresh : SocialIntent
    data object Retry : SocialIntent
}
