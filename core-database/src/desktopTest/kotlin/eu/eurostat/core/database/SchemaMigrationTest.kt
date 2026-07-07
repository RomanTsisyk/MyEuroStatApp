package eu.eurostat.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import eu.eurostat.core.database.generated.AppDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the migration chain — the drivers only run `Schema.create()` on
 * brand-new files, so migrations are the only path existing users have to
 * new tables:
 *  - v1 -> v2 (`migrations/1.sqm`): an install predating the blob cache must
 *    gain `MultiDimCacheEntity` and lose the replaced `TourismCacheEntity`.
 *  - v2 -> v3 (`migrations/2.sqm`): an install predating Settings persistence
 *    must gain the `PreferenceEntity` key/value table.
 */
class SchemaMigrationTest {

    @Test
    fun schema_version_is_3() {
        assertEquals(3L, AppDatabase.Schema.version)
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

    @Test
    fun migrate_v2_to_v3_creates_preferences_table() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        AppDatabase.Schema.migrate(driver, oldVersion = 2L, newVersion = 3L)

        assertTrue(tableExists(driver, "PreferenceEntity"), "2.sqm must create PreferenceEntity")

        // The migrated table must be usable through the generated queries.
        val db = AppDatabase(driver)
        db.preferencesQueries.upsert("theme", "dark")
        assertEquals("dark", db.preferencesQueries.selectByKey("theme").executeAsOne())
    }

    @Test
    fun migrate_v1_to_v3_runs_the_full_chain() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        AppDatabase.Schema.migrate(driver, oldVersion = 1L, newVersion = 3L)

        assertTrue(tableExists(driver, "MultiDimCacheEntity"), "chain must apply 1.sqm")
        assertTrue(tableExists(driver, "PreferenceEntity"), "chain must apply 2.sqm")
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
