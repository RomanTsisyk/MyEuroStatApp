package eu.eurostat.feature.population.data

import app.cash.turbine.test
import eu.eurostat.core.common.AppError
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.common.cache.BlobCacheEntry
import eu.eurostat.core.common.cache.BlobCacheStore
import eu.eurostat.core.common.cache.JsonBlobCache
import eu.eurostat.feature.population.domain.PopulationCohort
import eu.eurostat.feature.population.domain.PopulationData
import eu.eurostat.feature.population.domain.PopulationDataPoint
import eu.eurostat.feature.population.domain.PopulationQuery
import eu.eurostat.feature.population.domain.PopulationSnapshot
import eu.eurostat.feature.population.domain.PopulationTimeSeries
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

class FakePopulationApiService : PopulationApiService {
    private var result: Result<PopulationData> = Result.Success(PopulationData(emptyList(), emptyMap()))
    var callCount = 0

    fun willReturn(series: List<PopulationTimeSeries>) {
        result = Result.Success(PopulationData(timeSeries = series, snapshots = emptyMap()))
    }

    /** Full-frame variant: lets tests return cohort snapshots from the fake network. */
    fun willReturnData(data: PopulationData) {
        result = Result.Success(data)
    }

    fun willThrow(t: Throwable) {
        result = Result.Error(AppError.Unknown(t))
    }

