package eu.eurostat.feature.economy.domain

import eu.eurostat.core.common.Result
import kotlinx.coroutines.flow.Flow

interface EconomyRepository {
    fun observe(query: EconomyQuery): Flow<Result<List<EconomyTimeSeries>>>

    /** Force refresh, ignore cache freshness. Throws on failure. */
    suspend fun refresh(query: EconomyQuery)
}

class GetEconomyTimeSeriesUseCase(
    private val repository: EconomyRepository,
) {
    fun observe(query: EconomyQuery): Flow<Result<List<EconomyTimeSeries>>> =
        repository.observe(query)

    suspend fun refresh(query: EconomyQuery) = repository.refresh(query)
}
