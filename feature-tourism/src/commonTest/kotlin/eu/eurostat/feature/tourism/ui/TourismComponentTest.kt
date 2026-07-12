package eu.eurostat.feature.tourism.ui

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import eu.eurostat.core.common.AppError
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.common.prefs.AppPreferences
import eu.eurostat.core.common.prefs.ThemePreference
import eu.eurostat.feature.tourism.domain.GetTourismTimeSeriesUseCase
import eu.eurostat.feature.tourism.domain.TourismData
import eu.eurostat.feature.tourism.domain.TourismDataPoint
import eu.eurostat.feature.tourism.domain.TourismQuery
import eu.eurostat.feature.tourism.domain.TourismRepository
import eu.eurostat.feature.tourism.domain.TourismResidence
import eu.eurostat.feature.tourism.domain.TourismTimeSeries
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

private class FakeTourismRepository : TourismRepository {
    var lastQuery: TourismQuery? = null
    var refreshCallCount = 0
    var refreshThrows: Throwable? = null

    val emissions = MutableStateFlow<Result<TourismData>>(Result.Loading)

    override fun observe(query: TourismQuery): Flow<Result<TourismData>> {
        lastQuery = query
        return emissions.asStateFlow()
    }

    override suspend fun refresh(query: TourismQuery) {
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

private fun sampleTourismSeries(country: String = "DE"): TourismTimeSeries =
    TourismTimeSeries(
        countryCode = country,
        countryName = country,
        points = listOf(
            TourismDataPoint(
                countryCode = country,
                year = 2018,
                domesticNights = 55_000_000L,
                foreignNights = 20_000_000L,
                totalNights = 75_000_000L,
                trips = 16_000_000L,
            ),
            TourismDataPoint(
                countryCode = country,
                year = 2019,
                domesticNights = 60_000_000L,
                foreignNights = 23_000_000L,
                totalNights = 83_000_000L,
                trips = 18_000_000L,
            ),
        )
    )

private fun sampleData(vararg countries: String = arrayOf("DE")): TourismData =
    TourismData(
        timeSeries = countries.map { sampleTourismSeries(it) },
        heatmapCells = emptyList(),
    )

@OptIn(ExperimentalCoroutinesApi::class)
class TourismComponentTest {

    private val lifecycle = LifecycleRegistry()
    private val context = DefaultComponentContext(lifecycle)

    @BeforeTest fun resume() { lifecycle.resume() }
    @AfterTest fun destroy() { lifecycle.destroy() }

    private fun buildComponent(
        repo: FakeTourismRepository,
        dispatcher: kotlinx.coroutines.test.TestDispatcher,
        preferences: AppPreferences = FakeAppPreferences(),
    ): DefaultTourismComponent {
        val useCase = GetTourismTimeSeriesUseCase(repo)
        val dispatchers = TestDispatcherProviderLocal(dispatcher)
        return DefaultTourismComponent(context, useCase, dispatchers, preferences)
    }

    @Test
    fun stored_default_country_seeds_active_country_and_first_query() = runTest {
        val repo = FakeTourismRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher, FakeAppPreferences(defaultCountry = "IT"))

        repo.emissions.value = Result.Success(sampleData("IT", "DE"), isStale = false)
        testScheduler.advanceUntilIdle()

        // "IT" is already part of the default tourism list, so the query is unchanged.
        assertEquals(listOf("EU27_2020", "DE", "FR", "PL", "IT", "ES"), repo.lastQuery?.countryCodes)

        val state = component.state.value
        assertIs<TourismUiState.Content>(state)
        assertEquals("IT", state.activeCountry)
    }

    @Test
    fun init_emits_loading_immediately() = runTest {
        val repo = FakeTourismRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        assertEquals(TourismUiState.Loading, component.state.value)
    }

    @Test
    fun emits_content_when_repo_emits_success_with_data() = runTest {
        val repo = FakeTourismRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(sampleData("DE"), isStale = false)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<TourismUiState.Content>(state)
        assertEquals(1, state.timeSeries.size)
        assertFalse(state.isStale)
    }

    @Test
    fun emits_empty_when_repo_emits_success_with_empty_list() = runTest {
        val repo = FakeTourismRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(TourismData(emptyList(), emptyList()), isStale = false)
        testScheduler.advanceUntilIdle()

        assertEquals(TourismUiState.Empty, component.state.value)
    }

    @Test
    fun emits_error_with_canRetry_when_repo_emits_error() = runTest {
        val repo = FakeTourismRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Error(AppError.NoNetwork)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<TourismUiState.Error>(state)
        assertTrue(state.canRetry)
    }

    @Test
    fun stale_content_emits_isStale_true() = runTest {
        val repo = FakeTourismRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(sampleData("DE"), isStale = true)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<TourismUiState.Content>(state)
        assertTrue(state.isStale)
    }

    @Test
    fun on_select_countries_updates_query_and_reloads() = runTest {
        val repo = FakeTourismRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(TourismIntent.SelectCountries(listOf("ES", "GR")))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("ES", "GR"), repo.lastQuery?.countryCodes)
    }

