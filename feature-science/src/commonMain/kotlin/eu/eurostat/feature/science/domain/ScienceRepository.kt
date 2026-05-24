package eu.eurostat.feature.science.domain

import eu.eurostat.core.common.Result
import kotlinx.coroutines.flow.Flow

interface ScienceRepository {
    fun observe(query: ScienceQuery): Flow<Result<List<ScienceTimeSeries>>>
    suspend fun refresh(query: ScienceQuery)
}

class GetScienceTimeSeriesUseCase(
    private val repository: ScienceRepository,
) {
    fun observe(query: ScienceQuery): Flow<Result<List<ScienceTimeSeries>>> =
        repository.observe(query)

    suspend fun refresh(query: ScienceQuery) = repository.refresh(query)
}
