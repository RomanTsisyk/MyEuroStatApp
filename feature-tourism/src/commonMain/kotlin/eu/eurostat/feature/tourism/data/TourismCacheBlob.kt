package eu.eurostat.feature.tourism.data

import eu.eurostat.feature.tourism.domain.TourismData
import eu.eurostat.feature.tourism.domain.TourismDataPoint
import kotlinx.serialization.Serializable

/**
 * Serializable cache DTO for one tourism query result, persisted as a single
 * JSON blob via `JsonBlobCache` (see `MultiDimCacheEntity`).
 *
 * Replaces the residence-tall `TourismCacheEntity` table, which needed three
 * rows per (country, year) plus a `-1` sentinel for "nights unavailable"
 * because its `nights` column was NOT NULL. JSON has real `null`, so the
 * sentinel is gone; and because [countryLabels] and [heatmapCells] ride along
 * in the blob, country names and the seasonality heatmap now survive process
 * restarts instead of living only in repository memory.
 *
 * The DTO mirrors the domain shape but stays a separate type so the domain
 * layer remains free of serialization annotations; bump the `v{N}` segment of
 * the cache key when this shape changes incompatibly.
 */
@Serializable
data class TourismCacheBlob(
    val points: List<Point>,
    val countryLabels: Map<String, String> = emptyMap(),
    val heatmapCells: List<List<Float>> = emptyList(),
) {

    /**
     * Cached counterpart of [TourismDataPoint]. Every metric is genuinely
     * nullable — "no data" round-trips as JSON `null`, not a sentinel.
     */
    @Serializable
    data class Point(
        val countryCode: String,
        val year: Int,
        val domesticNights: Long? = null,
        val foreignNights: Long? = null,
        val totalNights: Long? = null,
        val trips: Long? = null,
    )

    /** Rebuilds the domain aggregate, regrouping points into per-country time series. */
    fun toDomain(): TourismData = TourismData(
        timeSeries = TourismCellMapper.toTimeSeries(
            points.map { p ->
                TourismDataPoint(
                    countryCode = p.countryCode,
                    year = p.year,
                    domesticNights = p.domesticNights,
                    foreignNights = p.foreignNights,
                    totalNights = p.totalNights,
                    trips = p.trips,
                )
            },
            countryLabels,
        ),
        heatmapCells = heatmapCells,
    )

    companion object {

        /** Flattens a network fetch result into its serializable cache shape. */
        fun fromFetchResult(result: TourismFetchResult): TourismCacheBlob = TourismCacheBlob(
            points = result.points.map { p ->
                Point(
                    countryCode = p.countryCode,
                    year = p.year,
                    domesticNights = p.domesticNights,
                    foreignNights = p.foreignNights,
                    totalNights = p.totalNights,
                    trips = p.trips,
                )
            },
            countryLabels = result.countryLabels,
            heatmapCells = result.heatmapCells,
        )
    }
}
