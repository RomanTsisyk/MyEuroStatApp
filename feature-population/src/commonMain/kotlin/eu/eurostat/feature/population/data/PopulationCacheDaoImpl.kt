package eu.eurostat.feature.population.data

import eu.eurostat.core.database.generated.AppDatabase
import eu.eurostat.feature.population.domain.PopulationDataPoint
import eu.eurostat.feature.population.domain.PopulationQuery
import eu.eurostat.feature.population.domain.PopulationTimeSeries
import kotlinx.datetime.Instant

class PopulationCacheDaoImpl(
    private val db: AppDatabase,
) : PopulationCacheDao {

    override suspend fun query(query: PopulationQuery): CacheResult? {
        val queries = db.populationCacheQueries
        val yearFrom = query.yearRange.first.toLong()
        val yearTo = query.yearRange.last.toLong()

        val oldest = queries.oldestFetchedAt(
            country_code = query.countryCodes,
            year = yearFrom,
            year_ = yearTo,
        ).executeAsOneOrNull()?.oldest ?: return null

        val entities = queries.queryByCountriesAndYears(
            country_code = query.countryCodes,
            year = yearFrom,
            year_ = yearTo,
        ).executeAsList()

        if (entities.isEmpty()) return null

        val byCountry = entities.groupBy { it.country_code }
        val series = byCountry.map { (country, rows) ->
            PopulationTimeSeries(
                countryCode = country,
                countryName = country,
                points = rows.map { e ->
                    PopulationDataPoint(
                        countryCode = e.country_code,
                        year = e.year.toInt(),
                        totalPopulation = e.total_population,
                        malePopulation = e.male_population,
                        femalePopulation = e.female_population,
                    )
                }.sortedBy { it.year }
            )
        }.sortedBy { it.countryCode }

        return CacheResult(
            series = series,
            oldestFetchedAt = Instant.fromEpochMilliseconds(oldest),
        )
    }

    override suspend fun upsert(series: List<PopulationTimeSeries>, fetchedAt: Instant) {
        val queries = db.populationCacheQueries
        val fetchedAtMs = fetchedAt.toEpochMilliseconds()
        db.transaction {
            for (timeSeries in series) {
                for (point in timeSeries.points) {
                    queries.upsert(
                        country_code = point.countryCode,
                        year = point.year.toLong(),
                        total_population = point.totalPopulation,
                        male_population = point.malePopulation,
                        female_population = point.femalePopulation,
                        fetched_at_epoch_ms = fetchedAtMs,
                    )
                }
            }
        }
    }
}
