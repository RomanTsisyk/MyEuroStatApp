package eu.eurostat.feature.overview.ui

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import eu.eurostat.core.common.AppError
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.navigation.ChildConfig
import eu.eurostat.feature.economy.domain.EconomyDataPoint
import eu.eurostat.feature.economy.domain.EconomyQuery
import eu.eurostat.feature.economy.domain.EconomyRepository
import eu.eurostat.feature.economy.domain.EconomyTimeSeries
import eu.eurostat.feature.environment.domain.EnvSector
import eu.eurostat.feature.environment.domain.EnvironmentDataPoint
import eu.eurostat.feature.environment.domain.EnvironmentQuery
import eu.eurostat.feature.environment.domain.EnvironmentRepository
import eu.eurostat.feature.environment.domain.EnvironmentTimeSeries
import eu.eurostat.feature.population.domain.PopulationData
import eu.eurostat.feature.population.domain.PopulationDataPoint
import eu.eurostat.feature.population.domain.PopulationQuery
import eu.eurostat.feature.population.domain.PopulationRepository
import eu.eurostat.feature.population.domain.PopulationTimeSeries
import eu.eurostat.feature.science.domain.ScienceDataPoint
import eu.eurostat.feature.science.domain.ScienceQuery
import eu.eurostat.feature.science.domain.ScienceRepository
import eu.eurostat.feature.science.domain.ScienceTimeSeries
import eu.eurostat.feature.social.domain.SocialDataPoint
import eu.eurostat.feature.social.domain.SocialQuery
import eu.eurostat.feature.social.domain.SocialRepository
import eu.eurostat.feature.social.domain.SocialTimeSeries
import eu.eurostat.feature.tourism.domain.TourismData
import eu.eurostat.feature.tourism.domain.TourismDataPoint
import eu.eurostat.feature.tourism.domain.TourismQuery
import eu.eurostat.feature.tourism.domain.TourismRepository
import eu.eurostat.feature.tourism.domain.TourismTimeSeries
import eu.eurostat.feature.trade.domain.TradeDataPoint
import eu.eurostat.feature.trade.domain.TradeQuery
import eu.eurostat.feature.trade.domain.TradeRepository
import eu.eurostat.feature.trade.domain.TradeTimeSeries
import eu.eurostat.feature.transport.domain.TransportDataPoint
import eu.eurostat.feature.transport.domain.TransportMode
import eu.eurostat.feature.transport.domain.TransportQuery
import eu.eurostat.feature.transport.domain.TransportRepository
import eu.eurostat.feature.transport.domain.TransportTimeSeries
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
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// Fake repositories — each exposes a MutableStateFlow the test drives.
// ---------------------------------------------------------------------------

private class FakePopulationRepository : PopulationRepository {
    val emissions = MutableStateFlow<Result<PopulationData>>(Result.Loading)
    override fun observe(query: PopulationQuery): Flow<Result<PopulationData>> = emissions
    override suspend fun refresh(query: PopulationQuery) {}
}

private class FakeEconomyRepository : EconomyRepository {
    val emissions = MutableStateFlow<Result<List<EconomyTimeSeries>>>(Result.Loading)
    override fun observe(query: EconomyQuery): Flow<Result<List<EconomyTimeSeries>>> = emissions
    override suspend fun refresh(query: EconomyQuery) {}
}

private class FakeEnvironmentRepository : EnvironmentRepository {
    val emissions = MutableStateFlow<Result<List<EnvironmentTimeSeries>>>(Result.Loading)
    override fun observe(query: EnvironmentQuery): Flow<Result<List<EnvironmentTimeSeries>>> = emissions
    override suspend fun refresh(query: EnvironmentQuery) {}
}

private class FakeTradeRepository : TradeRepository {
    val emissions = MutableStateFlow<Result<List<TradeTimeSeries>>>(Result.Loading)
    override fun observe(query: TradeQuery): Flow<Result<List<TradeTimeSeries>>> = emissions
    override suspend fun refresh(query: TradeQuery) {}
}

