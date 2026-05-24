package eu.eurostat.core.database

import eu.eurostat.core.database.generated.AppDatabase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SocialCacheQueriesTest {

    private lateinit var db: AppDatabase
    private val queries get() = db.socialCacheQueries

    @BeforeTest
    fun setup() {
        db = createInMemoryDatabase()
    }

    @Test
    fun insert_then_query_returns_row() {
        queries.upsert("FR", 2020, 14.5, 21.1, 7.2, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("FR"), 2019, 2021).executeAsList()
        assertEquals(1, rows.size)
        assertEquals("FR", rows[0].country_code)
        assertEquals(2020, rows[0].year)
        assertEquals(14.5, rows[0].poverty_rate)
    }

    @Test
    fun query_returns_empty_when_no_match() {
        queries.upsert("FR", 2020, 14.5, null, null, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("ES"), 2019, 2021).executeAsList()
        assertTrue(rows.isEmpty())
    }

    @Test
    fun upsert_replaces_existing_row_by_primary_key() {
        queries.upsert("FR", 2020, 14.5, null, null, 1000L)
        queries.upsert("FR", 2020, 13.8, 20.5, 7.8, 2000L)
        val rows = queries.queryByCountriesAndYears(listOf("FR"), 2020, 2020).executeAsList()
        assertEquals(1, rows.size)
        assertEquals(13.8, rows[0].poverty_rate)
        assertEquals(20.5, rows[0].at_risk_rate)
        assertEquals(2000L, rows[0].fetched_at_epoch_ms)
    }

    @Test
    fun oldestFetchedAt_returns_null_when_no_rows() {
        assertNull(queries.oldestFetchedAt(listOf("FR"), 2020, 2020).executeAsOne().oldest)
    }

    @Test
    fun oldestFetchedAt_returns_minimum_value() {
        queries.upsert("FR", 2020, 1.0, null, null, 1500L)
        queries.upsert("FR", 2021, 1.0, null, null, 1000L)
        queries.upsert("FR", 2022, 1.0, null, null, 2000L)
        val result = queries.oldestFetchedAt(listOf("FR"), 2020, 2022).executeAsOne()
        assertEquals(1000L, result.oldest)
    }

    @Test
    fun delete_removes_matching_rows_only() {
        queries.upsert("FR", 2020, 1.0, null, null, 1000L)
        queries.upsert("ES", 2020, 1.0, null, null, 1000L)
        queries.upsert("FR", 2021, 1.0, null, null, 1000L)
        queries.deleteByCountriesAndYears(listOf("FR"), 2020, 2020)
        val remaining = queries.queryByCountriesAndYears(listOf("FR", "ES"), 2019, 2022).executeAsList()
        assertEquals(2, remaining.size)
        assertTrue(remaining.any { it.country_code == "ES" && it.year == 2020L })
        assertTrue(remaining.any { it.country_code == "FR" && it.year == 2021L })
        assertFalse(remaining.any { it.country_code == "FR" && it.year == 2020L })
    }

    @Test
    fun query_handles_year_range_inclusively() {
        queries.upsert("FR", 2019, 1.0, null, null, 1000L)
        queries.upsert("FR", 2020, 2.0, null, null, 1000L)
        queries.upsert("FR", 2021, 3.0, null, null, 1000L)
        queries.upsert("FR", 2022, 4.0, null, null, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("FR"), 2020, 2021).executeAsList()
        assertEquals(2, rows.size)
    }

    @Test
    fun query_handles_multiple_countries() {
        queries.upsert("FR", 2020, 1.0, null, null, 1000L)
        queries.upsert("ES", 2020, 2.0, null, null, 1000L)
        queries.upsert("IT", 2020, 3.0, null, null, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("FR", "ES"), 2020, 2020).executeAsList()
        assertEquals(2, rows.size)
    }

    @Test
    fun upsert_with_all_nullable_columns_null_persists_nulls() {
        queries.upsert("FR", 2020, null, null, null, 1000L)
        val row = queries.queryByCountriesAndYears(listOf("FR"), 2020, 2020).executeAsOne()
        assertNull(row.poverty_rate)
        assertNull(row.at_risk_rate)
        assertNull(row.health_satisfaction)
    }
}
