package eu.eurostat.feature.environment.data

import eu.eurostat.feature.environment.domain.EnvironmentQuery
import eu.eurostat.feature.environment.domain.EnvironmentTimeSeries
import kotlinx.datetime.Instant

/** Cache-layer contract for environment data. */
interface EnvironmentCacheDao {
    /**
     * Returns all cached [EnvironmentTimeSeries] for the given query, along with the
     * oldest fetch timestamp, or null if the cache is empty for those parameters.
     */
    suspend fun query(query: EnvironmentQuery): EnvironmentCacheResult?

    /**
     * Upserts all rows from the provided time series with the given fetch timestamp.
     */
    suspend fun upsert(series: List<EnvironmentTimeSeries>, fetchedAt: Instant)
}

/** Snapshot returned from [EnvironmentCacheDao.query]. */
data class EnvironmentCacheResult(
    val series: List<EnvironmentTimeSeries>,
    val oldestFetchedAt: Instant,
)
