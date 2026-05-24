package eu.eurostat.core.database

import eu.eurostat.core.database.generated.AppDatabase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransportCacheQueriesTest {

    private lateinit var db: AppDatabase
    private val queries get() = db.transportCacheQueries
    private val mode = "ROAD"

    @BeforeTest
    fun setup() {
        db = createInMemoryDatabase()
    }

    @Test
    fun insert_then_query_returns_row() {
        queries.upsert("DE", 2020, 500_000L, 100_000L, 50_000L, mode, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("DE"), 2019, 2021, mode).executeAsList()
        assertEquals(1, rows.size)
        assertEquals("DE", rows[0].country_code)
        assertEquals(2020, rows[0].year)
        assertEquals(500_000L, rows[0].road_passengers)
    }

    @Test
    fun query_returns_empty_when_no_match() {
        queries.upsert("DE", 2020, 500_000L, null, null, mode, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("FR"), 2019, 2021, mode).executeAsList()
        assertTrue(rows.isEmpty())
    }

    @Test
    fun upsert_replaces_existing_row_by_primary_key() {
        queries.upsert("DE", 2020, 500_000L, null, null, mode, 1000L)
        queries.upsert("DE", 2020, 550_000L, 110_000L, 60_000L, mode, 2000L)
        val rows = queries.queryByCountriesAndYears(listOf("DE"), 2020, 2020, mode).executeAsList()
        assertEquals(1, rows.size)
        assertEquals(550_000L, rows[0].road_passengers)
        assertEquals(110_000L, rows[0].air_passengers)
        assertEquals(2000L, rows[0].fetched_at_epoch_ms)
    }

    @Test
    fun oldestFetchedAt_returns_null_when_no_rows() {
        assertNull(queries.oldestFetchedAt(listOf("DE"), 2020, 2020, mode).executeAsOne().oldest)
    }

    @Test
    fun oldestFetchedAt_returns_minimum_value() {
        queries.upsert("DE", 2020, 1L, null, null, mode, 1500L)
        queries.upsert("DE", 2021, 1L, null, null, mode, 1000L)
        queries.upsert("DE", 2022, 1L, null, null, mode, 2000L)
        val result = queries.oldestFetchedAt(listOf("DE"), 2020, 2022, mode).executeAsOne()
        assertEquals(1000L, result.oldest)
    }

    @Test
    fun delete_removes_matching_rows_only() {
        queries.upsert("DE", 2020, 1L, null, null, mode, 1000L)
        queries.upsert("FR", 2020, 1L, null, null, mode, 1000L)
        queries.upsert("DE", 2021, 1L, null, null, mode, 1000L)
        queries.deleteByCountriesAndYears(listOf("DE"), 2020, 2020, mode)
        val remaining = queries.queryByCountriesAndYears(listOf("DE", "FR"), 2019, 2022, mode).executeAsList()
        assertEquals(2, remaining.size)
        assertTrue(remaining.any { it.country_code == "FR" && it.year == 2020L })
        assertTrue(remaining.any { it.country_code == "DE" && it.year == 2021L })
        assertFalse(remaining.any { it.country_code == "DE" && it.year == 2020L })
    }

    @Test
    fun query_handles_year_range_inclusively() {
        queries.upsert("DE", 2019, 1L, null, null, mode, 1000L)
        queries.upsert("DE", 2020, 2L, null, null, mode, 1000L)
        queries.upsert("DE", 2021, 3L, null, null, mode, 1000L)
        queries.upsert("DE", 2022, 4L, null, null, mode, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("DE"), 2020, 2021, mode).executeAsList()
        assertEquals(2, rows.size)
    }

    @Test
    fun query_handles_multiple_countries() {
        queries.upsert("DE", 2020, 1L, null, null, mode, 1000L)
        queries.upsert("FR", 2020, 2L, null, null, mode, 1000L)
        queries.upsert("IT", 2020, 3L, null, null, mode, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("DE", "FR"), 2020, 2020, mode).executeAsList()
        assertEquals(2, rows.size)
    }

    @Test
    fun query_filters_by_mode() {
        queries.upsert("DE", 2020, 500_000L, null, null, "ROAD", 1000L)
        queries.upsert("DE", 2020, null, 100_000L, null, "AIR", 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("DE"), 2020, 2020, "ROAD").executeAsList()
        assertEquals(1, rows.size)
        assertEquals("ROAD", rows[0].mode)
        assertEquals(500_000L, rows[0].road_passengers)
    }
}
