package eu.eurostat.feature.science.data

import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.network.toAppError
import eu.eurostat.feature.science.domain.ScienceDataPoint
import eu.eurostat.feature.science.domain.ScienceQuery
import eu.eurostat.feature.science.domain.ScienceRepository
import eu.eurostat.feature.science.domain.ScienceTimeSeries
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * Stale-while-revalidate repository for the science feature.
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
class ScienceRepositoryImpl(
    private val api: ScienceApiService,
    private val dao: ScienceCacheDao,
    private val dispatchers: DispatcherProvider,
    private val clock: Clock,
) : ScienceRepository {

    override fun observe(query: ScienceQuery): Flow<Result<List<ScienceTimeSeries>>> = flow {
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
            emit(Result.Success(cached.toTimeSeries(), isStale = isStale))
            // Fresh cache: no need to hit the network.
            if (!isStale) return@flow
        }
        try {
            val fresh = api.fetch(query)
            dao.upsertAll(fresh, fetchedAt = clock.now().toEpochMilliseconds())
            emit(Result.Success(fresh.toTimeSeries(), isStale = false))
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            if (!hadCache) emit(Result.Error(t.toAppError()))
        }
    }.flowOn(dispatchers.io)

    override suspend fun refresh(query: ScienceQuery) {
        withContext(dispatchers.io) {
            val fresh = api.fetch(query)
            dao.upsertAll(fresh, fetchedAt = clock.now().toEpochMilliseconds())
        }
    }

    private fun List<ScienceDataPoint>.toTimeSeries(): List<ScienceTimeSeries> =
        groupBy { it.countryCode }.map { (country, points) ->
            ScienceTimeSeries(
                countryCode = country,
                countryName = country,
                points = points.sortedBy { it.year },
            )
        }.sortedBy { it.countryCode }

    private companion object {
        const val TTL_MS = 12 * 60 * 60 * 1000L
    }
}
