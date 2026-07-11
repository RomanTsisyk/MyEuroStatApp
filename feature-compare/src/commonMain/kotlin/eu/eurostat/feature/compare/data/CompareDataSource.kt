package eu.eurostat.feature.compare.data

import eu.eurostat.core.common.Result
import eu.eurostat.core.common.map
import eu.eurostat.feature.compare.domain.CompareIndicator
import eu.eurostat.feature.compare.domain.CompareSeries
import eu.eurostat.feature.compare.domain.CompareSeriesPoint
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Millions-of-EUR → billions-of-EUR conversion factor for currency indicators. */
private const val MILLIONS_PER_BILLION: Double = 1_000.0

/**
 * Adapts the eight feature repositories to the Compare screen's uniform
 * [CompareSeries] shape. Given a [CompareIndicator], it delegates to that
 * indicator's owning repository and maps the module-specific domain series into
 * one [CompareSeries] per country.
 *
 * This generalizes the per-module extractor lambdas in
 * `DefaultOverviewComponent` from "latest point" to "the full year/value
 * series", using the same field selections (e.g. environment keeps only
 * Total-sector GHG points). One deliberate divergence: the transport indicator
 * is strictly air passengers — the Overview teaser's air→road fallback is NOT
 * carried over, because on an overlaid multi-country chart labeled "Air
 * passengers" silently substituting bus/coach numbers would corrupt the
 * comparison; missing years stay null gaps instead. Values are converted to
 * each indicator's display unit here (e.g. GDP and exports are divided from
 * millions to billions of EUR) so the UI never re-derives units.
 *
 * Repository [Result] semantics are preserved verbatim: `Loading` and `Error`
 * pass through untouched (via [Result.map]); only `Success` payloads are mapped,
 * carrying [Result.Success.isStale] forward unchanged.
 */
class CompareDataSource(
    private val populationRepo: PopulationRepository,
    private val economyRepo: EconomyRepository,
    private val environmentRepo: EnvironmentRepository,
    private val tradeRepo: TradeRepository,
    private val transportRepo: TransportRepository,
    private val tourismRepo: TourismRepository,
    private val socialRepo: SocialRepository,
    private val scienceRepo: ScienceRepository,
) {

    /**
     * Observes the chosen [indicator] for the given [countries] over [years],
     * as a stream of [CompareSeries] (one per country that returned data).
     *
     * @param indicator which headline metric (and therefore which repository) to read.
     * @param countries Eurostat `geo` codes to include, in selection order.
     * @param years inclusive year range mapped to each repository query.
     */
    fun observe(
        indicator: CompareIndicator,
        countries: List<String>,
        years: IntRange,
    ): Flow<Result<List<CompareSeries>>> = when (indicator) {
        CompareIndicator.POPULATION ->
            populationRepo.observe(PopulationQuery(countries, years, includeCohorts = false))
                .mapResult { it.toPopulationSeries() }

        CompareIndicator.GDP ->
            economyRepo.observe(EconomyQuery(countries, years))
                .mapResult { it.toGdpSeries() }

        CompareIndicator.GHG ->
            environmentRepo.observe(EnvironmentQuery(countries, years))
                .mapResult { it.toGhgSeries() }

        CompareIndicator.EXPORTS ->
            tradeRepo.observe(TradeQuery(countries, years))
                .mapResult { it.toExportSeries() }

        CompareIndicator.AIR_PASSENGERS ->
            transportRepo.observe(TransportQuery(countries, years))
                .mapResult { it.toAirSeries() }

        CompareIndicator.TOURISM_NIGHTS ->
            tourismRepo.observe(TourismQuery(countries, years))
                .mapResult { it.toTourismSeries() }

        CompareIndicator.POVERTY_RATE ->
            socialRepo.observe(SocialQuery(countries, years))
                .mapResult { it.toPovertySeries() }

        CompareIndicator.RD_SPEND ->
            scienceRepo.observe(ScienceQuery(countries, years))
                .mapResult { it.toRdSeries() }
    }

    /**
     * Forces a network refetch of the chosen [indicator]'s dataset before the
     * next [observe] collection. The repositories are stale-while-revalidate:
     * a within-TTL cache is emitted and the flow completes without touching the
     * network, so a bare re-subscription would silently re-serve cached data —
     * this mirrors the per-feature screens' explicit `refresh(query)` step.
     * Network failures propagate to the caller (which surfaces them by
     * re-observing: the cache still satisfies the read path).
     */
    suspend fun refresh(
        indicator: CompareIndicator,
        countries: List<String>,
        years: IntRange,
    ) {
        when (indicator) {
            CompareIndicator.POPULATION ->
                populationRepo.refresh(PopulationQuery(countries, years, includeCohorts = false))
            CompareIndicator.GDP -> economyRepo.refresh(EconomyQuery(countries, years))
            CompareIndicator.GHG -> environmentRepo.refresh(EnvironmentQuery(countries, years))
            CompareIndicator.EXPORTS -> tradeRepo.refresh(TradeQuery(countries, years))
            CompareIndicator.AIR_PASSENGERS -> transportRepo.refresh(TransportQuery(countries, years))
            CompareIndicator.TOURISM_NIGHTS -> tourismRepo.refresh(TourismQuery(countries, years))
            CompareIndicator.POVERTY_RATE -> socialRepo.refresh(SocialQuery(countries, years))
            CompareIndicator.RD_SPEND -> scienceRepo.refresh(ScienceQuery(countries, years))
        }
    }
}

