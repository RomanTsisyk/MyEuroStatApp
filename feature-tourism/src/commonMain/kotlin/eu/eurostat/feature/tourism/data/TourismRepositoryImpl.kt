package eu.eurostat.feature.tourism.data

import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.network.toAppError
import eu.eurostat.feature.tourism.domain.TourismData
import eu.eurostat.feature.tourism.domain.TourismDataPoint
import eu.eurostat.feature.tourism.domain.TourismQuery
import eu.eurostat.feature.tourism.domain.TourismRepository
import eu.eurostat.feature.tourism.domain.TourismTimeSeries
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * Stale-while-revalidate repository for the tourism feature.
 *
 * 1. emit Loading
 * 2. if cache hit → emit Success(isStale=age>TTL)
 * 3. fetch network in parallel (tour_occ_ninat + tour_dem_tttot + tour_occ_nim):
 *      - success → persist + emit Success(isStale=false, heatmapCells=fresh)
 *      - failure → if no cache existed, emit Error; otherwise swallow.
 *
 * Country-name labels and the last-fetched heatmap grid survive across refreshes
 * by being stashed on the repository — the SQLDelight schema doesn't carry them,
 * so the most recent fetch wins.
 *
 * Both in-memory fields are guarded by [stateMutex] so that concurrent [observe]
 * flows or a simultaneous [refresh] call cannot produce torn reads on Android's
 * thread pool backing [dispatchers.io].
 *
 * [CancellationException] is always rethrown to preserve structured concurrency.
 */
class TourismRepositoryImpl(
    private val api: TourismApiService,
    private val dao: TourismCacheDao,
    private val dispatchers: DispatcherProvider,
    private val clock: Clock,
) : TourismRepository {

    /** Guards concurrent access to [lastLabels] and [lastHeatmapCells]. */
    private val stateMutex = Mutex()
    private var lastLabels: Map<String, String> = emptyMap()

    /**
     * Last successfully fetched seasonality grid. Kept in memory so that a
     * cache-hit emission (from the DAO) can still carry heatmap data that was
     * obtained during a previous network fetch in this process lifetime.
     *
     * Always accessed under [stateMutex].
     */
    private var lastHeatmapCells: List<List<Float>> = emptyList()

    override fun observe(query: TourismQuery): Flow<Result<TourismData>> = flow {
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
            val (labels, cells) = stateMutex.withLock { lastLabels to lastHeatmapCells }
            emit(
                Result.Success(
                    TourismData(
                        timeSeries = cached.toTimeSeries(labels),
                        heatmapCells = cells,
                    ),
                    isStale = isStale,
                ),
            )
            // Early-return guard: skip network fetch when cached data is fresh.
            if (!isStale) return@flow
        }
        try {
            val fresh = api.fetch(query)
            val (labels, cells) = stateMutex.withLock {
                if (fresh.countryLabels.isNotEmpty()) lastLabels = fresh.countryLabels
                lastHeatmapCells = fresh.heatmapCells.ifEmpty { lastHeatmapCells }
                lastLabels to lastHeatmapCells
            }
            dao.upsertAll(fresh.points, fetchedAt = clock.now().toEpochMilliseconds())
            emit(
                Result.Success(
                    TourismData(
                        timeSeries = fresh.points.toTimeSeries(labels),
                        heatmapCells = cells,
                    ),
                    isStale = false,
                ),
            )
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            if (!hadCache) emit(Result.Error(t.toAppError()))
        }
    }.flowOn(dispatchers.io)

    override suspend fun refresh(query: TourismQuery) {
        withContext(dispatchers.io) {
            val fresh = api.fetch(query)
            stateMutex.withLock {
                if (fresh.countryLabels.isNotEmpty()) lastLabels = fresh.countryLabels
                lastHeatmapCells = fresh.heatmapCells.ifEmpty { lastHeatmapCells }
            }
            dao.upsertAll(fresh.points, fetchedAt = clock.now().toEpochMilliseconds())
        }
    }

    private fun List<TourismDataPoint>.toTimeSeries(
        labels: Map<String, String>,
    ): List<TourismTimeSeries> = TourismCellMapper.toTimeSeries(this, labels)

    private companion object {
        const val TTL_MS = 12 * 60 * 60 * 1000L
    }
}
