package eu.eurostat.feature.trade.data

import eu.eurostat.feature.trade.domain.TradeDataPoint
import eu.eurostat.feature.trade.domain.TradeQuery

interface TradeCacheDao {
    suspend fun query(query: TradeQuery): List<TradeDataPoint>
    suspend fun upsertAll(rows: List<TradeDataPoint>, fetchedAt: Long)
    suspend fun oldestFetchedAt(query: TradeQuery): Long?
}
