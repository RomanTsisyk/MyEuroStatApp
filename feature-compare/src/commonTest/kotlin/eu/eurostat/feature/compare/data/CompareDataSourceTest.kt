package eu.eurostat.feature.compare.data

import app.cash.turbine.test
import eu.eurostat.core.common.Result
import eu.eurostat.feature.compare.FakeEconomyRepository
import eu.eurostat.feature.compare.FakeEnvironmentRepository
import eu.eurostat.feature.compare.FakePopulationRepository
import eu.eurostat.feature.compare.FakeScienceRepository
import eu.eurostat.feature.compare.FakeSocialRepository
import eu.eurostat.feature.compare.FakeTourismRepository
import eu.eurostat.feature.compare.FakeTradeRepository
import eu.eurostat.feature.compare.FakeTransportRepository
import eu.eurostat.feature.compare.domain.CompareIndicator
import eu.eurostat.feature.compare.domain.CompareSeriesPoint
import eu.eurostat.feature.economy.domain.EconomyDataPoint
import eu.eurostat.feature.economy.domain.EconomyTimeSeries
import eu.eurostat.feature.environment.domain.EnvSector
import eu.eurostat.feature.environment.domain.EnvironmentDataPoint
import eu.eurostat.feature.environment.domain.EnvironmentTimeSeries
import eu.eurostat.feature.population.domain.PopulationData
import eu.eurostat.feature.population.domain.PopulationDataPoint
import eu.eurostat.feature.population.domain.PopulationTimeSeries
import eu.eurostat.feature.transport.domain.TransportDataPoint
import eu.eurostat.feature.transport.domain.TransportMode
import eu.eurostat.feature.transport.domain.TransportTimeSeries
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies [CompareDataSource]'s per-module mapping: correct field selection,
 * unit conversion, gap preservation and sector/fallback handling. Repository
 * [Result] semantics (Loading passes through) are exercised by consuming the
 * fakes' initial [Result.Loading] before pushing data.
 */
class CompareDataSourceTest {

    private val years = 2010..2024

    private val population = FakePopulationRepository()
    private val economy = FakeEconomyRepository()
    private val environment = FakeEnvironmentRepository()
    private val trade = FakeTradeRepository()
    private val transport = FakeTransportRepository()
    private val tourism = FakeTourismRepository()
    private val social = FakeSocialRepository()
    private val science = FakeScienceRepository()

    private val dataSource = CompareDataSource(
        population, economy, environment, trade,
        transport, tourism, social, science,
    )

