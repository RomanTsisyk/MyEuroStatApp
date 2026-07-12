package eu.eurostat.feature.settings.ui

import eu.eurostat.core.common.prefs.ThemePreference

/** User actions on the Settings screen. */
sealed interface SettingsIntent {

    /** Persist a new theme mode. */
    data class SetTheme(val theme: ThemePreference) : SettingsIntent

    /** Persist a new UI language code (`"system"`, `"en"`, `"pl"`, `"uk"`). */
    data class SetLanguage(val language: String) : SettingsIntent

    /** Persist a new default Eurostat geo code (e.g. `"PL"`, `"EU27_2020"`). */
    data class SetDefaultCountry(val countryCode: String) : SettingsIntent

    /** Wipe every cached statistics table. Ignored while a wipe is running. */
    data object ClearCache : SettingsIntent
}
