package eu.eurostat.feature.transport.ui

import eu.eurostat.feature.transport.domain.TransportQuery
import eu.eurostat.feature.transport.domain.TransportTimeSeries

/** Controls which small-multiple panels are visible in the transport screen. */
enum class TransportPanelMode { ROAD, AIR, ALL }

sealed interface TransportUiState {
    data object Loading : TransportUiState
    data class Content(
        val series: List<TransportTimeSeries>,
        val isStale: Boolean,
        val query: TransportQuery,
        /** Country code currently in focus (drives headlines + panels). */
        val activeCountry: String,
        /** All country codes present in [series]. */
        val availableCountries: List<String>,
        /** Which panel(s) are shown: ROAD, AIR, or ALL. */
        val displayPanelMode: TransportPanelMode = TransportPanelMode.ALL,
        /** Whether the chart y-axis uses a logarithmic scale. */
        val logScale: Boolean = false,
        /** Year currently selected in the year dropdown (drives headline + stat tiles). */
        val selectedYear: Int,
        /** Union of years for the active country across both road and air datasets. */
        val availableYears: List<Int>,
    ) : TransportUiState
    data class Empty(val query: TransportQuery) : TransportUiState
    data class Error(val message: String, val canRetry: Boolean) : TransportUiState
}
