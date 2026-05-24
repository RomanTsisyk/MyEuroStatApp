package eu.eurostat.feature.population.data

import eu.eurostat.feature.population.domain.PopulationData
import eu.eurostat.feature.population.domain.PopulationQuery

/** Network-layer contract for population data. */
interface PopulationApiService {
    /**
     * Fetches the population dataset (`demo_pjan`).
     *
     * Returns both the trend (per-country time series for `age=TOTAL`) and the
     * per-(country, year) cohort snapshots, the latter populated only when
     * [PopulationQuery.includeCohorts] is `true`.
     */
    suspend fun fetchPopulation(query: PopulationQuery): PopulationData
}
