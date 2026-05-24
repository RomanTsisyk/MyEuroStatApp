package eu.eurostat.feature.science.data

import eu.eurostat.core.jsonstat.JsonStatCell

object ScienceCellMapper {

    fun mapRdSpend(cells: List<JsonStatCell>): Map<Pair<String, Int>, Double?> =
        mapFiltered(cells) { dims ->
            dims["unit"] == "PC_GDP" && dims["sectperf"] == "TOTAL"
        }

    fun mapInternetUsage(cells: List<JsonStatCell>): Map<Pair<String, Int>, Double?> =
        mapFiltered(cells) { dims ->
            dims["unit"] == "PC_IND" && dims["indic_is"] == "I_IU3" && dims["ind_type"] == "IND_TOTAL"
        }

    fun mapTertiaryEduc(cells: List<JsonStatCell>): Map<Pair<String, Int>, Double?> =
        mapFiltered(cells) { dims ->
            dims["unit"] == "PC" && dims["isced11"] == "ED5-8" && dims["sex"] == "T" && dims["age"] == "Y25-64"
        }

    private fun mapFiltered(
        cells: List<JsonStatCell>,
        predicate: (Map<String, String>) -> Boolean,
    ): Map<Pair<String, Int>, Double?> {
        val result = mutableMapOf<Pair<String, Int>, Double?>()
        for (cell in cells) {
            if (!predicate(cell.dimensions)) continue
            val country = cell.dimensions["geo"] ?: continue
            val yearStr = cell.dimensions["time"] ?: continue
            val year = yearStr.toIntOrNull() ?: continue
            result[country to year] = cell.value
        }
        return result
    }

    /**
     * Collect country labels from cells.
     * Returns a map of countryCode -> human-readable country name sourced from
     * [eu.eurostat.core.jsonstat.JsonStatCell.dimensionLabels].
     */
    fun buildCountryLabels(cells: List<JsonStatCell>): Map<String, String> {
        val labels = mutableMapOf<String, String>()
        for (cell in cells) {
            val code = cell.dimensions["geo"] ?: continue
            if (code !in labels) {
                labels[code] = cell.dimensionLabels["geo"] ?: code
            }
        }
        return labels
    }
}
