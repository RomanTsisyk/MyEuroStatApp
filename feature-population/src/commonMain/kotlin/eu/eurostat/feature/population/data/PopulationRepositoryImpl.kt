package eu.eurostat.feature.population.data

import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.network.toAppError
import eu.eurostat.feature.population.domain.PopulationData
import eu.eurostat.feature.population.domain.PopulationQuery
import eu.eurostat.feature.population.domain.PopulationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.hours

/**
 * Stale-while-revalidate repository.
 *
 * Cache freshness TTL: 12 hours.
 *
 * Flow contract:
 *   1. emit Loading
 *   2. read cache:
 *      - hit  -> emit Success(isStale = cacheAge > TTL)  (snapshots = empty map,
 *                because cohort rows are not persisted — see PopulationCacheDao)
 *      - miss -> proceed to network without intermediate emission
 *   3. fetch network:
 *      - success -> persist time-series + emit Success(isStale = false,
 *                   snapshots = freshly built from JSON-stat)
 *      - failure -> if cache hit existed, swallow; else emit Error
 */
class PopulationRepositoryImpl(
    private val api: PopulationApiService,
    private val dao: PopulationCacheDao,
    private val dispatchers: DispatcherProvider,
    private val clock: Clock = Clock.System,
) : PopulationRepository {

    private val ttl = 12.hours

    override fun observe(query: PopulationQuery): Flow<Result<PopulationData>> = flow {
        emit(Result.Loading)

        val cacheResult = dao.query(query)
        val hadCache = cacheResult != null

        if (cacheResult != null) {
            val isStale = (clock.now() - cacheResult.oldestFetchedAt) > ttl
            // Cohorts are not cached; emit empty snapshot map for the cached frame.
            emit(
                Result.Success(
                    PopulationData(timeSeries = cacheResult.series, snapshots = emptyMap()),
                    isStale,
                )
            )
            // Only stop here if the cache satisfies the query. When cohorts
            // are needed (pyramid hero), always revalidate — the cache layer
            // does not persist cohort data, so without this re-fetch the
            // pyramid would stay "loading…" forever on every fresh-cache hit.
            if (!isStale && !query.includeCohorts) return@flow
        }

        // Revalidation branch
        try {
            val fresh = api.fetchPopulation(query)
            dao.upsert(fresh.timeSeries, clock.now())
            emit(Result.Success(fresh, isStale = false))
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            if (!hadCache) {
                emit(Result.Error(t.toAppError()))
            }
            // else: stale cache was already emitted, suppress the network error
        }
    }.flowOn(dispatchers.io)

    override suspend fun refresh(query: PopulationQuery) {
        withContext(dispatchers.io) {
            val fresh = api.fetchPopulation(query)
            dao.upsert(fresh.timeSeries, clock.now())
        }
    }

}