    @Test
    fun on_change_year_range_updates_query_and_reloads() = runTest {
        val repo = FakeTourismRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(TourismIntent.ChangeYearRange(2016..2023))
        testScheduler.advanceUntilIdle()

        assertEquals(2016..2023, repo.lastQuery?.yearRange)
    }

    @Test
    fun highlight_residence_updates_state_without_query_change() = runTest {
        val repo = FakeTourismRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(sampleData("DE"), isStale = false)
        testScheduler.advanceUntilIdle()
        val queryBefore = repo.lastQuery

        component.onIntent(TourismIntent.HighlightResidence(TourismResidence.Domestic))
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<TourismUiState.Content>(state)
        assertEquals(TourismResidence.Domestic, state.highlightedResidence)
        assertEquals(queryBefore, repo.lastQuery)
    }

    @Test
    fun select_active_country_updates_state_without_query_change() = runTest {
        val repo = FakeTourismRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(sampleData("DE", "ES"), isStale = false)
        testScheduler.advanceUntilIdle()

        component.onIntent(TourismIntent.SelectActiveCountry("ES"))
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<TourismUiState.Content>(state)
        assertEquals("ES", state.activeCountry)
    }

    @Test
    fun on_refresh_calls_repository_refresh() = runTest {
        val repo = FakeTourismRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(TourismIntent.Refresh)
        testScheduler.advanceUntilIdle()

        assertEquals(1, repo.refreshCallCount)
    }

    @Test
    fun on_retry_triggers_reload() = runTest {
        val repo = FakeTourismRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        repo.emissions.value = Result.Success(sampleData("DE"), isStale = false)
        component.onIntent(TourismIntent.Retry)
        testScheduler.advanceUntilIdle()

        assertIs<TourismUiState.Content>(component.state.value)
    }

    @Test
    fun select_year_updates_selected_year_without_api_call() = runTest {
        val repo = FakeTourismRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(sampleData("DE"), isStale = false)
        testScheduler.advanceUntilIdle()

        val observeCallsBefore = repo.lastQuery
        // Default state should show the latest year (2019).
        val stateBefore = assertIs<TourismUiState.Content>(component.state.value)
        assertEquals(2019, stateBefore.selectedYear)
        assertEquals(listOf(2018, 2019), stateBefore.availableYears)

        // Select an earlier year.
        component.onIntent(TourismIntent.SelectYear(2018))
        testScheduler.advanceUntilIdle()

        val stateAfter = assertIs<TourismUiState.Content>(component.state.value)
        assertEquals(2018, stateAfter.selectedYear)
        // No new network query should have been issued (query object is unchanged).
        assertEquals(observeCallsBefore, repo.lastQuery)
    }
}
