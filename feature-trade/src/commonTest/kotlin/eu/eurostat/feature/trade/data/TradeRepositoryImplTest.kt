package eu.eurostat.feature.trade.data

import app.cash.turbine.test
import eu.eurostat.core.common.Result
import eu.eurostat.feature.trade.domain.TradeDataPoint
import eu.eurostat.feature.trade.domain.TradeQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalCoroutinesApi::class)
class TradeRepositoryImplTest {

    private val baseInstant = Instant.parse("2026-05-16T10:00:00Z")
    private val defaultQuery = TradeQuery(listOf("PL"), 2020..2024, "EU27_2020")

    private fun freshPoints() = listOf(
        TradeDataPoint("PL", 2020, "EU27_2020", 100_000L, 90_000L, 10_000L),
        TradeDataPoint("PL", 2021, "EU27_2020", 110_000L, 95_000L, 15_000L),
    )

    private fun stalePoints() = listOf(
        TradeDataPoint("PL", 2020, "EU27_2020", 80_000L, 75_000L, 5_000L),
    )

    private fun makeRepo(
        api: FakeTradeApiService,
        dao: FakeTradeCacheDao,
        clock: FakeClock,
        dispatcher: kotlinx.coroutines.test.TestDispatcher,
    ) = TradeRepositoryImpl(api, dao, TestDispatcherProviderLocal(dispatcher), clock)

    @Test
    fun loading_then_cache_hit_fresh_emits_loading_then_success_notStale_no_network() = runTest {
        val clock = FakeClock(baseInstant)
        val api = FakeTradeApiService().apply { willReturn = freshPoints() }
        val dao = FakeTradeCacheDao().apply {
            seed(freshPoints(), fetchedAt = clock.now().toEpochMilliseconds() - 1.hours.inWholeMilliseconds)
        }
        val repo = makeRepo(api, dao, clock, StandardTestDispatcher(testScheduler))

        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val cached = awaitItem() as Result.Success<*>
            assertFalse(cached.isStale) // 1 hour old, under 12h TTL → early return
            awaitComplete()
        }
        // TTL guard: network must NOT be called when cache is fresh
        assertEquals(0, api.callCount)
    }

    @Test
    fun cache_hit_stale_emits_stale_then_fresh() = runTest {
        val clock = FakeClock(baseInstant)
        val api = FakeTradeApiService().apply { willReturn = freshPoints() }
        val dao = FakeTradeCacheDao().apply {
            seed(stalePoints(), fetchedAt = clock.now().minus(13.hours).toEpochMilliseconds())
        }
        val repo = makeRepo(api, dao, clock, StandardTestDispatcher(testScheduler))

        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())
            assertTrue((awaitItem() as Result.Success<*>).isStale)
            assertFalse((awaitItem() as Result.Success<*>).isStale)
            awaitComplete()
        }
    }

    @Test
    fun cache_miss_network_success_emits_loading_then_fresh() = runTest {
        val clock = FakeClock(baseInstant)
        val api = FakeTradeApiService().apply { willReturn = freshPoints() }
        val dao = FakeTradeCacheDao()
        val repo = makeRepo(api, dao, clock, StandardTestDispatcher(testScheduler))

        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val fresh = awaitItem() as Result.Success<*>
            assertFalse(fresh.isStale)
            awaitComplete()
        }
    }

    @Test
    fun cache_miss_network_failure_emits_loading_then_error() = runTest {
        val clock = FakeClock(baseInstant)
        val api = FakeTradeApiService().apply { throwable = RuntimeException("no network") }
        val dao = FakeTradeCacheDao()
        val repo = makeRepo(api, dao, clock, StandardTestDispatcher(testScheduler))

        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())
            assertIs<Result.Error>(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun stale_cache_with_network_failure_swallows_error() = runTest {
        val clock = FakeClock(baseInstant)
        val api = FakeTradeApiService().apply { throwable = RuntimeException("no network") }
        val dao = FakeTradeCacheDao().apply {
            seed(stalePoints(), fetchedAt = clock.now().minus(13.hours).toEpochMilliseconds())
        }
        val repo = makeRepo(api, dao, clock, StandardTestDispatcher(testScheduler))

        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val stale = awaitItem() as Result.Success<*>
            assertTrue(stale.isStale)
            // No error emitted — swallowed because cache existed
            awaitComplete()
        }
    }

    @Test
    fun force_refresh_bypasses_cache() = runTest {
        val clock = FakeClock(baseInstant)
        val api = FakeTradeApiService().apply { willReturn = freshPoints() }
        val dao = FakeTradeCacheDao()
        val repo = makeRepo(api, dao, clock, StandardTestDispatcher(testScheduler))

        repo.refresh(defaultQuery)

        assertEquals(1, api.callCount)
        val cached = dao.query(defaultQuery)
        assertTrue(cached.isNotEmpty())
    }

    @Test
    fun cancellation_during_fetch_is_propagated_not_swallowed() = runTest {
        val clock = FakeClock(baseInstant)
        val dispatcher = StandardTestDispatcher(testScheduler)

        val hangingApi = object : TradeApiService {
            override suspend fun fetch(query: TradeQuery): List<TradeDataPoint> {
                suspendCancellableCoroutine<Nothing> { /* never completes */ }
            }
        }
        val dao = FakeTradeCacheDao()
        val repo = TradeRepositoryImpl(hangingApi, dao, TestDispatcherProviderLocal(dispatcher), clock)

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
