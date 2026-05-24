package eu.eurostat.feature.trade.domain

import eu.eurostat.core.common.Result
import kotlinx.coroutines.flow.Flow

interface TradeRepository {
    fun observe(query: TradeQuery): Flow<Result<List<TradeTimeSeries>>>

    /** Force refresh, ignoring cache freshness. Throws on failure. */
    suspend fun refresh(query: TradeQuery)
}

class GetTradeTimeSeriesUseCase(
    private val repository: TradeRepository,
) {
    fun observe(query: TradeQuery): Flow<Result<List<TradeTimeSeries>>> =
        repository.observe(query)

    suspend fun refresh(query: TradeQuery) = repository.refresh(query)
}
