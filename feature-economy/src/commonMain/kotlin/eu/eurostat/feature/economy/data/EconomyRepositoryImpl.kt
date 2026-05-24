package eu.eurostat.feature.economy.data

import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.network.toAppError
import eu.eurostat.feature.economy.domain.EconomyQuery
import eu.eurostat.feature.economy.domain.EconomyRepository
import eu.eurostat.feature.economy.domain.EconomyTimeSeries
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.hours

class EconomyRepositoryImpl(
    private val api: EconomyApiService,
    private val dao: EconomyCacheDao,
    private val dispatchers: DispatcherProvider,
    private val clock: Clock = Clock.System,
) : EconomyRepository {

    private val ttl = 12.hours

    override fun observe(query: EconomyQuery): Flow<Result<List<EconomyTimeSeries>>> = flow {
        emit(Result.Loading)

        val cacheResult = dao.query(query)
        val hadCache = cacheResult != null

        if (cacheResult != null) {
            val isStale = (clock.now() - cacheResult.oldestFetchedAt) > ttl
            emit(Result.Success(cacheResult.series, isStale))
            if (!isStale) return@flow
        }

        // Revalidation branch
        try {
            val fresh = api.fetchEconomy(query)
            dao.upsert(fresh, clock.now())
            emit(Result.Success(fresh, isStale = false))
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            if (!hadCache) {
                emit(Result.Error(t.toAppError()))
            }
            // else: stale cache was already emitted, suppress the network error
        }
    }.flowOn(dispatchers.io)

    override suspend fun refresh(query: EconomyQuery) {
        withContext(dispatchers.io) {
            val fresh = api.fetchEconomy(query)
            dao.upsert(fresh, clock.now())
        }
    }

}
