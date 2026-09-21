package eu.eurostat.feature.compare.ui

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import eu.eurostat.core.common.AppError
import eu.eurostat.core.common.Result
import eu.eurostat.core.common.prefs.AppPreferences
import eu.eurostat.feature.compare.FakeAppPreferences
import eu.eurostat.feature.compare.FakeEconomyRepository
import eu.eurostat.feature.compare.FakeEnvironmentRepository
import eu.eurostat.feature.compare.FakePopulationRepository
import eu.eurostat.feature.compare.FakeScienceRepository
import eu.eurostat.feature.compare.FakeSocialRepository
import eu.eurostat.feature.compare.FakeTourismRepository
import eu.eurostat.feature.compare.FakeTradeRepository
import eu.eurostat.feature.compare.FakeTransportRepository
import eu.eurostat.feature.compare.TestDispatchers
import eu.eurostat.feature.compare.data.CompareDataSource
import eu.eurostat.feature.compare.domain.CompareIndicator
import eu.eurostat.feature.economy.domain.EconomyDataPoint
import eu.eurostat.feature.economy.domain.EconomyTimeSeries
import eu.eurostat.feature.population.domain.PopulationData
import eu.eurostat.feature.population.domain.PopulationDataPoint
import eu.eurostat.feature.population.domain.PopulationTimeSeries
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultCompareComponentTest {

    private val lifecycle = LifecycleRegistry()
    private val context = DefaultComponentContext(lifecycle)

    private val population = FakePopulationRepository()
    private val economy = FakeEconomyRepository()
    private val environment = FakeEnvironmentRepository()
    private val trade = FakeTradeRepository()
    private val transport = FakeTransportRepository()
    private val tourism = FakeTourismRepository()
    private val social = FakeSocialRepository()
    private val science = FakeScienceRepository()

    @BeforeTest
    fun resume() {
        lifecycle.resume()
    }

    @AfterTest
    fun destroy() {
        lifecycle.destroy()
    }

    private fun build(
        dispatcher: TestDispatcher,
        preferences: AppPreferences = FakeAppPreferences(),
    ): DefaultCompareComponent {
        val dataSource = CompareDataSource(
            population, economy, environment, trade,
            transport, tourism, social, science,
        )
        return DefaultCompareComponent(context, dataSource, TestDispatchers(dispatcher), preferences)
    }

    private fun economyDeFrPl() = Result.Success(
        listOf(
            EconomyTimeSeries("DE", "Germany", listOf(EconomyDataPoint("DE", 2023, gdpEur = 4_500_000L))),
            EconomyTimeSeries("FR", "France", listOf(EconomyDataPoint("FR", 2023, gdpEur = 2_800_000L))),
            EconomyTimeSeries("PL", "Poland", listOf(EconomyDataPoint("PL", 2023, gdpEur = 650_000L))),
        ),
    )

    @Test
    fun initial_load_emits_content_with_gdp_series_for_default_countries() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        economy.emissions.value = economyDeFrPl()
        testScheduler.advanceUntilIdle()

        val content = assertIs<CompareUiState.Content>(component.state.value)
        assertEquals(CompareIndicator.GDP, content.indicator)
        assertEquals(listOf("DE", "FR", "PL"), content.countries)
        assertEquals(listOf("DE", "FR", "PL"), content.series.map { it.countryCode })
        // The default countries drove the economy query.
        assertEquals(listOf("DE", "FR", "PL"), economy.lastQuery?.countryCodes)
    }

    @Test
    fun select_indicator_refetches_from_the_target_repository() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        economy.emissions.value = economyDeFrPl()
        population.emissions.value = Result.Success(
            PopulationData(
                timeSeries = listOf(
                    PopulationTimeSeries("DE", "Germany", listOf(PopulationDataPoint("DE", 2023, 83_000_000L, null, null))),
                ),
                snapshots = emptyMap(),
            ),
        )
        testScheduler.advanceUntilIdle()
        // Starts on economy (population untouched).
        assertEquals(0, population.observeCount)

        component.onIntent(CompareIntent.SelectIndicator(CompareIndicator.POPULATION))
        testScheduler.advanceUntilIdle()

        val content = assertIs<CompareUiState.Content>(component.state.value)
        assertEquals(CompareIndicator.POPULATION, content.indicator)
        assertEquals(listOf("DE"), content.series.map { it.countryCode })
        // The population repo was queried with the same country selection.
        assertEquals(listOf("DE", "FR", "PL"), population.lastQuery?.countryCodes)
        assertTrue(population.observeCount >= 1)
    }

    @Test
    fun select_countries_preserves_selection_order_dedups_and_refetches() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        economy.emissions.value = economyDeFrPl()
        testScheduler.advanceUntilIdle()

        // Duplicates collapse but the user's order is KEPT (palette colours are
        // assigned by list position, so reordering would recolour untouched
        // countries — the SeriesPalette contract forbids that).
        component.onIntent(CompareIntent.SelectCountries(listOf("IT", "DE", "IT")))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("IT", "DE"), economy.lastQuery?.countryCodes)
        val content = assertIs<CompareUiState.Content>(component.state.value)
        assertEquals(listOf("IT", "DE"), content.countries)
    }

    @Test
    fun select_countries_below_the_minimum_is_ignored() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        economy.emissions.value = economyDeFrPl()
        testScheduler.advanceUntilIdle()
        val subscriptionsBefore = economy.observeCount

        // A single-country "comparison" is off-contract (MIN_COUNTRIES = 2).
        component.onIntent(CompareIntent.SelectCountries(listOf("DE")))
        testScheduler.advanceUntilIdle()

        val content = assertIs<CompareUiState.Content>(component.state.value)
        assertEquals(listOf("DE", "FR", "PL"), content.countries)
        assertEquals(subscriptionsBefore, economy.observeCount)
    }

    @Test
    fun refresh_forces_a_repository_refetch_before_reobserving() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        economy.emissions.value = economyDeFrPl()
        testScheduler.advanceUntilIdle()
        assertEquals(0, economy.refreshCount)
        val subscriptionsBefore = economy.observeCount

        component.onIntent(CompareIntent.Refresh)
        testScheduler.advanceUntilIdle()

        // A bare re-subscription would re-serve the within-TTL cache; Refresh
        // must hit the repository's refresh path first, then re-observe.
        assertEquals(1, economy.refreshCount)
        assertTrue(economy.observeCount > subscriptionsBefore)
        assertIs<CompareUiState.Content>(component.state.value)
    }

    @Test
    fun stale_success_surfaces_as_stale_content() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        economy.emissions.value = Result.Success(economyDeFrPl().data, isStale = true)
        testScheduler.advanceUntilIdle()

        val content = assertIs<CompareUiState.Content>(component.state.value)
        assertTrue(content.isStale)
    }

    @Test
    fun toggle_normalization_flips_flag_without_refetching() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        economy.emissions.value = economyDeFrPl()
        testScheduler.advanceUntilIdle()

        val before = assertIs<CompareUiState.Content>(component.state.value)
        assertEquals(false, before.normalized)
        val subscriptionsBefore = economy.observeCount

        component.onIntent(CompareIntent.ToggleNormalization)
        testScheduler.advanceUntilIdle()

        val after = assertIs<CompareUiState.Content>(component.state.value)
        assertEquals(true, after.normalized)
        // Same series, and the repository was not re-observed.
        assertEquals(before.series, after.series)
        assertEquals(subscriptionsBefore, economy.observeCount)
    }

    @Test
    fun error_with_no_data_maps_to_error_state_carrying_the_cause() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        economy.emissions.value = Result.Error(AppError.NoNetwork)
        testScheduler.advanceUntilIdle()

        val error = assertIs<CompareUiState.Error>(component.state.value)
        assertEquals(AppError.NoNetwork, error.error)
        assertTrue(error.canRetry)
    }

    @Test
    fun empty_success_maps_to_empty_state() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        economy.emissions.value = Result.Success(emptyList())
        testScheduler.advanceUntilIdle()

        assertIs<CompareUiState.Empty>(component.state.value)
    }

    @Test
    fun stored_default_country_joins_and_leads_the_selection() = runTest {
        val component = build(
            StandardTestDispatcher(testScheduler),
            FakeAppPreferences(defaultCountry = "IT"),
        )
        economy.emissions.value = Result.Success(
            listOf(
                EconomyTimeSeries("IT", "Italy", listOf(EconomyDataPoint("IT", 2022, gdpEur = 2_000_000L))),
            ),
        )
        testScheduler.advanceUntilIdle()

        // Preference country leads; fixed defaults follow, clamped to the max.
        assertEquals(listOf("IT", "DE", "FR", "PL"), economy.lastQuery?.countryCodes)
    }

    /** Loads GDP content, makes the economy refresh fail, refreshes, and returns the resulting Content. */
    private fun TestScope.failedRefreshContent(
        component: DefaultCompareComponent,
    ): CompareUiState.Content {
        economy.emissions.value = economyDeFrPl()
        testScheduler.advanceUntilIdle()
        economy.refreshThrows = IllegalStateException("network down")
        component.onIntent(CompareIntent.Refresh)
        testScheduler.advanceUntilIdle()
        val failed = assertIs<CompareUiState.Content>(component.state.value)
        assertTrue(failed.refreshFailed)
        return failed
    }

    @Test
    fun refresh_failed_is_false_initially() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        economy.emissions.value = economyDeFrPl()
        testScheduler.advanceUntilIdle()

        val content = assertIs<CompareUiState.Content>(component.state.value)
        assertFalse(content.refreshFailed)
    }

    @Test
    fun failed_refresh_with_cache_sets_refresh_failed() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))

        val content = failedRefreshContent(component)

        assertEquals(1, economy.refreshCount)
        // The failure is swallowed for data purposes: the cached series still render, not stale.
        assertFalse(content.isStale)
        assertEquals(listOf("DE", "FR", "PL"), content.series.map { it.countryCode })
    }

    @Test
    fun successful_refresh_leaves_refresh_failed_false() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        economy.emissions.value = economyDeFrPl()
        testScheduler.advanceUntilIdle()

        component.onIntent(CompareIntent.Refresh)
        testScheduler.advanceUntilIdle()

        assertEquals(1, economy.refreshCount)
        val content = assertIs<CompareUiState.Content>(component.state.value)
        assertFalse(content.refreshFailed)
    }

    @Test
    fun successful_refresh_after_failed_one_clears_refresh_failed() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        failedRefreshContent(component)

        economy.refreshThrows = null
        component.onIntent(CompareIntent.Refresh)
        testScheduler.advanceUntilIdle()

        assertEquals(2, economy.refreshCount)
        val content = assertIs<CompareUiState.Content>(component.state.value)
        assertFalse(content.refreshFailed)
    }

    @Test
    fun cancelled_refresh_is_not_reported_as_failure() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        economy.emissions.value = economyDeFrPl()
        testScheduler.advanceUntilIdle()
        val subscriptionsBefore = economy.observeCount

        economy.refreshThrows = CancellationException("cancelled")
        component.onIntent(CompareIntent.Refresh)
        testScheduler.advanceUntilIdle()

        // Cancellation is rethrown: observation is not restarted and no hint is raised.
        assertEquals(1, economy.refreshCount)
        assertEquals(subscriptionsBefore, economy.observeCount)
        val state = component.state.value
        assertFalse(state is CompareUiState.Content && state.refreshFailed)
    }

    @Test
    fun any_repository_failure_during_refresh_counts_as_failure() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        testScheduler.advanceUntilIdle()
        population.emissions.value = Result.Success(
            PopulationData(
                timeSeries = listOf(
                    PopulationTimeSeries("DE", "Germany", listOf(PopulationDataPoint("DE", 2023, 83_000_000L, null, null))),
                ),
                snapshots = emptyMap(),
            ),
        )
        component.onIntent(CompareIntent.SelectIndicator(CompareIndicator.POPULATION))
        testScheduler.advanceUntilIdle()

        population.refreshThrows = IllegalStateException("network down")
        component.onIntent(CompareIntent.Refresh)
        testScheduler.advanceUntilIdle()

        assertEquals(1, population.refreshCount)
        val content = assertIs<CompareUiState.Content>(component.state.value)
        assertEquals(CompareIndicator.POPULATION, content.indicator)
        assertTrue(content.refreshFailed)
    }

    @Test
    fun refresh_failed_clears_on_next_select_countries() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        failedRefreshContent(component)

        component.onIntent(CompareIntent.SelectCountries(listOf("FR", "ES")))
        testScheduler.advanceUntilIdle()

        val content = assertIs<CompareUiState.Content>(component.state.value)
        assertEquals(listOf("FR", "ES"), content.countries)
        assertFalse(content.refreshFailed)
    }

    @Test
    fun refresh_failed_clears_on_indicator_change() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        failedRefreshContent(component)
        population.emissions.value = Result.Success(
            PopulationData(
                timeSeries = listOf(
                    PopulationTimeSeries("DE", "Germany", listOf(PopulationDataPoint("DE", 2023, 83_000_000L, null, null))),
                ),
                snapshots = emptyMap(),
            ),
        )

        component.onIntent(CompareIntent.SelectIndicator(CompareIndicator.POPULATION))
        testScheduler.advanceUntilIdle()

        val content = assertIs<CompareUiState.Content>(component.state.value)
        assertEquals(CompareIndicator.POPULATION, content.indicator)
        assertFalse(content.refreshFailed)
    }

    @Test
    fun stale_emission_clears_refresh_failed_and_it_stays_cleared() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        failedRefreshContent(component)

        economy.emissions.value = Result.Success(economyDeFrPl().data, isStale = true)
        testScheduler.advanceUntilIdle()

        val stale = assertIs<CompareUiState.Content>(component.state.value)
        assertTrue(stale.isStale)
        assertFalse(stale.refreshFailed)

        // A later successful revalidation must not bring the hint back.
        economy.emissions.value = Result.Success(economyDeFrPl().data, isStale = false)
        testScheduler.advanceUntilIdle()

        val fresh = assertIs<CompareUiState.Content>(component.state.value)
        assertFalse(fresh.isStale)
        assertFalse(fresh.refreshFailed)
    }

    @Test
    fun toggle_normalization_keeps_refresh_failed() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        failedRefreshContent(component)
        val subscriptionsBefore = economy.observeCount

        component.onIntent(CompareIntent.ToggleNormalization)
        testScheduler.advanceUntilIdle()

        val content = assertIs<CompareUiState.Content>(component.state.value)
        assertTrue(content.normalized)
        assertTrue(content.refreshFailed)
        assertEquals(subscriptionsBefore, economy.observeCount)
    }
}
