package eu.eurostat.feature.trade.ui

import eu.eurostat.core.common.AppError
import eu.eurostat.feature.trade.domain.TradeQuery
import eu.eurostat.feature.trade.domain.TradeTimeSeries

sealed interface TradeUiState {
    data object Loading : TradeUiState
    data class Content(
        val series: List<TradeTimeSeries>,
        val isStale: Boolean,
        val query: TradeQuery,
        /** The country code currently highlighted in the hero chart and KPI tiles. */
        val activeCountry: String,
        /** All country codes present in [series], in order. */
        val availableCountries: List<String>,
        /** Index of the currently selected Exports/Imports/Balance tab (survives rotation). Default: 2 = Balance. */
        val selectedTabIndex: Int = 2,
        /** Year currently shown in the headline and KPI tiles. Defaults to the latest year available for [activeCountry]. */
        val selectedYear: Int,
        /** All years with data for [activeCountry], sorted ascending. Drives the [YearDropdown] options. */
        val availableYears: List<Int>,
    ) : TradeUiState
    data class Empty(val query: TradeQuery) : TradeUiState

    /**
     * Error branch.
     *
     * @property error Raw [AppError] cause — the screen resolves user-facing
     *   text via [eu.eurostat.ui.component.states.localizedMessage], so the
     *   copy follows the app locale (including runtime language switches).
     * @property canRetry Whether the [TradeIntent.Retry] action should be offered.
     */
    data class Error(val error: AppError, val canRetry: Boolean) : TradeUiState
}
