package eu.eurostat.feature.tourism.data

import eu.eurostat.feature.tourism.domain.TourismDataPoint
import eu.eurostat.feature.tourism.domain.TourismQuery
import kotlinx.datetime.Instant

class FakeTourismCacheDao : TourismCacheDao {
    private data class CacheKey(val countryCode: String, val year: Int)

    private val store = mutableMapOf<CacheKey, Pair<TourismDataPoint, Long>>()

    fun seed(rows: List<TourismDataPoint>, fetchedAt: Instant) {
        for (row in rows) {
            store[CacheKey(row.countryCode, row.year)] = row to fetchedAt.toEpochMilliseconds()
        }
    }

    override suspend fun query(query: TourismQuery): List<TourismDataPoint> {
        return store.values
            .filter { (point, _) ->
                point.countryCode in query.countryCodes && point.year in query.yearRange
            }
            .map { it.first }
    }

    override suspend fun upsertAll(rows: List<TourismDataPoint>, fetchedAt: Long) {
        for (row in rows) {
            store[CacheKey(row.countryCode, row.year)] = row to fetchedAt
        }
    }

    override suspend fun oldestFetchedAt(query: TourismQuery): Long? {
        return store.values
            .filter { (point, _) ->
                point.countryCode in query.countryCodes && point.year in query.yearRange
            }
            .minOfOrNull { it.second }
    }
}
