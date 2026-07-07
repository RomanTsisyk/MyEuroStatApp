package eu.eurostat.feature.population.data

import eu.eurostat.feature.population.domain.PopulationCohort
import eu.eurostat.feature.population.domain.PopulationData
import eu.eurostat.feature.population.domain.PopulationDataPoint
import eu.eurostat.feature.population.domain.PopulationSnapshot
import eu.eurostat.feature.population.domain.PopulationTimeSeries
import kotlinx.serialization.Serializable

/**
 * Serializable cache DTO for a full [PopulationData] frame — time series *and*
 * per-(country, year) cohort snapshots — persisted as one JSON blob per query
 * via `JsonBlobCache` (see `MultiDimCacheEntity`).
 *
 * This is what makes the demographic pyramid available offline: the legacy
 * flat `PopulationCacheEntity` table only holds `age=TOTAL` rows, so cohort
 * data used to be re-fetched on every screen open and was lost without
 * network. The DTO mirrors the domain shape but stays a separate type so the
 * domain layer remains free of serialization annotations; bump the `v{N}`
 * segment of the cache key when this shape changes incompatibly.
 */
@Serializable
data class PopulationCacheBlob(
    val series: List<Series>,
    val snapshots: List<Snapshot>,
) {

    /** Cached counterpart of [PopulationTimeSeries]. */
    @Serializable
    data class Series(
        val countryCode: String,
        val countryName: String,
        val points: List<Point>,
    )

    /**
     * Cached counterpart of [PopulationDataPoint]. The country code lives on
     * the enclosing [Series] and is re-attached when mapping back to domain.
     */
    @Serializable
    data class Point(
        val year: Int,
        val totalPopulation: Long,
        val malePopulation: Long? = null,
        val femalePopulation: Long? = null,
    )

    /** Cached counterpart of [PopulationSnapshot]. */
    @Serializable
    data class Snapshot(
        val countryCode: String,
        val countryName: String,
        val year: Int,
        val cohorts: List<Cohort>,
        val totalMale: Long,
        val totalFemale: Long,
        val total: Long,
    )

    /** Cached counterpart of [PopulationCohort]. */
    @Serializable
    data class Cohort(
        val ageCode: String,
        val ageLabel: String,
        val male: Long,
        val female: Long,
    )

    /** Rebuilds the domain aggregate, restoring the (countryCode, year)-keyed snapshot map. */
    fun toDomain(): PopulationData = PopulationData(
        timeSeries = series.map { s ->
            PopulationTimeSeries(
                countryCode = s.countryCode,
                countryName = s.countryName,
                points = s.points.map { p ->
                    PopulationDataPoint(
                        countryCode = s.countryCode,
                        year = p.year,
                        totalPopulation = p.totalPopulation,
                        malePopulation = p.malePopulation,
                        femalePopulation = p.femalePopulation,
                    )
                },
            )
        },
        snapshots = snapshots.associate { snap ->
            (snap.countryCode to snap.year) to PopulationSnapshot(
                countryCode = snap.countryCode,
                countryName = snap.countryName,
                year = snap.year,
                cohorts = snap.cohorts.map { c ->
                    PopulationCohort(
                        ageCode = c.ageCode,
                        ageLabel = c.ageLabel,
                        male = c.male,
                        female = c.female,
                    )
                },
                totalMale = snap.totalMale,
                totalFemale = snap.totalFemale,
                total = snap.total,
            )
        },
    )

    companion object {

        /** Flattens the domain aggregate into its serializable cache shape. */
        fun fromDomain(data: PopulationData): PopulationCacheBlob = PopulationCacheBlob(
            series = data.timeSeries.map { s ->
                Series(
                    countryCode = s.countryCode,
                    countryName = s.countryName,
                    points = s.points.map { p ->
                        Point(
                            year = p.year,
                            totalPopulation = p.totalPopulation,
                            malePopulation = p.malePopulation,
                            femalePopulation = p.femalePopulation,
                        )
                    },
                )
            },
            snapshots = data.snapshots.values.map { snap ->
                Snapshot(
                    countryCode = snap.countryCode,
                    countryName = snap.countryName,
                    year = snap.year,
                    cohorts = snap.cohorts.map { c ->
                        Cohort(
                            ageCode = c.ageCode,
                            ageLabel = c.ageLabel,
                            male = c.male,
                            female = c.female,
                        )
                    },
                    totalMale = snap.totalMale,
                    totalFemale = snap.totalFemale,
                    total = snap.total,
                )
            },
        )
    }
}
