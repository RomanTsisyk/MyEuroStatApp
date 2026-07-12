package eu.eurostat.core.database

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlDelightCacheMaintenanceTest {

    @Test
    fun clearAllCaches_empties_every_cache_table_but_keeps_preferences() = runTest {
        val db = createInMemoryDatabase()

        // Seed a representative subset of the cache tables plus the blob store.
        db.populationCacheQueries.upsert("PL", 2020L, 38_000_000L, null, null, 1L)
        db.economyCacheQueries.upsert("PL", 2020L, 500_000L, null, null, "CP_MEUR", 1L)
        db.multiDimCacheQueries.upsert("population:cohorts:v1:PL", "{}", 1L)
        // Preferences are user data — they must survive a cache clear.
        db.preferencesQueries.upsert("theme", "dark")

        val maintenance = SqlDelightCacheMaintenance(
            db,
            TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
        )
        maintenance.clearAllCaches()

        assertTrue(
            db.populationCacheQueries
                .queryByCountriesAndYears(listOf("PL"), 2000L, 2030L)
                .executeAsList()
                .isEmpty(),
            "population cache must be empty after clearAllCaches",
        )
        assertTrue(
            db.economyCacheQueries
                .queryByCountriesAndYears(listOf("PL"), 2000L, 2030L, "CP_MEUR")
                .executeAsList()
                .isEmpty(),
            "economy cache must be empty after clearAllCaches",
        )
        assertTrue(
            db.multiDimCacheQueries.selectByKey("population:cohorts:v1:PL").executeAsOneOrNull() == null,
            "blob cache must be empty after clearAllCaches",
        )
        assertEquals(
            "dark",
            db.preferencesQueries.selectByKey("theme").executeAsOne(),
            "preferences must not be touched by clearAllCaches",
        )
    }
}
