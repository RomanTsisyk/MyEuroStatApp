package eu.eurostat.feature.overview.ui

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.common.formatCompactNumber
import eu.eurostat.core.common.formatGrouped
import eu.eurostat.core.common.formatPercent
import eu.eurostat.core.navigation.ChildConfig
import eu.eurostat.feature.economy.domain.EconomyQuery
import eu.eurostat.feature.economy.domain.EconomyRepository
import eu.eurostat.feature.economy.domain.EconomyTimeSeries
import eu.eurostat.feature.environment.domain.EnvSector
import eu.eurostat.feature.environment.domain.EnvironmentQuery
import eu.eurostat.feature.environment.domain.EnvironmentRepository
import eu.eurostat.feature.environment.domain.EnvironmentTimeSeries
import eu.eurostat.feature.population.domain.PopulationData
import eu.eurostat.feature.population.domain.PopulationQuery
import eu.eurostat.feature.population.domain.PopulationRepository
import eu.eurostat.feature.science.domain.ScienceQuery
import eu.eurostat.feature.science.domain.ScienceRepository
import eu.eurostat.feature.science.domain.ScienceTimeSeries
import eu.eurostat.feature.social.domain.SocialQuery
import eu.eurostat.feature.social.domain.SocialRepository
import eu.eurostat.feature.social.domain.SocialTimeSeries
import eu.eurostat.feature.tourism.domain.TourismData
import eu.eurostat.feature.tourism.domain.TourismQuery
import eu.eurostat.feature.tourism.domain.TourismRepository
import eu.eurostat.feature.trade.domain.TradeQuery
import eu.eurostat.feature.trade.domain.TradeRepository
import eu.eurostat.feature.trade.domain.TradeTimeSeries
import eu.eurostat.feature.transport.domain.TransportQuery
import eu.eurostat.feature.transport.domain.TransportRepository
import eu.eurostat.feature.transport.domain.TransportTimeSeries
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Landing-screen component. Aggregates one at-a-glance teaser metric per feature
 * for the default country ([DEFAULT_COUNTRY]) by observing all eight feature
 * repositories concurrently and merging their [Result] streams into a single
 * [OverviewUiState].
 *
 * Each teaser degrades independently: a broken or empty dataset yields an
 * [TeaserStatus.Error] / [TeaserStatus.Empty] tile rather than failing the whole
 * dashboard, so the landing screen always renders.
 */
interface OverviewComponent {
    val state: StateFlow<OverviewUiState>

    /** Re-observe every feature repository (re-checks cache freshness). */
    fun onRefresh()
}

