package eu.eurostat.feature.population.domain

/**
 * Domain model — clean, framework-agnostic, no JSON-stat artefacts.
 *
 * The repository is responsible for unpacking the JSON-stat flat array
 * into typed instances of this class. UseCases and UI never see raw DTOs.
 */
data class PopulationDataPoint(
    val countryCode: String,      // ISO 3166-1 alpha-2, e.g. "PL"
    val year: Int,
    val totalPopulation: Long,
    val malePopulation: Long?,    // null when dimension not present in response
    val femalePopulation: Long?,
)

/**
 * Aggregated time series for a single country.
 * Sorted ascending by year. Empty list is a legitimate empty-dataset signal,
 * not an error — UseCase or UI decides how to render that.
 */
data class PopulationTimeSeries(
    val countryCode: String,
    val countryName: String,
    val points: List<PopulationDataPoint>,
)

/**
 * One 5-year age cohort row for a demographic pyramid.
 *
 * @property ageCode Eurostat age dimension code (e.g. "Y5-9", "Y_LT5", "Y_GE85").
 * @property ageLabel Human-readable label (e.g. "5-9 years"). Sourced from
 *   `dimensionLabels["age"]` when available, otherwise falls back to [ageCode].
 * @property male Male population count for the cohort.
 * @property female Female population count for the cohort.
 */
data class PopulationCohort(
    val ageCode: String,
    val ageLabel: String,
    val male: Long,
    val female: Long,
)

/**
 * Single-year, single-country snapshot used to render the pyramid hero.
 *
 * Cohorts are ordered from youngest to oldest (Y_LT5 → Y_GE85).
 * UI flips the rendering order if needed.
 *
 * @property totalMale Sum of [PopulationCohort.male] across [cohorts] (or `sex=M` total).
 * @property totalFemale Sum of [PopulationCohort.female] across [cohorts] (or `sex=F` total).
 * @property total Headline total population for the (country, year).
 */
data class PopulationSnapshot(
    val countryCode: String,
    val countryName: String,
    val year: Int,
    val cohorts: List<PopulationCohort>,
    val totalMale: Long,
    val totalFemale: Long,
    val total: Long,
)

/**
 * Aggregate result returned by [PopulationRepository.observe].
 *
 * Bundles both the trend [timeSeries] (one per country, sex=T, age=TOTAL) and the
 * per-(country, year) [snapshots] used to build the demographic pyramid.
 *
 * Snapshots are keyed by (countryCode, year). UI picks one by user selection.
 */
data class PopulationData(
    val timeSeries: List<PopulationTimeSeries>,
    val snapshots: Map<Pair<String, Int>, PopulationSnapshot>,
)
