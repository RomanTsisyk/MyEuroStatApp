package eu.eurostat.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import eu.eurostat.core.database.generated.AppDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the v1 -> v2 migration (`migrations/1.sqm`): an install whose
 * database predates the blob cache must gain `MultiDimCacheEntity` and lose
 * the replaced `TourismCacheEntity` when migrated — the drivers only run
 * `Schema.create()` on brand-new files, so migrations are the only path
 * existing users have to the new table.
 */
class SchemaMigrationTest {

    @Test
    fun schema_version_is_2() {
        assertEquals(2L, AppDatabase.Schema.version)
    }

    @Test
    fun migrate_v1_to_v2_creates_blob_table_and_drops_tourism_table() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        // Rebuild the relevant slice of the historical v1 schema by hand:
        // the residence-tall tourism table that 1.sqm drops.
        driver.execute(
            identifier = null,
            sql = """
                CREATE TABLE TourismCacheEntity (
                    country_code TEXT NOT NULL,
                    year INTEGER NOT NULL,
                    nights INTEGER NOT NULL,
                    trips INTEGER,
                    residence TEXT NOT NULL,
                    fetched_at_epoch_ms INTEGER NOT NULL,
                    PRIMARY KEY (country_code, year, residence)
                )
            """.trimIndent(),
            parameters = 0,
        )

        AppDatabase.Schema.migrate(driver, oldVersion = 1L, newVersion = 2L)

        assertTrue(tableExists(driver, "MultiDimCacheEntity"), "1.sqm must create MultiDimCacheEntity")
        assertTrue(!tableExists(driver, "TourismCacheEntity"), "1.sqm must drop TourismCacheEntity")

        // The migrated table must be usable through the generated queries.
        val db = AppDatabase(driver)
        db.multiDimCacheQueries.upsert("k", "{}", 1L)
        assertEquals("{}", db.multiDimCacheQueries.selectByKey("k").executeAsOne().data_json)
    }

    private fun tableExists(driver: JdbcSqliteDriver, name: String): Boolean =
        driver.executeQuery(
            identifier = null,
            sql = "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?",
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value(requireNotNull(cursor.getLong(0)) > 0L)
            },
            parameters = 1,
            binders = { bindString(0, name) },
        ).value
}
