package eu.eurostat.feature.social.data

import eu.eurostat.feature.social.domain.SocialDataPoint
import eu.eurostat.feature.social.domain.SocialQuery
import kotlinx.datetime.Instant

class FakeSocialCacheDao : SocialCacheDao {
    private data class CacheKey(val countryCode: String, val year: Int)

    private val store = mutableMapOf<CacheKey, Pair<SocialDataPoint, Long>>()

    fun seed(rows: List<SocialDataPoint>, fetchedAt: Instant) {
        for (row in rows) {
            val key = CacheKey(row.countryCode, row.year)
            store[key] = row to fetchedAt.toEpochMilliseconds()
        }
    }

    override suspend fun query(query: SocialQuery): List<SocialDataPoint> {
        return store.values
            .filter { (point, _) ->
                point.countryCode in query.countryCodes && point.year in query.yearRange
            }
            .map { it.first }
    }

    override suspend fun upsertAll(rows: List<SocialDataPoint>, fetchedAt: Long) {
        for (row in rows) {
            val key = CacheKey(row.countryCode, row.year)
            store[key] = row to fetchedAt
        }
    }

    override suspend fun oldestFetchedAt(query: SocialQuery): Long? {
        return store.values
            .filter { (point, _) ->
                point.countryCode in query.countryCodes && point.year in query.yearRange
            }
            .minOfOrNull { it.second }
    }
}
