package eu.eurostat.core.database

import eu.eurostat.core.database.generated.AppDatabase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PopulationCacheQueriesTest {

    private lateinit var db: AppDatabase
    private val queries get() = db.populationCacheQueries

    @BeforeTest
    fun setup() {
        db = createInMemoryDatabase()
    }

    @Test
    fun insert_then_query_returns_row() {
        queries.upsert("PL", 2020, 38_000_000L, 18_000_000L, 20_000_000L, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("PL"), 2019, 2021).executeAsList()
        assertEquals(1, rows.size)
        assertEquals("PL", rows[0].country_code)
        assertEquals(2020, rows[0].year)
        assertEquals(38_000_000L, rows[0].total_population)
    }

    @Test
    fun query_returns_empty_when_no_match() {
        queries.upsert("PL", 2020, 38_000_000L, null, null, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("DE"), 2019, 2021).executeAsList()
        assertTrue(rows.isEmpty())
    }

    @Test
    fun upsert_replaces_existing_row_by_primary_key() {
        queries.upsert("PL", 2020, 38_000_000L, null, null, 1000L)
        queries.upsert("PL", 2020, 37_900_000L, 18_000_000L, 19_900_000L, 2000L)
        val rows = queries.queryByCountriesAndYears(listOf("PL"), 2020, 2020).executeAsList()
        assertEquals(1, rows.size)
        assertEquals(37_900_000L, rows[0].total_population)
        assertEquals(18_000_000L, rows[0].male_population)
        assertEquals(2000L, rows[0].fetched_at_epoch_ms)
    }

    @Test
    fun oldestFetchedAt_returns_null_when_no_rows() {
        assertNull(queries.oldestFetchedAt(listOf("PL"), 2020, 2020).executeAsOne().oldest)
    }

    @Test
    fun oldestFetchedAt_returns_minimum_value() {
        queries.upsert("PL", 2020, 1L, null, null, 1500L)
        queries.upsert("PL", 2021, 1L, null, null, 1000L)
        queries.upsert("PL", 2022, 1L, null, null, 2000L)
        val result = queries.oldestFetchedAt(listOf("PL"), 2020, 2022).executeAsOne()
        assertEquals(1000L, result.oldest)
    }

    @Test
    fun delete_removes_matching_rows_only() {
        queries.upsert("PL", 2020, 1L, null, null, 1000L)
        queries.upsert("DE", 2020, 1L, null, null, 1000L)
        queries.upsert("PL", 2021, 1L, null, null, 1000L)
        queries.deleteByCountriesAndYears(listOf("PL"), 2020, 2020)
        val remaining = queries.queryByCountriesAndYears(listOf("PL", "DE"), 2019, 2022).executeAsList()
        assertEquals(2, remaining.size)
        assertTrue(remaining.any { it.country_code == "DE" && it.year == 2020L })
        assertTrue(remaining.any { it.country_code == "PL" && it.year == 2021L })
        assertFalse(remaining.any { it.country_code == "PL" && it.year == 2020L })
    }

    @Test
    fun query_handles_year_range_inclusively() {
        queries.upsert("PL", 2019, 1L, null, null, 1000L)
        queries.upsert("PL", 2020, 2L, null, null, 1000L)
        queries.upsert("PL", 2021, 3L, null, null, 1000L)
        queries.upsert("PL", 2022, 4L, null, null, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("PL"), 2020, 2021).executeAsList()
        assertEquals(2, rows.size)
    }

    @Test
    fun query_handles_multiple_countries() {
        queries.upsert("PL", 2020, 1L, null, null, 1000L)
        queries.upsert("DE", 2020, 2L, null, null, 1000L)
        queries.upsert("FR", 2020, 3L, null, null, 1000L)
        val rows = queries.queryByCountriesAndYears(listOf("PL", "DE"), 2020, 2020).executeAsList()
        assertEquals(2, rows.size)
    }

    @Test
    fun upsert_with_null_male_and_female_persists_nulls() {
        queries.upsert("PL", 2020, 38_000_000L, null, null, 1000L)
        val row = queries.queryByCountriesAndYears(listOf("PL"), 2020, 2020).executeAsOne()
        assertNull(row.male_population)
        assertNull(row.female_population)
    }
}
