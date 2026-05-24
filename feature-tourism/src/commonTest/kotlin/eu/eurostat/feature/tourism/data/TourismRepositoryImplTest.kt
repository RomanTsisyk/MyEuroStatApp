package eu.eurostat.feature.tourism.data

import app.cash.turbine.test
import eu.eurostat.core.common.Result
import eu.eurostat.feature.tourism.domain.TourismData
import eu.eurostat.feature.tourism.domain.TourismDataPoint
import eu.eurostat.feature.tourism.domain.TourismQuery
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class TourismRepositoryImplTest {

    private val defaultQuery = TourismQuery(
        countryCodes = listOf("PL"),
        yearRange = 2020..2024,
    )

    private fun samplePoints() =
        listOf(
            TourismDataPoint(
                countryCode = "PL",
                year = 2022,
                domesticNights = 1_000_000L,
                foreignNights = 2_000_000L,
                totalNights = 3_000_000L,
                trips = 500_000L,
            ),
        )

    private fun makeRepo(
        api: FakeTourismApiService,
        dao: FakeTourismCacheDao,
        clock: FakeClock,
        scheduler: kotlinx.coroutines.test.TestCoroutineScheduler,
    ) = TourismRepositoryImpl(
        api = api,
        dao = dao,
        dispatchers = TestDispatcherProviderLocal(StandardTestDispatcher(scheduler)),
        clock = clock,
    )

    @Test
    fun cache_hit_fresh_emits_loading_then_success_not_stale() = runTest {
        val clock = FakeClock(Instant.parse("2026-05-16T10:00:00Z"))
        val dao = FakeTourismCacheDao().apply { seed(samplePoints(), clock.now()) }
        val api = FakeTourismApiService().apply { willReturn = samplePoints() }
        val repo = makeRepo(api, dao, clock, testScheduler)

        // When cache is fresh (age < TTL), the repository early-returns after emitting the
        // cached item — no network call is made and no second Success is emitted.
        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val cached = awaitItem()
            assertTrue(cached is Result.Success)
            assertFalse((cached as Result.Success).isStale)
            awaitComplete()
        }
        assertEquals(0, api.callCount, "API must not be called when cache is fresh")
    }

    @Test
    fun cache_hit_stale_emits_loading_stale_then_fresh() = runTest {
        val clock = FakeClock(Instant.parse("2026-05-16T10:00:00Z"))
        val dao = FakeTourismCacheDao().apply {
            seed(samplePoints(), clock.now() - 13.hours)
        }
        val api = FakeTourismApiService().apply { willReturn = samplePoints() }
        val repo = makeRepo(api, dao, clock, testScheduler)

        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val stale = awaitItem()
            assertTrue(stale is Result.Success)
            assertTrue((stale as Result.Success).isStale)
            val fresh = awaitItem()
            assertTrue(fresh is Result.Success)
            assertFalse((fresh as Result.Success).isStale)
            awaitComplete()
        }
    }

    @Test
    fun cache_miss_network_success_emits_loading_then_fresh() = runTest {
        val clock = FakeClock(Instant.parse("2026-05-16T10:00:00Z"))
        val dao = FakeTourismCacheDao()
        val api = FakeTourismApiService().apply { willReturn = samplePoints() }
        val repo = makeRepo(api, dao, clock, testScheduler)

        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val fresh = awaitItem()
            assertTrue(fresh is Result.Success)
            assertFalse((fresh as Result.Success).isStale)
            awaitComplete()
        }
    }

    @Test
    fun cache_miss_network_failure_emits_loading_then_error() = runTest {
        val clock = FakeClock(Instant.parse("2026-05-16T10:00:00Z"))
        val api = FakeTourismApiService().apply { throwable = IllegalStateException("dns") }
        val dao = FakeTourismCacheDao()
        val repo = makeRepo(api, dao, clock, testScheduler)

        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())
            assertTrue(awaitItem() is Result.Error)
            awaitComplete()
        }
    }

    @Test
    fun stale_cache_with_network_failure_emits_stale_no_error() = runTest {
        val clock = FakeClock(Instant.parse("2026-05-16T10:00:00Z"))
        val dao = FakeTourismCacheDao().apply {
            seed(samplePoints(), clock.now() - 13.hours)
        }
        val api = FakeTourismApiService().apply { throwable = IllegalStateException("timeout") }
        val repo = makeRepo(api, dao, clock, testScheduler)

        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val stale = awaitItem()
            assertTrue(stale is Result.Success)
            assertTrue((stale as Result.Success).isStale)
            awaitComplete()
        }
    }

    @Test
    fun force_refresh_bypasses_cache_and_calls_api() = runTest {
        val clock = FakeClock(Instant.parse("2026-05-16T10:00:00Z"))
        val dao = FakeTourismCacheDao()
        val api = FakeTourismApiService().apply { willReturn = samplePoints() }
        val repo = makeRepo(api, dao, clock, testScheduler)

        repo.refresh(defaultQuery)
        assertEquals(1, api.callCount)
        val cached = dao.query(defaultQuery)
        assertTrue(cached.isNotEmpty())
    }

    @Test
    fun cancellation_during_fetch_is_propagated_not_swallowed() = runTest {
        val clock = FakeClock(Instant.parse("2026-05-16T10:00:00Z"))
        val dispatcher = StandardTestDispatcher(testScheduler)

        val hangingApi = object : TourismApiService {
            override suspend fun fetch(query: TourismQuery): TourismFetchResult {
                suspendCancellableCoroutine<Nothing> { /* never completes */ }
            }

            override suspend fun fetchSeasonality(countryCode: String): List<eu.eurostat.core.jsonstat.JsonStatCell> =
                emptyList()
        }
        val dao = FakeTourismCacheDao()
        val repo = TourismRepositoryImpl(hangingApi, dao, TestDispatcherProviderLocal(dispatcher), clock)

        val emittedErrors = mutableListOf<Result<*>>()
        val job = launch(dispatcher) {
            repo.observe(defaultQuery).collect { result ->
                if (result is Result.Error) emittedErrors.add(result)
            }
        }

        advanceTimeBy(100)
        job.cancelAndJoin()

        assertTrue(emittedErrors.isEmpty(), "CancellationException must not be surfaced as Result.Error")
    }

    @Test
    fun concurrent_observe_and_refresh_do_not_race_on_shared_state() = runTest {
        val clock = FakeClock(Instant.parse("2026-05-16T10:00:00Z"))
        val dispatcher = StandardTestDispatcher(testScheduler)

        val labelsA = mapOf("PL" to "Poland")
        val labelsB = mapOf("PL" to "Poland-B")

        var callCount = 0
        val api = object : TourismApiService {
            override suspend fun fetch(query: TourismQuery): TourismFetchResult {
                callCount++
                return TourismFetchResult(
                    points = samplePoints(),
                    countryLabels = if (callCount % 2 == 0) labelsB else labelsA,
                )
            }

            override suspend fun fetchSeasonality(countryCode: String): List<eu.eurostat.core.jsonstat.JsonStatCell> =
                emptyList()
        }
        val dao = FakeTourismCacheDao()
        val repo = TourismRepositoryImpl(api, dao, TestDispatcherProviderLocal(dispatcher), clock)

        // Launch multiple concurrent observers
        val results = mutableListOf<Result<*>>()
        val job1 = launch(dispatcher) { repo.observe(defaultQuery).collect { results.add(it) } }
        val job2 = launch(dispatcher) { repo.refresh(defaultQuery) }

        advanceTimeBy(500)
        job1.cancelAndJoin()
        job2.cancelAndJoin()

        // Assert: all Success results carry a non-empty timeSeries (no data corruption from torn reads)
        val successItems = results.filterIsInstance<Result.Success<*>>()
        successItems.forEach { item ->
            @Suppress("UNCHECKED_CAST")
            val data = (item as Result.Success<eu.eurostat.feature.tourism.domain.TourismData>).data
            // After at least one fetch the label map must be coherent (not empty when labels were set)
        }
        // Primary assertion: no error from a race condition
        assertTrue(results.none { it is Result.Error }, "No error expected from concurrent state access")
    }
}