class DefaultOverviewComponent(
    componentContext: ComponentContext,
    private val populationRepo: PopulationRepository,
    private val economyRepo: EconomyRepository,
    private val environmentRepo: EnvironmentRepository,
    private val tradeRepo: TradeRepository,
    private val transportRepo: TransportRepository,
    private val tourismRepo: TourismRepository,
    private val socialRepo: SocialRepository,
    private val scienceRepo: ScienceRepository,
    private val dispatchers: DispatcherProvider,
) : OverviewComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(SupervisorJob() + dispatchers.main)

    private val _state = MutableStateFlow(OverviewUiState(BASE_TEASERS))
    override val state: StateFlow<OverviewUiState> = _state.asStateFlow()

    private val countries = listOf("EU27_2020", DEFAULT_COUNTRY, "FR", "PL")
    private val years = 2010..2024

    private var collectJob: Job? = null

    init {
        start()
    }

    override fun onRefresh() = start()

    private fun start() {
        collectJob?.cancel()
        collectJob = scope.launch {
            combine(
                populationRepo.observe(PopulationQuery(countries, years, includeCohorts = false))
                    .map { it.toPopulationTeaser() },
                economyRepo.observe(EconomyQuery(countries, years)).map { it.toEconomyTeaser() },
                environmentRepo.observe(EnvironmentQuery(countries, years)).map { it.toEnvironmentTeaser() },
                tradeRepo.observe(TradeQuery(countries, years)).map { it.toTradeTeaser() },
                transportRepo.observe(TransportQuery(countries, years)).map { it.toTransportTeaser() },
                tourismRepo.observe(TourismQuery(countries, years)).map { it.toTourismTeaser() },
                socialRepo.observe(SocialQuery(countries, years)).map { it.toSocialTeaser() },
                scienceRepo.observe(ScienceQuery(countries, years)).map { it.toScienceTeaser() },
            ) { teasers -> OverviewUiState(teasers.toList()) }
                .collect { _state.value = it }
        }
    }

    // -- Result -> teaser plumbing --------------------------------------------

    private data class Headline(val value: String, val unit: String, val year: Int?)

    private fun <T> Result<T>.teaser(base: ModuleTeaser, extract: (T) -> Headline?): ModuleTeaser =
        when (this) {
            is Result.Loading -> base.copy(status = TeaserStatus.Loading, value = "—")
            is Result.Success -> extract(data)?.let {
                base.copy(status = TeaserStatus.Loaded, value = it.value, unit = it.unit, year = it.year)
            } ?: base.copy(status = TeaserStatus.Empty, value = "—")
            is Result.Error -> base.copy(status = TeaserStatus.Error, value = "—")
        }

    private fun Result<PopulationData>.toPopulationTeaser() = teaser(BASE_POPULATION) { d ->
        val series = d.timeSeries.pickDefault { it.countryCode } ?: return@teaser null
        val point = series.points.lastOrNull() ?: return@teaser null
        Headline(formatCompactNumber(point.totalPopulation.toDouble()), "people", point.year)
    }

    private fun Result<List<EconomyTimeSeries>>.toEconomyTeaser() = teaser(BASE_ECONOMY) { d ->
        val series = d.pickDefault { it.countryCode } ?: return@teaser null
        val point = series.points.lastOrNull { it.gdpEur != null } ?: return@teaser null
        val gdp = point.gdpEur ?: return@teaser null
        Headline(formatGrouped(gdp / 1000.0), "B € · GDP", point.year)
    }

    private fun Result<List<EnvironmentTimeSeries>>.toEnvironmentTeaser() = teaser(BASE_ENVIRONMENT) { d ->
        val series = d.pickDefault { it.countryCode } ?: return@teaser null
        val point = series.points.lastOrNull { it.sector == EnvSector.Total && it.ghgMtCo2eq != null }
            ?: return@teaser null
        val ghg = point.ghgMtCo2eq ?: return@teaser null
        Headline(formatGrouped(ghg), "Mt CO₂e", point.year)
    }

    private fun Result<List<TradeTimeSeries>>.toTradeTeaser() = teaser(BASE_TRADE) { d ->
        val series = d.pickDefault { it.countryCode } ?: return@teaser null
        val point = series.points.lastOrNull { it.exportsEur != null } ?: return@teaser null
        val exports = point.exportsEur ?: return@teaser null
        Headline(formatGrouped(exports / 1000.0), "B € · exports", point.year)
    }

    private fun Result<List<TransportTimeSeries>>.toTransportTeaser() = teaser(BASE_TRANSPORT) { d ->
        val series = d.pickDefault { it.countryCode } ?: return@teaser null
        val point = series.points.lastOrNull { it.airPassengers != null || it.roadPassengers != null }
            ?: return@teaser null
        val passengers = point.airPassengers ?: point.roadPassengers ?: return@teaser null
        Headline(formatCompactNumber(passengers.toDouble()), "passengers", point.year)
    }

    private fun Result<TourismData>.toTourismTeaser() = teaser(BASE_TOURISM) { d ->
        val series = d.timeSeries.pickDefault { it.countryCode } ?: return@teaser null
        val point = series.points.lastOrNull { it.totalNights != null } ?: return@teaser null
        val nights = point.totalNights ?: return@teaser null
        Headline(formatCompactNumber(nights.toDouble()), "nights", point.year)
    }

    private fun Result<List<SocialTimeSeries>>.toSocialTeaser() = teaser(BASE_SOCIAL) { d ->
        val series = d.pickDefault { it.countryCode } ?: return@teaser null
        val point = series.points.lastOrNull { it.povertyRate != null } ?: return@teaser null
        val rate = point.povertyRate ?: return@teaser null
        Headline(formatPercent(rate, 1), "at-risk-of-poverty", point.year)
    }

    private fun Result<List<ScienceTimeSeries>>.toScienceTeaser() = teaser(BASE_SCIENCE) { d ->
        val series = d.pickDefault { it.countryCode } ?: return@teaser null
        val point = series.points.lastOrNull { it.rdSpendPctGdp != null } ?: return@teaser null
        val rd = point.rdSpendPctGdp ?: return@teaser null
        Headline(formatPercent(rd, 1), "R&D of GDP", point.year)
    }

    /** Default country's series, falling back to the first available. */
    private inline fun <S> List<S>.pickDefault(code: (S) -> String): S? =
        firstOrNull { code(it) == DEFAULT_COUNTRY } ?: firstOrNull()

    companion object {
        /** Country whose latest values the dashboard headlines (matches feature defaults). */
        const val DEFAULT_COUNTRY = "DE"

        private val BASE_POPULATION = ModuleTeaser(ChildConfig.Population, "population", "Population", "👥", "—", "people", null, TeaserStatus.Loading)
        private val BASE_ECONOMY = ModuleTeaser(ChildConfig.Economy, "economy", "Economy", "💶", "—", "B € · GDP", null, TeaserStatus.Loading)
        private val BASE_ENVIRONMENT = ModuleTeaser(ChildConfig.Environment, "environment", "Environment", "🌍", "—", "Mt CO₂e", null, TeaserStatus.Loading)
        private val BASE_TRADE = ModuleTeaser(ChildConfig.Trade, "trade", "Trade", "📦", "—", "B € · exports", null, TeaserStatus.Loading)
        private val BASE_TRANSPORT = ModuleTeaser(ChildConfig.Transport, "transport", "Transport", "🚆", "—", "passengers", null, TeaserStatus.Loading)
        private val BASE_TOURISM = ModuleTeaser(ChildConfig.Tourism, "tourism", "Tourism", "🏨", "—", "nights", null, TeaserStatus.Loading)
        private val BASE_SOCIAL = ModuleTeaser(ChildConfig.Social, "social", "Social", "🤝", "—", "at-risk-of-poverty", null, TeaserStatus.Loading)
        private val BASE_SCIENCE = ModuleTeaser(ChildConfig.Science, "science", "Science", "🔬", "—", "R&D of GDP", null, TeaserStatus.Loading)

        /** Initial teasers in display order (also the [combine] emission order). */
        private val BASE_TEASERS = listOf(
            BASE_POPULATION, BASE_ECONOMY, BASE_ENVIRONMENT, BASE_TRADE,
            BASE_TRANSPORT, BASE_TOURISM, BASE_SOCIAL, BASE_SCIENCE,
        )
    }
}
