package eu.eurostat.feature.environment.data

import eu.eurostat.core.database.EnvironmentCacheEntity
import eu.eurostat.core.database.generated.AppDatabase
import eu.eurostat.feature.environment.domain.EnvSector
import eu.eurostat.feature.environment.domain.EnvironmentDataPoint
import eu.eurostat.feature.environment.domain.EnvironmentQuery
import eu.eurostat.feature.environment.domain.EnvironmentTimeSeries
import kotlinx.datetime.Instant

/**
 * SQLDelight-backed implementation of [EnvironmentCacheDao].
 *
 * Storage layout (existing [EnvironmentCacheEntity] table):
 *  - Sector-specific GHG / Energy rows  → `sector` = "TOTAL" | "TRANSPORT" | "INDUSTRY"
 *  - SDG-13 rows (no real sector)        → `sector` = [SECTOR_SDG] sentinel
 *
 * All three sector codes for GHG and Energy are stored individually, so the
 * cache faithfully reproduces the multi-sector domain model without schema changes.
 */
class EnvironmentCacheDaoImpl(
    private val db: AppDatabase,
) : EnvironmentCacheDao {

    override suspend fun query(query: EnvironmentQuery): EnvironmentCacheResult? {
        val queries = db.environmentCacheQueries
        val yearFrom = query.yearRange.first.toLong()
        val yearTo = query.yearRange.last.toLong()

        // Collect rows across all logical sectors (including the SDG sentinel).
        val allSectors = SECTOR_CODES + SECTOR_SDG
        var globalOldest: Long? = null
        val allEntities = mutableListOf<EnvironmentCacheEntity>()

        for (sectorCode in allSectors) {
            val oldest = queries.oldestFetchedAt(
                country_code = query.countryCodes,
                year = yearFrom,
                year_ = yearTo,
                sector = sectorCode,
            ).executeAsOneOrNull()?.oldest ?: continue

            if (globalOldest == null || oldest < globalOldest) globalOldest = oldest

            allEntities += queries.queryByCountriesAndYears(
                country_code = query.countryCodes,
                year = yearFrom,
                year_ = yearTo,
                sector = sectorCode,
            ).executeAsList()
        }

        if (globalOldest == null || allEntities.isEmpty()) return null

        val series = allEntities
            .groupBy { it.country_code }
            .map { (code, rows) ->
                EnvironmentTimeSeries(
                    countryCode = code,
                    countryName = code,
                    points = rows.map { e -> e.toDataPoint() }
                        .sortedWith(
                            compareBy({ it.sector?.ordinal ?: Int.MAX_VALUE }, { it.year }),
                        ),
                )
            }
            .sortedBy { it.countryCode }

        return EnvironmentCacheResult(
            series = series,
            oldestFetchedAt = Instant.fromEpochMilliseconds(globalOldest),
        )
    }

    override suspend fun upsert(series: List<EnvironmentTimeSeries>, fetchedAt: Instant) {
        val queries = db.environmentCacheQueries
        val fetchedAtMs = fetchedAt.toEpochMilliseconds()
        db.transaction {
            for (timeSeries in series) {
                for (point in timeSeries.points) {
                    queries.upsert(
                        country_code = point.countryCode,
                        year = point.year.toLong(),
                        ghg_emissions = point.ghgMtCo2eq,
                        energy_consumption = point.energyKtoe,
                        sdg13_score = point.sdg13Index,
                        sector = point.sector.toCacheCode(),
                        fetched_at_epoch_ms = fetchedAtMs,
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Mapping helpers
    // ------------------------------------------------------------------

    private fun EnvironmentCacheEntity.toDataPoint() =
        EnvironmentDataPoint(
            countryCode = country_code,
            year = year.toInt(),
            sector = sector.toEnvSector(),
            ghgMtCo2eq = ghg_emissions,
            energyKtoe = energy_consumption,
            sdg13Index = sdg13_score,
        )

    private fun EnvSector?.toCacheCode(): String = when (this) {
        EnvSector.Total -> "TOTAL"
        EnvSector.Transport -> "TRANSPORT"
        EnvSector.Industry -> "INDUSTRY"
        null -> SECTOR_SDG
    }

    private fun String.toEnvSector(): EnvSector? = when (this) {
        "TOTAL" -> EnvSector.Total
        "TRANSPORT" -> EnvSector.Transport
        "INDUSTRY" -> EnvSector.Industry
        else -> null // SECTOR_SDG sentinel → null
    }

    private companion object {
        /** Sentinel sector code used in the DB for SDG-13 observations (domain sector = null). */
        const val SECTOR_SDG = "SDG"
        val SECTOR_CODES = listOf("TOTAL", "TRANSPORT", "INDUSTRY")
    }
}
