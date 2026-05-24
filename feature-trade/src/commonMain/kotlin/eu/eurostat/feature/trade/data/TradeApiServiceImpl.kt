package eu.eurostat.feature.trade.data

import eu.eurostat.core.network.EurostatApiClient
import eu.eurostat.feature.trade.domain.TradeDataPoint
import eu.eurostat.feature.trade.domain.TradeQuery

class TradeApiServiceImpl(
    private val apiClient: EurostatApiClient,
) : TradeApiService {

    override suspend fun fetch(query: TradeQuery): List<TradeDataPoint> {
        val geoValues = query.countryCodes
        val timeValues = (query.yearRange.first..query.yearRange.last).map { it.toString() }

        val cells = apiClient.fetchDataset(
            datasetCode = "ext_lt_intratrd",
            filters = mapOf(
                "geo" to geoValues,
                "time" to timeValues,
                "partner" to listOf(query.partner),
                "indic_et" to listOf("MIO_EXP_VAL", "MIO_IMP_VAL", "MIO_BAL_VAL"),
                "sitc06" to listOf("TOTAL"),
            ),
        )

        return TradeCellMapper.mapCells(cells, query.partner)
    }
}
