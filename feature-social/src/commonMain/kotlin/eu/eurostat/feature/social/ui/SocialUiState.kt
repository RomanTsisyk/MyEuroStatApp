package eu.eurostat.feature.social.ui

import eu.eurostat.core.common.AppError
import eu.eurostat.feature.social.domain.SocialQuery
import eu.eurostat.feature.social.domain.SocialTimeSeries

/** Keys for the three KPI tiles on the Social screen. */
enum class SocialKpiKey { POVERTY, AT_RISK, HEALTH }

sealed interface SocialUiState {
    data object Loading : SocialUiState
    data class Content(
        val series: List<SocialTimeSeries>,
        val isStale: Boolean,
        val query: SocialQuery,
        /** The country code currently highlighted in the chart. */
        val activeCountry: String,
        /** All country codes currently fetched (drives the chip row). */
        val availableCountries: List<String>,
        /** The KPI tile currently selected by the user. */
        val selectedKpiKey: SocialKpiKey = SocialKpiKey.POVERTY,
        /** The year range currently shown in the scrubber, derived from live data. */
        val displayYearRange: IntRange = query.yearRange,
        /** Year currently shown in the headline and KPI tiles. */
        val selectedYear: Int = displayYearRange.last,
        /** All years available for the active country (sorted ascending). */
        val availableYears: List<Int> = emptyList(),
    ) : SocialUiState
    data class Empty(val query: SocialQuery) : SocialUiState
    /**
     * The screen resolves user-facing text via [AppError.localizedMessage]
     * (`eu.eurostat.ui.component.states`), so error copy localizes and
     * follows runtime language switches.
     */
    data class Error(val error: AppError, val canRetry: Boolean) : SocialUiState
}
