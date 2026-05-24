package eu.eurostat.feature.social.data

import eu.eurostat.feature.social.domain.SocialDataPoint
import eu.eurostat.feature.social.domain.SocialQuery

interface SocialApiService {
    suspend fun fetch(query: SocialQuery): List<SocialDataPoint>
}
