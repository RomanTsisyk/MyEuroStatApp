package eu.eurostat.feature.economy.data

import eu.eurostat.feature.economy.domain.EconomyQuery
import eu.eurostat.feature.economy.domain.EconomyTimeSeries
import kotlinx.datetime.Instant

interface EconomyCacheDao {
    /** Returns cached series and the oldest fetched-at timestamp for the given query, or null if empty. */
    suspend fun query(query: EconomyQuery): EconomyCacheResult?

    /** Upserts all rows from the provided series with the given fetch timestamp. */
    suspend fun upsert(series: List<EconomyTimeSeries>, fetchedAt: Instant)
}

data class EconomyCacheResult(
    val series: List<EconomyTimeSeries>,
    val oldestFetchedAt: Instant,
)
