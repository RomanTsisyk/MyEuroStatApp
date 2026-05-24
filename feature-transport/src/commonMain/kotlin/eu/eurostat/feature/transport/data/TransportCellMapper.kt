package eu.eurostat.feature.transport.data

import eu.eurostat.core.jsonstat.JsonStatCell
import eu.eurostat.feature.transport.domain.TransportDataPoint
import eu.eurostat.feature.transport.domain.TransportMode

/**
 * Maps raw JSON-stat cells from transport datasets into domain [TransportDataPoint]s.
 *
 * road_pa_buscoa: dimension keys "geo", "time" → roadPassengers
 * avia_paoc:      dimension keys "geo", "time" → airPassengers
 * mar_pa_aa:      dimension keys "geo", "time" → seaPassengers
 *
 * Country name labels are available via [buildCountryLabels] and can be consumed by the
 * caller when building [eu.eurostat.feature.transport.domain.TransportTimeSeries].
 */
object TransportCellMapper {

    /**
     * Collect country labels from one or more cell lists.
     * Returns a map of countryCode -> human-readable country name sourced from
     * [eu.eurostat.core.jsonstat.JsonStatCell.dimensionLabels].
     */
    fun buildCountryLabels(vararg cellLists: List<JsonStatCell>): Map<String, String> {
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

    fun mapRoadCells(cells: List<JsonStatCell>): List<TransportDataPoint> =
        cells.mapNotNull { cell ->
            val country = cell.dimensions["geo"] ?: return@mapNotNull null
            val year = cell.dimensions["time"]?.toIntOrNull() ?: return@mapNotNull null
            TransportDataPoint(
                countryCode = country,
                year = year,
                mode = TransportMode.ROAD,
                roadPassengers = cell.value?.let { (it * 1000.0).toLong() },
                airPassengers = null,
                seaPassengers = null,
            )
        }

    fun mapAirCells(cells: List<JsonStatCell>): List<TransportDataPoint> =
        cells.mapNotNull { cell ->
            val country = cell.dimensions["geo"] ?: return@mapNotNull null
            val year = cell.dimensions["time"]?.toIntOrNull() ?: return@mapNotNull null
            TransportDataPoint(
                countryCode = country,
                year = year,
                mode = TransportMode.AIR,
                roadPassengers = null,
                airPassengers = cell.value?.toLong(),
                seaPassengers = null,
            )
        }

    fun mapSeaCells(cells: List<JsonStatCell>): List<TransportDataPoint> =
        cells.mapNotNull { cell ->
            val country = cell.dimensions["geo"] ?: return@mapNotNull null
            val year = cell.dimensions["time"]?.toIntOrNull() ?: return@mapNotNull null
            TransportDataPoint(
                countryCode = country,
                year = year,
                mode = TransportMode.SEA,
                roadPassengers = null,
                airPassengers = null,
                seaPassengers = cell.value?.toLong(),
            )
        }

    /**
     * Merge road, air, and sea points by (countryCode, year) for the ALL mode.
     * Combines all three passenger columns into a single ALL-mode data point.
     */
    fun mergeAll(
        roadPoints: List<TransportDataPoint>,
        airPoints: List<TransportDataPoint>,
        seaPoints: List<TransportDataPoint>,
    ): List<TransportDataPoint> {
        data class Key(val country: String, val year: Int)

        val roadMap = roadPoints.associateBy { Key(it.countryCode, it.year) }
        val airMap = airPoints.associateBy { Key(it.countryCode, it.year) }
        val seaMap = seaPoints.associateBy { Key(it.countryCode, it.year) }

        val allKeys = roadMap.keys + airMap.keys + seaMap.keys

        return allKeys.distinct().map { key ->
            TransportDataPoint(
                countryCode = key.country,
                year = key.year,
                mode = TransportMode.ALL,
                roadPassengers = roadMap[key]?.roadPassengers,
                airPassengers = airMap[key]?.airPassengers,
                seaPassengers = seaMap[key]?.seaPassengers,
            )
        }
    }
}
