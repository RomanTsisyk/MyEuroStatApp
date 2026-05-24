package eu.eurostat.feature.environment.data

import eu.eurostat.feature.environment.domain.EnvironmentQuery
import eu.eurostat.feature.environment.domain.EnvironmentTimeSeries
import kotlinx.datetime.Instant

/**
 * In-memory fake implementation of [EnvironmentCacheDao] for unit tests.
 *
 * Behaviour:
 *  - [query] returns [cacheResult] if set, otherwise null.
 *  - [upsert] records calls in [upsertCallCount] and caches the data.
 */
class FakeEnvironmentCacheDao : EnvironmentCacheDao {
    /** Pre-seeded cache result; null means cache miss. */
    var cacheResult: EnvironmentCacheResult? = null

    var upsertCallCount: Int = 0
    var lastUpsertedSeries: List<EnvironmentTimeSeries>? = null
    var lastUpsertedAt: Instant? = null

    override suspend fun query(query: EnvironmentQuery): EnvironmentCacheResult? = cacheResult

    override suspend fun upsert(series: List<EnvironmentTimeSeries>, fetchedAt: Instant) {
        upsertCallCount++
        lastUpsertedSeries = series
        lastUpsertedAt = fetchedAt
    }
}
