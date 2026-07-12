package eu.eurostat.feature.settings.ui

import eu.eurostat.core.common.prefs.ThemePreference

/**
 * Sealed UI state for the Settings screen.
 *
 * - [Loading] the persisted preferences have not been read yet (first frame only).
 * - [Content] preferences loaded and editable.
 *
 * There are no Empty/Error variants: preference reads never fail and always
 * yield defaults, so the state machine is Loading -> Content and stays there.
 */
sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    /**
     * @property themePreference the persisted theme mode.
     * @property language persisted UI language code (`"system"`, `"en"`, `"pl"`, `"uk"`).
     * @property defaultCountry persisted Eurostat geo code (e.g. `"EU27_2020"`).
     * @property appVersion display version for the About row (e.g. `"0.4.0"`).
     * @property isClearingCache true while a cache wipe is in flight; guards re-entry.
     * @property cacheCleared true after the last cache wipe finished successfully;
     *   reset when a new wipe starts.
     */
    data class Content(
        val themePreference: ThemePreference,
        val language: String,
        val defaultCountry: String,
        val appVersion: String,
        val isClearingCache: Boolean = false,
        val cacheCleared: Boolean = false,
    ) : SettingsUiState
}
