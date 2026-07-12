package eu.eurostat.core.common.prefs

/**
 * User-selectable theme mode persisted by [AppPreferences].
 *
 * [SYSTEM] follows the platform dark-mode setting; [LIGHT] and [DARK] pin the
 * palette regardless of the OS preference.
 */
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    /**
     * Stable string persisted in storage. Deliberately not derived from [name]
     * so enum renames can never corrupt previously written preferences.
     */
    val storageValue: String
        get() = when (this) {
            SYSTEM -> "system"
            LIGHT -> "light"
            DARK -> "dark"
        }

    companion object {
        /**
         * Parses a persisted [storageValue] back into a [ThemePreference].
         * Absent or unrecognised values fall back to [SYSTEM].
         */
        fun fromStorage(value: String?): ThemePreference =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}
