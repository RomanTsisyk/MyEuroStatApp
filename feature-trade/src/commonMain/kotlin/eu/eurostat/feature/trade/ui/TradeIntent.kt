package eu.eurostat.feature.trade.ui

sealed interface TradeIntent {
    /** Updates the focused country for the hero chart and KPI tiles. No re-fetch. */
    data class SelectActiveCountry(val code: String) : TradeIntent

    /** Replaces the fetched country set and triggers a fresh data load. */
    data class SelectCountries(val codes: List<String>) : TradeIntent
    data class ChangeYearRange(val range: IntRange) : TradeIntent
    data class ChangePartner(val partner: String) : TradeIntent

    /** Switches the Exports/Imports/Balance tab; persisted in [TradeUiState.Content]. */
    data class SelectTab(val index: Int) : TradeIntent

    /** Updates the year shown in the headline and KPI tiles. No re-fetch. */
    data class SelectYear(val year: Int) : TradeIntent

    data object Refresh : TradeIntent
    data object Retry : TradeIntent
}
