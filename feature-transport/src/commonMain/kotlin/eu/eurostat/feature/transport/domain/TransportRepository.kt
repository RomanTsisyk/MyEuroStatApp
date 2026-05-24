package eu.eurostat.feature.transport.domain

import eu.eurostat.core.common.Result
import kotlinx.coroutines.flow.Flow

interface TransportRepository {
    fun observe(query: TransportQuery): Flow<Result<List<TransportTimeSeries>>>

    /** Force refresh, ignoring cache freshness. Throws on failure. */
    suspend fun refresh(query: TransportQuery)
}

class GetTransportTimeSeriesUseCase(
    private val repository: TransportRepository,
) {
    fun observe(query: TransportQuery): Flow<Result<List<TransportTimeSeries>>> =
        repository.observe(query)

    suspend fun refresh(query: TransportQuery) = repository.refresh(query)
}
