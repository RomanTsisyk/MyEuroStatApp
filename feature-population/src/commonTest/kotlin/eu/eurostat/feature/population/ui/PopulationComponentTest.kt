package eu.eurostat.feature.population.ui

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import eu.eurostat.core.common.AppError
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.feature.population.domain.GetPopulationTimeSeriesUseCase
import eu.eurostat.feature.population.domain.PopulationCohort
import eu.eurostat.feature.population.domain.PopulationData
import eu.eurostat.feature.population.domain.PopulationDataPoint
import eu.eurostat.feature.population.domain.PopulationQuery
import eu.eurostat.feature.population.domain.PopulationRepository
import eu.eurostat.feature.population.domain.PopulationSnapshot
import eu.eurostat.feature.population.domain.PopulationTimeSeries
import kotlin.test.assertNotNull
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

private class FakePopulationRepository : PopulationRepository {
    var lastQuery: PopulationQuery? = null
    var refreshCallCount = 0
    var refreshThrows: Throwable? = null

    val emissions = MutableStateFlow<Result<eu.eurostat.feature.population.domain.PopulationData>>(Result.Loading)

    override fun observe(query: PopulationQuery): Flow<Result<eu.eurostat.feature.population.domain.PopulationData>> {
        lastQuery = query
        return emissions.asStateFlow()
    }

    /** Convenience: wrap a plain time-series emission. */
    fun emitSeries(series: List<PopulationTimeSeries>, isStale: Boolean = false) {
        emissions.value = Result.Success(
            PopulationData(
                timeSeries = series,
                snapshots = emptyMap(),
            ),
            isStale = isStale,
        )
    }

    /**
     * Emits a [PopulationData] with both time-series AND a pre-built snapshot map.
     * This is the realistic fresh-network-response shape — used to assert that snapshot
     * data actually flows from the repository all the way into [PopulationUiState.Content].
     */
    fun emitWithSnapshots(
        series: List<PopulationTimeSeries>,
        snapshots: Map<Pair<String, Int>, PopulationSnapshot>,
        isStale: Boolean = false,
    ) {
        emissions.value = Result.Success(
            PopulationData(timeSeries = series, snapshots = snapshots),
            isStale = isStale,
        )
    }

    override suspend fun refresh(query: PopulationQuery) {
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

private fun samplePopulationSeries(country: String = "PL"): PopulationTimeSeries =
    PopulationTimeSeries(
        countryCode = country,
        countryName = country,
        points = listOf(
            PopulationDataPoint(
                countryCode = country,
                year = 2020,
                totalPopulation = 38_000_000L,
                malePopulation = 18_000_000L,
                femalePopulation = 20_000_000L,
            )
        )
    )

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

@OptIn(ExperimentalCoroutinesApi::class)
class PopulationComponentTest {

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
        repo: FakePopulationRepository,
        dispatcher: kotlinx.coroutines.test.TestDispatcher,
    ): DefaultPopulationComponent {
        val useCase = GetPopulationTimeSeriesUseCase(repo)
        val dispatchers = TestDispatcherProviderLocal(dispatcher)
        return DefaultPopulationComponent(context, useCase, dispatchers)
    }

    @Test
    fun init_emits_loading_immediately() = runTest {
        val repo = FakePopulationRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        // Before any emission from repo the initial state is Loading
        assertEquals(PopulationUiState.Loading, component.state.value)
    }

    @Test
    fun emits_content_when_repo_emits_success_with_data() = runTest {
        val repo = FakePopulationRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emitSeries(listOf(samplePopulationSeries()), isStale = false)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<PopulationUiState.Content>(state)
        assertEquals(1, state.timeSeries.size)
        assertFalse(state.isStale)
    }

    @Test
    fun emits_empty_when_repo_emits_success_with_empty_list() = runTest {
        val repo = FakePopulationRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emitSeries(emptyList(), isStale = false)
        testScheduler.advanceUntilIdle()

        assertIs<PopulationUiState.Empty>(component.state.value)
    }

    @Test
    fun emits_error_with_canRetry_when_repo_emits_error() = runTest {
        val repo = FakePopulationRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Error(AppError.NoNetwork)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<PopulationUiState.Error>(state)
        assertTrue(state.canRetry)
    }

    @Test
    fun stale_content_emits_isStale_true() = runTest {
        val repo = FakePopulationRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emitSeries(listOf(samplePopulationSeries()), isStale = true)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<PopulationUiState.Content>(state)
        assertTrue(state.isStale)
    }

    @Test
    fun on_select_countries_updates_query_and_reloads() = runTest {
        val repo = FakePopulationRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(PopulationIntent.SelectCountries(listOf("DE", "IT")))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("DE", "IT"), repo.lastQuery?.countryCodes)
    }