private class FakeTransportRepository : TransportRepository {
    val emissions = MutableStateFlow<Result<List<TransportTimeSeries>>>(Result.Loading)
    override fun observe(query: TransportQuery): Flow<Result<List<TransportTimeSeries>>> = emissions
    override suspend fun refresh(query: TransportQuery) {}
}

private class FakeTourismRepository : TourismRepository {
    val emissions = MutableStateFlow<Result<TourismData>>(Result.Loading)
    override fun observe(query: TourismQuery): Flow<Result<TourismData>> = emissions
    override suspend fun refresh(query: TourismQuery) {}
}

private class FakeSocialRepository : SocialRepository {
    val emissions = MutableStateFlow<Result<List<SocialTimeSeries>>>(Result.Loading)
    override fun observe(query: SocialQuery): Flow<Result<List<SocialTimeSeries>>> = emissions
    override suspend fun refresh(query: SocialQuery) {}
}

private class FakeScienceRepository : ScienceRepository {
    val emissions = MutableStateFlow<Result<List<ScienceTimeSeries>>>(Result.Loading)
    override fun observe(query: ScienceQuery): Flow<Result<List<ScienceTimeSeries>>> = emissions
    override suspend fun refresh(query: ScienceQuery) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
private class TestDispatchers(dispatcher: TestDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

// ---------------------------------------------------------------------------
// Sample data
// ---------------------------------------------------------------------------

private fun economyDe(gdpMEur: Long = 4_500_000L, year: Int = 2023) = listOf(
    EconomyTimeSeries("DE", "Germany", listOf(EconomyDataPoint("DE", year, gdpEur = gdpMEur))),
)

private fun populationDe(total: Long = 83_000_000L, year: Int = 2023) = PopulationData(
    timeSeries = listOf(
        PopulationTimeSeries("DE", "Germany", listOf(PopulationDataPoint("DE", year, total, null, null))),
    ),
    snapshots = emptyMap(),
)

private fun tourismDe(nights: Long = 400_000_000L, year: Int = 2023) = TourismData(
    timeSeries = listOf(
        TourismTimeSeries("DE", "Germany", listOf(TourismDataPoint("DE", year, totalNights = nights))),
    ),
    heatmapCells = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewComponentTest {

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

    private fun build(dispatcher: TestDispatcher) = DefaultOverviewComponent(
        context,
        population, economy, environment, trade,
        transport, tourism, social, science,
        TestDispatchers(dispatcher),
    )

    private fun OverviewUiState.teaser(destination: ChildConfig): ModuleTeaser =
        teasers.first { it.destination == destination }

    @Test
    fun exposes_eight_teasers_all_loading_before_any_data() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertEquals(8, state.teasers.size)
        assertTrue(state.teasers.all { it.status == TeaserStatus.Loading })
    }

    @Test
    fun economy_success_populates_gdp_teaser_wiring() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        economy.emissions.value = Result.Success(economyDe(gdpMEur = 4_500_000L, year = 2023))
        testScheduler.advanceUntilIdle()

        val teaser = component.state.value.teaser(ChildConfig.Economy)
        assertEquals(TeaserStatus.Loaded, teaser.status)
        assertEquals(2023, teaser.year)
        assertEquals("B € · GDP", teaser.unit)
        assertTrue(teaser.value != "—" && teaser.value.isNotBlank(), "value=${teaser.value}")
    }

    @Test
    fun percentage_teasers_render_a_percent_suffix() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        social.emissions.value = Result.Success(
            listOf(SocialTimeSeries("DE", "Germany", listOf(SocialDataPoint("DE", 2023, povertyRate = 14.4)))),
        )
        science.emissions.value = Result.Success(
            listOf(ScienceTimeSeries("DE", "Germany", listOf(ScienceDataPoint("DE", 2023, rdSpendPctGdp = 3.1)))),
        )
        testScheduler.advanceUntilIdle()

        assertTrue(component.state.value.teaser(ChildConfig.Social).value.endsWith("%"))
        assertTrue(component.state.value.teaser(ChildConfig.Science).value.endsWith("%"))
    }