    override suspend fun fetchPopulation(query: PopulationQuery): PopulationData {
        callCount++
        return when (val r = result) {
            is Result.Success -> r.data
            is Result.Error -> throw (r.cause as? AppError.Unknown)?.throwable ?: RuntimeException("fake error")
            is Result.Loading -> PopulationData(emptyList(), emptyMap())
        }
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

/**
 * In-memory [BlobCacheStore] backing the cohort blob tier in tests.
 * [failReads]/[failWrites] simulate a broken storage layer for degradation tests.
 */
class FakeBlobCacheStore : BlobCacheStore {
    private val entries = mutableMapOf<String, BlobCacheEntry>()

    /** When `true`, every [get] throws to simulate a storage failure. */
    var failReads: Boolean = false

    /** When `true`, every [put] throws to simulate a storage failure. */
    var failWrites: Boolean = false

    override suspend fun get(key: String): BlobCacheEntry? {
        check(!failReads) { "simulated storage failure" }
        return entries[key]
    }

    override suspend fun put(key: String, dataJson: String, fetchedAtEpochMs: Long) {
        check(!failWrites) { "simulated storage failure" }
        entries[key] = BlobCacheEntry(dataJson, fetchedAtEpochMs)
    }

    override suspend fun deleteByPrefix(prefix: String) {
        entries.keys.filter { it.startsWith(prefix) }.forEach { entries.remove(it) }
    }

    /** Seeds raw (possibly malformed) JSON directly, bypassing serialization. */
    fun seedRaw(key: String, dataJson: String, fetchedAtEpochMs: Long) {
        entries[key] = BlobCacheEntry(dataJson, fetchedAtEpochMs)
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

private fun fakeDataWithSnapshots(country: String = "PL", year: Int = 2020) = PopulationData(
    timeSeries = fakeSeries(country, year),
    snapshots = mapOf(
        (country to year) to PopulationSnapshot(
            countryCode = country,
            countryName = country,
            year = year,
            cohorts = listOf(
                PopulationCohort("Y_LT5", "Less than 5 years", 900_000L, 850_000L),
                PopulationCohort("Y5-9", "From 5 to 9 years", 950_000L, 900_000L),
            ),
            totalMale = 18_000_000L,
            totalFemale = 20_000_000L,
            total = 38_000_000L,
        )
    ),
)

private val testQuery = PopulationQuery(listOf("PL"), 2020..2024)

private fun cohortCacheOver(store: FakeBlobCacheStore) =
    JsonBlobCache(store, PopulationCacheBlob.serializer())

/** Seeds one cohort blob for [query] the same way the repository persists it. */
private suspend fun seedCohortBlob(
    store: FakeBlobCacheStore,
    query: PopulationQuery,
    data: PopulationData,
    fetchedAt: Instant,
) {
    cohortCacheOver(store).put(
        key = PopulationRepositoryImpl.cohortCacheKey(query),
        value = PopulationCacheBlob.fromDomain(data),
        fetchedAt = fetchedAt,
    )
}

private fun makeRepo(
    api: PopulationApiService,
    dao: PopulationCacheDao,
    dispatcher: TestDispatcher,
    clock: FakeClock,
    store: FakeBlobCacheStore = FakeBlobCacheStore(),
) = PopulationRepositoryImpl(
    api = api,
    dao = dao,
    cohortCache = cohortCacheOver(store),
    dispatchers = TestDispatcherProvider(dispatcher),
    clock = clock,
)

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
        val repo = makeRepo(api, dao, dispatcher, clock)

        repo.observe(cohortlessQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val item = awaitItem()
            assertIs<Result.Success<*>>(item)
            assertFalse((item as Result.Success<*>).isStale, "Fresh cache should not be stale")
            awaitComplete()
        }
    }

    @Test
    fun fresh_dao_cache_with_cohorts_but_no_blob_revalidates_to_get_cohort_data() = runTest {
        // The flat table has no cohort rows. With a fresh dao cache but an
        // empty blob tier, the repo must still revalidate when the query
        // includes cohorts so the pyramid hero does not stay empty forever.
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = FakeClock(BASE_TIME)
        val api = FakePopulationApiService()
        val dao = FakePopulationCacheDao()
        dao.seed(fakeSeries(), fetchedAt = BASE_TIME.minus(1.hours))
        api.willReturn(fakeSeries())

        // testQuery uses includeCohorts=true by default.
        val repo = makeRepo(api, dao, dispatcher, clock)

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
    fun failing_blob_store_read_degrades_to_miss_instead_of_throwing() = runTest {
        // A storage-layer exception (missing table, disk error) on the blob
        // read must degrade to a cache miss and recover via the network —
        // never escape the flow (the data layer's never-throw contract).
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = FakeClock(BASE_TIME)
        val api = FakePopulationApiService()
        val dao = FakePopulationCacheDao()
        val store = FakeBlobCacheStore().apply { failReads = true }
        api.willReturnData(fakeDataWithSnapshots())

        val repo = makeRepo(api, dao, dispatcher, clock, store)

        repo.observe(testQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val fresh = awaitItem()
            assertIs<Result.Success<PopulationData>>(fresh)
            assertFalse(fresh.isStale)
            awaitComplete()
        }
    }

    @Test
    fun failing_blob_store_write_still_emits_fresh_network_data() = runTest {
        // A storage-layer exception while persisting the blob must not
        // discard a successful network fetch.
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = FakeClock(BASE_TIME)
        val api = FakePopulationApiService()
        val dao = FakePopulationCacheDao()
        val store = FakeBlobCacheStore().apply { failWrites = true }
        api.willReturnData(fakeDataWithSnapshots())

        val repo = makeRepo(api, dao, dispatcher, clock, store)

        repo.observe(testQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val fresh = awaitItem()
            assertIs<Result.Success<PopulationData>>(fresh)
            assertFalse(fresh.isStale)
            assertEquals(2, fresh.data.snapshots[("PL" to 2020)]?.cohorts?.size)
            awaitComplete()
        }
    }

    @Test
    fun fresh_cohort_blob_hit_serves_snapshots_without_network() = runTest {
        // The blob tier persists the full frame including pyramid snapshots,
        // so a fresh hit satisfies a cohort query entirely offline.
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = FakeClock(BASE_TIME)
        val api = FakePopulationApiService()
        val dao = FakePopulationCacheDao()
        val store = FakeBlobCacheStore()
        seedCohortBlob(store, testQuery, fakeDataWithSnapshots(), fetchedAt = BASE_TIME.minus(1.hours))

        val repo = makeRepo(api, dao, dispatcher, clock, store)

        repo.observe(testQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val cached = awaitItem()
            assertIs<Result.Success<PopulationData>>(cached)
            assertFalse(cached.isStale)
            val snapshot = cached.data.snapshots[("PL" to 2020)]
            assertEquals(2, snapshot?.cohorts?.size, "Cohort snapshots must come from the blob cache")
            assertEquals(38_000_000L, snapshot?.total)
            awaitComplete()
        }
        assertEquals(0, api.callCount, "Fresh blob hit must not trigger a network call")
    }

    @Test
    fun stale_cohort_blob_emits_stale_snapshots_then_fresh() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = FakeClock(BASE_TIME)
        val api = FakePopulationApiService()
        val dao = FakePopulationCacheDao()
        val store = FakeBlobCacheStore()
        seedCohortBlob(store, testQuery, fakeDataWithSnapshots(), fetchedAt = BASE_TIME.minus(13.hours))
        api.willReturnData(fakeDataWithSnapshots())

        val repo = makeRepo(api, dao, dispatcher, clock, store)

        repo.observe(testQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val stale = awaitItem()
            assertIs<Result.Success<PopulationData>>(stale)
            assertTrue(stale.isStale, "Blob older than TTL must be stale")
            assertTrue(stale.data.snapshots.isNotEmpty(), "Stale emission still carries snapshots")
            val fresh = awaitItem()
            assertIs<Result.Success<PopulationData>>(fresh)
            assertFalse(fresh.isStale)
            awaitComplete()
        }
        assertEquals(1, api.callCount)
    }

    @Test
    fun network_success_persists_cohort_blob_for_next_observe() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = FakeClock(BASE_TIME)
        val api = FakePopulationApiService()
        val dao = FakePopulationCacheDao()
        val store = FakeBlobCacheStore()
        api.willReturnData(fakeDataWithSnapshots())

        val repo = makeRepo(api, dao, dispatcher, clock, store)

        // First observe: cache miss -> network -> persists the blob.
        repo.observe(testQuery).test {
            assertEquals(Result.Loading, awaitItem())
            assertIs<Result.Success<*>>(awaitItem())
            awaitComplete()
        }
        assertEquals(1, api.callCount)

        // Second observe: fresh blob hit with snapshots, no further network call.
        repo.observe(testQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val cached = awaitItem()
            assertIs<Result.Success<PopulationData>>(cached)
            assertFalse(cached.isStale)
            assertTrue(cached.data.snapshots.isNotEmpty(), "Persisted blob must restore snapshots")
            awaitComplete()
        }
        assertEquals(1, api.callCount, "Second observe must be served from the blob cache")
    }

    @Test
    fun corrupted_cohort_blob_falls_back_to_dao_then_revalidates() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = FakeClock(BASE_TIME)
        val api = FakePopulationApiService()
        val dao = FakePopulationCacheDao()
        val store = FakeBlobCacheStore()
        store.seedRaw(
            key = PopulationRepositoryImpl.cohortCacheKey(testQuery),
            dataJson = "{ not a PopulationCacheBlob",
            fetchedAtEpochMs = BASE_TIME.toEpochMilliseconds(),
        )
        dao.seed(fakeSeries(), fetchedAt = BASE_TIME.minus(1.hours))
        api.willReturnData(fakeDataWithSnapshots())

        val repo = makeRepo(api, dao, dispatcher, clock, store)

        repo.observe(testQuery).test {
            assertEquals(Result.Loading, awaitItem())
            // Corrupted blob is a miss -> dao tier serves the trend (no snapshots)…
            val cached = awaitItem()
            assertIs<Result.Success<PopulationData>>(cached)
            assertTrue(cached.data.snapshots.isEmpty())
            // …and the cohort query still revalidates for the pyramid.
            val fresh = awaitItem()
            assertIs<Result.Success<PopulationData>>(fresh)
            assertTrue(fresh.data.snapshots.isNotEmpty())
            awaitComplete()
        }
        assertEquals(1, api.callCount)
    }

