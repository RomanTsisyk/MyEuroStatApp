package eu.eurostat.feature.population.data

import eu.eurostat.feature.population.domain.PopulationQuery
import eu.eurostat.feature.population.domain.PopulationTimeSeries
import kotlinx.datetime.Instant

/** Cache-layer contract for population data. */
interface PopulationCacheDao {
    /** Returns cached series and the oldest fetched-at timestamp for the given query, or null if empty. */
    suspend fun query(query: PopulationQuery): CacheResult?

    /** Upserts all rows from the provided series with the given fetch timestamp. */
    suspend fun upsert(series: List<PopulationTimeSeries>, fetchedAt: Instant)
}

data class CacheResult(
    val series: List<PopulationTimeSeries>,
    val oldestFetchedAt: Instant,
)
