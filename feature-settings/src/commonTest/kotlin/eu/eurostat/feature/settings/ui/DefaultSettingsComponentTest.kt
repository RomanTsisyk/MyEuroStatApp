package eu.eurostat.feature.settings.ui

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.cache.CacheMaintenance
import eu.eurostat.core.common.prefs.AppPreferences
import eu.eurostat.core.common.prefs.ThemePreference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// Fakes — implement the interfaces directly (no mocking framework on KMP).
// ---------------------------------------------------------------------------

private class FakeAppPreferences(
    theme: ThemePreference = ThemePreference.SYSTEM,
    language: String = AppPreferences.DEFAULT_LANGUAGE,
    defaultCountry: String = AppPreferences.DEFAULT_COUNTRY,
) : AppPreferences {
    val themeFlow = MutableStateFlow(theme)
    val languageFlow = MutableStateFlow(language)
    val countryFlow = MutableStateFlow(defaultCountry)

    override val themePreference: Flow<ThemePreference> = themeFlow
    override val language: Flow<String> = languageFlow
    override val defaultCountry: Flow<String> = countryFlow

    override suspend fun setThemePreference(value: ThemePreference) {
        themeFlow.value = value
    }

    override suspend fun setLanguage(value: String) {
        languageFlow.value = value
    }

    override suspend fun setDefaultCountry(value: String) {
        countryFlow.value = value
    }
}

private class FakeCacheMaintenance : CacheMaintenance {
    var clearCalls = 0
    var failure: Throwable? = null

    override suspend fun clearAllCaches() {
        clearCalls++
        failure?.let { throw it }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class TestDispatchers(dispatcher: TestDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSettingsComponentTest {

    private val lifecycle = LifecycleRegistry()
    private val context = DefaultComponentContext(lifecycle)

    private val preferences = FakeAppPreferences()
    private val cacheMaintenance = FakeCacheMaintenance()

    @BeforeTest
    fun resume() {
        lifecycle.resume()
    }

    @AfterTest
    fun destroy() {
        lifecycle.destroy()
    }

    private fun build(dispatcher: TestDispatcher) = DefaultSettingsComponent(
        context,
        preferences,
        cacheMaintenance,
        TestDispatchers(dispatcher),
    )

    private fun content(component: SettingsComponent): SettingsUiState.Content =
        assertIs<SettingsUiState.Content>(component.state.value)

    @Test
    fun state_reflects_persisted_preferences() = runTest {
        preferences.themeFlow.value = ThemePreference.DARK
        preferences.languageFlow.value = "pl"
        preferences.countryFlow.value = "PL"

        val component = build(StandardTestDispatcher(testScheduler))
        testScheduler.advanceUntilIdle()

        val state = content(component)
        assertEquals(ThemePreference.DARK, state.themePreference)
        assertEquals("pl", state.language)
        assertEquals("PL", state.defaultCountry)
        assertEquals(DefaultSettingsComponent.APP_VERSION, state.appVersion)
        assertFalse(state.isClearingCache)
        assertFalse(state.cacheCleared)
    }

    @Test
    fun set_theme_persists_and_updates_state() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        testScheduler.advanceUntilIdle()

        component.onIntent(SettingsIntent.SetTheme(ThemePreference.LIGHT))
        testScheduler.advanceUntilIdle()

        assertEquals(ThemePreference.LIGHT, preferences.themeFlow.value)
        assertEquals(ThemePreference.LIGHT, content(component).themePreference)
    }

    @Test
    fun set_language_persists_and_updates_state() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        testScheduler.advanceUntilIdle()

        component.onIntent(SettingsIntent.SetLanguage("uk"))
        testScheduler.advanceUntilIdle()

        assertEquals("uk", preferences.languageFlow.value)
        assertEquals("uk", content(component).language)
    }

    @Test
    fun set_default_country_persists_and_updates_state() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        testScheduler.advanceUntilIdle()

        component.onIntent(SettingsIntent.SetDefaultCountry("DE"))
        testScheduler.advanceUntilIdle()

        assertEquals("DE", preferences.countryFlow.value)
        assertEquals("DE", content(component).defaultCountry)
    }

    @Test
    fun clear_cache_invokes_maintenance_and_reports_completion() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        testScheduler.advanceUntilIdle()

        component.onIntent(SettingsIntent.ClearCache)
        testScheduler.advanceUntilIdle()

        assertEquals(1, cacheMaintenance.clearCalls)
        val state = content(component)
        assertFalse(state.isClearingCache)
        assertTrue(state.cacheCleared)
    }

    @Test
    fun clear_cache_failure_resets_the_flag_without_claiming_success() = runTest {
        cacheMaintenance.failure = IllegalStateException("disk full")
        val component = build(StandardTestDispatcher(testScheduler))
        testScheduler.advanceUntilIdle()

        component.onIntent(SettingsIntent.ClearCache)
        testScheduler.advanceUntilIdle()

        assertEquals(1, cacheMaintenance.clearCalls)
        val state = content(component)
        assertFalse(state.isClearingCache)
        assertFalse(state.cacheCleared)
    }

    @Test
    fun fake_cache_maintenance_actually_throws_when_configured() = runTest {
        // Guards the fake itself so the failure test above cannot silently pass.
        cacheMaintenance.failure = IllegalStateException("disk full")
        assertFailsWith<IllegalStateException> { cacheMaintenance.clearAllCaches() }
    }
}
