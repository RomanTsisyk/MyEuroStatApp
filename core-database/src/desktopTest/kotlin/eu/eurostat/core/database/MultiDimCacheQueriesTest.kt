package eu.eurostat.core.database

import eu.eurostat.core.database.generated.AppDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MultiDimCacheQueriesTest {

    private lateinit var db: AppDatabase
    private val queries get() = db.multiDimCacheQueries

    @BeforeTest
    fun setup() {
        db = createInMemoryDatabase()
    }

    @Test
    fun insert_then_select_returns_row() {
        queries.upsert("population:cohorts:v1:PL:2020:2024", """{"a":1}""", 1000L)
        val row = queries.selectByKey("population:cohorts:v1:PL:2020:2024").executeAsOne()
        assertEquals("""{"a":1}""", row.data_json)
        assertEquals(1000L, row.fetched_at_epoch_ms)
    }

    @Test
    fun select_missing_key_returns_null() {
        assertNull(queries.selectByKey("absent").executeAsOneOrNull())
    }

    @Test
    fun upsert_replaces_existing_row_by_key() {
        queries.upsert("k", """{"a":1}""", 1000L)
        queries.upsert("k", """{"a":2}""", 2000L)
        val row = queries.selectByKey("k").executeAsOne()
        assertEquals("""{"a":2}""", row.data_json)
        assertEquals(2000L, row.fetched_at_epoch_ms)
    }

    @Test
    fun deleteByKeyPrefix_removes_only_matching_rows() {
        queries.upsert("tourism:series:v1:PL:2020:2024", "{}", 1000L)
        queries.upsert("tourism:series:v1:DE:2020:2024", "{}", 1000L)
        queries.upsert("population:cohorts:v1:PL:2020:2024", "{}", 1000L)

        queries.deleteByKeyPrefix("tourism:series:")

        assertNull(queries.selectByKey("tourism:series:v1:PL:2020:2024").executeAsOneOrNull())
        assertNull(queries.selectByKey("tourism:series:v1:DE:2020:2024").executeAsOneOrNull())
        assertEquals(
            "{}",
            queries.selectByKey("population:cohorts:v1:PL:2020:2024").executeAsOne().data_json,
        )
    }

    @Test
    fun clearAll_removes_every_row() {
        queries.upsert("a", "{}", 1000L)
        queries.upsert("b", "{}", 1000L)
        queries.clearAll()
        assertNull(queries.selectByKey("a").executeAsOneOrNull())
        assertNull(queries.selectByKey("b").executeAsOneOrNull())
    }

    @Test
    fun blob_store_round_trips_through_real_database() = runTest {
        val store = SqlDelightBlobCacheStore(db)
        store.put("k", """{"x":42}""", 5000L)

        val entry = store.get("k")
        assertEquals("""{"x":42}""", entry?.dataJson)
        assertEquals(5000L, entry?.fetchedAtEpochMs)

        store.deleteByPrefix("k")
        assertTrue(store.get("k") == null, "deleteByPrefix must remove the entry")
    }
}
