package eu.eurostat.core.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.prefs.AppPreferences
import eu.eurostat.core.common.prefs.ThemePreference
import eu.eurostat.core.database.generated.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * SQLDelight-backed [AppPreferences] persisting one row per key in the
 * `PreferenceEntity` table (see `Preferences.sq`).
 *
 * Reads are driver-notified flows: `asFlow()` re-runs the query whenever a
 * write through the same [AppDatabase] touches the table, so every observer
 * (Settings screen, theme root, …) sees changes immediately without polling.
 * Absent rows map to the defaults documented on [AppPreferences].
 *
 * Bound as the [AppPreferences] singleton in
 * [eu.eurostat.core.database.di.coreDatabaseModule].
 */
class SqlDelightAppPreferences(
    private val db: AppDatabase,
    private val dispatchers: DispatcherProvider,
) : AppPreferences {

    override val themePreference: Flow<ThemePreference> =
        rawValue(KEY_THEME).map { ThemePreference.fromStorage(it) }

    override val language: Flow<String> =
        rawValue(KEY_LANGUAGE).map { it ?: AppPreferences.DEFAULT_LANGUAGE }

    override val defaultCountry: Flow<String> =
        rawValue(KEY_DEFAULT_COUNTRY).map { it ?: AppPreferences.DEFAULT_COUNTRY }

    override suspend fun setThemePreference(value: ThemePreference) =
        write(KEY_THEME, value.storageValue)

    override suspend fun setLanguage(value: String) =
        write(KEY_LANGUAGE, value)

    override suspend fun setDefaultCountry(value: String) =
        write(KEY_DEFAULT_COUNTRY, value)

    /** The stored string for [key], re-emitted on every table change; `null` when unset. */
    private fun rawValue(key: String): Flow<String?> =
        db.preferencesQueries.selectByKey(key).asFlow().mapToOneOrNull(dispatchers.io)

    private suspend fun write(key: String, value: String) = withContext(dispatchers.io) {
        db.preferencesQueries.upsert(pref_key = key, pref_value = value)
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_LANGUAGE = "language"
        const val KEY_DEFAULT_COUNTRY = "default_country"
    }
}
