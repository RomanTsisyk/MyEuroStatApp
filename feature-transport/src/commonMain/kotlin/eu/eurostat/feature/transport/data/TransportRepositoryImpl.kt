package eu.eurostat.feature.transport.data

import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.network.toAppError
import eu.eurostat.feature.transport.domain.TransportDataPoint
import eu.eurostat.feature.transport.domain.TransportQuery
import eu.eurostat.feature.transport.domain.TransportRepository
import eu.eurostat.feature.transport.domain.TransportTimeSeries
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * Stale-while-revalidate repository for the transport feature.
 *
 * Cache freshness TTL: 12 hours.
 *
 * Flow contract:
 *   1. emit Loading
 *   2. read cache:
 *      - hit  → emit Success(isStale = cacheAge > TTL) immediately
 *      - miss → proceed to network without an intermediate emission
 *   3. fetch network:
 *      - success → persist data + emit Success(isStale = false)
 *      - failure → if cache existed, swallow silently; else emit Error
 *
 * [CancellationException] is always rethrown to preserve structured concurrency.
 */
class TransportRepositoryImpl(
    private val api: TransportApiService,
    private val dao: TransportCacheDao,
    private val dispatchers: DispatcherProvider,
    private val clock: Clock,
) : TransportRepository {

    override fun observe(query: TransportQuery): Flow<Result<List<TransportTimeSeries>>> = flow {
        emit(Result.Loading)

        val cached = try {
            dao.query(query)
        } catch (_: Throwable) {
            emptyList()
        }
        val hadCache = cached.isNotEmpty()

        var isStale = false
        if (hadCache) {
            val oldest = dao.oldestFetchedAt(query) ?: 0L
            val ageMs = clock.now().toEpochMilliseconds() - oldest
            isStale = ageMs > TTL_MS
            emit(Result.Success(cached.toTimeSeries(query), isStale = isStale))
            if (!isStale) return@flow
        }

        try {
            val fresh = api.fetch(query)
            dao.upsertAll(fresh, fetchedAt = clock.now().toEpochMilliseconds())
            emit(Result.Success(fresh.toTimeSeries(query), isStale = false))
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            if (!hadCache) emit(Result.Error(t.toAppError()))
            // Silently keep stale cache on network failure
        }
    }.flowOn(dispatchers.io)

    override suspend fun refresh(query: TransportQuery) {
        withContext(dispatchers.io) {
            val fresh = api.fetch(query)
            dao.upsertAll(fresh, fetchedAt = clock.now().toEpochMilliseconds())
        }
    }

    private companion object {
        const val TTL_MS = 12 * 60 * 60 * 1000L
    }
}

private fun List<TransportDataPoint>.toTimeSeries(query: TransportQuery): List<TransportTimeSeries> =
    groupBy { it.countryCode }.map { (countryCode, points) ->
        TransportTimeSeries(
            countryCode = countryCode,
            countryName = countryCode, // Placeholder — resolved by UI layer via locale map
            mode = query.mode,
            points = points.sortedBy { it.year },
        )
    }
