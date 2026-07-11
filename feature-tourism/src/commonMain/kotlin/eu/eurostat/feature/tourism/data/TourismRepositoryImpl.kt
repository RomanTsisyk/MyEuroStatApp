package eu.eurostat.feature.tourism.data

import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.common.cache.JsonBlobCache
import eu.eurostat.core.common.safeFetch
import eu.eurostat.core.network.toAppError
import eu.eurostat.feature.tourism.domain.TourismData
import eu.eurostat.feature.tourism.domain.TourismQuery
import eu.eurostat.feature.tourism.domain.TourismRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.hours

/**
 * Stale-while-revalidate repository for the tourism feature.
 *
 * Cache freshness TTL: 12 hours. One JSON blob per query (see
 * [TourismCacheBlob]) carries the yearly points, the country-name labels *and*
 * the monthly seasonality heatmap — so all three survive process restarts and
 * are available offline. This replaces both the residence-tall SQL table with
 * its `-1` nights sentinel and the in-memory label/heatmap stash.
 *
 * Flow contract:
 *   1. emit Loading
 *   2. if cache hit → emit Success(isStale = cacheAge > TTL); stop when fresh
 *   3. fetch network (tour_occ_ninat + tour_dem_tttot + tour_occ_nim):
 *      - success → persist blob + emit Success(isStale = false)
 *      - failure → if no cache was emitted, emit Error; otherwise swallow.
 *
 * When a fetch succeeds but its seasonality slice comes back empty (the
 * `tour_occ_nim` call degrades to an empty grid on failure), the previously
 * cached labels/heatmap are carried forward into the new blob so the UI keeps
 * its last known grid instead of blanking out.
 *
 * [CancellationException] is always rethrown to preserve structured concurrency.
 */
class TourismRepositoryImpl(
    private val api: TourismApiService,
    private val cache: JsonBlobCache<TourismCacheBlob>,
    private val dispatchers: DispatcherProvider,
    private val clock: Clock,
) : TourismRepository {

    private val ttl = 12.hours

    override fun observe(query: TourismQuery): Flow<Result<TourismData>> = flow {
        emit(Result.Loading)

        // A failing cache read (storage error) degrades to a miss — the network
        // fetch below is the recovery path. safeFetch rethrows cancellation.
        val stored = safeFetch(onError = {}) { cache.get(cacheKey(query)) }
        // A blob with no yearly points (persisted from a degraded fetch) must
        // not act as a 12h negative cache: treat it as a miss and revalidate.
        // Its labels/heatmap still feed the carry-forward merge below.
        val cached = stored?.takeIf { it.value.points.isNotEmpty() }
        val hadCache = cached != null
        if (cached != null) {
            val isStale = (clock.now() - cached.fetchedAt) > ttl
            emit(Result.Success(cached.value.toDomain(), isStale))
            // Early-return guard: skip network fetch when cached data is fresh.
            if (!isStale) return@flow
        }

        try {
            val blob = fetchAndPersist(query, previous = stored?.value)
            emit(Result.Success(blob.toDomain(), isStale = false))
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            if (!hadCache) emit(Result.Error(t.toAppError()))
            // else: stale cache was already emitted, suppress the network error
        }
    }.flowOn(dispatchers.io)

    override suspend fun refresh(query: TourismQuery) {
        withContext(dispatchers.io) {
            val previous = safeFetch(onError = {}) { cache.get(cacheKey(query)) }
            fetchAndPersist(query, previous = previous?.value)
        }
    }

    /**
     * Fetches fresh data, merges in the [previous] blob's labels/heatmap when
     * the fresh slices are empty, persists the result, and returns it.
     */
    private suspend fun fetchAndPersist(
        query: TourismQuery,
        previous: TourismCacheBlob?,
    ): TourismCacheBlob {
        val fresh = TourismCacheBlob.fromFetchResult(api.fetch(query))
        val merged = fresh.copy(
            countryLabels = fresh.countryLabels.ifEmpty { previous?.countryLabels.orEmpty() },
            heatmapCells = fresh.heatmapCells.ifEmpty { previous?.heatmapCells.orEmpty() },
        )
        // A failing blob write must not discard successfully fetched network
        // data — the caller still emits the fresh frame.
        safeFetch(onError = {}) { cache.put(cacheKey(query), merged, clock.now()) }
        return merged
    }

    companion object {

        /**
         * Deterministic, versioned cache key for one tourism query.
         *
         * Country codes are sorted so logically equal queries share one entry
         * regardless of list order. Bump `v1` when [TourismCacheBlob] changes
         * incompatibly — old rows then simply decode as cache misses.
         */
        internal fun cacheKey(query: TourismQuery): String =
            "tourism:series:v1:" +
                query.countryCodes.sorted().joinToString(",") +
                ":${query.yearRange.first}:${query.yearRange.last}"
    }
}
