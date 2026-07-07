package eu.eurostat.core.database

import app.cash.turbine.test
import eu.eurostat.core.common.prefs.AppPreferences
import eu.eurostat.core.common.prefs.ThemePreference
import eu.eurostat.core.database.generated.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SqlDelightAppPreferencesTest {

    private fun prefs(db: AppDatabase, scheduler: kotlinx.coroutines.test.TestCoroutineScheduler) =
        SqlDelightAppPreferences(db, TestDispatcherProvider(StandardTestDispatcher(scheduler)))

    @Test
    fun unset_keys_emit_the_documented_defaults() = runTest {
        val prefs = prefs(createInMemoryDatabase(), testScheduler)

        assertEquals(ThemePreference.SYSTEM, prefs.themePreference.first())
        assertEquals(AppPreferences.DEFAULT_LANGUAGE, prefs.language.first())
        assertEquals(AppPreferences.DEFAULT_COUNTRY, prefs.defaultCountry.first())
    }

    @Test
    fun setters_persist_values_visible_to_a_fresh_store_over_the_same_database() = runTest {
        val db = createInMemoryDatabase()
        val writer = prefs(db, testScheduler)

        writer.setThemePreference(ThemePreference.DARK)
        writer.setLanguage("pl")
        writer.setDefaultCountry("PL")

        val reader = prefs(db, testScheduler)
        assertEquals(ThemePreference.DARK, reader.themePreference.first())
        assertEquals("pl", reader.language.first())
        assertEquals("PL", reader.defaultCountry.first())
    }

    @Test
    fun theme_flow_re_emits_when_the_preference_is_written() = runTest {
        val db = createInMemoryDatabase()
        val prefs = prefs(db, testScheduler)

        prefs.themePreference.test {
            assertEquals(ThemePreference.SYSTEM, awaitItem())

            prefs.setThemePreference(ThemePreference.DARK)
            assertEquals(ThemePreference.DARK, awaitItem())

            prefs.setThemePreference(ThemePreference.LIGHT)
            assertEquals(ThemePreference.LIGHT, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun corrupt_theme_value_falls_back_to_system() = runTest {
        val db = createInMemoryDatabase()
        db.preferencesQueries.upsert("theme", "solarized")

        val prefs = prefs(db, testScheduler)
        assertEquals(ThemePreference.SYSTEM, prefs.themePreference.first())
    }
}
