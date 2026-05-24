package eu.eurostat.feature.social.data

import eu.eurostat.feature.social.domain.SocialDataPoint
import eu.eurostat.feature.social.domain.SocialQuery

class FakeSocialApiService : SocialApiService {
    var willReturn: List<SocialDataPoint> = emptyList()
    var throwable: Throwable? = null
    var callCount = 0

    override suspend fun fetch(query: SocialQuery): List<SocialDataPoint> {
        callCount++
        throwable?.let { throw it }
        return willReturn
    }
}
