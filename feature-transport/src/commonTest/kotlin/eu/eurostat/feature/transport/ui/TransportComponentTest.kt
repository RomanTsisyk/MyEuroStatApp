package eu.eurostat.feature.transport.ui

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import eu.eurostat.core.common.AppError
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.common.prefs.AppPreferences
import eu.eurostat.core.common.prefs.ThemePreference
import eu.eurostat.feature.transport.domain.GetTransportTimeSeriesUseCase
import eu.eurostat.feature.transport.domain.TransportDataPoint
import eu.eurostat.feature.transport.domain.TransportMode
import eu.eurostat.feature.transport.domain.TransportQuery
import eu.eurostat.feature.transport.domain.TransportRepository
import eu.eurostat.feature.transport.domain.TransportTimeSeries
import kotlinx.coroutines.CancellationException
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

private class FakeTransportRepository : TransportRepository {
    var lastQuery: TransportQuery? = null
    var observeCallCount = 0
    var refreshCallCount = 0
    var refreshThrows: Throwable? = null

    val emissions = MutableStateFlow<Result<List<TransportTimeSeries>>>(Result.Loading)

    override fun observe(query: TransportQuery): Flow<Result<List<TransportTimeSeries>>> {
        lastQuery = query
        observeCallCount++
        return emissions.asStateFlow()
    }

    override suspend fun refresh(query: TransportQuery) {
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

private fun sampleTransportSeries(
    country: String = "DE",
    mode: TransportMode = TransportMode.ALL,
): TransportTimeSeries =
    TransportTimeSeries(
        countryCode = country,
        countryName = country,
        mode = mode,
        points = listOf(
            TransportDataPoint(
                countryCode = country,
                year = 2020,
                mode = mode,
                roadPassengers = 800_000L,
                airPassengers = 150_000L,
                seaPassengers = 50_000L,
            )
        )
    )

/** Series where road ends at 2022 and air continues to 2024. */
private fun gapTransportSeries(country: String = "DE"): TransportTimeSeries =
    TransportTimeSeries(
        countryCode = country,
        countryName = country,
        mode = TransportMode.ALL,
        points = listOf(
            TransportDataPoint(country, 2021, TransportMode.ALL, roadPassengers = 500_000L, airPassengers = 100_000L, seaPassengers = null),
            TransportDataPoint(country, 2022, TransportMode.ALL, roadPassengers = 520_000L, airPassengers = null, seaPassengers = null),
            TransportDataPoint(country, 2023, TransportMode.ALL, roadPassengers = null, airPassengers = 110_000L, seaPassengers = null),
            TransportDataPoint(country, 2024, TransportMode.ALL, roadPassengers = null, airPassengers = 120_000L, seaPassengers = null),
        )
    )

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

@OptIn(ExperimentalCoroutinesApi::class)
class TransportComponentTest {

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
        repo: FakeTransportRepository,
        dispatcher: kotlinx.coroutines.test.TestDispatcher,
        preferences: AppPreferences = FakeAppPreferences(),
    ): DefaultTransportComponent {
        val useCase = GetTransportTimeSeriesUseCase(repo)
        val dispatchers = TestDispatcherProviderLocal(dispatcher)
        return DefaultTransportComponent(context, useCase, dispatchers, preferences)
    }

    @Test
    fun stored_default_country_seeds_active_country_and_first_query() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher, FakeAppPreferences(defaultCountry = "IT"))

        repo.emissions.value = Result.Success(
            listOf(sampleTransportSeries("IT"), sampleTransportSeries("DE")),
            isStale = false,
        )
        testScheduler.advanceUntilIdle()

        // Preference country joins the list right after the EU aggregate.
        assertEquals(listOf("EU27_2020", "IT", "DE", "FR", "PL"), repo.lastQuery?.countryCodes)

