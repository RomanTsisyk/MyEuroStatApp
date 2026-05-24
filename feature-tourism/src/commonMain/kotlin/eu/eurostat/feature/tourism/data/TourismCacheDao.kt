package eu.eurostat.feature.tourism.data

import eu.eurostat.feature.tourism.domain.TourismDataPoint
import eu.eurostat.feature.tourism.domain.TourismQuery

/**
 * Cache contract for the tourism feature. Returns wide-row data points
 * (domestic / foreign / total nights merged per country-year). Implementations
 * split storage across the three residence categories under the hood — that's
 * an implementation detail of the SQLDelight schema.
 */
interface TourismCacheDao {
    suspend fun query(query: TourismQuery): List<TourismDataPoint>
    suspend fun upsertAll(rows: List<TourismDataPoint>, fetchedAt: Long)
    suspend fun oldestFetchedAt(query: TourismQuery): Long?
}
