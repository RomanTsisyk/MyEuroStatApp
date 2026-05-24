package eu.eurostat.feature.trade.data

import eu.eurostat.feature.trade.domain.TradeDataPoint
import eu.eurostat.feature.trade.domain.TradeQuery

class FakeTradeApiService : TradeApiService {
    var willReturn: List<TradeDataPoint> = emptyList()
    var throwable: Throwable? = null
    var callCount: Int = 0

    override suspend fun fetch(query: TradeQuery): List<TradeDataPoint> {
        callCount++
        throwable?.let { throw it }
        return willReturn
    }
}
