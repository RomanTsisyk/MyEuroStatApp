package eu.eurostat.feature.trade.data

import eu.eurostat.feature.trade.domain.TradeDataPoint
import eu.eurostat.feature.trade.domain.TradeQuery

class FakeTradeCacheDao : TradeCacheDao {
    private data class StoredRow(val point: TradeDataPoint, val fetchedAt: Long)

    private val store = mutableListOf<StoredRow>()

    fun seed(rows: List<TradeDataPoint>, fetchedAt: Long) {
        rows.forEach { store.add(StoredRow(it, fetchedAt)) }
    }

    override suspend fun query(query: TradeQuery): List<TradeDataPoint> =
        store
            .filter { row ->
                row.point.countryCode in query.countryCodes &&
                    row.point.year in query.yearRange &&
                    row.point.partner == query.partner
            }
            .map { it.point }

    override suspend fun upsertAll(rows: List<TradeDataPoint>, fetchedAt: Long) {
        rows.forEach { newRow ->
            store.removeAll { existing ->
                existing.point.countryCode == newRow.countryCode &&
                    existing.point.year == newRow.year &&
                    existing.point.partner == newRow.partner
            }
            store.add(StoredRow(newRow, fetchedAt))
        }
    }

    override suspend fun oldestFetchedAt(query: TradeQuery): Long? =
        store
            .filter { row ->
                row.point.countryCode in query.countryCodes &&
                    row.point.year in query.yearRange &&
                    row.point.partner == query.partner
            }
            .minOfOrNull { it.fetchedAt }
}
