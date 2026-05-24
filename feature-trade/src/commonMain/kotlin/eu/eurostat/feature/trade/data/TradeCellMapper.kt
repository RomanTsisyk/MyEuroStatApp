package eu.eurostat.feature.trade.data

import eu.eurostat.core.jsonstat.JsonStatCell
import eu.eurostat.feature.trade.domain.TradeDataPoint

/**
 * Maps raw JSON-stat cells from ext_lt_intratrd into domain [TradeDataPoint]s.
 *
 * Eurostat dimension keys for ext_lt_intratrd:
 *   "geo"       - reporting country
 *   "time"      - year
 *   "partner"   - trading partner
 *   "indic_et"  - indicator: MIO_EXP_VAL (exports), MIO_IMP_VAL (imports), MIO_BAL_VAL (balance)
 *
 * The dataset returns one cell per (geo, time, partner, indic_et) combination.
 * We group by (geo, time, partner) and collect the three indicator values.
 */
object TradeCellMapper {

    fun mapCells(cells: List<JsonStatCell>, partner: String): List<TradeDataPoint> {
        // Group by (country, year) — partner is fixed per query
        data class Key(val country: String, val year: Int)

        val grouped = mutableMapOf<Key, MutableMap<String, Long?>>()

        cells.forEach { cell ->
            val country = cell.dimensions["geo"] ?: return@forEach
            val year = cell.dimensions["time"]?.toIntOrNull() ?: return@forEach
            val indicator = cell.dimensions["indic_et"] ?: return@forEach
            val key = Key(country, year)
            grouped.getOrPut(key) { mutableMapOf() }[indicator] = cell.value?.toLong()
        }

        return grouped.map { (key, indicators) ->
            TradeDataPoint(
                countryCode = key.country,
                year = key.year,
                partner = partner,
                exportsEur = indicators["MIO_EXP_VAL"],
                importsEur = indicators["MIO_IMP_VAL"],
                balanceEur = indicators["MIO_BAL_VAL"],
            )
        }
    }

    /**
     * Collect country labels from trade cells.
     * Returns a map of countryCode -> human-readable country name.
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