    @Test
    fun cohort_cache_key_is_versioned_and_order_insensitive() {
        val key = PopulationRepositoryImpl.cohortCacheKey(
            PopulationQuery(listOf("PL", "DE"), 2020..2024)
        )
        assertEquals("population:cohorts:v1:DE,PL:2020:2024", key)
        assertEquals(
            key,
            PopulationRepositoryImpl.cohortCacheKey(PopulationQuery(listOf("DE", "PL"), 2020..2024)),
            "Country order must not change the cache key",
        )
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

        val repo = makeRepo(api, dao, dispatcher, clock)

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

        val repo = makeRepo(api, dao, dispatcher, clock)

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

        val repo = makeRepo(api, dao, dispatcher, clock)

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

        val repo = makeRepo(api, dao, dispatcher, clock)

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
    fun stale_blob_with_network_failure_swallows_error() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = FakeClock(BASE_TIME)
        val api = FakePopulationApiService()
        val dao = FakePopulationCacheDao() // empty — only the blob tier has data
        val store = FakeBlobCacheStore()
        seedCohortBlob(store, testQuery, fakeDataWithSnapshots(), fetchedAt = BASE_TIME.minus(13.hours))
        api.willThrow(RuntimeException("network down"))

        val repo = makeRepo(api, dao, dispatcher, clock, store)

        repo.observe(testQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val item = awaitItem()
            assertIs<Result.Success<PopulationData>>(item)
            assertTrue(item.isStale)
            assertTrue(item.data.snapshots.isNotEmpty(), "Offline pyramid: stale blob still has cohorts")
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

        val repo = makeRepo(api, dao, dispatcher, clock)

        repo.refresh(testQuery)

        assertEquals(1, api.callCount, "API should have been called exactly once during refresh")
    }

    @Test
    fun cancellation_during_fetch_is_propagated_not_swallowed() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = FakeClock(BASE_TIME)

        // API service that suspends indefinitely — simulates a slow network call
        val hangingApi = object : PopulationApiService {
            override suspend fun fetchPopulation(query: PopulationQuery): PopulationData {
                suspendCancellableCoroutine<Nothing> { /* never completes */ }
            }
        }
        val dao = FakePopulationCacheDao() // empty cache → network will be attempted

        val repo = makeRepo(hangingApi, dao, dispatcher, clock)

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
