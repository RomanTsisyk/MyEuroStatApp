package eu.eurostat.feature.population.data

import eu.eurostat.core.jsonstat.JsonStatCell
import eu.eurostat.feature.population.domain.PopulationCohort
import eu.eurostat.feature.population.domain.PopulationDataPoint
import eu.eurostat.feature.population.domain.PopulationSnapshot
import eu.eurostat.feature.population.domain.PopulationTimeSeries

object PopulationCellMapper {

    /** Canonical youngest-to-oldest ordering for 5-year cohort codes. */
    private val COHORT_ORDER: List<String> = listOf(
        "Y_LT5", "Y5-9", "Y10-14", "Y15-19", "Y20-24",
        "Y25-29", "Y30-34", "Y35-39", "Y40-44", "Y45-49",
        "Y50-54", "Y55-59", "Y60-64", "Y65-69", "Y70-74",
        "Y75-79", "Y80-84", "Y_GE85",
    )
    private val COHORT_INDEX: Map<String, Int> =
        COHORT_ORDER.withIndex().associate { (i, code) -> code to i }

    /**
     * Maps a flat list of JsonStatCells into PopulationTimeSeries per country.
     *
     * Only cells where `age == "TOTAL"` participate. Bins by (geo, time), reads
     * sex=T for total, sex=M/sex=F for breakdown. Eurostat time values are year
     * strings like "2020".
     */
    fun map(cells: List<JsonStatCell>): List<PopulationTimeSeries> {
        // Group cells by (country, year) for age=TOTAL only.
        data class Key(val country: String, val year: Int)

        val byKey = mutableMapOf<Key, MutableMap<String, Double?>>()

        for (cell in cells) {
            val age = cell.dimensions["age"]
            if (age != null && age != "TOTAL") continue
            val country = cell.dimensions["geo"] ?: continue
            val yearStr = cell.dimensions["time"] ?: continue
            val year = yearStr.toIntOrNull() ?: continue
            val sex = cell.dimensions["sex"] ?: continue
            val key = Key(country, year)
            byKey.getOrPut(key) { mutableMapOf() }[sex] = cell.value
        }

        val byCountry = mutableMapOf<String, MutableList<PopulationDataPoint>>()
        val countryLabels = mutableMapOf<String, String>()

        for ((key, sexMap) in byKey) {
            val totalRaw = sexMap["T"]
            val total = totalRaw?.toLong() ?: continue // skip rows without total
            val male = sexMap["M"]?.toLong()
            val female = sexMap["F"]?.toLong()

            byCountry.getOrPut(key.country) { mutableListOf() }.add(
                PopulationDataPoint(
                    countryCode = key.country,
                    year = key.year,
                    totalPopulation = total,
                    malePopulation = male,
                    femalePopulation = female,
                )
            )
        }

        // Collect country labels from cells (dimensionLabels["geo"] holds the human-readable name)
        for (cell in cells) {
            val code = cell.dimensions["geo"] ?: continue
            if (code !in countryLabels) {
                countryLabels[code] = cell.dimensionLabels["geo"] ?: code
            }
        }

        return byCountry.map { (country, points) ->
            PopulationTimeSeries(
                countryCode = country,
                countryName = countryLabels[country] ?: country,
                points = points.sortedBy { it.year },
            )
        }.sortedBy { it.countryCode }
    }

    /**
     * Builds a (country, year) → [PopulationSnapshot] map for the demographic
     * pyramid. Considers only cells where `age` matches a known 5-year cohort
     * code (see [COHORT_ORDER]). Cells with null values are skipped.
     *
     * `totalMale` / `totalFemale` are taken from `sex=M` / `sex=F` with
     * `age=TOTAL` when present, otherwise computed as the cohort sum.
     * `total` falls back to `sex=T`+`age=TOTAL` or to `totalMale + totalFemale`.
     */
    fun mapToSnapshots(cells: List<JsonStatCell>): Map<Pair<String, Int>, PopulationSnapshot> {
        data class Key(val country: String, val year: Int)

        // (country, year) → ageCode → (male, female) and labels
        val cohortBuckets =
            mutableMapOf<Key, MutableMap<String, LongArray>>() // [male, female]
        val cohortLabels = mutableMapOf<String, String>()
        // (country, year) → sex → value at age=TOTAL
        val totals = mutableMapOf<Key, MutableMap<String, Long>>()
        val countryLabels = mutableMapOf<String, String>()

        for (cell in cells) {
            val country = cell.dimensions["geo"] ?: continue
            val yearStr = cell.dimensions["time"] ?: continue
            val year = yearStr.toIntOrNull() ?: continue
            val sex = cell.dimensions["sex"] ?: continue
            val age = cell.dimensions["age"] ?: continue
            val raw = cell.value ?: continue
            val key = Key(country, year)

            if (country !in countryLabels) {
                countryLabels[country] = cell.dimensionLabels["geo"] ?: country
            }

            when {
                age == "TOTAL" -> {
                    totals.getOrPut(key) { mutableMapOf() }[sex] = raw.toLong()
                }
                COHORT_INDEX.containsKey(age) -> {
                    if (age !in cohortLabels) {
                        cohortLabels[age] = cell.dimensionLabels["age"] ?: age
                    }
                    if (sex == "M" || sex == "F") {
                        val bucket = cohortBuckets
                            .getOrPut(key) { mutableMapOf() }
                            .getOrPut(age) { longArrayOf(0L, 0L) }
                        if (sex == "M") bucket[0] = raw.toLong() else bucket[1] = raw.toLong()
                    }
                }
            }
        }

        val result = mutableMapOf<Pair<String, Int>, PopulationSnapshot>()
        for ((key, ageMap) in cohortBuckets) {
            val cohorts = ageMap
                .map { (code, mf) ->
                    PopulationCohort(
                        ageCode = code,
                        ageLabel = cohortLabels[code] ?: code,
                        male = mf[0],
                        female = mf[1],
                    )
                }
                .sortedBy { COHORT_INDEX[it.ageCode] ?: Int.MAX_VALUE }

            val totalForKey = totals[key].orEmpty()
            val totalMale = totalForKey["M"] ?: cohorts.sumOf { it.male }
            val totalFemale = totalForKey["F"] ?: cohorts.sumOf { it.female }
            val total = totalForKey["T"] ?: (totalMale + totalFemale)

            result[key.country to key.year] = PopulationSnapshot(
                countryCode = key.country,
                countryName = countryLabels[key.country] ?: key.country,
                year = key.year,
                cohorts = cohorts,
                totalMale = totalMale,
                totalFemale = totalFemale,
                total = total,
            )
        }
        return result
    }
}
