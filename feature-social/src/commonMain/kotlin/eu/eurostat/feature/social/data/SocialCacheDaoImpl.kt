package eu.eurostat.feature.social.data

import eu.eurostat.core.database.generated.AppDatabase
import eu.eurostat.feature.social.domain.SocialDataPoint
import eu.eurostat.feature.social.domain.SocialQuery

class SocialCacheDaoImpl(
    private val db: AppDatabase,
) : SocialCacheDao {

    override suspend fun query(query: SocialQuery): List<SocialDataPoint> {
        val queries = db.socialCacheQueries
        val yearFrom = query.yearRange.first.toLong()
        val yearTo = query.yearRange.last.toLong()
        val entities = queries.queryByCountriesAndYears(
            country_code = query.countryCodes,
            year = yearFrom,
            year_ = yearTo,
        ).executeAsList()

        return entities.map { e ->
            SocialDataPoint(
                countryCode = e.country_code,
                year = e.year.toInt(),
                povertyRate = e.poverty_rate,
                atRiskRate = e.at_risk_rate,
                healthSatisfaction = e.health_satisfaction,
            )
        }
    }

    override suspend fun upsertAll(rows: List<SocialDataPoint>, fetchedAt: Long) {
        val queries = db.socialCacheQueries
        db.transaction {
            for (row in rows) {
                queries.upsert(
                    country_code = row.countryCode,
                    year = row.year.toLong(),
                    poverty_rate = row.povertyRate,
                    at_risk_rate = row.atRiskRate,
                    health_satisfaction = row.healthSatisfaction,
                    fetched_at_epoch_ms = fetchedAt,
                )
            }
        }
    }

    override suspend fun oldestFetchedAt(query: SocialQuery): Long? {
        val queries = db.socialCacheQueries
        return queries.oldestFetchedAt(
            country_code = query.countryCodes,
            year = query.yearRange.first.toLong(),
            year_ = query.yearRange.last.toLong(),
        ).executeAsOneOrNull()?.oldest
    }
}
