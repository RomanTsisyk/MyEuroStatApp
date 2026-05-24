package eu.eurostat.feature.population.data

import app.cash.turbine.test
import eu.eurostat.core.common.AppError
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.feature.population.domain.PopulationDataPoint
import eu.eurostat.feature.population.domain.PopulationQuery
import eu.eurostat.feature.population.domain.PopulationTimeSeries
import kotlinx.coroutines.CancellationException
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
import kotlin.time.Duration.Companion.minutes

// ---------------------------------------------------------------------------
// Test doubles
// ---------------------------------------------------------------------------

class FakePopulationApiService : PopulationApiService {
    private var result: Result<List<PopulationTimeSeries>> = Result.Success(emptyList())
    var callCount = 0

    fun willReturn(series: List<PopulationTimeSeries>) {
        result = Result.Success(series)
    }

    fun willThrow(t: Throwable) {
        result = Result.Error(AppError.Unknown(t))
    }

    override suspend fun fetchPopulation(query: PopulationQuery): eu.eurostat.feature.population.domain.PopulationData {
        callCount++
        val series = when (val r = result) {
            is Result.Success -> r.data
            is Result.Error -> throw (r.cause as? AppError.Unknown)?.throwable ?: RuntimeException("fake error")
            is Result.Loading -> emptyList()
        }
        return eu.eurostat.feature.population.domain.PopulationData(
            timeSeries = series,
            snapshots = emptyMap(),
        )
    }
}

class FakePopulationCacheDao : PopulationCacheDao {
    private var stored: List<PopulationTimeSeries> = emptyList()
    private var storedFetchedAt: Instant? = null

    fun seed(series: List<PopulationTimeSeries>, fetchedAt: Instant) {
        stored = series
        storedFetchedAt = fetchedAt
    }

    override suspend fun query(query: PopulationQuery): CacheResult? {
        val at = storedFetchedAt ?: return null
        if (stored.isEmpty()) return null
        return CacheResult(series = stored, oldestFetchedAt = at)
    }

    override suspend fun upsert(series: List<PopulationTimeSeries>, fetchedAt: Instant) {
        stored = series
        storedFetchedAt = fetchedAt
    }
}

class FakeClock(private var current: Instant) : kotlinx.datetime.Clock {
    fun advance(d: Duration) { current += d }
    override fun now(): Instant = current
}

