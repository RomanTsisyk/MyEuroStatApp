package eu.eurostat.feature.science.data

import eu.eurostat.feature.science.domain.ScienceDataPoint
import eu.eurostat.feature.science.domain.ScienceQuery

class FakeScienceApiService : ScienceApiService {
    var willReturn: List<ScienceDataPoint> = emptyList()
    var throwable: Throwable? = null
    var callCount = 0

    override suspend fun fetch(query: ScienceQuery): List<ScienceDataPoint> {
        callCount++
        throwable?.let { throw it }
        return willReturn
    }
}
