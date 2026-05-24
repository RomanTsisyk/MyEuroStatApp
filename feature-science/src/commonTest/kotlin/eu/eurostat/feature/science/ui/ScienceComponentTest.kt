package eu.eurostat.feature.science.ui

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import eu.eurostat.core.common.AppError
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.feature.science.domain.GetScienceTimeSeriesUseCase
import eu.eurostat.feature.science.domain.ScienceDataPoint
import eu.eurostat.feature.science.domain.ScienceQuery
import eu.eurostat.feature.science.domain.ScienceRepository
import eu.eurostat.feature.science.domain.ScienceTimeSeries
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

private class FakeScienceRepository : ScienceRepository {
    var lastQuery: ScienceQuery? = null
    var refreshCallCount = 0
    var refreshThrows: Throwable? = null

    val emissions = MutableStateFlow<Result<List<ScienceTimeSeries>>>(Result.Loading)

    override fun observe(query: ScienceQuery): Flow<Result<List<ScienceTimeSeries>>> {
        lastQuery = query
        return emissions.asStateFlow()
    }

    override suspend fun refresh(query: ScienceQuery) {
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

// ---------------------------------------------------------------------------
// Sample data
// ---------------------------------------------------------------------------

private fun sampleScienceSeries(country: String = "FI"): ScienceTimeSeries =
    ScienceTimeSeries(
        countryCode = country,
        countryName = country,
        points = listOf(
            ScienceDataPoint(
                countryCode = country,
                year = 2019,
                rdSpendPctGdp = 2.7,
                internetUsagePct = 90.0,
                tertiaryEducPct = 43.0,
            ),
            ScienceDataPoint(
                countryCode = country,
                year = 2020,
                rdSpendPctGdp = 2.9,
                internetUsagePct = 92.0,
                tertiaryEducPct = 45.0,
            ),
        )
    )

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

@OptIn(ExperimentalCoroutinesApi::class)
class ScienceComponentTest {

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
        repo: FakeScienceRepository,
        dispatcher: kotlinx.coroutines.test.TestDispatcher,
    ): DefaultScienceComponent {
        val useCase = GetScienceTimeSeriesUseCase(repo)
        val dispatchers = TestDispatcherProviderLocal(dispatcher)
        return DefaultScienceComponent(context, useCase, dispatchers)
    }

    @Test
    fun init_emits_loading_immediately() = runTest {
        val repo = FakeScienceRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        assertEquals(ScienceUiState.Loading, component.state.value)
    }

    @Test
    fun emits_content_when_repo_emits_success_with_data() = runTest {
        val repo = FakeScienceRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(sampleScienceSeries()), isStale = false)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<ScienceUiState.Content>(state)
        assertEquals(1, state.series.size)
        assertFalse(state.isStale)
    }

    @Test
    fun emits_empty_when_repo_emits_success_with_empty_list() = runTest {
        val repo = FakeScienceRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(emptyList(), isStale = false)
        testScheduler.advanceUntilIdle()

        assertIs<ScienceUiState.Empty>(component.state.value)
    }

    @Test
    fun emits_error_with_canRetry_when_repo_emits_error() = runTest {
        val repo = FakeScienceRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Error(AppError.NoNetwork)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<ScienceUiState.Error>(state)
        assertTrue(state.canRetry)
    }

    @Test
    fun stale_content_emits_isStale_true() = runTest {
        val repo = FakeScienceRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(sampleScienceSeries()), isStale = true)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<ScienceUiState.Content>(state)
        assertTrue(state.isStale)
    }

    @Test
    fun on_select_countries_updates_query_and_reloads() = runTest {
        val repo = FakeScienceRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(ScienceIntent.SelectCountries(listOf("SE", "DK")))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("SE", "DK"), repo.lastQuery?.countryCodes)
    }

    @Test
    fun on_change_year_range_updates_query_and_reloads() = runTest {
        val repo = FakeScienceRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(ScienceIntent.ChangeYearRange(2011..2020))
        testScheduler.advanceUntilIdle()

        assertEquals(2011..2020, repo.lastQuery?.yearRange)
    }

    @Test
    fun on_refresh_calls_repository_refresh() = runTest {
        val repo = FakeScienceRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(ScienceIntent.Refresh)
        testScheduler.advanceUntilIdle()

        assertEquals(1, repo.refreshCallCount)
    }

    @Test
    fun on_retry_triggers_reload() = runTest {
        val repo = FakeScienceRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        repo.emissions.value = Result.Success(listOf(sampleScienceSeries()), isStale = false)
        component.onIntent(ScienceIntent.Retry)
        testScheduler.advanceUntilIdle()

        assertIs<ScienceUiState.Content>(component.state.value)
    }

    @Test
    fun content_defaults_to_latest_year() = runTest {
        val repo = FakeScienceRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(sampleScienceSeries()), isStale = false)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<ScienceUiState.Content>(state)
        // sampleScienceSeries has years 2019 and 2020; default should be 2020 (latest).
        assertEquals(2020, state.selectedYear)
        assertEquals(listOf(2019, 2020), state.availableYears)
    }

    @Test
    fun select_year_updates_state_without_network_call() = runTest {
        val repo = FakeScienceRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(sampleScienceSeries()), isStale = false)
        testScheduler.advanceUntilIdle()

        val refreshCountBefore = repo.refreshCallCount
        component.onIntent(ScienceIntent.SelectYear(2019))
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<ScienceUiState.Content>(state)
        assertEquals(2019, state.selectedYear)
        // No additional network / refresh calls should have been made.
        assertEquals(refreshCountBefore, repo.refreshCallCount)
    }

    @Test
    fun select_active_country_resets_selected_year_to_latest() = runTest {
        val repo = FakeScienceRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        val seriesFI = sampleScienceSeries("FI")
        val seriesDe = ScienceTimeSeries(
            countryCode = "DE",
            countryName = "DE",
            points = listOf(
                ScienceDataPoint("DE", 2018, 3.1, 88.0, 32.0),
                ScienceDataPoint("DE", 2020, 3.2, 89.0, 33.0),
            ),
        )
        repo.emissions.value = Result.Success(listOf(seriesFI, seriesDe), isStale = false)
        testScheduler.advanceUntilIdle()

        // Pick an earlier year for FI.
        component.onIntent(ScienceIntent.SelectYear(2019))
        testScheduler.advanceUntilIdle()
        assertEquals(2019, (component.state.value as ScienceUiState.Content).selectedYear)

        // Switching country should reset to that country's latest year.
        component.onIntent(ScienceIntent.SelectActiveCountry("DE"))
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<ScienceUiState.Content>(state)
        assertEquals("DE", state.activeCountry)
        assertEquals(2020, state.selectedYear)
        assertEquals(listOf(2018, 2020), state.availableYears)
    }
}
