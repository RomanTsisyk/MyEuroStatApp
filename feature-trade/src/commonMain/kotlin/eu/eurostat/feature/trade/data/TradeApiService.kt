package eu.eurostat.feature.trade.data

import eu.eurostat.feature.trade.domain.TradeDataPoint
import eu.eurostat.feature.trade.domain.TradeQuery

interface TradeApiService {
    suspend fun fetch(query: TradeQuery): List<TradeDataPoint>
}
