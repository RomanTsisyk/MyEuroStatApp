package eu.eurostat.feature.transport.data

import eu.eurostat.feature.transport.domain.TransportDataPoint
import eu.eurostat.feature.transport.domain.TransportQuery

class FakeTransportCacheDao : TransportCacheDao {
    private data class StoredRow(val point: TransportDataPoint, val fetchedAt: Long)

    private val store = mutableListOf<StoredRow>()

    fun seed(rows: List<TransportDataPoint>, fetchedAt: Long) {
        rows.forEach { store.add(StoredRow(it, fetchedAt)) }
    }

    override suspend fun query(query: TransportQuery): List<TransportDataPoint> =
        store
            .filter { row ->
                row.point.countryCode in query.countryCodes &&
                    row.point.year in query.yearRange &&
                    row.point.mode == query.mode
            }
            .map { it.point }

    override suspend fun upsertAll(rows: List<TransportDataPoint>, fetchedAt: Long) {
        rows.forEach { newRow ->
            store.removeAll { existing ->
                existing.point.countryCode == newRow.countryCode &&
                    existing.point.year == newRow.year &&
                    existing.point.mode == newRow.mode
            }
            store.add(StoredRow(newRow, fetchedAt))
        }
    }

    override suspend fun oldestFetchedAt(query: TransportQuery): Long? =
        store
            .filter { row ->
                row.point.countryCode in query.countryCodes &&
                    row.point.year in query.yearRange &&
                    row.point.mode == query.mode
            }
            .minOfOrNull { it.fetchedAt }
}
