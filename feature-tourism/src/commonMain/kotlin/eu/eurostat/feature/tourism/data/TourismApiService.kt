package eu.eurostat.feature.tourism.data

import eu.eurostat.core.jsonstat.JsonStatCell
import eu.eurostat.feature.tourism.domain.TourismDataPoint
import eu.eurostat.feature.tourism.domain.TourismQuery

/**
 * Result of one tourism API fetch — flat (country, year) rows plus the
 * country-code → display-name map sourced from the JSON-stat dimension labels,
 * plus the monthly seasonality heatmap for the active country.
 *
 * [heatmapCells] is empty when the `tour_occ_nim` fetch fails or returns no data.
 */
data class TourismFetchResult(
    val points: List<TourismDataPoint>,
    val countryLabels: Map<String, String>,
    val heatmapCells: List<List<Float>> = emptyList(),
)

/**
 * Abstraction over the Eurostat tourism endpoints — kept narrow so it can
 * be faked in tests without standing up an HTTP client.
 */
interface TourismApiService {
    suspend fun fetch(query: TourismQuery): TourismFetchResult

    /**
     * Fetch monthly nights data from `tour_occ_nim` for the given country code
     * (NUTS-0 level, e.g. "DE"). Returns raw JSON-stat cells — the caller is
     * responsible for mapping them into heatmap rows via [TourismCellMapper].
     *
     * Returns an empty list on failure so the UI can fall back gracefully.
     */
    suspend fun fetchSeasonality(countryCode: String): List<JsonStatCell>
}
