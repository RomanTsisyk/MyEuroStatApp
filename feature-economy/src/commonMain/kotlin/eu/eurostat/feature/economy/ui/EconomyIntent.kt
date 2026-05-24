package eu.eurostat.feature.economy.ui

import eu.eurostat.feature.economy.domain.EconomyMetric
import eu.eurostat.feature.economy.domain.EconomyUnit

sealed interface EconomyIntent {
    /** Sets which country's series is highlighted in the chart and headline. */
    data class SelectActiveCountry(val code: String) : EconomyIntent

    /** Updates the set of fetched countries and triggers a re-fetch via the repository. */
    data class SelectCountries(val codes: List<String>) : EconomyIntent
    data class ChangeYearRange(val range: IntRange) : EconomyIntent
    data class ChangeUnit(val unit: EconomyUnit) : EconomyIntent
    data object Refresh : EconomyIntent
    data object Retry : EconomyIntent

    /** Switches the active metric tab (GDP / Inflation / Deficit); client-side only, no re-fetch. */
    data class SelectMetric(val metric: EconomyMetric) : EconomyIntent

    /** Updates the client-side year filter applied to the chart; does NOT trigger a re-fetch. */
    data class SetDisplayYearRange(val range: IntRange) : EconomyIntent

    /** Selects the year shown in the headline and stat tiles; client-side only, no re-fetch. */
    data class SelectYear(val year: Int) : EconomyIntent
}
