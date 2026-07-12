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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
