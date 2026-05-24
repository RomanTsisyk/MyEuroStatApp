package eu.eurostat.feature.trade.data

import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.network.toAppError
import eu.eurostat.feature.trade.domain.TradeDataPoint
import eu.eurostat.feature.trade.domain.TradeQuery
import eu.eurostat.feature.trade.domain.TradeRepository
import eu.eurostat.feature.trade.domain.TradeTimeSeries
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * Stale-while-revalidate repository for the trade feature.
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
class TradeRepositoryImpl(
    private val api: TradeApiService,
    private val dao: TradeCacheDao,
    private val dispatchers: DispatcherProvider,
    private val clock: Clock,
) : TradeRepository {

    override fun observe(query: TradeQuery): Flow<Result<List<TradeTimeSeries>>> = flow {
        emit(Result.Loading)

        val cached = try {
            dao.query(query)
        } catch (_: Throwable) {
            emptyList()
        }
        val hadCache = cached.isNotEmpty()

        if (hadCache) {
            val oldest = dao.oldestFetchedAt(query) ?: 0L
            val ageMs = clock.now().toEpochMilliseconds() - oldest
            val isStale = ageMs > TTL_MS
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

    override suspend fun refresh(query: TradeQuery) {
        withContext(dispatchers.io) {
            val fresh = api.fetch(query)
            dao.upsertAll(fresh, fetchedAt = clock.now().toEpochMilliseconds())
        }
    }

    private companion object {
        const val TTL_MS = 12 * 60 * 60 * 1000L
    }
}

private fun List<TradeDataPoint>.toTimeSeries(query: TradeQuery): List<TradeTimeSeries> =
    groupBy { it.countryCode }.map { (countryCode, points) ->
        TradeTimeSeries(
            countryCode = countryCode,
            countryName = countryCode, // Placeholder — resolved by UI layer via locale map
            partner = query.partner,
            points = points.sortedBy { it.year },
        )
    }
