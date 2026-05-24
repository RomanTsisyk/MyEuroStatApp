package eu.eurostat.feature.transport.data

import eu.eurostat.feature.transport.domain.TransportDataPoint
import eu.eurostat.feature.transport.domain.TransportQuery

interface TransportCacheDao {
    suspend fun query(query: TransportQuery): List<TransportDataPoint>
    suspend fun upsertAll(rows: List<TransportDataPoint>, fetchedAt: Long)
    suspend fun oldestFetchedAt(query: TransportQuery): Long?
}
