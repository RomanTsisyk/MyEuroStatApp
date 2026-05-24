package eu.eurostat.feature.environment.data

import eu.eurostat.feature.environment.domain.EnvironmentQuery
import eu.eurostat.feature.environment.domain.EnvironmentTimeSeries

/**
 * Network boundary for Environment data. Implementations fetch the three
 * underlying Eurostat datasets (`env_air_gge`, `nrg_bal_c`, `sdg_13_10`)
 * in parallel and return one merged time series per country.
 */
interface EnvironmentApiService {
    suspend fun fetch(query: EnvironmentQuery): List<EnvironmentTimeSeries>
}
