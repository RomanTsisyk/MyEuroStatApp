package eu.eurostat.feature.science.data

import app.cash.turbine.test
import eu.eurostat.core.common.Result
import eu.eurostat.feature.science.domain.ScienceDataPoint
import eu.eurostat.feature.science.domain.ScienceQuery
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

class ScienceRepositoryImplTest {

    private val defaultQuery = ScienceQuery(
        countryCodes = listOf("PL"),
        yearRange = 2020..2024,
    )

    private fun samplePoints() = listOf(
        ScienceDataPoint("PL", 2022, rdSpendPctGdp = 1.4, internetUsagePct = 87.5, tertiaryEducPct = 45.2),
    )

    private fun makeRepo(
        api: FakeScienceApiService,
        dao: FakeScienceCacheDao,
        clock: FakeClock,
        scheduler: kotlinx.coroutines.test.TestCoroutineScheduler,
    ) = ScienceRepositoryImpl(
        api = api,
        dao = dao,
        dispatchers = TestDispatcherProviderLocal(StandardTestDispatcher(scheduler)),
        clock = clock,
    )

    @Test
    fun cache_hit_fresh_emits_loading_then_success_not_stale_and_skips_network() = runTest {
        val clock = FakeClock(Instant.parse("2026-05-16T10:00:00Z"))
        val dao = FakeScienceCacheDao().apply { seed(samplePoints(), clock.now()) }
        val api = FakeScienceApiService().apply { willReturn = samplePoints() }
        val repo = makeRepo(api, dao, clock, testScheduler)

        // Fresh cache: TTL early-return fires — only Loading + cached emitted; no network call.
        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val cached = awaitItem()
            assertTrue(cached is Result.Success)
            assertFalse((cached as Result.Success).isStale)
            awaitComplete()
        }
        assertEquals(0, api.callCount, "Network must NOT be called when cache is fresh")
    }

    @Test
    fun cache_hit_stale_emits_loading_stale_then_fresh() = runTest {
        val clock = FakeClock(Instant.parse("2026-05-16T10:00:00Z"))
        val dao = FakeScienceCacheDao().apply {
            seed(samplePoints(), clock.now() - 13.hours)
        }
        val api = FakeScienceApiService().apply { willReturn = samplePoints() }
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
        val dao = FakeScienceCacheDao()
        val api = FakeScienceApiService().apply { willReturn = samplePoints() }
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
        val api = FakeScienceApiService().apply { throwable = IllegalStateException("dns") }
        val dao = FakeScienceCacheDao()
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
        val dao = FakeScienceCacheDao().apply {
            seed(samplePoints(), clock.now() - 13.hours)
        }
        val api = FakeScienceApiService().apply { throwable = IllegalStateException("timeout") }
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
        val dao = FakeScienceCacheDao()
        val api = FakeScienceApiService().apply { willReturn = samplePoints() }
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

        val hangingApi = object : ScienceApiService {
            override suspend fun fetch(query: ScienceQuery): List<ScienceDataPoint> {
                suspendCancellableCoroutine<Nothing> { /* never completes */ }
            }
        }
        val dao = FakeScienceCacheDao()
        val repo = ScienceRepositoryImpl(hangingApi, dao, TestDispatcherProviderLocal(dispatcher), clock)

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
}
