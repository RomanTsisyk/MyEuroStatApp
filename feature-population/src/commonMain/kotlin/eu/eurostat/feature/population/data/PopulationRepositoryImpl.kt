package eu.eurostat.feature.population.data

import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.common.cache.JsonBlobCache
import eu.eurostat.core.common.safeFetch
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
 * Two cache tiers:
 *  - [cohortCache] — one JSON blob per cohort query holding the full
 *    [PopulationData] frame (time series **and** pyramid snapshots). A hit
 *    fully satisfies `includeCohorts` queries, so the pyramid works offline.
 *  - [dao] — the legacy flat `age=TOTAL` table. Fallback when the blob misses
 *    (e.g. first run after an app update) and the only tier consulted for
 *    trend-only queries (`includeCohorts = false`).
 *
 * Flow contract:
 *   1. emit Loading
 *   2. read cache (blob first for cohort queries, then the flat table):
 *      - hit  -> emit Success(isStale = cacheAge > TTL)
 *      - miss -> proceed to network without intermediate emission
 *   3. fetch network unless a fresh cache hit already satisfied the query
 *      (a flat-table hit never satisfies a cohort query — it has no snapshots):
 *      - success -> persist both tiers + emit Success(isStale = false)
 *      - failure -> if any cache hit was emitted, swallow; else emit Error
 */
class PopulationRepositoryImpl(
    private val api: PopulationApiService,
    private val dao: PopulationCacheDao,
    private val cohortCache: JsonBlobCache<PopulationCacheBlob>,
    private val dispatchers: DispatcherProvider,
    private val clock: Clock = Clock.System,
) : PopulationRepository {

    private val ttl = 12.hours

    override fun observe(query: PopulationQuery): Flow<Result<PopulationData>> = flow {
        emit(Result.Loading)

        var hadCache = false

        if (query.includeCohorts) {
            // A failing cache read (storage error) degrades to a miss — the
            // network fetch below is the recovery path. safeFetch rethrows
            // cancellation.
            val blobHit = safeFetch(onError = {}) { cohortCache.get(cohortCacheKey(query)) }
            if (blobHit != null) {
                hadCache = true
                val isStale = (clock.now() - blobHit.fetchedAt) > ttl
                emit(Result.Success(blobHit.value.toDomain(), isStale))
                // The blob carries snapshots, so a fresh hit fully satisfies
                // the query — no revalidation needed.
                if (!isStale) return@flow
            }
        }

        if (!hadCache) {
            val cacheResult = dao.query(query)
            if (cacheResult != null) {
                hadCache = true
                val isStale = (clock.now() - cacheResult.oldestFetchedAt) > ttl
                // The flat table has no cohort rows; emit an empty snapshot map.
                emit(
                    Result.Success(
                        PopulationData(timeSeries = cacheResult.series, snapshots = emptyMap()),
                        isStale,
                    )
                )
                // Only stop here if the flat table satisfies the query. When
                // cohorts are needed (pyramid hero), always revalidate — this
                // tier has no cohort data, so without the re-fetch the pyramid
                // would stay empty on every fresh-cache hit.
                if (!isStale && !query.includeCohorts) return@flow
            }
        }

        // Revalidation branch
        try {
            val fresh = api.fetchPopulation(query)
            persist(fresh, query)
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
            persist(fresh, query)
        }
    }

    /** Writes the fresh frame to both cache tiers (blob tier only for cohort queries). */
    private suspend fun persist(fresh: PopulationData, query: PopulationQuery) {
        val now = clock.now()
        dao.upsert(fresh.timeSeries, now)
        if (query.includeCohorts) {
            // A failing blob write must not discard successfully fetched
            // network data — the caller still emits the fresh frame.
            safeFetch(onError = {}) {
                cohortCache.put(cohortCacheKey(query), PopulationCacheBlob.fromDomain(fresh), now)
            }
        }
    }

    companion object {

        /**
         * Deterministic, versioned cache key for the cohort blob tier.
         *
         * Country codes are sorted so logically equal queries share one entry
         * regardless of list order. Bump `v1` when [PopulationCacheBlob]
         * changes incompatibly — old rows then simply decode as cache misses.
         */
        internal fun cohortCacheKey(query: PopulationQuery): String =
            "population:cohorts:v1:" +
                query.countryCodes.sorted().joinToString(",") +
                ":${query.yearRange.first}:${query.yearRange.last}"
    }
}
