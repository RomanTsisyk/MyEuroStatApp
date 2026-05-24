package eu.eurostat.core.database

import eu.eurostat.core.database.generated.AppDatabase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScienceCacheQueriesTest {

    private lateinit var db: AppDatabase
    private val queries get() = db.scienceCacheQueries

    @BeforeTest
    fun setup() {
        db = createInMemoryDatabase()
    }

    @Test
    fun insert_then_query_returns_row() {
        queries.upsert("FI", 2020, 2.8, 92.5, 45.0, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("FI"), 2019, 2021).executeAsList()
        assertEquals(1, rows.size)
        assertEquals("FI", rows[0].country_code)
        assertEquals(2020, rows[0].year)
        assertEquals(2.8, rows[0].rd_spend_pct_gdp)
    }

    @Test
    fun query_returns_empty_when_no_match() {
        queries.upsert("FI", 2020, 2.8, null, null, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("SE"), 2019, 2021).executeAsList()
        assertTrue(rows.isEmpty())
    }

    @Test
    fun upsert_replaces_existing_row_by_primary_key() {
        queries.upsert("FI", 2020, 2.8, null, null, 1000L)
        queries.upsert("FI", 2020, 3.1, 93.0, 46.5, 2000L)
        val rows = queries.queryByCountriesAndYears(listOf("FI"), 2020, 2020).executeAsList()
        assertEquals(1, rows.size)
        assertEquals(3.1, rows[0].rd_spend_pct_gdp)
        assertEquals(93.0, rows[0].internet_usage_pct)
        assertEquals(2000L, rows[0].fetched_at_epoch_ms)
    }

    @Test
    fun oldestFetchedAt_returns_null_when_no_rows() {
        assertNull(queries.oldestFetchedAt(listOf("FI"), 2020, 2020).executeAsOne().oldest)
    }

    @Test
    fun oldestFetchedAt_returns_minimum_value() {
        queries.upsert("FI", 2020, 1.0, null, null, 1500L)
        queries.upsert("FI", 2021, 1.0, null, null, 1000L)
        queries.upsert("FI", 2022, 1.0, null, null, 2000L)
        val result = queries.oldestFetchedAt(listOf("FI"), 2020, 2022).executeAsOne()
        assertEquals(1000L, result.oldest)
    }

    @Test
    fun delete_removes_matching_rows_only() {
        queries.upsert("FI", 2020, 1.0, null, null, 1000L)
        queries.upsert("SE", 2020, 1.0, null, null, 1000L)
        queries.upsert("FI", 2021, 1.0, null, null, 1000L)
        queries.deleteByCountriesAndYears(listOf("FI"), 2020, 2020)
        val remaining = queries.queryByCountriesAndYears(listOf("FI", "SE"), 2019, 2022).executeAsList()
        assertEquals(2, remaining.size)
        assertTrue(remaining.any { it.country_code == "SE" && it.year == 2020L })
        assertTrue(remaining.any { it.country_code == "FI" && it.year == 2021L })
        assertFalse(remaining.any { it.country_code == "FI" && it.year == 2020L })
    }

    @Test
    fun query_handles_year_range_inclusively() {
        queries.upsert("FI", 2019, 1.0, null, null, 1000L)
        queries.upsert("FI", 2020, 2.0, null, null, 1000L)
        queries.upsert("FI", 2021, 3.0, null, null, 1000L)
        queries.upsert("FI", 2022, 4.0, null, null, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("FI"), 2020, 2021).executeAsList()
        assertEquals(2, rows.size)
    }

    @Test
    fun query_handles_multiple_countries() {
        queries.upsert("FI", 2020, 1.0, null, null, 1000L)
        queries.upsert("SE", 2020, 2.0, null, null, 1000L)
        queries.upsert("DK", 2020, 3.0, null, null, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("FI", "SE"), 2020, 2020).executeAsList()
        assertEquals(2, rows.size)
    }

    @Test
    fun upsert_with_all_nullable_columns_null_persists_nulls() {
        queries.upsert("FI", 2020, null, null, null, 1000L)
        val row = queries.queryByCountriesAndYears(listOf("FI"), 2020, 2020).executeAsOne()
        assertNull(row.rd_spend_pct_gdp)
        assertNull(row.internet_usage_pct)
        assertNull(row.tertiary_educ_pct)
    }
}
