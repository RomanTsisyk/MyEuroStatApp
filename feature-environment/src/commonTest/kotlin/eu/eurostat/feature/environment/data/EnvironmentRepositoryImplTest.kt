package eu.eurostat.feature.environment.data

import app.cash.turbine.test
import eu.eurostat.core.common.Result
import eu.eurostat.feature.environment.domain.EnvironmentDataPoint
import eu.eurostat.feature.environment.domain.EnvironmentQuery
import eu.eurostat.feature.environment.domain.EnvironmentTimeSeries
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

/**
 * Unit tests for [EnvironmentRepositoryImpl].
 *
 * The repository now implements stale-while-revalidate:
 *   - Loading → (cached Success if cache hit) → Success(isStale=false) on network success
 *   - Loading → Error only when cache is empty AND network fails
 *   - Loading → cached Success (silently suppressed network error) when cache exists AND network fails
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EnvironmentRepositoryImplTest {

    private val defaultQuery = EnvironmentQuery(listOf("PL", "DE"), 2020..2022)

    private fun sampleSeries(country: String = "PL"): EnvironmentTimeSeries =
        EnvironmentTimeSeries(
            countryCode = country,
            countryName = country,
            points = listOf(
                EnvironmentDataPoint(
                    countryCode = country,
                    year = 2020,
                    sector = eu.eurostat.feature.environment.domain.EnvSector.Total,
                    ghgMtCo2eq = 400.0,
                    energyKtoe = 100_000.0,
                )
            ),
        )

    private fun makeRepo(
        api: EnvironmentApiService,
        dispatcher: kotlinx.coroutines.test.TestDispatcher,
        dao: FakeEnvironmentCacheDao = FakeEnvironmentCacheDao(),
        clock: Clock = Clock.System,
    ) = EnvironmentRepositoryImpl(api, dao, TestDispatcherProvider(dispatcher), clock)

    // ------------------------------------------------------------------------------------
    // 1. Cache miss, network success: emits Loading then Success(isStale=false)
    // ------------------------------------------------------------------------------------

    @Test
    fun observe_cacheMiss_networkSuccess_emitsLoadingThenFreshSuccess() = runTest {
        val api = FakeEnvironmentApiService().apply {
            willReturn = listOf(sampleSeries("PL"), sampleSeries("DE"))
        }
        val dao = FakeEnvironmentCacheDao() // cache miss
        val repo = makeRepo(api, StandardTestDispatcher(testScheduler), dao)

        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val success = awaitItem()
            assertIs<Result.Success<*>>(success)
            assertFalse(success.isStale, "First network Success must have isStale=false")
            awaitComplete()
        }
    }

    // ------------------------------------------------------------------------------------
    // 2. Cache miss, network success: Success carries the data
    // ------------------------------------------------------------------------------------

    @Test
    fun observe_cacheMiss_networkSuccess_successCarriesData() = runTest {
        val expectedSeries = listOf(sampleSeries("PL"), sampleSeries("DE"))
        val api = FakeEnvironmentApiService().apply { willReturn = expectedSeries }
        val repo = makeRepo(api, StandardTestDispatcher(testScheduler))

        repo.observe(defaultQuery).test {
            awaitItem() // Loading
            val success = awaitItem() as Result.Success<*>
            @Suppress("UNCHECKED_CAST")
            val data = success.data as List<EnvironmentTimeSeries>
            assertEquals(2, data.size)
            assertTrue(data.any { it.countryCode == "PL" })
            assertTrue(data.any { it.countryCode == "DE" })
            awaitComplete()
        }
    }

    // ------------------------------------------------------------------------------------
    // 3. Cache miss, network failure: emits Loading then Error
    // ------------------------------------------------------------------------------------

    @Test
    fun observe_cacheMiss_networkFailure_emitsLoadingThenError() = runTest {
        val api = FakeEnvironmentApiService().apply {
            throwable = RuntimeException("connection refused")
        }
        val repo = makeRepo(api, StandardTestDispatcher(testScheduler))

        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())
            assertIs<Result.Error>(awaitItem())
            awaitComplete()
        }
    }

    // ------------------------------------------------------------------------------------
    // 4. Cache hit (stale), network success: emits Loading, stale Success, fresh Success
    // ------------------------------------------------------------------------------------

    @Test
    fun observe_staleCache_networkSuccess_emitsThreeItems() = runTest {
        val cachedSeries = listOf(sampleSeries("PL"))
        val freshSeries = listOf(sampleSeries("PL"), sampleSeries("DE"))

        val staleInstant = Instant.fromEpochMilliseconds(0L) // far in the past
        val dao = FakeEnvironmentCacheDao().apply {
            cacheResult = EnvironmentCacheResult(cachedSeries, staleInstant)
        }
        val api = FakeEnvironmentApiService().apply { willReturn = freshSeries }
        val repo = makeRepo(api, StandardTestDispatcher(testScheduler), dao)

        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())

            val staleSuccess = awaitItem()
            assertIs<Result.Success<*>>(staleSuccess)
            assertTrue(staleSuccess.isStale, "Cache emission must be isStale=true")

            val freshSuccess = awaitItem()
            assertIs<Result.Success<*>>(freshSuccess)
            assertFalse(freshSuccess.isStale, "Network emission must be isStale=false")
            @Suppress("UNCHECKED_CAST")
            assertEquals(2, (freshSuccess.data as List<EnvironmentTimeSeries>).size)

            awaitComplete()
        }
    }

    // ------------------------------------------------------------------------------------
    // 5. Cache hit (fresh, within TTL), network success: emits Loading, fresh-cached Success,
    //    then revalidated Success
    // ------------------------------------------------------------------------------------

    @Test
    fun observe_freshCache_networkSuccess_emitsCachedThenFresh() = runTest {
        val cachedSeries = listOf(sampleSeries("PL"))
        val freshNow = Clock.System.now()
        val dao = FakeEnvironmentCacheDao().apply {
            cacheResult = EnvironmentCacheResult(cachedSeries, freshNow)
        }
        val api = FakeEnvironmentApiService().apply { willReturn = cachedSeries }
        val repo = makeRepo(api, StandardTestDispatcher(testScheduler), dao, clock = Clock.System)

        repo.observe(defaultQuery).test {
            awaitItem() // Loading
            val cached = awaitItem()
            assertIs<Result.Success<*>>(cached)
            // Within TTL — should NOT be stale
            assertFalse(cached.isStale, "Fresh cache must have isStale=false")
            awaitItem() // network revalidation
            awaitComplete()
        }
    }

    // ------------------------------------------------------------------------------------
    // 6. Cache hit, network failure: emits Loading + stale Success (no Error)
    // ------------------------------------------------------------------------------------

    @Test
    fun observe_cacheHit_networkFailure_suppressesError() = runTest {
        val cachedSeries = listOf(sampleSeries("PL"))
        val staleInstant = Instant.fromEpochMilliseconds(0L)
        val dao = FakeEnvironmentCacheDao().apply {
            cacheResult = EnvironmentCacheResult(cachedSeries, staleInstant)
        }
        val api = FakeEnvironmentApiService().apply {
            throwable = RuntimeException("offline")
        }
        val repo = makeRepo(api, StandardTestDispatcher(testScheduler), dao)

        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val stale = awaitItem()
            assertIs<Result.Success<*>>(stale)
            assertTrue(stale.isStale)
            awaitComplete() // no Error emitted
        }
    }

    // ------------------------------------------------------------------------------------
    // 7. Network success causes upsert on the cache DAO
    // ------------------------------------------------------------------------------------

    @Test
    fun observe_networkSuccess_upsertsCache() = runTest {
        val api = FakeEnvironmentApiService().apply { willReturn = listOf(sampleSeries()) }
        val dao = FakeEnvironmentCacheDao()
        val repo = makeRepo(api, StandardTestDispatcher(testScheduler), dao)

        repo.observe(defaultQuery).test {
            awaitItem() // Loading
            awaitItem() // Success
            awaitComplete()
        }

        assertEquals(1, dao.upsertCallCount, "Successful network fetch must trigger one cache upsert")
    }

    // ------------------------------------------------------------------------------------
    // 8. Query is passed through to the API service
    // ------------------------------------------------------------------------------------

    @Test
    fun observe_passesQueryToApi() = runTest {
        val api = FakeEnvironmentApiService().apply { willReturn = listOf(sampleSeries()) }
        val repo = makeRepo(api, StandardTestDispatcher(testScheduler))
        val specificQuery = EnvironmentQuery(listOf("FR", "ES"), 2018..2023)

        repo.observe(specificQuery).test {
            awaitItem()
            awaitItem()
            awaitComplete()
        }

        val capturedQuery = api.lastQuery
        assertNotNull(capturedQuery)
        assertEquals(listOf("FR", "ES"), capturedQuery!!.countryCodes)
        assertEquals(2018..2023, capturedQuery.yearRange)
    }

    // ------------------------------------------------------------------------------------
    // 9. Empty result from API: emits Success with empty list (not Error)
    // ------------------------------------------------------------------------------------

    @Test
    fun observe_emptyApiResult_emitsSuccessWithEmptyList() = runTest {
        val api = FakeEnvironmentApiService().apply { willReturn = emptyList() }
        val repo = makeRepo(api, StandardTestDispatcher(testScheduler))

        repo.observe(defaultQuery).test {
            awaitItem() // Loading
            val success = awaitItem()
            assertIs<Result.Success<*>>(success)
            @Suppress("UNCHECKED_CAST")
            val data = success.data as List<EnvironmentTimeSeries>
            assertTrue(data.isEmpty(), "Empty API result must produce empty Success, not Error")
            awaitComplete()
        }
    }

    // ------------------------------------------------------------------------------------
    // 10. refresh() calls API once and upserts the cache
    // ------------------------------------------------------------------------------------

    @Test
    fun refresh_callsApiAndUpsertsCache() = runTest {
        val api = FakeEnvironmentApiService().apply { willReturn = listOf(sampleSeries()) }
        val dao = FakeEnvironmentCacheDao()
        val repo = makeRepo(api, StandardTestDispatcher(testScheduler), dao)

        repo.refresh(defaultQuery)

        assertEquals(1, api.callCount, "refresh() must invoke the API exactly once")
        assertEquals(1, dao.upsertCallCount, "refresh() must upsert the cache once")
    }

    // ------------------------------------------------------------------------------------
    // 11. refresh() propagates API failure as thrown exception
    // ------------------------------------------------------------------------------------

    @Test
    fun refresh_apiFailure_throws() = runTest {
        val api = FakeEnvironmentApiService().apply {
            throwable = RuntimeException("network error")
        }
        val repo = makeRepo(api, StandardTestDispatcher(testScheduler))

        var caught: Throwable? = null
        try {
            repo.refresh(defaultQuery)
        } catch (t: Throwable) {
            caught = t
        }

        assertNotNull(caught, "refresh() must propagate API failure as an exception")
    }

    @Test
    fun cancellation_during_fetch_is_propagated_not_swallowed() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val hangingApi = object : EnvironmentApiService {
            override suspend fun fetch(query: EnvironmentQuery): List<EnvironmentTimeSeries> {
                suspendCancellableCoroutine<Nothing> { /* never completes */ }
            }
        }
        val repo = makeRepo(hangingApi, dispatcher)

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