    @Test
    fun population_maps_total_population_and_passes_loading_through() = runTest {
        dataSource.observe(CompareIndicator.POPULATION, listOf("DE"), years).test {
            // Loading passes through untouched.
            assertEquals(Result.Loading, awaitItem())

            population.emissions.value = Result.Success(
                PopulationData(
                    timeSeries = listOf(
                        PopulationTimeSeries(
                            "DE", "Germany",
                            listOf(
                                PopulationDataPoint("DE", 2020, 83_000_000L, null, null),
                                PopulationDataPoint("DE", 2021, 83_200_000L, null, null),
                            ),
                        ),
                    ),
                    snapshots = emptyMap(),
                ),
            )

            val series = (awaitItem() as Result.Success).data
            assertEquals(1, series.size)
            assertEquals("DE", series[0].countryCode)
            assertEquals(
                listOf(
                    CompareSeriesPoint(2020, 83_000_000.0),
                    CompareSeriesPoint(2021, 83_200_000.0),
                ),
                series[0].points,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun economy_converts_millions_to_billions_and_preserves_gaps() = runTest {
        dataSource.observe(CompareIndicator.GDP, listOf("DE"), years).test {
            assertEquals(Result.Loading, awaitItem())

            economy.emissions.value = Result.Success(
                listOf(
                    EconomyTimeSeries(
                        "DE", "Germany",
                        listOf(
                            EconomyDataPoint("DE", 2020, gdpEur = 3_000_000L),
                            EconomyDataPoint("DE", 2021, gdpEur = null), // gap preserved
                            EconomyDataPoint("DE", 2022, gdpEur = 3_400_000L),
                        ),
                    ),
                ),
            )

            val series = (awaitItem() as Result.Success).data
            assertEquals(
                listOf(
                    CompareSeriesPoint(2020, 3_000.0),
                    CompareSeriesPoint(2021, null),
                    CompareSeriesPoint(2022, 3_400.0),
                ),
                series[0].points,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun environment_keeps_only_total_sector_ghg_points() = runTest {
        dataSource.observe(CompareIndicator.GHG, listOf("DE"), years).test {
            assertEquals(Result.Loading, awaitItem())

            environment.emissions.value = Result.Success(
                listOf(
                    EnvironmentTimeSeries(
                        "DE", "Germany",
                        listOf(
                            EnvironmentDataPoint("DE", 2020, EnvSector.Total, ghgMtCo2eq = 700.0),
                            // Transport-sector GHG — excluded.
                            EnvironmentDataPoint("DE", 2020, EnvSector.Transport, ghgMtCo2eq = 160.0),
                            // Total-sector energy (ghg null) — excluded.
                            EnvironmentDataPoint("DE", 2020, EnvSector.Total, energyKtoe = 8_000.0),
                            EnvironmentDataPoint("DE", 2021, EnvSector.Total, ghgMtCo2eq = 680.0),
                            // SDG index (sector null) — excluded.
                            EnvironmentDataPoint("DE", 2019, null, sdg13Index = 90.0),
                        ),
                    ),
                ),
            )

            val series = (awaitItem() as Result.Success).data
            assertEquals(
                listOf(
                    CompareSeriesPoint(2020, 700.0),
                    CompareSeriesPoint(2021, 680.0),
                ),
                series[0].points,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun transport_is_strictly_air_and_keeps_missing_years_as_gaps() = runTest {
        dataSource.observe(CompareIndicator.AIR_PASSENGERS, listOf("DE"), years).test {
            assertEquals(Result.Loading, awaitItem())

            transport.emissions.value = Result.Success(
                listOf(
                    TransportTimeSeries(
                        "DE", "Germany", TransportMode.ALL,
                        listOf(
                            TransportDataPoint("DE", 2020, TransportMode.ALL, roadPassengers = 5_000L, airPassengers = 200_000_000L, seaPassengers = null),
                            // Air absent -> gap. Road numbers must NOT leak into a
                            // chart labeled "Air passengers" (unlike the Overview
                            // teaser, which may fall back for its single stat).
                            TransportDataPoint("DE", 2021, TransportMode.ALL, roadPassengers = 6_000L, airPassengers = null, seaPassengers = null),
                            TransportDataPoint("DE", 2022, TransportMode.ALL, roadPassengers = null, airPassengers = null, seaPassengers = null),
                        ),
                    ),
                ),
            )

            val series = (awaitItem() as Result.Success).data
            assertEquals(
                listOf(
                    CompareSeriesPoint(2020, 200_000_000.0),
                    CompareSeriesPoint(2021, null),
                    CompareSeriesPoint(2022, null),
                ),
                series[0].points,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refresh_delegates_to_the_selected_indicators_repository() = runTest {
        dataSource.refresh(CompareIndicator.GDP, listOf("DE", "FR"), years)
        assertEquals(1, economy.refreshCount)
        assertEquals(0, transport.refreshCount)

        dataSource.refresh(CompareIndicator.AIR_PASSENGERS, listOf("DE"), years)
        assertEquals(1, transport.refreshCount)
        assertEquals(1, economy.refreshCount)
    }

    @Test
    fun error_passes_through_unchanged() = runTest {
        dataSource.observe(CompareIndicator.GDP, listOf("DE"), years).test {
            assertEquals(Result.Loading, awaitItem())
            economy.emissions.value = Result.Error(eu.eurostat.core.common.AppError.NoNetwork)
            assertEquals(Result.Error(eu.eurostat.core.common.AppError.NoNetwork), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