        val state = component.state.value
        assertIs<TransportUiState.Content>(state)
        assertEquals("IT", state.activeCountry)
    }

    @Test
    fun init_emits_loading_immediately() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        assertEquals(TransportUiState.Loading, component.state.value)
    }

    @Test
    fun emits_content_when_repo_emits_success_with_data() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(sampleTransportSeries()), isStale = false)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<TransportUiState.Content>(state)
        assertEquals(1, state.series.size)
        assertFalse(state.isStale)
    }

    @Test
    fun emits_empty_when_repo_emits_success_with_empty_list() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(emptyList(), isStale = false)
        testScheduler.advanceUntilIdle()

        assertIs<TransportUiState.Empty>(component.state.value)
    }

    @Test
    fun emits_error_with_canRetry_when_repo_emits_error() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Error(AppError.NoNetwork)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<TransportUiState.Error>(state)
        assertTrue(state.canRetry)
        assertEquals(AppError.NoNetwork, state.error)
    }

    @Test
    fun stale_content_emits_isStale_true() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(sampleTransportSeries()), isStale = true)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<TransportUiState.Content>(state)
        assertTrue(state.isStale)
    }

    @Test
    fun on_select_countries_updates_query_and_reloads() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(TransportIntent.SelectCountries(listOf("FR", "IT")))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("FR", "IT"), repo.lastQuery?.countryCodes)
    }

    @Test
    fun on_change_year_range_updates_query_and_reloads() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(TransportIntent.ChangeYearRange(2014..2021))
        testScheduler.advanceUntilIdle()

        assertEquals(2014..2021, repo.lastQuery?.yearRange)
    }

    @Test
    fun on_change_mode_updates_query_and_reloads() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(TransportIntent.ChangeMode(TransportMode.AIR))
        testScheduler.advanceUntilIdle()

        assertEquals(TransportMode.AIR, repo.lastQuery?.mode)
    }

    @Test
    fun on_refresh_calls_repository_refresh() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(TransportIntent.Refresh)
        testScheduler.advanceUntilIdle()

        assertEquals(1, repo.refreshCallCount)
    }

    @Test
    fun on_retry_triggers_reload() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        repo.emissions.value = Result.Success(listOf(sampleTransportSeries()), isStale = false)
        component.onIntent(TransportIntent.Retry)
        testScheduler.advanceUntilIdle()

        assertIs<TransportUiState.Content>(component.state.value)
    }

    @Test
    fun on_select_panel_mode_updates_display_panel_mode_in_content() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(sampleTransportSeries()), isStale = false)
        testScheduler.advanceUntilIdle()

        component.onIntent(TransportIntent.SelectPanelMode(TransportPanelMode.ROAD))

        val state = component.state.value
        assertIs<TransportUiState.Content>(state)
        assertEquals(TransportPanelMode.ROAD, state.displayPanelMode)
    }

    @Test
    fun on_toggle_log_scale_flips_flag_in_content() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(sampleTransportSeries()), isStale = false)
        testScheduler.advanceUntilIdle()

        assertFalse((component.state.value as TransportUiState.Content).logScale)

        component.onIntent(TransportIntent.ToggleLogScale)

        assertTrue((component.state.value as TransportUiState.Content).logScale)

        component.onIntent(TransportIntent.ToggleLogScale)

        assertFalse((component.state.value as TransportUiState.Content).logScale)
    }

    @Test
    fun available_years_is_union_of_road_and_air_years_for_active_country() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(gapTransportSeries("DE")), isStale = false)
        testScheduler.advanceUntilIdle()

        val state = component.state.value as TransportUiState.Content
        // 2021 (road+air), 2022 (road only), 2023 (air only), 2024 (air only)
        assertEquals(listOf(2021, 2022, 2023, 2024), state.availableYears)
    }

    @Test
    fun default_year_is_latest_road_year_when_air_data_runs_further() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(gapTransportSeries("DE")), isStale = false)
        testScheduler.advanceUntilIdle()

        val state = component.state.value as TransportUiState.Content
        // Air reaches 2024 but the headline is road passengers, which end at 2022:
        // opening on 2024 would show "—" in the headline.
        assertEquals(2022, state.selectedYear)
        assertEquals(2024, state.availableYears.last())
    }

    @Test
    fun default_year_falls_back_to_last_available_year_without_road_data() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        val airOnly = TransportTimeSeries(
            countryCode = "DE",
            countryName = "DE",
            mode = TransportMode.AIR,
            points = listOf(
                TransportDataPoint("DE", 2022, TransportMode.AIR, roadPassengers = null, airPassengers = 100_000L, seaPassengers = null),
                TransportDataPoint("DE", 2023, TransportMode.AIR, roadPassengers = null, airPassengers = 110_000L, seaPassengers = null),
            ),
        )
        repo.emissions.value = Result.Success(listOf(airOnly), isStale = false)
        testScheduler.advanceUntilIdle()

        val state = component.state.value as TransportUiState.Content
        assertEquals(2023, state.selectedYear)
    }

    @Test
    fun select_year_with_road_only_shows_dash_for_air() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(gapTransportSeries("DE")), isStale = false)
        testScheduler.advanceUntilIdle()

        // 2022 has road data but no air data
        component.onIntent(TransportIntent.SelectYear(2022))

        val state = component.state.value as TransportUiState.Content
        assertEquals(2022, state.selectedYear)
        // The screen uses selectedYear to look up the point; verify the point has road but not air
        val point = state.series.first().points.firstOrNull { it.year == 2022 }
        assertEquals(520_000L, point?.roadPassengers)
        assertEquals(null, point?.airPassengers)
    }

    @Test
    fun select_year_does_not_trigger_a_fetch() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(gapTransportSeries("DE")), isStale = false)
        testScheduler.advanceUntilIdle()

        val callCountBefore = repo.refreshCallCount

        component.onIntent(TransportIntent.SelectYear(2022))
        testScheduler.advanceUntilIdle()

        assertEquals(callCountBefore, repo.refreshCallCount)
    }

    @Test
    fun refresh_failed_is_false_initially() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(sampleTransportSeries()), isStale = false)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<TransportUiState.Content>(state)
        assertFalse(state.refreshFailed)
    }

    @Test
    fun failed_refresh_with_cache_sets_refresh_failed() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        repo.emissions.value = Result.Success(listOf(sampleTransportSeries()), isStale = false)
        testScheduler.advanceUntilIdle()

        repo.refreshThrows = IllegalStateException("network down")
        component.onIntent(TransportIntent.Refresh)
        testScheduler.advanceUntilIdle()

        assertEquals(1, repo.refreshCallCount)
        val state = component.state.value
        assertIs<TransportUiState.Content>(state)
        assertTrue(state.refreshFailed)
        assertFalse(state.isStale)
    }

    @Test
    fun successful_refresh_leaves_refresh_failed_false() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        repo.emissions.value = Result.Success(listOf(sampleTransportSeries()), isStale = false)
        testScheduler.advanceUntilIdle()

        component.onIntent(TransportIntent.Refresh)
        testScheduler.advanceUntilIdle()

        assertEquals(1, repo.refreshCallCount)
        val state = component.state.value
        assertIs<TransportUiState.Content>(state)
        assertFalse(state.refreshFailed)
    }

    @Test
    fun successful_refresh_after_failed_one_clears_refresh_failed() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        repo.emissions.value = Result.Success(listOf(sampleTransportSeries()), isStale = false)
        testScheduler.advanceUntilIdle()

        repo.refreshThrows = IllegalStateException("network down")
        component.onIntent(TransportIntent.Refresh)
        testScheduler.advanceUntilIdle()
        val failedState = component.state.value
        assertIs<TransportUiState.Content>(failedState)
        assertTrue(failedState.refreshFailed)

        repo.refreshThrows = null
        component.onIntent(TransportIntent.Refresh)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<TransportUiState.Content>(state)
        assertFalse(state.refreshFailed)
    }

    @Test
    fun cancelled_refresh_is_not_reported_as_failure() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        repo.emissions.value = Result.Success(listOf(sampleTransportSeries()), isStale = false)
        testScheduler.advanceUntilIdle()
        val observeCallsBefore = repo.observeCallCount

        repo.refreshThrows = CancellationException("cancelled")
        component.onIntent(TransportIntent.Refresh)
        testScheduler.advanceUntilIdle()

        // Cancellation is rethrown: no reload is started and no hint is raised.
        assertEquals(observeCallsBefore, repo.observeCallCount)
        val state = component.state.value
        assertIs<TransportUiState.Content>(state)
        assertFalse(state.refreshFailed)
    }

    @Test
    fun refresh_failed_clears_on_next_select_countries() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        repo.emissions.value = Result.Success(listOf(sampleTransportSeries()), isStale = false)
        testScheduler.advanceUntilIdle()
        repo.refreshThrows = IllegalStateException("network down")
        component.onIntent(TransportIntent.Refresh)
        testScheduler.advanceUntilIdle()
        val failedState = component.state.value
        assertIs<TransportUiState.Content>(failedState)
        assertTrue(failedState.refreshFailed)

        component.onIntent(TransportIntent.SelectCountries(listOf("FR", "ES")))
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<TransportUiState.Content>(state)
        assertFalse(state.refreshFailed)
    }

    @Test
    fun refresh_failed_clears_on_next_year_range_change() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        repo.emissions.value = Result.Success(listOf(sampleTransportSeries()), isStale = false)
        testScheduler.advanceUntilIdle()
        repo.refreshThrows = IllegalStateException("network down")
        component.onIntent(TransportIntent.Refresh)
        testScheduler.advanceUntilIdle()
        val failedState = component.state.value
        assertIs<TransportUiState.Content>(failedState)
        assertTrue(failedState.refreshFailed)

        component.onIntent(TransportIntent.ChangeYearRange(2018..2022))
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<TransportUiState.Content>(state)
        assertFalse(state.refreshFailed)
    }

    @Test
    fun refresh_failed_clears_on_next_mode_change() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        repo.emissions.value = Result.Success(listOf(sampleTransportSeries()), isStale = false)
        testScheduler.advanceUntilIdle()
        repo.refreshThrows = IllegalStateException("network down")
        component.onIntent(TransportIntent.Refresh)
        testScheduler.advanceUntilIdle()
        val failedState = component.state.value
        assertIs<TransportUiState.Content>(failedState)
        assertTrue(failedState.refreshFailed)

        component.onIntent(TransportIntent.ChangeMode(TransportMode.AIR))
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<TransportUiState.Content>(state)
        assertFalse(state.refreshFailed)
    }

    @Test
    fun refresh_failed_survives_ui_only_rerender() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        repo.emissions.value = Result.Success(listOf(gapTransportSeries("DE")), isStale = false)
        testScheduler.advanceUntilIdle()
        repo.refreshThrows = IllegalStateException("network down")
        component.onIntent(TransportIntent.Refresh)
        testScheduler.advanceUntilIdle()

        component.onIntent(TransportIntent.SelectYear(2023))
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<TransportUiState.Content>(state)
        assertEquals(2023, state.selectedYear)
        assertTrue(state.refreshFailed)
    }

    @Test
    fun stale_emission_clears_refresh_failed_and_it_stays_cleared() = runTest {
        val repo = FakeTransportRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        repo.emissions.value = Result.Success(listOf(sampleTransportSeries()), isStale = false)
        testScheduler.advanceUntilIdle()
        repo.refreshThrows = IllegalStateException("network down")
        component.onIntent(TransportIntent.Refresh)
        testScheduler.advanceUntilIdle()
        val failedState = component.state.value
        assertIs<TransportUiState.Content>(failedState)
        assertTrue(failedState.refreshFailed)

        repo.emissions.value = Result.Success(listOf(sampleTransportSeries()), isStale = true)
        testScheduler.advanceUntilIdle()

        val staleState = component.state.value
        assertIs<TransportUiState.Content>(staleState)
        assertTrue(staleState.isStale)
        assertFalse(staleState.refreshFailed)

        // A later successful revalidation must not bring the hint back.
        repo.emissions.value = Result.Success(listOf(sampleTransportSeries()), isStale = false)
        testScheduler.advanceUntilIdle()

        val freshState = component.state.value
        assertIs<TransportUiState.Content>(freshState)
        assertFalse(freshState.isStale)
        assertFalse(freshState.refreshFailed)
    }
}
