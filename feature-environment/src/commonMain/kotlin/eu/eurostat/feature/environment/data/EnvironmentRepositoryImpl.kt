package eu.eurostat.feature.environment.data

import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.network.toAppError
import eu.eurostat.feature.environment.domain.EnvironmentQuery
import eu.eurostat.feature.environment.domain.EnvironmentRepository
import eu.eurostat.feature.environment.domain.EnvironmentTimeSeries
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.hours

/**
 * Stale-while-revalidate environment repository.
 *
 * Cache freshness TTL: 12 hours.
 *
 * Flow contract:
 *   1. emit Loading
 *   2. read cache:
 *      - hit  → emit Success(isStale = cacheAge > TTL) immediately, then fall through
 *               to network to revalidate.
 *      - miss → proceed to network without an intermediate emission.
 *   3. fetch network:
 *      - success → persist data, emit Success(isStale = false).
 *      - failure → if cache hit existed, swallow silently; else emit Error.
 */
class EnvironmentRepositoryImpl(
    private val api: EnvironmentApiService,
    private val dao: EnvironmentCacheDao,
    private val dispatchers: DispatcherProvider,
    private val clock: Clock = Clock.System,
) : EnvironmentRepository {

    private val ttl = 12.hours

    override fun observe(query: EnvironmentQuery): Flow<Result<List<EnvironmentTimeSeries>>> = flow {
        emit(Result.Loading)

        val cacheResult = dao.query(query)
        val hadCache = cacheResult != null

        if (cacheResult != null) {
            val isStale = (clock.now() - cacheResult.oldestFetchedAt) > ttl
            emit(Result.Success(cacheResult.series, isStale))
        }

        // Revalidation — always run to get fresh data.
        try {
            val fresh = api.fetch(query)
            dao.upsert(fresh, clock.now())
            emit(Result.Success(fresh, isStale = false))
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            if (!hadCache) {
                emit(Result.Error(t.toAppError()))
            }
            // Stale cache already emitted — suppress network error silently.
        }
    }.flowOn(dispatchers.io)

    override suspend fun refresh(query: EnvironmentQuery) {
        withContext(dispatchers.io) {
            val fresh = api.fetch(query)
            dao.upsert(fresh, clock.now())
        }
    }
}
