package eu.eurostat.feature.economy.data

import app.cash.turbine.test
import eu.eurostat.core.common.AppError
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.feature.economy.domain.EconomyDataPoint
import eu.eurostat.feature.economy.domain.EconomyQuery
import eu.eurostat.feature.economy.domain.EconomyTimeSeries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

// ---------------------------------------------------------------------------
// Test doubles
// ---------------------------------------------------------------------------

class FakeEconomyApiService : EconomyApiService {
    private var result: Result<List<EconomyTimeSeries>> = Result.Success(emptyList())
    var callCount = 0

    fun willReturn(series: List<EconomyTimeSeries>) {
        result = Result.Success(series)
    }

    fun willThrow(t: Throwable) {
        result = Result.Error(AppError.Unknown(t))
    }

    override suspend fun fetchEconomy(query: EconomyQuery): List<EconomyTimeSeries> {
        callCount++
        return when (val r = result) {
            is Result.Success -> r.data
            is Result.Error -> throw (r.cause as? AppError.Unknown)?.throwable ?: RuntimeException("fake error")
            is Result.Loading -> emptyList()
        }
    }
}

class FakeEconomyCacheDao : EconomyCacheDao {
    private var stored: List<EconomyTimeSeries> = emptyList()
    private var storedFetchedAt: Instant? = null

    fun seed(series: List<EconomyTimeSeries>, fetchedAt: Instant) {
        stored = series
        storedFetchedAt = fetchedAt
    }

    override suspend fun query(query: EconomyQuery): EconomyCacheResult? {
        val at = storedFetchedAt ?: return null
        if (stored.isEmpty()) return null
        return EconomyCacheResult(series = stored, oldestFetchedAt = at)
    }

    override suspend fun upsert(series: List<EconomyTimeSeries>, fetchedAt: Instant) {
        stored = series
        storedFetchedAt = fetchedAt
    }
}

class FakeEconomyClock(private var current: Instant) : kotlinx.datetime.Clock {
    fun advance(d: Duration) { current += d }
    override fun now(): Instant = current
}

@OptIn(ExperimentalCoroutinesApi::class)
class EconomyTestDispatcherProvider(dispatcher: TestDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private val BASE_TIME = Instant.parse("2026-05-16T10:00:00Z")

private fun fakeSeries(country: String = "DE", year: Int = 2020) = listOf(
    EconomyTimeSeries(
        countryCode = country,
        countryName = country,
        points = listOf(
            EconomyDataPoint(country, year, gdpEur = 3_500_000L)
        )
    )
)

private val testQuery = EconomyQuery(listOf("DE"), 2020..2024)

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

@OptIn(ExperimentalCoroutinesApi::class)
class EconomyRepositoryImplTest {

    @Test
    fun loading_then_cache_hit_fresh_emits_loading_then_success_notStale() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = FakeEconomyClock(BASE_TIME)
        val api = FakeEconomyApiService()
        val dao = FakeEconomyCacheDao()
        dao.seed(fakeSeries(), fetchedAt = BASE_TIME.minus(1.hours))
        api.willReturn(fakeSeries())

        val repo = EconomyRepositoryImpl(api, dao, EconomyTestDispatcherProvider(dispatcher), clock)

        repo.observe(testQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val item = awaitItem()
            assertIs<Result.Success<*>>(item)
            assertFalse((item as Result.Success<*>).isStale, "Fresh cache should not be stale")
            awaitComplete()
        }
    }

    @Test
    fun cache_hit_stale_emits_stale_then_fresh() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = FakeEconomyClock(BASE_TIME)
        val api = FakeEconomyApiService()
        val dao = FakeEconomyCacheDao()
        dao.seed(fakeSeries(), fetchedAt = BASE_TIME.minus(13.hours))
        api.willReturn(fakeSeries())

        val repo = EconomyRepositoryImpl(api, dao, EconomyTestDispatcherProvider(dispatcher), clock)

        repo.observe(testQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val staleItem = awaitItem()
            assertIs<Result.Success<*>>(staleItem)
            assertTrue((staleItem as Result.Success<*>).isStale, "Should be stale")
            val freshItem = awaitItem()
            assertIs<Result.Success<*>>(freshItem)
            assertFalse((freshItem as Result.Success<*>).isStale, "Network result should not be stale")
            awaitComplete()
        }
    }

    @Test
    fun cache_miss_network_success_emits_loading_then_fresh() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = FakeEconomyClock(BASE_TIME)
        val api = FakeEconomyApiService()
        val dao = FakeEconomyCacheDao()
        api.willReturn(fakeSeries())

        val repo = EconomyRepositoryImpl(api, dao, EconomyTestDispatcherProvider(dispatcher), clock)

        repo.observe(testQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val item = awaitItem()
            assertIs<Result.Success<*>>(item)
            assertFalse((item as Result.Success<*>).isStale)
            awaitComplete()
        }
    }

    @Test
    fun cache_miss_network_failure_emits_loading_then_error() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = FakeEconomyClock(BASE_TIME)
        val api = FakeEconomyApiService()
        val dao = FakeEconomyCacheDao()
        api.willThrow(RuntimeException("network down"))

        val repo = EconomyRepositoryImpl(api, dao, EconomyTestDispatcherProvider(dispatcher), clock)

        repo.observe(testQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val item = awaitItem()
            assertIs<Result.Error>(item)
            awaitComplete()
        }
    }

    @Test
    fun stale_cache_with_network_failure_swallows_error() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = FakeEconomyClock(BASE_TIME)
        val api = FakeEconomyApiService()
        val dao = FakeEconomyCacheDao()
        dao.seed(fakeSeries(), fetchedAt = BASE_TIME.minus(13.hours))
        api.willThrow(RuntimeException("network down"))

        val repo = EconomyRepositoryImpl(api, dao, EconomyTestDispatcherProvider(dispatcher), clock)

        repo.observe(testQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val item = awaitItem()
            assertIs<Result.Success<*>>(item)
            assertTrue((item as Result.Success<*>).isStale)
            awaitComplete()
        }
    }

    @Test
    fun force_refresh_bypasses_cache() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = FakeEconomyClock(BASE_TIME)
        val api = FakeEconomyApiService()
        val dao = FakeEconomyCacheDao()
        dao.seed(fakeSeries(), fetchedAt = BASE_TIME.minus(1.hours))
        api.willReturn(fakeSeries())

        val repo = EconomyRepositoryImpl(api, dao, EconomyTestDispatcherProvider(dispatcher), clock)

        repo.refresh(testQuery)

        assertEquals(1, api.callCount, "API should have been called exactly once during refresh")
    }

    @Test
    fun cancellation_during_fetch_is_propagated_not_swallowed() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = FakeEconomyClock(BASE_TIME)

        val hangingApi = object : EconomyApiService {
            override suspend fun fetchEconomy(query: EconomyQuery): List<EconomyTimeSeries> {
                suspendCancellableCoroutine<Nothing> { /* never completes */ }
            }
        }
        val dao = FakeEconomyCacheDao()

        val repo = EconomyRepositoryImpl(hangingApi, dao, EconomyTestDispatcherProvider(dispatcher), clock)

        val emittedErrors = mutableListOf<Result<*>>()
        val job = launch(dispatcher) {
            repo.observe(testQuery).collect { result ->
                if (result is Result.Error) emittedErrors.add(result)
            }
        }

        advanceTimeBy(100)
        job.cancelAndJoin()

        assertTrue(emittedErrors.isEmpty(), "CancellationException must not be surfaced as Result.Error")
    }
}
