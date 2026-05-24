package eu.eurostat.feature.economy.data

import eu.eurostat.core.jsonstat.JsonStatCell
import eu.eurostat.feature.economy.domain.EconomyDataPoint
import eu.eurostat.feature.economy.domain.EconomyTimeSeries

/**
 * Merges raw `JsonStatCell` rows from the three Economy datasets
 * (GDP, HICP, deficit) into a per-country list of [EconomyTimeSeries]
 * where each [EconomyDataPoint] holds all three metric values for one year.
 */
object EconomyCellMapper {

    /**
     * Convenience: map GDP-only cells with no secondary enrichment.
     * Equivalent to calling [mergeIntoTimeSeries] with null secondary lists.
     */
    fun map(cells: List<JsonStatCell>): List<EconomyTimeSeries> =
        mergeIntoTimeSeries(cells, null, null)

    /**
     * Merges GDP cells with optional HICP and deficit cells.
     *
     * - [gdpCells]     from `nama_10_gdp`     (`na_item=B1GQ`).
     * - [hicpCells]    from `prc_hicp_aind`   (`coicop=CP00`, `unit=INX_A_AVG`); null if fetch failed.
     * - [deficitCells] from `gov_10dd_edpt1`  (`na_item=B9`, `unit=PC_GDP`, `sector=S13`); null if fetch failed.
     *
     * Each observation is keyed by (`geo`, `time`) — values from the three datasets
     * with the same key are merged into one [EconomyDataPoint]. Country name is
     * resolved from [JsonStatCell.dimensionLabels]`["geo"]` with the code as fallback.
     */
    fun mergeIntoTimeSeries(
        gdpCells: List<JsonStatCell>,
        hicpCells: List<JsonStatCell>?,
        deficitCells: List<JsonStatCell>?,
    ): List<EconomyTimeSeries> {
        data class Key(val country: String, val year: Int)

        val countryLabels = mutableMapOf<String, String>()
        val gdpByKey = mutableMapOf<Key, Long?>()
        val hicpByKey = mutableMapOf<Key, Double>()
        val deficitByKey = mutableMapOf<Key, Double>()

        fun JsonStatCell.keyAndCountry(): Pair<Key, String>? {
            val country = dimensions["geo"] ?: return null
            val year = dimensions["time"]?.toIntOrNull() ?: return null
            if (country !in countryLabels) {
                countryLabels[country] = dimensionLabels["geo"] ?: country
            }
            return Key(country, year) to country
        }

        gdpCells.forEach { cell ->
            val (key, _) = cell.keyAndCountry() ?: return@forEach
            gdpByKey[key] = cell.value?.toLong()
        }
        hicpCells?.forEach { cell ->
            val (key, _) = cell.keyAndCountry() ?: return@forEach
            cell.value?.let { hicpByKey[key] = it }
        }
        deficitCells?.forEach { cell ->
            val (key, _) = cell.keyAndCountry() ?: return@forEach
            cell.value?.let { deficitByKey[key] = it }
        }

        // Union of all keys so HICP-only or deficit-only countries still surface.
        val allKeys = gdpByKey.keys + hicpByKey.keys + deficitByKey.keys
        val byCountry = mutableMapOf<String, MutableList<EconomyDataPoint>>()

        for (key in allKeys) {
            byCountry.getOrPut(key.country) { mutableListOf() }.add(
                EconomyDataPoint(
                    countryCode = key.country,
                    year = key.year,
                    gdpEur = gdpByKey[key],
                    hicpIndex = hicpByKey[key],
                    deficitPctGdp = deficitByKey[key],
                )
            )
        }

        return byCountry.map { (country, points) ->
            EconomyTimeSeries(
                countryCode = country,
                countryName = countryLabels[country] ?: country,
                points = points.sortedBy { it.year },
            )
        }.sortedBy { it.countryCode }
    }
}
