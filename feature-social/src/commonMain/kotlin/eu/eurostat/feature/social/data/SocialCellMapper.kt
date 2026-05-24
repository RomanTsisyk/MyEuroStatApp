package eu.eurostat.feature.social.data

import eu.eurostat.core.jsonstat.JsonStatCell

object SocialCellMapper {

    fun mapToValueMap(cells: List<JsonStatCell>): Map<Pair<String, Int>, Double?> {
        val result = mutableMapOf<Pair<String, Int>, Double?>()
        for (cell in cells) {
            val country = cell.dimensions["geo"] ?: continue
            val yearStr = cell.dimensions["time"] ?: continue
            val year = yearStr.toIntOrNull() ?: continue
            result[country to year] = cell.value
        }
        return result
    }

    /**
     * Maps hlth_silc_01 cells to a deterministic (country, year) → value map.
     * Filters to a single slice: very good health (VGOOD), total sex (T),
     * age 16+ (Y_GE16), total population (POP). This is the only combination
     * that yields exactly one cell per (geo, time) for this dataset.
     */
    fun mapHealthToValueMap(cells: List<JsonStatCell>): Map<Pair<String, Int>, Double?> {
        val result = mutableMapOf<Pair<String, Int>, Double?>()
        for (cell in cells) {
            if (cell.dimensions["levels"] != "VGOOD") continue
            if (cell.dimensions["sex"] != "T") continue
            if (cell.dimensions["age"] != "Y_GE16") continue
            if (cell.dimensions["wstatus"] != "POP") continue
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
