package eu.eurostat.core.database

import eu.eurostat.core.database.generated.AppDatabase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnvironmentCacheQueriesTest {

    private lateinit var db: AppDatabase
    private val queries get() = db.environmentCacheQueries
    private val sector = "TOTAL"

    @BeforeTest
    fun setup() {
        db = createInMemoryDatabase()
    }

    @Test
    fun insert_then_query_returns_row() {
        queries.upsert("PL", 2020, 350_000.0, 0.5, null, sector, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("PL"), 2019, 2021, sector).executeAsList()
        assertEquals(1, rows.size)
        assertEquals("PL", rows[0].country_code)
        assertEquals(2020, rows[0].year)
        assertEquals(350_000.0, rows[0].ghg_emissions)
    }

    @Test
    fun query_returns_empty_when_no_match() {
        queries.upsert("PL", 2020, 350_000.0, null, null, sector, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("DE"), 2019, 2021, sector).executeAsList()
        assertTrue(rows.isEmpty())
    }

    @Test
    fun upsert_replaces_existing_row_by_primary_key() {
        queries.upsert("PL", 2020, 350_000.0, null, null, sector, 1000L)
        queries.upsert("PL", 2020, 340_000.0, 0.6, 85.0, sector, 2000L)
        val rows = queries.queryByCountriesAndYears(listOf("PL"), 2020, 2020, sector).executeAsList()
        assertEquals(1, rows.size)
        assertEquals(340_000.0, rows[0].ghg_emissions)
        assertEquals(0.6, rows[0].energy_consumption)
        assertEquals(2000L, rows[0].fetched_at_epoch_ms)
    }

    @Test
    fun oldestFetchedAt_returns_null_when_no_rows() {
        assertNull(queries.oldestFetchedAt(listOf("PL"), 2020, 2020, sector).executeAsOne().oldest)
    }

    @Test
    fun oldestFetchedAt_returns_minimum_value() {
        queries.upsert("PL", 2020, 1.0, null, null, sector, 1500L)
        queries.upsert("PL", 2021, 1.0, null, null, sector, 1000L)
        queries.upsert("PL", 2022, 1.0, null, null, sector, 2000L)
        val result = queries.oldestFetchedAt(listOf("PL"), 2020, 2022, sector).executeAsOne()
        assertEquals(1000L, result.oldest)
    }

    @Test
    fun delete_removes_matching_rows_only() {
        queries.upsert("PL", 2020, 1.0, null, null, sector, 1000L)
        queries.upsert("DE", 2020, 1.0, null, null, sector, 1000L)
        queries.upsert("PL", 2021, 1.0, null, null, sector, 1000L)
        queries.deleteByCountriesAndYears(listOf("PL"), 2020, 2020, sector)
        val remaining = queries.queryByCountriesAndYears(listOf("PL", "DE"), 2019, 2022, sector).executeAsList()
        assertEquals(2, remaining.size)
        assertTrue(remaining.any { it.country_code == "DE" && it.year == 2020L })
        assertTrue(remaining.any { it.country_code == "PL" && it.year == 2021L })
        assertFalse(remaining.any { it.country_code == "PL" && it.year == 2020L })
    }

    @Test
    fun query_handles_year_range_inclusively() {
        queries.upsert("PL", 2019, 1.0, null, null, sector, 1000L)
        queries.upsert("PL", 2020, 2.0, null, null, sector, 1000L)
        queries.upsert("PL", 2021, 3.0, null, null, sector, 1000L)
        queries.upsert("PL", 2022, 4.0, null, null, sector, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("PL"), 2020, 2021, sector).executeAsList()
        assertEquals(2, rows.size)
    }

    @Test
    fun query_handles_multiple_countries() {
        queries.upsert("PL", 2020, 1.0, null, null, sector, 1000L)
        queries.upsert("DE", 2020, 2.0, null, null, sector, 1000L)
        queries.upsert("FR", 2020, 3.0, null, null, sector, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("PL", "DE"), 2020, 2020, sector).executeAsList()
        assertEquals(2, rows.size)
    }

    @Test
    fun query_filters_by_sector() {
        queries.upsert("PL", 2020, 350_000.0, null, null, "TOTAL", 1000L)
        queries.upsert("PL", 2020, 100_000.0, null, null, "INDUSTRY", 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("PL"), 2020, 2020, "TOTAL").executeAsList()
        assertEquals(1, rows.size)
        assertEquals("TOTAL", rows[0].sector)
        assertEquals(350_000.0, rows[0].ghg_emissions)
    }
}
