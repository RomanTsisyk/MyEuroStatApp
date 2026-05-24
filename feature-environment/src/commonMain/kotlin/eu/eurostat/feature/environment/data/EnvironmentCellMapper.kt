package eu.eurostat.feature.environment.data

import eu.eurostat.core.jsonstat.JsonStatCell
import eu.eurostat.feature.environment.domain.EnvSector
import eu.eurostat.feature.environment.domain.EnvironmentDataPoint
import eu.eurostat.feature.environment.domain.EnvironmentTimeSeries

/**
 * Maps raw JSON-stat cells from the three Environment datasets into one
 * [EnvironmentTimeSeries] per country.
 *
 * Three input streams:
 *  - GHG cells from `env_air_gge` (dimensions: geo, time, src_crf, airpol, unit)
 *  - Energy cells from `nrg_bal_c` (dimensions: geo, time, nrg_bal, siec, unit)
 *  - SDG cells from `sdg_13_10`   (dimensions: geo, time, unit)
 *
 * GHG and Energy each yield one [EnvironmentDataPoint] per (country, year, sector).
 * SDG yields one point per (country, year) with `sector = null`.
 */
object EnvironmentCellMapper {

    private val GHG_SECTOR_BY_CODE = mapOf(
        "TOTX4_MEMO" to EnvSector.Total,
        "CRF1A3" to EnvSector.Transport,
        "CRF1A2" to EnvSector.Industry,
    )

    private val ENERGY_SECTOR_BY_CODE = mapOf(
        "FC_E" to EnvSector.Total,
        "FC_TRA_E" to EnvSector.Transport,
        "FC_IND_E" to EnvSector.Industry,
    )

    /** Build country-keyed time series, merging all three datasets. */
    fun buildTimeSeries(
        ghgCells: List<JsonStatCell>,
        energyCells: List<JsonStatCell>,
        sdgCells: List<JsonStatCell>?,
    ): List<EnvironmentTimeSeries> {
        val ghgPoints = mapGhgCells(ghgCells)
        val energyPoints = mapEnergyCells(energyCells)
        val sdgPoints = mapSdgCells(sdgCells ?: emptyList())

        val labels = buildCountryLabels(ghgCells, energyCells, sdgCells ?: emptyList())

        // Merge GHG and Energy by (country, year, sector); keep SDG points (sector=null) separate.
        val sectorKey: (EnvironmentDataPoint) -> Triple<String, Int, EnvSector?> =
            { Triple(it.countryCode, it.year, it.sector) }

        val ghgMap = ghgPoints.associateBy(sectorKey)
        val energyMap = energyPoints.associateBy(sectorKey)
        val mergedKeys = ghgMap.keys + energyMap.keys
        val mergedSectorPoints = mergedKeys.map { key ->
            val g = ghgMap[key]
            val e = energyMap[key]
            EnvironmentDataPoint(
                countryCode = key.first,
                year = key.second,
                sector = key.third,
                ghgMtCo2eq = g?.ghgMtCo2eq,
                energyKtoe = e?.energyKtoe,
                sdg13Index = null,
            )
        }

        val allPoints = mergedSectorPoints + sdgPoints

        return allPoints
            .groupBy { it.countryCode }
            .map { (code, points) ->
                EnvironmentTimeSeries(
                    countryCode = code,
                    countryName = labels[code] ?: code,
                    points = points.sortedWith(
                        compareBy({ it.sector?.ordinal ?: Int.MAX_VALUE }, { it.year }),
                    ),
                )
            }
            .sortedBy { it.countryCode }
    }

    private fun mapGhgCells(cells: List<JsonStatCell>): List<EnvironmentDataPoint> =
        cells.mapNotNull { cell ->
            val country = cell.dimensions["geo"] ?: return@mapNotNull null
            val year = cell.dimensions["time"]?.toIntOrNull() ?: return@mapNotNull null
            val sectorCode = cell.dimensions["src_crf"] ?: return@mapNotNull null
            val sector = GHG_SECTOR_BY_CODE[sectorCode] ?: return@mapNotNull null
            EnvironmentDataPoint(
                countryCode = country,
                year = year,
                sector = sector,
                ghgMtCo2eq = cell.value,
            )
        }

    private fun mapEnergyCells(cells: List<JsonStatCell>): List<EnvironmentDataPoint> =
        cells.mapNotNull { cell ->
            val country = cell.dimensions["geo"] ?: return@mapNotNull null
            val year = cell.dimensions["time"]?.toIntOrNull() ?: return@mapNotNull null
            val sectorCode = cell.dimensions["nrg_bal"] ?: return@mapNotNull null
            val sector = ENERGY_SECTOR_BY_CODE[sectorCode] ?: return@mapNotNull null
            EnvironmentDataPoint(
                countryCode = country,
                year = year,
                sector = sector,
                energyKtoe = cell.value,
            )
        }

    private fun mapSdgCells(cells: List<JsonStatCell>): List<EnvironmentDataPoint> =
        cells.mapNotNull { cell ->
            val country = cell.dimensions["geo"] ?: return@mapNotNull null
            val year = cell.dimensions["time"]?.toIntOrNull() ?: return@mapNotNull null
            EnvironmentDataPoint(
                countryCode = country,
                year = year,
                sector = null,
                sdg13Index = cell.value,
            )
        }

    private fun buildCountryLabels(vararg cellLists: List<JsonStatCell>): Map<String, String> {
        val labels = mutableMapOf<String, String>()
        for (cells in cellLists) {
            for (cell in cells) {
                val code = cell.dimensions["geo"] ?: continue
                if (code !in labels) {
                    labels[code] = cell.dimensionLabels["geo"] ?: code
                }
            }
        }
        return labels
    }
}
