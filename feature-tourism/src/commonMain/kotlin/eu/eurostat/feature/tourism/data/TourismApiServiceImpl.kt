package eu.eurostat.feature.tourism.data

import eu.eurostat.core.common.safeFetch
import eu.eurostat.core.jsonstat.JsonStatCell
import eu.eurostat.core.network.EurostatApiClient
import eu.eurostat.feature.tourism.domain.TourismQuery
import eu.eurostat.feature.tourism.domain.TourismResidence
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Live API service for the tourism feature.
 *
 * Fetches the three datasets in parallel:
 *  - `tour_occ_ninat` filtered by `c_resid` ∈ {DOM, FOR, TOTAL},
 *     `unit=NR` and `nace_r2` ∈ {I551, I552, I553} (hotels, camping,
 *     holiday resorts). The mapper sums across NACE categories per
 *     (geo, year, residence) so the wide-row model carries the full
 *     accommodation total.
 *  - `tour_dem_tttot` filtered by `unit=NR`, `purpose=TOTAL`, `duration=N_GE1`,
 *     `c_dest=WORLD`. These slice filters pin the response to exactly one
 *     cell per (geo, year) — the headline "all overnight trips" aggregate —
 *     preventing the mapper from summing across purpose/duration/c_dest
 *     combinations and producing inflated trip counts (5–20× actual).
 *     Per CLAUDE.md, this dataset has no `c_resid` dimension; that filter
 *     is intentionally absent. NOTE: the destination dimension is named
 *     `c_dest` (NOT `partner` — `partner` is for trade datasets).
 *  - `tour_occ_nim` — monthly nights for the primary country, used to build
 *     the seasonality heatmap.
 *
 * If the trips call or the seasonality call fails, they are swallowed gracefully
 * (trips becomes null; heatmap becomes empty). Failures in the nights call
 * propagate — the repository surfaces them.
 */
class TourismApiServiceImpl(
    private val apiClient: EurostatApiClient,
) : TourismApiService {

    override suspend fun fetch(query: TourismQuery): TourismFetchResult = coroutineScope {
        val years = query.yearRange.map { it.toString() }

        val nightsFilters = mapOf(
            "geo" to query.countryCodes,
            "time" to years,
            "c_resid" to listOf(
                TourismResidence.Domestic.code,
                TourismResidence.Foreign.code,
                TourismResidence.Total.code,
            ),
            "unit" to listOf("NR"),
            "nace_r2" to listOf("I551", "I552", "I553"),
        )
        // Slice filters ensure exactly one cell per (geo, year): the headline
        // "all overnight trips" aggregate across all purposes, all durations
        // (≥1 night), and world-level destination aggregation.  Without these pins
        // the dataset returns ~30 cells per (geo, year) and the mapper sums
        // them all, over-counting by a factor of 5–20.
        // Note: `c_resid` is intentionally absent — per CLAUDE.md tour_dem_tttot
        // has no c_resid dimension and adding it would return an empty response.
        // IMPORTANT: `c_dest` is the correct dimension name for destination
        // (NOT `partner` — `partner` is used by trade datasets, not tourism).
        val tripsFilters = mapOf(
            "geo" to query.countryCodes,
            "time" to years,
            "unit" to listOf("NR"),
            "purpose" to listOf("TOTAL"),
            "duration" to listOf("N_GE1"),
            "c_dest" to listOf("WORLD"),
        )

        val nightsDeferred = async { apiClient.fetchDataset("tour_occ_ninat", nightsFilters) }
        val tripsDeferred = async {
            safeFetch(onError = { println("[Tourism] Trips fetch failed: ${it.message}") }) {
                apiClient.fetchDataset("tour_dem_tttot", tripsFilters)
            } ?: emptyList()
        }
        // Fetch seasonality for the first (primary) NUTS-0 country in the query.
        val primaryCountry = query.countryCodes
            .firstOrNull { it.length == 2 } ?: query.countryCodes.firstOrNull()
        val seasonalityDeferred = async {
            if (primaryCountry != null) fetchSeasonality(primaryCountry) else emptyList()
        }

        val (nightsCells, tripsCells) = awaitAll(nightsDeferred, tripsDeferred)
        val seasonalityCells = seasonalityDeferred.await()

        TourismFetchResult(
            points = TourismCellMapper.mergeToDataPoints(nightsCells, tripsCells),
            countryLabels = TourismCellMapper.buildCountryLabels(nightsCells, tripsCells),
            heatmapCells = TourismCellMapper.toHeatmapCells(seasonalityCells),
        )
    }

    /**
     * Fetches monthly nights from `tour_occ_nim` for the given NUTS-0 country
     * code over the last [SEASONALITY_YEARS] years. Uses `TOTAL` for c_resid
     * and `I551`, `I552`, `I553` for nace_r2 to capture all accommodation types;
     * the mapper sums across nace_r2 values per (year, month).
     *
     * Failures are caught and logged — an empty list is returned so the UI
     * displays the placeholder grid instead of an error.
     */
    override suspend fun fetchSeasonality(countryCode: String): List<JsonStatCell> {
        val currentYear = Clock.System.now()
            .toLocalDateTime(TimeZone.UTC).year
        val startYear = currentYear - SEASONALITY_YEARS
        // Monthly time codes: "2020-01" .. "<currentYear>-12"
        val months = (startYear..currentYear).flatMap { yr ->
            (1..12).map { m -> "${yr}-${m.toString().padStart(2, '0')}" }
        }
        val filters = mapOf(
            "geo" to listOf(countryCode),
            "time" to months,
            "c_resid" to listOf("TOTAL"),
            "unit" to listOf("NR"),
            "nace_r2" to listOf("I551", "I552", "I553"),
        )
        return safeFetch(
            onError = { println("[Tourism] Seasonality fetch failed for $countryCode: ${it.message}") },
        ) {
            apiClient.fetchDataset("tour_occ_nim", filters)
        } ?: emptyList()
    }

    private companion object {
        /** Number of full years to look back for the seasonality heatmap. */
        const val SEASONALITY_YEARS = 5
    }
}