    @Test
    fun on_change_year_range_updates_query_and_reloads() = runTest {
        val repo = FakePopulationRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(PopulationIntent.ChangeYearRange(2015..2020))
        testScheduler.advanceUntilIdle()

        assertEquals(2015..2020, repo.lastQuery?.yearRange)
    }

    @Test
    fun on_refresh_calls_repository_refresh() = runTest {
        val repo = FakePopulationRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(PopulationIntent.Refresh)
        testScheduler.advanceUntilIdle()

        assertEquals(1, repo.refreshCallCount)
    }

    @Test
    fun on_retry_triggers_reload() = runTest {
        val repo = FakePopulationRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        repo.emitSeries(listOf(samplePopulationSeries()), isStale = false)
        component.onIntent(PopulationIntent.Retry)
        testScheduler.advanceUntilIdle()

        assertIs<PopulationUiState.Content>(component.state.value)
    }

    // ---------------------------------------------------------------------------
    // Snapshot regression tests — prevent pyramid "always loading" bug
    // ---------------------------------------------------------------------------

    /**
     * Regression test: when the repository emits a [PopulationData] with a non-empty
     * snapshot map, the component's [PopulationUiState.Content.snapshot] must be non-null
     * for the selected (country, year). This validates the full flow from repository
     * emission → component buildContent → UiState.Content.snapshot.
     *
     * Previously the FakePopulationRepository always returned snapshots=emptyMap(),
     * hiding any regression in the snapshot lookup path.
     */
    @Test
    fun snapshot_flows_from_repository_emission_into_ui_state() = runTest {
        val repo = FakePopulationRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        val series = listOf(samplePopulationSeries("PL"))
        // Build a snapshot for the same (country, year) that samplePopulationSeries uses.
        val snapshot = PopulationSnapshot(
            countryCode = "PL",
            countryName = "PL",
            year = 2020,
            cohorts = listOf(
                PopulationCohort("Y_LT5", "Under 5", male = 200_000L, female = 190_000L),
                PopulationCohort("Y5-9", "5-9 years", male = 210_000L, female = 200_000L),
            ),
            totalMale = 18_000_000L,
            totalFemale = 20_000_000L,
            total = 38_000_000L,
        )
        val snapshots = mapOf(("PL" to 2020) to snapshot)

        repo.emitWithSnapshots(series, snapshots, isStale = false)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<PopulationUiState.Content>(state)
        assertNotNull(state.snapshot, "UiState.Content.snapshot must be non-null when repository emits a matching snapshot")
        assertEquals("PL", state.snapshot!!.countryCode)
        assertEquals(2, state.snapshot!!.cohorts.size)
    }

    /**
     * Regression test: a two-emission sequence (first cached with no snapshots, then fresh
     * with snapshots) must result in a non-null snapshot in the final UiState.Content.
     * This mirrors the stale-while-revalidate flow in PopulationRepositoryImpl.
     */
    @Test
    fun snapshot_populated_after_fresh_emission_follows_cached_empty_emission() = runTest {
        val repo = FakePopulationRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        val series = listOf(samplePopulationSeries("PL"))

        // First emission: cached data, no snapshots (as PopulationRepositoryImpl emits on cache hit)
        repo.emitSeries(series, isStale = false)
        testScheduler.advanceUntilIdle()

        val stateAfterCache = component.state.value
        assertIs<PopulationUiState.Content>(stateAfterCache)
        // Snapshot is null at this point — pyramid shows shimmer.
        // (This is expected transient state while network fetch is in-flight.)

        // Second emission: fresh network response with real snapshots
        val snapshot = PopulationSnapshot(
            countryCode = "PL",
            countryName = "PL",
            year = 2020,
            cohorts = listOf(
                PopulationCohort("Y_LT5", "Under 5", male = 200_000L, female = 190_000L),
            ),
            totalMale = 18_000_000L,
            totalFemale = 20_000_000L,
            total = 38_000_000L,
        )
        repo.emitWithSnapshots(series, mapOf(("PL" to 2020) to snapshot), isStale = false)
        testScheduler.advanceUntilIdle()

        val stateAfterFresh = component.state.value
        assertIs<PopulationUiState.Content>(stateAfterFresh)
        assertNotNull(
            stateAfterFresh.snapshot,
            "Snapshot must be non-null after fresh emission — pyramid must not stay in 'loading' state",
        )
    }
}
