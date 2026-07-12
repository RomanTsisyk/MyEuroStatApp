package eu.eurostat.core.common.cache

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours

class JsonBlobCacheTest {

    private val fetchedAt = Instant.parse("2026-05-16T10:00:00Z")

    private fun listCache(store: BlobCacheStore) =
        JsonBlobCache(store, ListSerializer(Long.serializer()))

    @Test
    fun put_then_get_round_trips_value_and_timestamp() = runTest {
        val store = InMemoryBlobCacheStore()
        val cache = listCache(store)

        cache.put("feature:slice:v1:PL:2020:2024", listOf(1L, 2L, 3L), fetchedAt)
        val hit = cache.get("feature:slice:v1:PL:2020:2024")

        assertNotNull(hit, "Stored key must be a cache hit")
        assertEquals(listOf(1L, 2L, 3L), hit.value)
        assertEquals(fetchedAt, hit.fetchedAt)
    }

    @Test
    fun get_missing_key_returns_null() = runTest {
        val cache = listCache(InMemoryBlobCacheStore())
        assertNull(cache.get("feature:slice:v1:absent"))
    }

    @Test
    fun corrupted_json_is_treated_as_cache_miss() = runTest {
        val store = InMemoryBlobCacheStore()
        val cache = listCache(store)
        store.put("k", "{ not valid json !!", fetchedAtEpochMs = 1_000L)

        assertNull(cache.get("k"), "Corrupted payload must decode to a miss, not throw")
    }

    @Test
    fun shape_incompatible_json_is_treated_as_cache_miss() = runTest {
        val store = InMemoryBlobCacheStore()
        val cache = listCache(store)
        // Valid JSON, but a string — not a list of longs (simulates DTO drift).
        store.put("k", "\"hello\"", fetchedAtEpochMs = 1_000L)

        assertNull(cache.get("k"), "Wrong-shape payload must decode to a miss, not throw")
    }

    @Test
    fun put_overwrites_existing_entry() = runTest {
        val store = InMemoryBlobCacheStore()
        val cache = listCache(store)
        cache.put("k", listOf(1L), fetchedAt)
        cache.put("k", listOf(2L), fetchedAt + 1.hours)

        val hit = cache.get("k")
        assertNotNull(hit)
        assertEquals(listOf(2L), hit.value)
        assertEquals(fetchedAt + 1.hours, hit.fetchedAt)
    }

    @Test
    fun deleteByPrefix_removes_only_matching_keys() = runTest {
        val store = InMemoryBlobCacheStore()
        val cache = JsonBlobCache(
            store,
            MapSerializer(String.serializer(), Long.serializer()),
        )
        cache.put("population:cohorts:v1:PL", mapOf("a" to 1L), fetchedAt)
        cache.put("population:cohorts:v1:DE", mapOf("b" to 2L), fetchedAt)
        cache.put("tourism:series:v1:PL", mapOf("c" to 3L), fetchedAt)

        cache.deleteByPrefix("population:cohorts:")

        assertNull(cache.get("population:cohorts:v1:PL"))
        assertNull(cache.get("population:cohorts:v1:DE"))
        assertNotNull(cache.get("tourism:series:v1:PL"), "Non-matching key must survive")
        assertEquals(1, store.size)
    }
}
