package eu.eurostat.core.common.prefs

import kotlinx.coroutines.flow.Flow

/**
 * Persisted user preferences, observable as [Flow]s that re-emit whenever the
 * underlying value changes — including writes made from other screens, so the
 * theme root and the Settings screen always agree.
 *
 * Reads never fail: keys that were never written emit the documented defaults.
 * Setters are `suspend` because implementations write to disk; they may be
 * called from any dispatcher — implementations hop to IO internally.
 *
 * The production implementation is SQLDelight-backed and lives in
 * `core-database` (`SqlDelightAppPreferences`); tests use in-memory fakes.
 */
interface AppPreferences {

    /** The user's theme mode. Defaults to [ThemePreference.SYSTEM] when unset. */
    val themePreference: Flow<ThemePreference>

    /**
     * UI language code: `"system"` (follow the OS locale) or one of the bundled
     * translations (`"en"`, `"pl"`, `"uk"`). Defaults to [DEFAULT_LANGUAGE].
     */
    val language: Flow<String>

    /**
     * Eurostat geo code pre-selected across feature screens (e.g. `"PL"`,
     * `"EU27_2020"`). Defaults to [DEFAULT_COUNTRY]. Persisted and exposed
     * here; wiring feature components to it is a follow-up pass.
     */
    val defaultCountry: Flow<String>

    /** Persists the theme mode. */
    suspend fun setThemePreference(value: ThemePreference)

    /** Persists the UI language code (see [language] for accepted values). */
    suspend fun setLanguage(value: String)

    /** Persists the default Eurostat geo code (see [defaultCountry]). */
    suspend fun setDefaultCountry(value: String)

    companion object {
        /** Default [language] value: follow the OS locale. */
        const val DEFAULT_LANGUAGE: String = "system"

        /** Default [defaultCountry] value: the EU-27 aggregate. */
        const val DEFAULT_COUNTRY: String = "EU27_2020"

        /** Language codes the Settings screen offers, in display order. */
        val SUPPORTED_LANGUAGES: List<String> = listOf("system", "en", "pl", "uk")
    }
}
