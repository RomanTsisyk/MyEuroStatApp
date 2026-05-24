package eu.eurostat.feature.economy.data

import eu.eurostat.core.common.safeFetch
import eu.eurostat.core.network.EurostatApiClient
import eu.eurostat.feature.economy.domain.EconomyQuery
import eu.eurostat.feature.economy.domain.EconomyTimeSeries
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class EconomyApiServiceImpl(
    private val eurostatApiClient: EurostatApiClient,
) : EconomyApiService {

    /**
     * Fetch GDP, HICP, and government deficit in parallel and merge into enriched time series.
     *
     * Secondary datasets (HICP, deficit) use [safeFetch] so failures do not propagate —
     * the caller gets partial enrichment rather than a complete failure. [CancellationException]
     * is always rethrown by [safeFetch], so sibling-coroutine cancellation is preserved.
     */
    override suspend fun fetchEconomy(query: EconomyQuery): List<EconomyTimeSeries> = coroutineScope {
        val geo = query.countryCodes
        val time = query.yearRange.map { it.toString() }

        // Primary: GDP from nama_10_gdp
        val gdpDeferred = async {
            eurostatApiClient.fetchDataset(
                "nama_10_gdp",
                mapOf(
                    "geo" to geo,
                    "time" to time,
                    "na_item" to listOf("B1GQ"),
                    "unit" to listOf(query.unit.code),
                ),
            )
        }

        // Secondary: HICP all-items annual index (base 2015) from prc_hicp_aind
        val hicpDeferred = async {
            safeFetch(onError = { /* non-fatal: partial enrichment continues without HICP */ }) {
                eurostatApiClient.fetchDataset(
                    "prc_hicp_aind",
                    mapOf(
                        "geo" to geo,
                        "time" to time,
                        "coicop" to listOf("CP00"),
                        "unit" to listOf("INX_A_AVG"),
                    ),
                )
            }
        }

        // Secondary: Government deficit/surplus (% of GDP) from gov_10dd_edpt1
        val deficitDeferred = async {
            safeFetch(onError = { /* non-fatal: partial enrichment continues without deficit data */ }) {
                eurostatApiClient.fetchDataset(
                    "gov_10dd_edpt1",
                    mapOf(
                        "geo" to geo,
                        "time" to time,
                        "na_item" to listOf("B9"),
                        "unit" to listOf("PC_GDP"),
                        "sector" to listOf("S13"),
                    ),
                )
            }
        }

        val gdpCells = gdpDeferred.await()
        val hicpCells = hicpDeferred.await()
        val deficitCells = deficitDeferred.await()

        EconomyCellMapper.mergeIntoTimeSeries(gdpCells, hicpCells, deficitCells)
    }
}
