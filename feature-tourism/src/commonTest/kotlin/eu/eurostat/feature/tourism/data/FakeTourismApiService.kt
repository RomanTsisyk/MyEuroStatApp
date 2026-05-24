package eu.eurostat.feature.tourism.data

import eu.eurostat.core.jsonstat.JsonStatCell
import eu.eurostat.feature.tourism.domain.TourismDataPoint
import eu.eurostat.feature.tourism.domain.TourismQuery

class FakeTourismApiService : TourismApiService {
    var willReturn: List<TourismDataPoint> = emptyList()
    var willReturnLabels: Map<String, String> = emptyMap()
    var throwable: Throwable? = null
    var callCount = 0

    override suspend fun fetch(query: TourismQuery): TourismFetchResult {
        callCount++
        throwable?.let { throw it }
        return TourismFetchResult(points = willReturn, countryLabels = willReturnLabels)
    }

    override suspend fun fetchSeasonality(countryCode: String): List<JsonStatCell> = emptyList()
}
