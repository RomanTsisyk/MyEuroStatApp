package eu.eurostat.feature.transport.data

import eu.eurostat.feature.transport.domain.TransportDataPoint
import eu.eurostat.feature.transport.domain.TransportQuery

class FakeTransportApiService : TransportApiService {
    var willReturn: List<TransportDataPoint> = emptyList()
    var throwable: Throwable? = null
    var callCount: Int = 0

    override suspend fun fetch(query: TransportQuery): List<TransportDataPoint> {
        callCount++
        throwable?.let { throw it }
        return willReturn
    }
}
