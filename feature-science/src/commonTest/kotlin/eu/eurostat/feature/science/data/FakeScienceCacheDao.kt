package eu.eurostat.feature.science.data

import eu.eurostat.feature.science.domain.ScienceDataPoint
import eu.eurostat.feature.science.domain.ScienceQuery
import kotlinx.datetime.Instant

class FakeScienceCacheDao : ScienceCacheDao {
    private data class CacheKey(val countryCode: String, val year: Int)

    private val store = mutableMapOf<CacheKey, Pair<ScienceDataPoint, Long>>()

    fun seed(rows: List<ScienceDataPoint>, fetchedAt: Instant) {
        for (row in rows) {
            val key = CacheKey(row.countryCode, row.year)
            store[key] = row to fetchedAt.toEpochMilliseconds()
        }
    }

    override suspend fun query(query: ScienceQuery): List<ScienceDataPoint> {
        return store.values
            .filter { (point, _) ->
                point.countryCode in query.countryCodes && point.year in query.yearRange
            }
            .map { it.first }
    }

    override suspend fun upsertAll(rows: List<ScienceDataPoint>, fetchedAt: Long) {
        for (row in rows) {
            val key = CacheKey(row.countryCode, row.year)
            store[key] = row to fetchedAt
        }
    }

    override suspend fun oldestFetchedAt(query: ScienceQuery): Long? {
        return store.values
            .filter { (point, _) ->
                point.countryCode in query.countryCodes && point.year in query.yearRange
            }
            .minOfOrNull { it.second }
    }
}
