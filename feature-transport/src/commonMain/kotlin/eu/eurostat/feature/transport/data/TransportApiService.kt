package eu.eurostat.feature.transport.data

import eu.eurostat.feature.transport.domain.TransportDataPoint
import eu.eurostat.feature.transport.domain.TransportQuery

interface TransportApiService {
    suspend fun fetch(query: TransportQuery): List<TransportDataPoint>
}
