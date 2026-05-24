package eu.eurostat.core.database

import eu.eurostat.core.database.generated.AppDatabase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TourismCacheQueriesTest {

    private lateinit var db: AppDatabase
    private val queries get() = db.tourismCacheQueries
    private val residence = "DOMESTIC"

    @BeforeTest
    fun setup() {
        db = createInMemoryDatabase()
    }

    @Test
    fun insert_then_query_returns_row() {
        queries.upsert("ES", 2020, 120_000_000L, 50_000_000L, residence, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("ES"), 2019, 2021, residence).executeAsList()
        assertEquals(1, rows.size)
        assertEquals("ES", rows[0].country_code)
        assertEquals(2020, rows[0].year)
        assertEquals(120_000_000L, rows[0].nights)
    }

    @Test
    fun query_returns_empty_when_no_match() {
        queries.upsert("ES", 2020, 120_000_000L, null, residence, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("IT"), 2019, 2021, residence).executeAsList()
        assertTrue(rows.isEmpty())
    }

    @Test
    fun upsert_replaces_existing_row_by_primary_key() {
        queries.upsert("ES", 2020, 120_000_000L, null, residence, 1000L)
        queries.upsert("ES", 2020, 130_000_000L, 55_000_000L, residence, 2000L)
        val rows = queries.queryByCountriesAndYears(listOf("ES"), 2020, 2020, residence).executeAsList()
        assertEquals(1, rows.size)
        assertEquals(130_000_000L, rows[0].nights)
        assertEquals(55_000_000L, rows[0].trips)
        assertEquals(2000L, rows[0].fetched_at_epoch_ms)
    }

    @Test
    fun oldestFetchedAt_returns_null_when_no_rows() {
        assertNull(queries.oldestFetchedAt(listOf("ES"), 2020, 2020, residence).executeAsOne().oldest)
    }

    @Test
    fun oldestFetchedAt_returns_minimum_value() {
        queries.upsert("ES", 2020, 1L, null, residence, 1500L)
        queries.upsert("ES", 2021, 1L, null, residence, 1000L)
        queries.upsert("ES", 2022, 1L, null, residence, 2000L)
        val result = queries.oldestFetchedAt(listOf("ES"), 2020, 2022, residence).executeAsOne()
        assertEquals(1000L, result.oldest)
    }

    @Test
    fun delete_removes_matching_rows_only() {
        queries.upsert("ES", 2020, 1L, null, residence, 1000L)
        queries.upsert("IT", 2020, 1L, null, residence, 1000L)
        queries.upsert("ES", 2021, 1L, null, residence, 1000L)
        queries.deleteByCountriesAndYears(listOf("ES"), 2020, 2020, residence)
        val remaining = queries.queryByCountriesAndYears(listOf("ES", "IT"), 2019, 2022, residence).executeAsList()
        assertEquals(2, remaining.size)
        assertTrue(remaining.any { it.country_code == "IT" && it.year == 2020L })
        assertTrue(remaining.any { it.country_code == "ES" && it.year == 2021L })
        assertFalse(remaining.any { it.country_code == "ES" && it.year == 2020L })
    }

    @Test
    fun query_handles_year_range_inclusively() {
        queries.upsert("ES", 2019, 1L, null, residence, 1000L)
        queries.upsert("ES", 2020, 2L, null, residence, 1000L)
        queries.upsert("ES", 2021, 3L, null, residence, 1000L)
        queries.upsert("ES", 2022, 4L, null, residence, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("ES"), 2020, 2021, residence).executeAsList()
        assertEquals(2, rows.size)
    }

    @Test
    fun query_handles_multiple_countries() {
        queries.upsert("ES", 2020, 1L, null, residence, 1000L)
        queries.upsert("IT", 2020, 2L, null, residence, 1000L)
        queries.upsert("FR", 2020, 3L, null, residence, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("ES", "IT"), 2020, 2020, residence).executeAsList()
        assertEquals(2, rows.size)
    }

    @Test
    fun query_filters_by_residence() {
        queries.upsert("ES", 2020, 120_000_000L, null, "DOMESTIC", 1000L)
        queries.upsert("ES", 2020, 80_000_000L, null, "INBOUND", 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("ES"), 2020, 2020, "DOMESTIC").executeAsList()
        assertEquals(1, rows.size)
        assertEquals("DOMESTIC", rows[0].residence)
        assertEquals(120_000_000L, rows[0].nights)
    }
}