@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherProvider(dispatcher: TestDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private val BASE_TIME = Instant.parse("2026-05-16T10:00:00Z")

private fun fakeSeries(country: String = "PL", year: Int = 2020) = listOf(
    PopulationTimeSeries(
        countryCode = country,
        countryName = country,
        points = listOf(
            PopulationDataPoint(country, year, 38_000_000L, 18_000_000L, 20_000_000L)
        )
    )
)

private val testQuery = PopulationQuery(listOf("PL"), 2020..2024)

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

@OptIn(ExperimentalCoroutinesApi::class)
class PopulationRepositoryImplTest {

    @Test
    fun loading_then_cache_hit_fresh_emits_loading_then_success_notStale() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = FakeClock(BASE_TIME)
        val api = FakePopulationApiService()
        val dao = FakePopulationCacheDao()
        // fresh cache: fetched 1 hour ago (< 12h TTL)
        dao.seed(fakeSeries(), fetchedAt = BASE_TIME.minus(1.hours))
        api.willReturn(fakeSeries())

        // includeCohorts=false: cache hit is authoritative; no revalidation needed.
        val cohortlessQuery = testQuery.copy(includeCohorts = false)
        val repo = PopulationRepositoryImpl(api, dao, TestDispatcherProvider(dispatcher), clock)

        repo.observe(cohortlessQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val item = awaitItem()
            assertIs<Result.Success<*>>(item)
            assertFalse((item as Result.Success<*>).isStale, "Fresh cache should not be stale")
            awaitComplete()
        }
    }

    @Test
    fun fresh_cache_with_cohorts_revalidates_to_get_cohort_data() = runTest {
        // Cohorts are not cached. Even with a fresh cache, the repo must
        // revalidate when the query includes cohorts so the pyramid hero
        // does not stay empty forever.
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = FakeClock(BASE_TIME)
        val api = FakePopulationApiService()
        val dao = FakePopulationCacheDao()
        dao.seed(fakeSeries(), fetchedAt = BASE_TIME.minus(1.hours))
        api.willReturn(fakeSeries())

        // testQuery uses includeCohorts=true by default.
        val repo = PopulationRepositoryImpl(api, dao, TestDispatcherProvider(dispatcher), clock)

        repo.observe(testQuery).test {
            assertEquals(Result.Loading, awaitItem())
            // cached emission first
            val cached = awaitItem()
            assertIs<Result.Success<*>>(cached)
            assertFalse((cached as Result.Success<*>).isStale)
            // then fresh emission with cohort data
            val fresh = awaitItem()
            assertIs<Result.Success<*>>(fresh)
            assertFalse((fresh as Result.Success<*>).isStale)
            awaitComplete()
        }
    }

    @Test
    fun cache_hit_stale_emits_stale_then_fresh() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = FakeClock(BASE_TIME)
        val api = FakePopulationApiService()
        val dao = FakePopulationCacheDao()
        // stale cache: fetched 13 hours ago (> 12h TTL)
        dao.seed(fakeSeries(), fetchedAt = BASE_TIME.minus(13.hours))
        api.willReturn(fakeSeries())

        val repo = PopulationRepositoryImpl(api, dao, TestDispatcherProvider(dispatcher), clock)

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
        val clock = FakeClock(BASE_TIME)
        val api = FakePopulationApiService()
        val dao = FakePopulationCacheDao() // empty
        api.willReturn(fakeSeries())

        val repo = PopulationRepositoryImpl(api, dao, TestDispatcherProvider(dispatcher), clock)

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
        val clock = FakeClock(BASE_TIME)
        val api = FakePopulationApiService()
        val dao = FakePopulationCacheDao() // empty
        api.willThrow(RuntimeException("network down"))

        val repo = PopulationRepositoryImpl(api, dao, TestDispatcherProvider(dispatcher), clock)

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
        val clock = FakeClock(BASE_TIME)
        val api = FakePopulationApiService()
        val dao = FakePopulationCacheDao()
        dao.seed(fakeSeries(), fetchedAt = BASE_TIME.minus(13.hours))
        api.willThrow(RuntimeException("network down"))

        val repo = PopulationRepositoryImpl(api, dao, TestDispatcherProvider(dispatcher), clock)

        repo.observe(testQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val item = awaitItem()
            // Should emit stale success, then complete without error
            assertIs<Result.Success<*>>(item)
            assertTrue((item as Result.Success<*>).isStale)
            // No error emitted — network failure swallowed
            awaitComplete()
        }
    }

    @Test
    fun force_refresh_bypasses_cache() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = FakeClock(BASE_TIME)
        val api = FakePopulationApiService()
        val dao = FakePopulationCacheDao()
        // fresh cache present — refresh should still call API
        dao.seed(fakeSeries(), fetchedAt = BASE_TIME.minus(1.hours))
        api.willReturn(fakeSeries())

        val repo = PopulationRepositoryImpl(api, dao, TestDispatcherProvider(dispatcher), clock)

        repo.refresh(testQuery)

        assertEquals(1, api.callCount, "API should have been called exactly once during refresh")
    }

    @Test
    fun cancellation_during_fetch_is_propagated_not_swallowed() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = FakeClock(BASE_TIME)

        // API service that suspends indefinitely — simulates a slow network call
        val hangingApi = object : PopulationApiService {
            override suspend fun fetchPopulation(query: PopulationQuery): eu.eurostat.feature.population.domain.PopulationData {
                suspendCancellableCoroutine<Nothing> { /* never completes */ }
            }
        }
        val dao = FakePopulationCacheDao() // empty cache → network will be attempted

        val repo = PopulationRepositoryImpl(hangingApi, dao, TestDispatcherProvider(dispatcher), clock)

        val emittedErrors = mutableListOf<Result<*>>()
        val job = launch(dispatcher) {
            repo.observe(testQuery).collect { result ->
                if (result is Result.Error) emittedErrors.add(result)
            }
        }

        // Advance past Loading emission, into the suspended network call
        advanceTimeBy(100)
        // Cancel the collecting job — should not produce an Error emission
        job.cancelAndJoin()

        assertTrue(emittedErrors.isEmpty(), "CancellationException must not be surfaced as Result.Error")
    }
}
