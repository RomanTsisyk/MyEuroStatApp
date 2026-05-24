package eu.eurostat.feature.tourism.data

import eu.eurostat.core.jsonstat.JsonStatCell
import eu.eurostat.feature.tourism.domain.TourismDataPoint
import eu.eurostat.feature.tourism.domain.TourismResidence
import eu.eurostat.feature.tourism.domain.TourismTimeSeries

/**
 * Translates raw JSON-stat cells from the two Eurostat tourism datasets into
 * the wide-row [TourismDataPoint] / [TourismTimeSeries] domain model.
 *
 * Two responsibilities:
 *  - merge `tour_occ_ninat` cells (one row per residence × geo × time) into
 *    `(geo, time)`-keyed rows with three nullable nights columns.
 *  - layer `tour_dem_tttot` cells on top, populating [TourismDataPoint.trips].
 *
 * Cells with missing `geo`, missing `time` or non-integer `time` are dropped.
 */
object TourismCellMapper {

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

    /**
     * Merge nights + trips cells into one [TourismDataPoint] per (country, year),
     * keyed by `geo` and `time` dimensions. The `c_resid` dimension on the
     * nights cells routes the value into [TourismDataPoint.domesticNights],
     * [TourismDataPoint.foreignNights] or [TourismDataPoint.totalNights].
     *
     * If multiple nights cells share the same (geo, year, c_resid) — typically
     * because the call also splits on `nace_r2` — the values are summed.
     */
    fun mergeToDataPoints(
        nightsCells: List<JsonStatCell>,
        tripsCells: List<JsonStatCell>,
    ): List<TourismDataPoint> {
        data class Acc(
            var domestic: Long? = null,
            var foreign: Long? = null,
            var total: Long? = null,
            var trips: Long? = null,
        )

        val acc = mutableMapOf<Pair<String, Int>, Acc>()

        for (cell in nightsCells) {
            val geo = cell.dimensions["geo"] ?: continue
            val year = cell.dimensions["time"]?.toIntOrNull() ?: continue
            val value = cell.value?.toLong() ?: continue
            val resid = cell.dimensions["c_resid"]
            val slot = acc.getOrPut(geo to year) { Acc() }
            when (resid) {
                TourismResidence.Domestic.code -> slot.domestic = (slot.domestic ?: 0L) + value
                TourismResidence.Foreign.code -> slot.foreign = (slot.foreign ?: 0L) + value
                TourismResidence.Total.code -> slot.total = (slot.total ?: 0L) + value
            }
        }

        for (cell in tripsCells) {
            val geo = cell.dimensions["geo"] ?: continue
            val year = cell.dimensions["time"]?.toIntOrNull() ?: continue
            val value = cell.value?.toLong() ?: continue
            val slot = acc.getOrPut(geo to year) { Acc() }
            slot.trips = (slot.trips ?: 0L) + value
        }

        return acc.entries.map { (key, a) ->
            TourismDataPoint(
                countryCode = key.first,
                year = key.second,
                domesticNights = a.domestic,
                foreignNights = a.foreign,
                totalNights = a.total,
                trips = a.trips,
            )
        }
    }

    /**
     * Convert raw `tour_occ_nim` cells (monthly nights) into a row-major
     * heatmap matrix suitable for [eu.eurostat.core.charts.EurostatHeatmapChart].
     *
     * The cells carry a `time` dimension in the form `"YYYY-MM"` (e.g.
     * `"2023-01"`). This function:
     *  1. Groups cells by year.
     *  2. For each year, produces a 12-element row where each column is the
     *     normalised nights count for that month (0f when data is absent).
     *  3. Normalises all values to the 0f..1f range based on the global maximum.
     *
     * Returns an empty list when [cells] is empty or contains no parseable times.
     */
    fun toHeatmapCells(cells: List<JsonStatCell>): List<List<Float>> {
        if (cells.isEmpty()) return emptyList()

        // month code format: "YYYY-MM" → split on '-'
        data class YearMonth(val year: Int, val month: Int)

        val raw = mutableMapOf<YearMonth, Long>()
        for (cell in cells) {
            val time = cell.dimensions["time"] ?: continue
            val parts = time.split("-")
            if (parts.size != 2) continue
            val year = parts[0].toIntOrNull() ?: continue
            val month = parts[1].toIntOrNull() ?: continue
            if (month < 1 || month > 12) continue
            val value = cell.value?.toLong() ?: 0L
            val key = YearMonth(year, month)
            raw[key] = (raw[key] ?: 0L) + value
        }

        if (raw.isEmpty()) return emptyList()

        val maxValue = raw.values.maxOrNull()?.toFloat()?.takeIf { it > 0f } ?: return emptyList()
        val years = raw.keys.map { it.year }.distinct().sorted()

        return years.map { yr ->
            (1..12).map { month ->
                ((raw[YearMonth(yr, month)] ?: 0L).toFloat() / maxValue).coerceIn(0f, 1f)
            }
        }
    }

    /**
     * Bundle a flat list of points into one [TourismTimeSeries] per country,
     * sorted by year, with human-readable [TourismTimeSeries.countryName]
     * resolved from the supplied [countryLabels] (falling back to the code).
     */
    fun toTimeSeries(
        points: List<TourismDataPoint>,
        countryLabels: Map<String, String> = emptyMap(),
    ): List<TourismTimeSeries> =
        points.groupBy { it.countryCode }
            .map { (code, rows) ->
                TourismTimeSeries(
                    countryCode = code,
                    countryName = countryLabels[code] ?: code,
                    points = rows.sortedBy { it.year },
                )
            }
            .sortedBy { it.countryCode }
}
