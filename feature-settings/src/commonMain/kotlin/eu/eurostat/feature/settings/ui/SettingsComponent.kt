package eu.eurostat.feature.settings.ui

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.cache.CacheMaintenance
import eu.eurostat.core.common.prefs.AppPreferences
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Decompose component for the Settings screen: exposes the persisted
 * preferences as [SettingsUiState] and dispatches [SettingsIntent]s to
 * [AppPreferences] / [CacheMaintenance].
 */
interface SettingsComponent {
    val state: StateFlow<SettingsUiState>
    fun onIntent(intent: SettingsIntent)
}

class DefaultSettingsComponent(
    componentContext: ComponentContext,
    private val preferences: AppPreferences,
    private val cacheMaintenance: CacheMaintenance,
    private val dispatchers: DispatcherProvider,
    private val appVersion: String = APP_VERSION,
) : SettingsComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(SupervisorJob() + dispatchers.main)

    /** Local, non-persisted cache-wipe progress merged into the state. */
    private val isClearingCache = MutableStateFlow(false)
    private val cacheCleared = MutableStateFlow(false)

    private val _state = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    override val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        scope.launch {
            combine(
                preferences.themePreference,
                preferences.language,
                preferences.defaultCountry,
                isClearingCache,
                cacheCleared,
            ) { theme, language, country, clearing, cleared ->
                SettingsUiState.Content(
                    themePreference = theme,
                    language = language,
                    defaultCountry = country,
                    appVersion = appVersion,
                    isClearingCache = clearing,
                    cacheCleared = cleared,
                )
            }.collect { _state.value = it }
        }
    }

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetTheme -> scope.launch {
                preferences.setThemePreference(intent.theme)
            }
            is SettingsIntent.SetLanguage -> scope.launch {
                preferences.setLanguage(intent.language)
            }
            is SettingsIntent.SetDefaultCountry -> scope.launch {
                preferences.setDefaultCountry(intent.countryCode)
            }
            SettingsIntent.ClearCache -> clearCache()
        }
    }

    private fun clearCache() {
        if (isClearingCache.value) return
        scope.launch {
            isClearingCache.value = true
            cacheCleared.value = false
            val succeeded = runCatching { cacheMaintenance.clearAllCaches() }.isSuccess
            isClearingCache.value = false
            cacheCleared.value = succeeded
        }
    }

    companion object {
        /** Display version shown on the About row; matches composeApp versionName. */
        const val APP_VERSION = "0.4.0"
    }
}