/**
 * Maps a `Flow<Result<T>>` into a `Flow<Result<List<CompareSeries>>>`,
 * preserving Loading/Error/isStale via [Result.map].
 */
private inline fun <T> Flow<Result<T>>.mapResult(
    crossinline transform: (T) -> List<CompareSeries>,
): Flow<Result<List<CompareSeries>>> = map { result -> result.map { transform(it) } }

// -- per-module extractors (full series, generalizing the Overview teasers) ----

private fun PopulationData.toPopulationSeries(): List<CompareSeries> =
    timeSeries.map { ts ->
        CompareSeries(
            countryCode = ts.countryCode,
            points = ts.points.map { CompareSeriesPoint(it.year, it.totalPopulation.toDouble()) },
        )
    }

private fun List<EconomyTimeSeries>.toGdpSeries(): List<CompareSeries> =
    map { ts ->
        CompareSeries(
            countryCode = ts.countryCode,
            // Null GDP years are kept as gaps; present values convert M€ → B€.
            points = ts.points.map { p ->
                CompareSeriesPoint(p.year, p.gdpEur?.let { it / MILLIONS_PER_BILLION })
            },
        )
    }

private fun List<EnvironmentTimeSeries>.toGhgSeries(): List<CompareSeries> =
    map { ts ->
        CompareSeries(
            countryCode = ts.countryCode,
            // Keep only Total-sector GHG rows: this excludes the Transport/Industry
            // GHG breakdowns and the Total-sector *energy* rows (which carry a null
            // ghgMtCo2eq), matching the Overview environment teaser's selection.
            points = ts.points
                .filter { it.sector == EnvSector.Total && it.ghgMtCo2eq != null }
                .map { CompareSeriesPoint(it.year, it.ghgMtCo2eq) },
        )
    }

private fun List<TradeTimeSeries>.toExportSeries(): List<CompareSeries> =
    map { ts ->
        CompareSeries(
            countryCode = ts.countryCode,
            // Null export years are kept as gaps; present values convert M€ → B€.
            points = ts.points.map { p ->
                CompareSeriesPoint(p.year, p.exportsEur?.let { it / MILLIONS_PER_BILLION })
            },
        )
    }

private fun List<TransportTimeSeries>.toAirSeries(): List<CompareSeries> =
    map { ts ->
        CompareSeries(
            countryCode = ts.countryCode,
            // Strictly air passengers — no road fallback (see class KDoc):
            // a year without air data stays a null gap.
            points = ts.points.map { p ->
                CompareSeriesPoint(p.year, p.airPassengers?.toDouble())
            },
        )
    }

private fun TourismData.toTourismSeries(): List<CompareSeries> =
    timeSeries.map { ts ->
        CompareSeries(
            countryCode = ts.countryCode,
            points = ts.points.map { CompareSeriesPoint(it.year, it.totalNights?.toDouble()) },
        )
    }

private fun List<SocialTimeSeries>.toPovertySeries(): List<CompareSeries> =
    map { ts ->
        CompareSeries(
            countryCode = ts.countryCode,
            points = ts.points.map { CompareSeriesPoint(it.year, it.povertyRate) },
        )
    }

private fun List<ScienceTimeSeries>.toRdSeries(): List<CompareSeries> =
    map { ts ->
        CompareSeries(
            countryCode = ts.countryCode,
            points = ts.points.map { CompareSeriesPoint(it.year, it.rdSpendPctGdp) },
        )
    }
