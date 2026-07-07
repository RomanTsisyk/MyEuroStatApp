package eu.eurostat.feature.social.ui

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import eu.eurostat.core.common.AppError
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.common.prefs.AppPreferences
import eu.eurostat.core.common.prefs.ThemePreference
import eu.eurostat.feature.social.domain.GetSocialTimeSeriesUseCase
import eu.eurostat.feature.social.domain.SocialDataPoint
import eu.eurostat.feature.social.domain.SocialQuery
import eu.eurostat.feature.social.domain.SocialRepository
import eu.eurostat.feature.social.domain.SocialTimeSeries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// Test doubles
// ---------------------------------------------------------------------------

private class FakeSocialRepository : SocialRepository {
    var lastQuery: SocialQuery? = null
    var refreshCallCount = 0
    var refreshThrows: Throwable? = null

    val emissions = MutableStateFlow<Result<List<SocialTimeSeries>>>(Result.Loading)

    override fun observe(query: SocialQuery): Flow<Result<List<SocialTimeSeries>>> {
        lastQuery = query
        return emissions.asStateFlow()
    }

    override suspend fun refresh(query: SocialQuery) {
        refreshCallCount++
        refreshThrows?.let { throw it }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class TestDispatcherProviderLocal(dispatcher: kotlinx.coroutines.test.TestDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

/** In-memory [AppPreferences] fake; only [defaultCountry] matters to the component. */
private class FakeAppPreferences(
    defaultCountry: String = AppPreferences.DEFAULT_COUNTRY,
) : AppPreferences {
    override val themePreference: Flow<ThemePreference> = MutableStateFlow(ThemePreference.SYSTEM)
    override val language: Flow<String> = MutableStateFlow(AppPreferences.DEFAULT_LANGUAGE)
    override val defaultCountry: Flow<String> = MutableStateFlow(defaultCountry)
    override suspend fun setThemePreference(value: ThemePreference) = Unit
    override suspend fun setLanguage(value: String) = Unit
    override suspend fun setDefaultCountry(value: String) = Unit
}

// ---------------------------------------------------------------------------
// Sample data
// ---------------------------------------------------------------------------

private fun sampleSocialSeries(country: String = "PL"): SocialTimeSeries =
    SocialTimeSeries(
        countryCode = country,
        countryName = country,
        points = listOf(
            SocialDataPoint(
                countryCode = country,
                year = 2019,
                povertyRate = 17.0,
                atRiskRate = 20.0,
                healthSatisfaction = 70.0,
            ),
            SocialDataPoint(
                countryCode = country,
                year = 2020,
                povertyRate = 18.2,
                atRiskRate = 21.4,
                healthSatisfaction = 72.0,
            )
        )
    )

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

@OptIn(ExperimentalCoroutinesApi::class)
class SocialComponentTest {

    private val lifecycle = LifecycleRegistry()
    private val context = DefaultComponentContext(lifecycle)

    @BeforeTest
    fun resume() {
        lifecycle.resume()
    }

    @AfterTest
    fun destroy() {
        lifecycle.destroy()
    }

    private fun buildComponent(
        repo: FakeSocialRepository,
        dispatcher: kotlinx.coroutines.test.TestDispatcher,
        preferences: AppPreferences = FakeAppPreferences(),
    ): DefaultSocialComponent {
        val useCase = GetSocialTimeSeriesUseCase(repo)
        val dispatchers = TestDispatcherProviderLocal(dispatcher)
        return DefaultSocialComponent(context, useCase, dispatchers, preferences)
    }

    @Test
    fun stored_default_country_seeds_active_country_and_first_query() = runTest {
        val repo = FakeSocialRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher, FakeAppPreferences(defaultCountry = "IT"))

        repo.emissions.value = Result.Success(
            listOf(sampleSocialSeries("IT"), sampleSocialSeries("DE")),
            isStale = false,
        )
        testScheduler.advanceUntilIdle()

        // Preference country joins the list right after the EU aggregate.
        assertEquals(listOf("EU27_2020", "IT", "DE", "FR", "PL"), repo.lastQuery?.countryCodes)

        val state = component.state.value
        assertIs<SocialUiState.Content>(state)
        assertEquals("IT", state.activeCountry)
    }

    @Test
    fun init_emits_loading_immediately() = runTest {
        val repo = FakeSocialRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        assertEquals(SocialUiState.Loading, component.state.value)
    }

    @Test
    fun emits_content_when_repo_emits_success_with_data() = runTest {
        val repo = FakeSocialRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(sampleSocialSeries()), isStale = false)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<SocialUiState.Content>(state)
        assertEquals(1, state.series.size)
        assertFalse(state.isStale)
    }

    @Test
    fun emits_empty_when_repo_emits_success_with_empty_list() = runTest {
        val repo = FakeSocialRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(emptyList(), isStale = false)
        testScheduler.advanceUntilIdle()

        assertIs<SocialUiState.Empty>(component.state.value)
    }

    @Test
    fun emits_error_with_canRetry_when_repo_emits_error() = runTest {
        val repo = FakeSocialRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Error(AppError.NoNetwork)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<SocialUiState.Error>(state)
        assertTrue(state.canRetry)
    }

    @Test
    fun stale_content_emits_isStale_true() = runTest {
        val repo = FakeSocialRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(sampleSocialSeries()), isStale = true)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<SocialUiState.Content>(state)
        assertTrue(state.isStale)
    }

    @Test
    fun on_select_countries_updates_query_and_reloads() = runTest {
        val repo = FakeSocialRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(SocialIntent.SelectCountries(listOf("HU", "CZ")))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("HU", "CZ"), repo.lastQuery?.countryCodes)
    }

    @Test
    fun on_change_year_range_updates_query_and_reloads() = runTest {
        val repo = FakeSocialRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(SocialIntent.ChangeYearRange(2013..2022))
        testScheduler.advanceUntilIdle()

        assertEquals(2013..2022, repo.lastQuery?.yearRange)
    }

    @Test
    fun on_refresh_calls_repository_refresh() = runTest {
        val repo = FakeSocialRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(SocialIntent.Refresh)
        testScheduler.advanceUntilIdle()

        assertEquals(1, repo.refreshCallCount)
    }

    @Test
    fun on_retry_triggers_reload() = runTest {
        val repo = FakeSocialRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        repo.emissions.value = Result.Success(listOf(sampleSocialSeries()), isStale = false)
        component.onIntent(SocialIntent.Retry)
        testScheduler.advanceUntilIdle()

        assertIs<SocialUiState.Content>(component.state.value)
    }

    @Test
    fun select_year_updates_selectedYear_without_extra_api_call() = runTest {
        val repo = FakeSocialRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(sampleSocialSeries()), isStale = false)
        testScheduler.advanceUntilIdle()

        val beforeCallCount = repo.refreshCallCount
        val stateBefore = assertIs<SocialUiState.Content>(component.state.value)
        // Default should be the latest year (2020).
        assertEquals(2020, stateBefore.selectedYear)
        assertEquals(listOf(2019, 2020), stateBefore.availableYears)

        component.onIntent(SocialIntent.SelectYear(2019))
        testScheduler.advanceUntilIdle()

        val stateAfter = assertIs<SocialUiState.Content>(component.state.value)
        assertEquals(2019, stateAfter.selectedYear)
        // No network call triggered.
        assertEquals(beforeCallCount, repo.refreshCallCount)
    }
}