    @Test
    fun population_and_tourism_wrapper_types_map_to_loaded_teasers() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        population.emissions.value = Result.Success(populationDe())
        tourism.emissions.value = Result.Success(tourismDe())
        testScheduler.advanceUntilIdle()

        val pop = component.state.value.teaser(ChildConfig.Population)
        assertEquals(TeaserStatus.Loaded, pop.status)
        assertEquals("people", pop.unit)
        assertTrue(pop.value != "—")

        val tour = component.state.value.teaser(ChildConfig.Tourism)
        assertEquals(TeaserStatus.Loaded, tour.status)
        assertEquals("nights", tour.unit)
    }

    @Test
    fun other_teasers_map_headline_fields() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        environment.emissions.value = Result.Success(
            listOf(EnvironmentTimeSeries("DE", "Germany", listOf(
                EnvironmentDataPoint("DE", 2022, EnvSector.Total, ghgMtCo2eq = 656.0),
            ))),
        )
        trade.emissions.value = Result.Success(
            listOf(TradeTimeSeries("DE", "Germany", "EU27_2020", listOf(
                TradeDataPoint("DE", 2023, "EU27_2020", exportsEur = 1_600_000L, importsEur = null, balanceEur = null),
            ))),
        )
        transport.emissions.value = Result.Success(
            listOf(TransportTimeSeries("DE", "Germany", TransportMode.ALL, listOf(
                TransportDataPoint("DE", 2022, TransportMode.ALL, roadPassengers = null, airPassengers = 200_000_000L, seaPassengers = null),
            ))),
        )
        testScheduler.advanceUntilIdle()

        assertEquals(TeaserStatus.Loaded, component.state.value.teaser(ChildConfig.Environment).status)
        assertEquals(TeaserStatus.Loaded, component.state.value.teaser(ChildConfig.Trade).status)
        assertEquals(TeaserStatus.Loaded, component.state.value.teaser(ChildConfig.Transport).status)
    }

    @Test
    fun a_repository_error_degrades_only_its_own_teaser() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        trade.emissions.value = Result.Error(AppError.NoNetwork)
        economy.emissions.value = Result.Success(economyDe())
        testScheduler.advanceUntilIdle()

        assertEquals(TeaserStatus.Error, component.state.value.teaser(ChildConfig.Trade).status)
        assertEquals(TeaserStatus.Loaded, component.state.value.teaser(ChildConfig.Economy).status)
        // Untouched repositories keep loading — the dashboard as a whole never errors.
        assertEquals(TeaserStatus.Loading, component.state.value.teaser(ChildConfig.Social).status)
    }

    @Test
    fun empty_success_marks_teaser_empty() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        economy.emissions.value = Result.Success(emptyList())
        testScheduler.advanceUntilIdle()

        val teaser = component.state.value.teaser(ChildConfig.Economy)
        assertEquals(TeaserStatus.Empty, teaser.status)
        assertEquals("—", teaser.value)
    }

    @Test
    fun falls_back_to_first_country_when_default_absent() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        economy.emissions.value = Result.Success(
            listOf(EconomyTimeSeries("FR", "France", listOf(EconomyDataPoint("FR", 2023, gdpEur = 2_800_000L)))),
        )
        testScheduler.advanceUntilIdle()

        val teaser = component.state.value.teaser(ChildConfig.Economy)
        assertEquals(TeaserStatus.Loaded, teaser.status)
        assertTrue(teaser.value != "—")
    }

    @Test
    fun hero_is_the_economy_teaser() = runTest {
        val component = build(StandardTestDispatcher(testScheduler))
        economy.emissions.value = Result.Success(economyDe())
        testScheduler.advanceUntilIdle()

        assertEquals(ChildConfig.Economy, component.state.value.hero?.destination)
    }
}
