package eu.eurostat.feature.trade.data

import eu.eurostat.core.database.generated.AppDatabase
import eu.eurostat.feature.trade.domain.TradeDataPoint
import eu.eurostat.feature.trade.domain.TradeQuery

class TradeCacheDaoImpl(
    private val db: AppDatabase,
) : TradeCacheDao {

    private val queries get() = db.tradeCacheQueries

    override suspend fun query(query: TradeQuery): List<TradeDataPoint> =
        queries.queryByCountriesAndYears(
            country_code = query.countryCodes,
            year = query.yearRange.first.toLong(),
            year_ = query.yearRange.last.toLong(),
            partner = query.partner,
        ).executeAsList().map { entity ->
            TradeDataPoint(
                countryCode = entity.country_code,
                year = entity.year.toInt(),
                partner = entity.partner,
                exportsEur = entity.exports_eur,
                importsEur = entity.imports_eur,
                balanceEur = entity.balance_eur,
            )
        }

    override suspend fun upsertAll(rows: List<TradeDataPoint>, fetchedAt: Long) {
        db.transaction {
            rows.forEach { dp ->
                queries.upsert(
                    country_code = dp.countryCode,
                    year = dp.year.toLong(),
                    exports_eur = dp.exportsEur,
                    imports_eur = dp.importsEur,
                    balance_eur = dp.balanceEur,
                    partner = dp.partner,
                    fetched_at_epoch_ms = fetchedAt,
                )
            }
        }
    }

    override suspend fun oldestFetchedAt(query: TradeQuery): Long? =
        queries.oldestFetchedAt(
            country_code = query.countryCodes,
            year = query.yearRange.first.toLong(),
            year_ = query.yearRange.last.toLong(),
            partner = query.partner,
        ).executeAsOneOrNull()?.oldest
}
