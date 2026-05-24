package eu.eurostat.feature.science.data

import eu.eurostat.core.database.generated.AppDatabase
import eu.eurostat.feature.science.domain.ScienceDataPoint
import eu.eurostat.feature.science.domain.ScienceQuery

class ScienceCacheDaoImpl(
    private val db: AppDatabase,
) : ScienceCacheDao {

    override suspend fun query(query: ScienceQuery): List<ScienceDataPoint> {
        val queries = db.scienceCacheQueries
        val yearFrom = query.yearRange.first.toLong()
        val yearTo = query.yearRange.last.toLong()
        val entities = queries.queryByCountriesAndYears(
            country_code = query.countryCodes,
            year = yearFrom,
            year_ = yearTo,
        ).executeAsList()

        return entities.map { e ->
            ScienceDataPoint(
                countryCode = e.country_code,
                year = e.year.toInt(),
                rdSpendPctGdp = e.rd_spend_pct_gdp,
                internetUsagePct = e.internet_usage_pct,
                tertiaryEducPct = e.tertiary_educ_pct,
            )
        }
    }

    override suspend fun upsertAll(rows: List<ScienceDataPoint>, fetchedAt: Long) {
        val queries = db.scienceCacheQueries
        db.transaction {
            for (row in rows) {
                queries.upsert(
                    country_code = row.countryCode,
                    year = row.year.toLong(),
                    rd_spend_pct_gdp = row.rdSpendPctGdp,
                    internet_usage_pct = row.internetUsagePct,
                    tertiary_educ_pct = row.tertiaryEducPct,
                    fetched_at_epoch_ms = fetchedAt,
                )
            }
        }
    }

    override suspend fun oldestFetchedAt(query: ScienceQuery): Long? {
        val queries = db.scienceCacheQueries
        return queries.oldestFetchedAt(
            country_code = query.countryCodes,
            year = query.yearRange.first.toLong(),
            year_ = query.yearRange.last.toLong(),
        ).executeAsOneOrNull()?.oldest
    }
}
