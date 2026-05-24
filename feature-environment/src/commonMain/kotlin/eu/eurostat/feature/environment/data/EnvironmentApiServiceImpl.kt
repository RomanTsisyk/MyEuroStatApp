package eu.eurostat.feature.environment.data

import eu.eurostat.core.common.safeFetch
import eu.eurostat.core.network.EurostatApiClient
import eu.eurostat.feature.environment.domain.EnvironmentQuery
import eu.eurostat.feature.environment.domain.EnvironmentTimeSeries
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Fetches the three Eurostat datasets backing the Environment feature in parallel:
 *
 *  - `env_air_gge`  → GHG emissions, segmented across the three sector codes in one call
 *  - `nrg_bal_c`    → final energy consumption, segmented across three balance codes
 *  - `sdg_13_10`    → SDG-13 climate index (no sector dimension)
 *
 * SDG is treated as a secondary dataset: a failure is swallowed via [safeFetch] so the hero
 * chart can still render GHG/Energy. [CancellationException] is always rethrown by [safeFetch],
 * so sibling-coroutine cancellation from a primary-dataset failure is preserved.
 * GHG and Energy failures propagate.
 */
class EnvironmentApiServiceImpl(
    private val apiClient: EurostatApiClient,
) : EnvironmentApiService {

    override suspend fun fetch(query: EnvironmentQuery): List<EnvironmentTimeSeries> = coroutineScope {
        val geoValues = query.countryCodes
        val timeValues = (query.yearRange.first..query.yearRange.last).map { it.toString() }

        val ghgDeferred = async {
            apiClient.fetchDataset(
                datasetCode = DATASET_GHG,
                filters = mapOf(
                    "geo" to geoValues,
                    "time" to timeValues,
                    "src_crf" to GHG_SECTOR_CODES,
                    "airpol" to listOf("GHG"),
                    "unit" to listOf("MIO_T"),
                ),
            )
        }

        val energyDeferred = async {
            apiClient.fetchDataset(
                datasetCode = DATASET_ENERGY,
                filters = mapOf(
                    "geo" to geoValues,
                    "time" to timeValues,
                    "siec" to listOf("TOTAL"),
                    "nrg_bal" to ENERGY_SECTOR_CODES,
                    "unit" to listOf("KTOE"),
                ),
            )
        }

        val sdgDeferred = async {
            safeFetch(onError = { /* non-fatal: SDG overlay absent, GHG/Energy still shown */ }) {
                apiClient.fetchDataset(
                    datasetCode = DATASET_SDG,
                    filters = mapOf(
                        "geo" to geoValues,
                        "time" to timeValues,
                        "unit" to listOf("I90"),
                    ),
                )
            }
        }

        EnvironmentCellMapper.buildTimeSeries(
            ghgCells = ghgDeferred.await(),
            energyCells = energyDeferred.await(),
            sdgCells = sdgDeferred.await(),
        )
    }

    private companion object {
        const val DATASET_GHG = "env_air_gge"
        const val DATASET_ENERGY = "nrg_bal_c"
        const val DATASET_SDG = "sdg_13_10"
        val GHG_SECTOR_CODES = listOf("TOTX4_MEMO", "CRF1A3", "CRF1A2")
        val ENERGY_SECTOR_CODES = listOf("FC_E", "FC_TRA_E", "FC_IND_E")
    }
}
