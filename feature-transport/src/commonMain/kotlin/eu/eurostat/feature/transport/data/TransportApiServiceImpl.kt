package eu.eurostat.feature.transport.data

import eu.eurostat.core.network.EurostatApiClient
import eu.eurostat.feature.transport.domain.TransportDataPoint
import eu.eurostat.feature.transport.domain.TransportMode
import eu.eurostat.feature.transport.domain.TransportQuery
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class TransportApiServiceImpl(
    private val apiClient: EurostatApiClient,
) : TransportApiService {

    override suspend fun fetch(query: TransportQuery): List<TransportDataPoint> = coroutineScope {
        val geoValues = query.countryCodes
        val timeValues = (query.yearRange.first..query.yearRange.last).map { it.toString() }

        when (query.mode) {
            TransportMode.ROAD -> {
                val cells = apiClient.fetchDataset(
                    datasetCode = "road_pa_buscoa",
                    filters = mapOf(
                        "geo" to geoValues,
                        "time" to timeValues,
                        "unit" to listOf("THS_PAS"),
                        "tra_cov" to listOf("TOTAL"),
                        // NOTE: `vehicle` dim removed — earlier attempt to pin
                        // it caused INVALID_QUERY_DIMENSION on the live API.
                        // The dataset emits one cell per (geo, time) at this
                        // filter level, so the headline number is accurate.
                    ),
                )
                TransportCellMapper.mapRoadCells(cells)
            }

            TransportMode.AIR -> {
                val cells = apiClient.fetchDataset(
                    datasetCode = "avia_paoc",
                    filters = mapOf(
                        "geo" to geoValues,
                        "time" to timeValues,
                        "unit" to listOf("PAS"),
                        "tra_meas" to listOf("PAS_CRD"),
                        "tra_cov" to listOf("TOTAL"),
                        "schedule" to listOf("TOT"),
                        // NOTE: `partner` dim removed — earlier attempt to pin
                        // it caused INVALID_QUERY_DIMENSION on the live API.
                        // avia_paoc returns one cell per (geo, time) with the
                        // existing tra_meas/tra_cov/schedule slice, so the
                        // headline number is accurate without partner.
                    ),
                )
                TransportCellMapper.mapAirCells(cells)
            }

            TransportMode.SEA -> emptyList()

            TransportMode.ALL -> {
                val roadDeferred = async {
                    apiClient.fetchDataset(
                        datasetCode = "road_pa_buscoa",
                        filters = mapOf(
                            "geo" to geoValues,
                            "time" to timeValues,
                            "unit" to listOf("THS_PAS"),
                            "tra_cov" to listOf("TOTAL"),
                        ),
                    )
                }
                val airDeferred = async {
                    apiClient.fetchDataset(
                        datasetCode = "avia_paoc",
                        filters = mapOf(
                            "geo" to geoValues,
                            "time" to timeValues,
                            "unit" to listOf("PAS"),
                            "tra_meas" to listOf("PAS_CRD"),
                            "tra_cov" to listOf("TOTAL"),
                            "schedule" to listOf("TOT"),
                        ),
                    )
                }

                val roadCells = roadDeferred.await()
                val airCells = airDeferred.await()

                TransportCellMapper.mergeAll(
                    TransportCellMapper.mapRoadCells(roadCells),
                    TransportCellMapper.mapAirCells(airCells),
                    emptyList(),
                )
            }
        }
    }
}
