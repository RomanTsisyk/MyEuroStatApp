package eu.eurostat.feature.environment.ui

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import eu.eurostat.core.common.AppError
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.feature.environment.domain.EnvironmentDataPoint
import eu.eurostat.feature.environment.domain.EnvironmentQuery
import eu.eurostat.feature.environment.domain.EnvironmentRepository
import eu.eurostat.feature.environment.domain.EnvironmentTimeSeries
import eu.eurostat.feature.environment.domain.EnvMetric
import eu.eurostat.feature.environment.domain.EnvSector
import eu.eurostat.feature.environment.domain.GetEnvironmentTimeSeriesUseCase
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

private class FakeEnvironmentRepository : EnvironmentRepository {
    var lastQuery: EnvironmentQuery? = null
    var refreshCallCount = 0
    var refreshThrows: Throwable? = null

    val emissions = MutableStateFlow<Result<List<EnvironmentTimeSeries>>>(Result.Loading)

    override fun observe(query: EnvironmentQuery): Flow<Result<List<EnvironmentTimeSeries>>> {
        lastQuery = query
        return emissions.asStateFlow()
    }

    override suspend fun refresh(query: EnvironmentQuery) {
        refreshCallCount++
        refreshThrows?.let { throw it }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class TestDispatcherProviderLocal(
    dispatcher: kotlinx.coroutines.test.TestDispatcher,
) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

// ---------------------------------------------------------------------------
// Sample data
// ---------------------------------------------------------------------------

private fun sampleEnvironmentSeries(country: String = "DE"): EnvironmentTimeSeries =
    EnvironmentTimeSeries(
        countryCode = country,
        countryName = country,
        points = listOf(
            EnvironmentDataPoint(
                countryCode = country,
                year = 2019,
                sector = EnvSector.Total,
                ghgMtCo2eq = 850.0,
                energyKtoe = 310_000.0,
            ),
            EnvironmentDataPoint(
                countryCode = country,
                year = 2020,
                sector = EnvSector.Total,
                ghgMtCo2eq = 800.0,
                energyKtoe = 300_000.0,
            ),
        ),
    )

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

/**
 * Unit tests for [DefaultEnvironmentComponent].
 *
 * Uses a [FakeEnvironmentRepository] to control emitted [Result] values without
 * touching the network. Tests verify the component's state machine (Loading →
 * Content / Empty / Error) and intent handling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EnvironmentComponentTest {

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
        repo: FakeEnvironmentRepository,
        dispatcher: kotlinx.coroutines.test.TestDispatcher,
    ): DefaultEnvironmentComponent {
        val useCase = GetEnvironmentTimeSeriesUseCase(repo)
        val dispatchers = TestDispatcherProviderLocal(dispatcher)
        return DefaultEnvironmentComponent(context, useCase, dispatchers)
    }

    // ------------------------------------------------------------------------------------
    // 1. Initial state is Loading
    // ------------------------------------------------------------------------------------

    @Test
    fun init_emitsLoadingImmediately() = runTest {
        val repo = FakeEnvironmentRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        assertEquals(EnvironmentUiState.Loading, component.state.value)
    }

    // ------------------------------------------------------------------------------------
    // 2. Repo Success with data → Content state
    // ------------------------------------------------------------------------------------

    @Test
    fun emitsContent_whenRepoEmitsSuccessWithData() = runTest {
        val repo = FakeEnvironmentRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(sampleEnvironmentSeries()), isStale = false)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<EnvironmentUiState.Content>(state)
        assertEquals(1, state.timeSeries.size)
        assertFalse(state.isStale)
    }

    // ------------------------------------------------------------------------------------
    // 3. Repo Success with empty list → Empty state
    // ------------------------------------------------------------------------------------

    @Test
    fun emitsEmpty_whenRepoEmitsSuccessWithEmptyList() = runTest {
        val repo = FakeEnvironmentRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(emptyList(), isStale = false)
        testScheduler.advanceUntilIdle()

        assertIs<EnvironmentUiState.Empty>(component.state.value)
    }

    // ------------------------------------------------------------------------------------
    // 4. Repo Error → Error state with canRetry=true
    // ------------------------------------------------------------------------------------

    @Test
    fun emitsError_withCanRetry_whenRepoEmitsError() = runTest {
        val repo = FakeEnvironmentRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Error(AppError.NoNetwork)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<EnvironmentUiState.Error>(state)
        assertTrue(state.canRetry)
    }

    // ------------------------------------------------------------------------------------
    // 5. Stale content → Content with isStale=true
    // ------------------------------------------------------------------------------------

    @Test
    fun emitsStaleContent_whenRepoEmitsStaleSuccess() = runTest {
        val repo = FakeEnvironmentRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(sampleEnvironmentSeries()), isStale = true)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<EnvironmentUiState.Content>(state)
        assertTrue(state.isStale)
    }

    // ------------------------------------------------------------------------------------
    // 6. SelectCountries intent: updates query and triggers reload
    // ------------------------------------------------------------------------------------

    @Test
    fun onSelectCountries_updatesQueryAndReloads() = runTest {
        val repo = FakeEnvironmentRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(EnvironmentIntent.SelectCountries(listOf("FR", "IT")))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("FR", "IT"), repo.lastQuery?.countryCodes)
    }

    // ------------------------------------------------------------------------------------
    // 7. ChangeYearRange intent: updates query and triggers reload
    // ------------------------------------------------------------------------------------

    @Test
    fun onChangeYearRange_updatesQueryAndReloads() = runTest {
        val repo = FakeEnvironmentRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(EnvironmentIntent.ChangeYearRange(2015..2023))
        testScheduler.advanceUntilIdle()

        assertEquals(2015..2023, repo.lastQuery?.yearRange)
    }

    // ------------------------------------------------------------------------------------
    // 8. Refresh intent: calls repository refresh once
    // ------------------------------------------------------------------------------------

    @Test
    fun onRefresh_callsRepositoryRefresh() = runTest {
        val repo = FakeEnvironmentRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(EnvironmentIntent.Refresh)
        testScheduler.advanceUntilIdle()

        assertEquals(1, repo.refreshCallCount)
    }

    // ------------------------------------------------------------------------------------
    // 9. Retry intent: triggers reload (re-subscribes to repository)
    // ------------------------------------------------------------------------------------

    @Test
    fun onRetry_triggersReload() = runTest {
        val repo = FakeEnvironmentRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        repo.emissions.value = Result.Success(listOf(sampleEnvironmentSeries()), isStale = false)
        component.onIntent(EnvironmentIntent.Retry)
        testScheduler.advanceUntilIdle()

        assertIs<EnvironmentUiState.Content>(component.state.value)
    }

    // ------------------------------------------------------------------------------------
    // 10. Content state carries query from last intent
    // ------------------------------------------------------------------------------------

    @Test
    fun contentState_carriesCurrentQuery() = runTest {
        val repo = FakeEnvironmentRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        component.onIntent(EnvironmentIntent.SelectCountries(listOf("PL")))
        repo.emissions.value = Result.Success(listOf(sampleEnvironmentSeries("PL")), isStale = false)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<EnvironmentUiState.Content>(state)
        assertEquals(listOf("PL"), state.query.countryCodes)
    }

    // ------------------------------------------------------------------------------------
    // 11. SelectSector intent: updates activeSector in Content without reload
    // ------------------------------------------------------------------------------------

    @Test
    fun onSelectSector_updatesActiveSectorInContent() = runTest {
        val repo = FakeEnvironmentRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(sampleEnvironmentSeries()), isStale = false)
        testScheduler.advanceUntilIdle()

        component.onIntent(EnvironmentIntent.SelectSector(EnvSector.Transport))
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<EnvironmentUiState.Content>(state)
        assertEquals(EnvSector.Transport, state.activeSector)
        // Verify refresh count stays at 0 (SelectSector does not trigger a network reload)
        assertEquals(0, repo.refreshCallCount)
    }

    // ------------------------------------------------------------------------------------
    // 12. SelectMetric intent: updates activeMetric in Content without reload
    // ------------------------------------------------------------------------------------

    @Test
    fun onSelectMetric_updatesActiveMetricInContent() = runTest {
        val repo = FakeEnvironmentRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(sampleEnvironmentSeries()), isStale = false)
        testScheduler.advanceUntilIdle()

        component.onIntent(EnvironmentIntent.SelectMetric(EnvMetric.Energy))
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<EnvironmentUiState.Content>(state)
        assertEquals(EnvMetric.Energy, state.activeMetric)
    }

    // ------------------------------------------------------------------------------------
    // 13. SelectActiveCountry during Loading: stored; applied when content arrives
    // ------------------------------------------------------------------------------------

    @Test
    fun selectActiveCountry_duringLoading_appliedWhenContentArrives() = runTest {
        val repo = FakeEnvironmentRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        // State is Loading — intent must not be silently lost
        assertEquals(EnvironmentUiState.Loading, component.state.value)
        component.onIntent(EnvironmentIntent.SelectActiveCountry("FR"))

        repo.emissions.value = Result.Success(
            listOf(sampleEnvironmentSeries("DE"), sampleEnvironmentSeries("FR")),
            isStale = false,
        )
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<EnvironmentUiState.Content>(state)
        assertEquals("FR", state.activeCountry)
    }

    // ------------------------------------------------------------------------------------
    // 14. SelectYear: updates selectedYear in Content without triggering a network reload
    // ------------------------------------------------------------------------------------

    @Test
    fun onSelectYear_updatesSelectedYearWithoutNetworkCall() = runTest {
        val repo = FakeEnvironmentRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(sampleEnvironmentSeries()), isStale = false)
        testScheduler.advanceUntilIdle()

        val initialState = component.state.value
        assertIs<EnvironmentUiState.Content>(initialState)
        // Default selectedYear is the latest (2020)
        assertEquals(2020, initialState.selectedYear)

        component.onIntent(EnvironmentIntent.SelectYear(2019))
        testScheduler.advanceUntilIdle()

        val updatedState = component.state.value
        assertIs<EnvironmentUiState.Content>(updatedState)
        assertEquals(2019, updatedState.selectedYear)
        // No refresh calls — SelectYear is a pure UI intent
        assertEquals(0, repo.refreshCallCount)
    }
}
