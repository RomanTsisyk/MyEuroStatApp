package eu.eurostat.feature.economy.data

import eu.eurostat.core.database.generated.AppDatabase
import eu.eurostat.feature.economy.domain.EconomyDataPoint
import eu.eurostat.feature.economy.domain.EconomyQuery
import eu.eurostat.feature.economy.domain.EconomyTimeSeries
import kotlinx.datetime.Instant

/**
 * SQLDelight-backed cache for merged Economy time series.
 *
 * The underlying table still carries a `unit` column (kept for backward
 * compatibility); rows are always written with [EconomyQuery.unit] from the
 * issuing query and read back with the same filter. The merged domain model
 * no longer exposes `unit` per row — it is implicit in the dataset.
 */
class EconomyCacheDaoImpl(
    private val db: AppDatabase,
) : EconomyCacheDao {

    override suspend fun query(query: EconomyQuery): EconomyCacheResult? {
        val queries = db.economyCacheQueries
        val yearFrom = query.yearRange.first.toLong()
        val yearTo = query.yearRange.last.toLong()
        val unitCode = query.unit.code

        val oldest = queries.oldestFetchedAt(
            country_code = query.countryCodes,
            year = yearFrom,
            year_ = yearTo,
            unit = unitCode,
        ).executeAsOneOrNull()?.oldest ?: return null

        val entities = queries.queryByCountriesAndYears(
            country_code = query.countryCodes,
            year = yearFrom,
            year_ = yearTo,
            unit = unitCode,
        ).executeAsList()

        if (entities.isEmpty()) return null

        val byCountry = entities.groupBy { it.country_code }
        val series = byCountry.map { (country, rows) ->
            EconomyTimeSeries(
                countryCode = country,
                countryName = country,
                points = rows.map { e ->
                    EconomyDataPoint(
                        countryCode = e.country_code,
                        year = e.year.toInt(),
                        gdpEur = e.gdp_eur,
                        hicpIndex = e.hicp_index,
                        deficitPctGdp = e.deficit_pct_gdp,
                    )
                }.sortedBy { it.year }
            )
        }.sortedBy { it.countryCode }

        return EconomyCacheResult(
            series = series,
            oldestFetchedAt = Instant.fromEpochMilliseconds(oldest),
        )
    }

    override suspend fun upsert(series: List<EconomyTimeSeries>, fetchedAt: Instant) {
        upsertWithUnit(series, fetchedAt, unitCode = "CP_MEUR")
    }

    /**
     * Variant used by the repository when it knows the issuing query's unit;
     * keeps cache rows tagged correctly for later lookup.
     */
    suspend fun upsertWithUnit(
        series: List<EconomyTimeSeries>,
        fetchedAt: Instant,
        unitCode: String,
    ) {
        val queries = db.economyCacheQueries
        val fetchedAtMs = fetchedAt.toEpochMilliseconds()
        db.transaction {
            for (timeSeries in series) {
                for (point in timeSeries.points) {
                    queries.upsert(
                        country_code = point.countryCode,
                        year = point.year.toLong(),
                        gdp_eur = point.gdpEur,
                        hicp_index = point.hicpIndex,
                        deficit_pct_gdp = point.deficitPctGdp,
                        unit = unitCode,
                        fetched_at_epoch_ms = fetchedAtMs,
                    )
                }
            }
        }
    }
}
