package eu.eurostat.feature.transport.data

import eu.eurostat.core.database.generated.AppDatabase
import eu.eurostat.feature.transport.domain.TransportDataPoint
import eu.eurostat.feature.transport.domain.TransportMode
import eu.eurostat.feature.transport.domain.TransportQuery

class TransportCacheDaoImpl(
    private val db: AppDatabase,
) : TransportCacheDao {

    private val queries get() = db.transportCacheQueries

    override suspend fun query(query: TransportQuery): List<TransportDataPoint> =
        queries.queryByCountriesAndYears(
            country_code = query.countryCodes,
            year = query.yearRange.first.toLong(),
            year_ = query.yearRange.last.toLong(),
            mode = query.mode.code,
        ).executeAsList().map { entity ->
            TransportDataPoint(
                countryCode = entity.country_code,
                year = entity.year.toInt(),
                mode = TransportMode.entries.firstOrNull { it.code == entity.mode }
                    ?: TransportMode.ALL,
                roadPassengers = entity.road_passengers,
                airPassengers = entity.air_passengers,
                seaPassengers = entity.sea_passengers,
            )
        }

    override suspend fun upsertAll(rows: List<TransportDataPoint>, fetchedAt: Long) {
        db.transaction {
            rows.forEach { dp ->
                queries.upsert(
                    country_code = dp.countryCode,
                    year = dp.year.toLong(),
                    road_passengers = dp.roadPassengers,
                    air_passengers = dp.airPassengers,
                    sea_passengers = dp.seaPassengers,
                    mode = dp.mode.code,
                    fetched_at_epoch_ms = fetchedAt,
                )
            }
        }
    }

    override suspend fun oldestFetchedAt(query: TransportQuery): Long? =
        queries.oldestFetchedAt(
            country_code = query.countryCodes,
            year = query.yearRange.first.toLong(),
            year_ = query.yearRange.last.toLong(),
            mode = query.mode.code,
        ).executeAsOneOrNull()?.oldest
}
