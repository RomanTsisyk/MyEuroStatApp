package eu.eurostat.feature.environment.domain

import eu.eurostat.core.common.Result
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for environment data. Returns one [EnvironmentTimeSeries]
 * per country in the query, each carrying GHG, Energy (across all three
 * sectors) and SDG-13 points.
 *
 * The combined view bypasses the SQLDelight cache for now — the on-disk schema
 * only models a single sector code per row and predates the multi-metric merge.
 * Stale-while-revalidate will return once a JSON-blob cache lands.
 */
interface EnvironmentRepository {
    fun observe(query: EnvironmentQuery): Flow<Result<List<EnvironmentTimeSeries>>>

    /** Force a network refetch. Throws on failure. */
    suspend fun refresh(query: EnvironmentQuery)
}

/** Use-case wrapper preserved for symmetry with sibling feature modules. */
class GetEnvironmentTimeSeriesUseCase(
    private val repository: EnvironmentRepository,
) {
    fun observe(query: EnvironmentQuery): Flow<Result<List<EnvironmentTimeSeries>>> =
        repository.observe(query)

    suspend fun refresh(query: EnvironmentQuery) = repository.refresh(query)
}
