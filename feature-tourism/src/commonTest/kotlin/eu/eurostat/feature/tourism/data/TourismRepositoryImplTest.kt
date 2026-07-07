package eu.eurostat.feature.tourism.data

import app.cash.turbine.test
import eu.eurostat.core.common.Result
import eu.eurostat.core.common.cache.JsonBlobCache
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

    private fun sampleHeatmap() = listOf(listOf(0.1f, 0.5f, 1.0f))

    private fun cacheOver(store: FakeBlobCacheStore) =
        JsonBlobCache(store, TourismCacheBlob.serializer())

    /** Seeds one blob for [query] the same way the repository persists it. */
    private suspend fun seed(
        store: FakeBlobCacheStore,
        query: TourismQuery,
        points: List<TourismDataPoint>,
        fetchedAt: Instant,
        labels: Map<String, String> = emptyMap(),
        heatmapCells: List<List<Float>> = emptyList(),
    ) {
        cacheOver(store).put(
            key = TourismRepositoryImpl.cacheKey(query),
            value = TourismCacheBlob.fromFetchResult(
                TourismFetchResult(points, labels, heatmapCells),
            ),
            fetchedAt = fetchedAt,
        )
    }

    private fun makeRepo(
        api: TourismApiService,
        store: FakeBlobCacheStore,
        clock: FakeClock,
        scheduler: kotlinx.coroutines.test.TestCoroutineScheduler,
    ) = TourismRepositoryImpl(
        api = api,
        cache = cacheOver(store),
        dispatchers = TestDispatcherProviderLocal(StandardTestDispatcher(scheduler)),
        clock = clock,
    )

    @Test
    fun cache_hit_fresh_emits_loading_then_success_not_stale() = runTest {
        val clock = FakeClock(Instant.parse("2026-05-16T10:00:00Z"))
        val store = FakeBlobCacheStore()
        seed(store, defaultQuery, samplePoints(), fetchedAt = clock.now())
        val api = FakeTourismApiService().apply { willReturn = samplePoints() }
        val repo = makeRepo(api, store, clock, testScheduler)

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
        val store = FakeBlobCacheStore()
        seed(store, defaultQuery, samplePoints(), fetchedAt = clock.now() - 13.hours)
        val api = FakeTourismApiService().apply { willReturn = samplePoints() }
        val repo = makeRepo(api, store, clock, testScheduler)

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
        val store = FakeBlobCacheStore()
        val api = FakeTourismApiService().apply { willReturn = samplePoints() }
        val repo = makeRepo(api, store, clock, testScheduler)

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
        val repo = makeRepo(api, FakeBlobCacheStore(), clock, testScheduler)

        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())
            assertTrue(awaitItem() is Result.Error)
            awaitComplete()
        }
    }

    @Test
    fun stale_cache_with_network_failure_emits_stale_no_error() = runTest {
        val clock = FakeClock(Instant.parse("2026-05-16T10:00:00Z"))
        val store = FakeBlobCacheStore()
        seed(store, defaultQuery, samplePoints(), fetchedAt = clock.now() - 13.hours)
        val api = FakeTourismApiService().apply { throwable = IllegalStateException("timeout") }
        val repo = makeRepo(api, store, clock, testScheduler)

        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val stale = awaitItem()
            assertTrue(stale is Result.Success)
            assertTrue((stale as Result.Success).isStale)
            awaitComplete()
        }
    }

    @Test
    fun corrupted_cache_blob_degrades_to_miss_and_refetches() = runTest {
        val clock = FakeClock(Instant.parse("2026-05-16T10:00:00Z"))
        val store = FakeBlobCacheStore()
        store.seedRaw(
            key = TourismRepositoryImpl.cacheKey(defaultQuery),
            dataJson = "{ definitely not a TourismCacheBlob",
            fetchedAtEpochMs = clock.now().toEpochMilliseconds(),
        )
        val api = FakeTourismApiService().apply { willReturn = samplePoints() }
        val repo = makeRepo(api, store, clock, testScheduler)

        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())
            // No cached emission — corrupted blob is a miss, straight to fresh.
            val fresh = awaitItem()
            assertTrue(fresh is Result.Success)
            assertFalse((fresh as Result.Success).isStale)
            awaitComplete()
        }
        assertEquals(1, api.callCount)
    }

    @Test
    fun failing_cache_storage_degrades_to_miss_and_refetches() = runTest {
        val clock = FakeClock(Instant.parse("2026-05-16T10:00:00Z"))
        val store = FakeBlobCacheStore().apply { failReads = true }
        val api = FakeTourismApiService().apply { willReturn = samplePoints() }
        val repo = makeRepo(api, store, clock, testScheduler)

        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())
            assertTrue(awaitItem() is Result.Success)
            awaitComplete()
        }
    }

    @Test
    fun fresh_empty_points_blob_is_a_miss_and_revalidates() = runTest {
        // A blob with no yearly points (persisted from a degraded fetch) must
        // not act as a 12h negative cache: the repository revalidates instead
        // of early-returning with empty content.
        val clock = FakeClock(Instant.parse("2026-05-16T10:00:00Z"))
        val store = FakeBlobCacheStore()
        seed(
            store,
            defaultQuery,
            points = emptyList(),
            fetchedAt = clock.now(), // fresh — would early-return if it counted as a hit
            heatmapCells = sampleHeatmap(),
        )
        val api = FakeTourismApiService().apply { willReturn = samplePoints() }
        val repo = makeRepo(api, store, clock, testScheduler)

        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())
            // No cached emission — straight to the fresh network result.
            val fresh = awaitItem()
            assertTrue(fresh is Result.Success)
            assertFalse((fresh as Result.Success).isStale)
            val data = fresh.data as TourismData
            assertEquals(1, data.timeSeries.sumOf { it.points.size })
            awaitComplete()
        }
        assertEquals(1, api.callCount, "Empty-points blob must trigger revalidation")
    }

    @Test
    fun null_metrics_survive_cache_round_trip_without_sentinels() = runTest {
        // Regression guard for the old -1L nights sentinel: with the JSON blob
        // "no data" is a real null and must round-trip as null, never 0 or -1.
        val clock = FakeClock(Instant.parse("2026-05-16T10:00:00Z"))
        val store = FakeBlobCacheStore()
        val point = TourismDataPoint(
            countryCode = "PL",
            year = 2022,
            domesticNights = 1_000_000L,
            foreignNights = null,
            totalNights = null,
            trips = 500_000L,
        )
        seed(store, defaultQuery, listOf(point), fetchedAt = clock.now())
        val repo = makeRepo(FakeTourismApiService(), store, clock, testScheduler)

        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val cached = awaitItem()
            assertTrue(cached is Result.Success)
            @Suppress("UNCHECKED_CAST")
            val data = (cached as Result.Success<TourismData>).data
            val restored = data.timeSeries.single().points.single()
            assertNull(restored.foreignNights, "foreignNights must round-trip as null")
            assertNull(restored.totalNights, "totalNights must round-trip as null")
            assertEquals(1_000_000L, restored.domesticNights)
            assertEquals(500_000L, restored.trips)
            awaitComplete()
        }
    }

    @Test
    fun labels_and_heatmap_survive_cache_round_trip() = runTest {
        // Previously labels/heatmap lived only in repository memory and were
        // lost across process restarts; the blob persists them.
        val clock = FakeClock(Instant.parse("2026-05-16T10:00:00Z"))
        val store = FakeBlobCacheStore()
        seed(
            store,
            defaultQuery,
            samplePoints(),
            fetchedAt = clock.now(),
            labels = mapOf("PL" to "Poland"),
            heatmapCells = sampleHeatmap(),
        )
        val api = FakeTourismApiService()
        val repo = makeRepo(api, store, clock, testScheduler)

        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())
            val cached = awaitItem()
            assertTrue(cached is Result.Success)
            @Suppress("UNCHECKED_CAST")
            val data = (cached as Result.Success<TourismData>).data
            assertEquals("Poland", data.timeSeries.single().countryName)
            assertEquals(sampleHeatmap(), data.heatmapCells)
            awaitComplete()
        }
        assertEquals(0, api.callCount)
    }

    @Test
    fun fresh_fetch_with_empty_heatmap_keeps_previously_cached_heatmap() = runTest {
        val clock = FakeClock(Instant.parse("2026-05-16T10:00:00Z"))
        val store = FakeBlobCacheStore()
        // Stale blob with a heatmap → revalidation will run.
        seed(
            store,
            defaultQuery,
            samplePoints(),
            fetchedAt = clock.now() - 13.hours,
            heatmapCells = sampleHeatmap(),
        )
        // Fresh fetch succeeds but its seasonality slice degraded to empty.
        val api = FakeTourismApiService().apply { willReturn = samplePoints() }
        val repo = makeRepo(api, store, clock, testScheduler)

        repo.observe(defaultQuery).test {
            assertEquals(Result.Loading, awaitItem())
            awaitItem() // stale cached emission
            val fresh = awaitItem()
            assertTrue(fresh is Result.Success)
            @Suppress("UNCHECKED_CAST")
            val data = (fresh as Result.Success<TourismData>).data
            assertEquals(
                sampleHeatmap(),
                data.heatmapCells,
                "Empty fresh heatmap must not wipe the cached grid",
            )
            awaitComplete()
        }
    }

    @Test
    fun force_refresh_bypasses_cache_and_persists_blob() = runTest {
        val clock = FakeClock(Instant.parse("2026-05-16T10:00:00Z"))
        val store = FakeBlobCacheStore()
        val api = FakeTourismApiService().apply { willReturn = samplePoints() }
        val repo = makeRepo(api, store, clock, testScheduler)

        repo.refresh(defaultQuery)
        assertEquals(1, api.callCount)

        val persisted = cacheOver(store).get(TourismRepositoryImpl.cacheKey(defaultQuery))
        assertNotNull(persisted, "refresh must persist the fetched blob")
        assertEquals(1, persisted.value.points.size)
        assertEquals(clock.now(), persisted.fetchedAt)
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
        val repo = TourismRepositoryImpl(
            api = hangingApi,
            cache = cacheOver(FakeBlobCacheStore()),
            dispatchers = TestDispatcherProviderLocal(dispatcher),
            clock = clock,
        )

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
    fun concurrent_observe_and_refresh_do_not_race() = runTest {
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
        val repo = TourismRepositoryImpl(
            api = api,
            cache = cacheOver(FakeBlobCacheStore()),
            dispatchers = TestDispatcherProviderLocal(dispatcher),
            clock = clock,
        )

        val results = mutableListOf<Result<*>>()
        val job1 = launch(dispatcher) { repo.observe(defaultQuery).collect { results.add(it) } }
        val job2 = launch(dispatcher) { repo.refresh(defaultQuery) }

        advanceTimeBy(500)
        job1.cancelAndJoin()
        job2.cancelAndJoin()

        // The repository holds no shared mutable state (everything rides in the
        // per-query blob), so no error may surface from concurrent access.
        assertTrue(results.none { it is Result.Error }, "No error expected from concurrent state access")
    }
}
