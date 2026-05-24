package eu.eurostat.feature.population.domain

import eu.eurostat.core.common.Result
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract. Lives in domain/, implemented in data/.
 *
 * The Flow contract is stale-while-revalidate:
 *   1. emit Loading
 *   2. emit Success(cached, isStale = true) if cache hit and age > TTL
 *   3. emit Success(fresh, isStale = false) when network succeeds
 *      OR emit Error if network fails and no cache existed
 *
 * Repository never throws — all failure paths become Result.Error.
 *
 * The wrapped [PopulationData] contains both per-country time series (for trend
 * visuals) and per-(country, year) snapshots (for the demographic pyramid).
 * Cached emissions populate `timeSeries`; `snapshots` is populated only on
 * fresh network responses because the SQLDelight cache does not store cohort
 * rows — see `PopulationCacheDao` for details.
 */
interface PopulationRepository {
    fun observe(query: PopulationQuery): Flow<Result<PopulationData>>

    /** Force refresh, ignore cache freshness. Used by pull-to-refresh. Throws on failure. */
    suspend fun refresh(query: PopulationQuery)
}

/**
 * Single-purpose use case. Forwards observe/refresh to the repository.
 */
class GetPopulationTimeSeriesUseCase(
    private val repository: PopulationRepository,
) {
    fun observe(query: PopulationQuery): Flow<Result<PopulationData>> =
        repository.observe(query)

    suspend fun refresh(query: PopulationQuery) = repository.refresh(query)
}
