package eu.eurostat.feature.social.data

import eu.eurostat.feature.social.domain.SocialDataPoint
import eu.eurostat.feature.social.domain.SocialQuery

interface SocialCacheDao {
    suspend fun query(query: SocialQuery): List<SocialDataPoint>
    suspend fun upsertAll(rows: List<SocialDataPoint>, fetchedAt: Long)
    suspend fun oldestFetchedAt(query: SocialQuery): Long?
}
