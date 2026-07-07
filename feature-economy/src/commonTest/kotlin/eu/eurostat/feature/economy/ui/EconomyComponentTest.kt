package eu.eurostat.feature.economy.ui

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import eu.eurostat.core.common.AppError
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.feature.economy.domain.EconomyDataPoint
import eu.eurostat.feature.economy.domain.EconomyQuery
import eu.eurostat.feature.economy.domain.EconomyRepository
import eu.eurostat.feature.economy.domain.EconomyTimeSeries
import eu.eurostat.feature.economy.domain.EconomyUnit
import eu.eurostat.feature.economy.domain.GetEconomyTimeSeriesUseCase
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

private class FakeEconomyRepository : EconomyRepository {
    var lastQuery: EconomyQuery? = null
    var observeCallCount = 0
    var refreshCallCount = 0
    var refreshThrows: Throwable? = null

    val emissions = MutableStateFlow<Result<List<EconomyTimeSeries>>>(Result.Loading)

    override fun observe(query: EconomyQuery): Flow<Result<List<EconomyTimeSeries>>> {
        lastQuery = query
        observeCallCount++
        return emissions.asStateFlow()
    }

    override suspend fun refresh(query: EconomyQuery) {
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

private fun sampleEconomySeries(country: String = "DE"): EconomyTimeSeries =
    EconomyTimeSeries(
        countryCode = country,
        countryName = country,
        points = listOf(
            EconomyDataPoint(
                countryCode = country,
                year = 2020,
                gdpEur = 3_300_000L,
                hicpIndex = null,
                deficitPctGdp = null,
            )
        )
    )

private fun multiYearEconomySeries(country: String = "DE"): EconomyTimeSeries =
    EconomyTimeSeries(
        countryCode = country,
        countryName = country,
        points = listOf(
            EconomyDataPoint(country, 2020, 3_000_000L, null, null),
            EconomyDataPoint(country, 2021, 3_100_000L, null, null),
            EconomyDataPoint(country, 2022, 3_200_000L, null, null),
        ),
    )

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

@OptIn(ExperimentalCoroutinesApi::class)
class EconomyComponentTest {

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
        repo: FakeEconomyRepository,
        dispatcher: kotlinx.coroutines.test.TestDispatcher,
    ): DefaultEconomyComponent {
        val useCase = GetEconomyTimeSeriesUseCase(repo)
        val dispatchers = TestDispatcherProviderLocal(dispatcher)
        return DefaultEconomyComponent(context, useCase, dispatchers)
    }

    @Test
    fun init_emits_loading_immediately() = runTest {
        val repo = FakeEconomyRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        assertEquals(EconomyUiState.Loading, component.state.value)
    }

    @Test
    fun emits_content_when_repo_emits_success_with_data() = runTest {
        val repo = FakeEconomyRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(sampleEconomySeries()), isStale = false)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<EconomyUiState.Content>(state)
        assertEquals(1, state.timeSeries.size)
        assertFalse(state.isStale)
    }

    @Test
    fun emits_empty_when_repo_emits_success_with_empty_list() = runTest {
        val repo = FakeEconomyRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(emptyList(), isStale = false)
        testScheduler.advanceUntilIdle()

        assertIs<EconomyUiState.Empty>(component.state.value)
    }

    @Test
    fun emits_error_with_canRetry_when_repo_emits_error() = runTest {
        val repo = FakeEconomyRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Error(AppError.NoNetwork)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<EconomyUiState.Error>(state)
        assertTrue(state.canRetry)
    }

    @Test
    fun stale_content_emits_isStale_true() = runTest {
        val repo = FakeEconomyRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(sampleEconomySeries()), isStale = true)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<EconomyUiState.Content>(state)
        assertTrue(state.isStale)
    }

    @Test
    fun on_select_countries_updates_query_and_reloads() = runTest {
        val repo = FakeEconomyRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(EconomyIntent.SelectCountries(listOf("FR", "ES")))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("FR", "ES"), repo.lastQuery?.countryCodes)
    }

    @Test
    fun on_change_year_range_updates_query_and_reloads() = runTest {
        val repo = FakeEconomyRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(EconomyIntent.ChangeYearRange(2018..2022))
        testScheduler.advanceUntilIdle()

        assertEquals(2018..2022, repo.lastQuery?.yearRange)
    }

    @Test
    fun on_change_unit_updates_query_and_reloads() = runTest {
        val repo = FakeEconomyRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(EconomyIntent.ChangeUnit(EconomyUnit.CLV_PCH_PRE))
        testScheduler.advanceUntilIdle()

        assertEquals(EconomyUnit.CLV_PCH_PRE, repo.lastQuery?.unit)
    }

    @Test
    fun on_refresh_calls_repository_refresh() = runTest {
        val repo = FakeEconomyRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(EconomyIntent.Refresh)
        testScheduler.advanceUntilIdle()

        assertEquals(1, repo.refreshCallCount)
    }

    @Test
    fun on_retry_triggers_reload() = runTest {
        val repo = FakeEconomyRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        repo.emissions.value = Result.Success(listOf(sampleEconomySeries()), isStale = false)
        component.onIntent(EconomyIntent.Retry)
        testScheduler.advanceUntilIdle()

        assertIs<EconomyUiState.Content>(component.state.value)
    }

    @Test
    fun year_picker_defaults_to_latest_and_updates_without_network_call() = runTest {
        val repo = FakeEconomyRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(multiYearEconomySeries("DE")), isStale = false)
        testScheduler.advanceUntilIdle()

        val initialState = component.state.value
        assertIs<EconomyUiState.Content>(initialState)
        assertEquals(listOf(2020, 2021, 2022), initialState.availableYears)
        assertEquals(2022, initialState.selectedYear)

        val observeCallsBefore = repo.observeCallCount
        component.onIntent(EconomyIntent.SelectYear(2021))
        testScheduler.advanceUntilIdle()

        val updatedState = component.state.value
        assertIs<EconomyUiState.Content>(updatedState)
        assertEquals(2021, updatedState.selectedYear)
        assertEquals(observeCallsBefore, repo.observeCallCount)
    }

    @Test
    fun set_normalized_updates_state_without_network_call_and_survives_re_emission() = runTest {
        val repo = FakeEconomyRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(multiYearEconomySeries("DE")), isStale = false)
        testScheduler.advanceUntilIdle()

        val initialState = component.state.value
        assertIs<EconomyUiState.Content>(initialState)
        assertFalse(initialState.normalized)

        val observeCallsBefore = repo.observeCallCount
        component.onIntent(EconomyIntent.SetNormalized(true))
        testScheduler.advanceUntilIdle()

        val updatedState = component.state.value
        assertIs<EconomyUiState.Content>(updatedState)
        assertTrue(updatedState.normalized)
        assertEquals(observeCallsBefore, repo.observeCallCount)

        // The flag is owned by the component (like selectedMetric), so a fresh
        // repository emission must not reset it.
        repo.emissions.value = Result.Success(listOf(multiYearEconomySeries("DE")), isStale = true)
        testScheduler.advanceUntilIdle()

        val reEmittedState = component.state.value
        assertIs<EconomyUiState.Content>(reEmittedState)
        assertTrue(reEmittedState.normalized)

        component.onIntent(EconomyIntent.SetNormalized(false))
        testScheduler.advanceUntilIdle()

        val disabledState = component.state.value
        assertIs<EconomyUiState.Content>(disabledState)
        assertFalse(disabledState.normalized)
    }
}
