package eu.eurostat.feature.tourism.domain

/**
 * The three c_resid categories Eurostat reports for tourism accommodation
 * nights in `tour_occ_ninat`.
 *
 * The on-the-wire codes (`DOM` / `FOR` / `TOTAL`) are exposed via [code] so that
 * mappers and API services don't have to maintain a parallel lookup.
 *
 * Note: Eurostat uses `FOR` (not `INTL`) for "foreign/international" residents.
 */
enum class TourismResidence(val code: String) {
    /** Nights spent by residents of the reporting country. */
    Domestic("DOM"),

    /** Nights spent by visitors from abroad. */
    Foreign("FOR"),

    /** Aggregate across both categories. */
    Total("TOTAL"),
}

/**
 * A single (country, year) tourism observation.
 *
 * The model is wide on the residence dimension — domestic, foreign and total
 * nights live in nullable columns on the same row — so the UI can chart all
 * three series in one pass without re-grouping. [trips] comes from
 * `tour_dem_tttot` and is independent of residence (the dataset has no
 * `c_resid` dimension), so it sits as a fourth metric.
 *
 * Any field may be `null` when the upstream cell is missing for that
 * (country, year, residence) tuple.
 */
data class TourismDataPoint(
    val countryCode: String,
    val year: Int,
    val domesticNights: Long? = null,
    val foreignNights: Long? = null,
    val totalNights: Long? = null,
    val trips: Long? = null,
)

/**
 * All tourism observations for a single country, sorted by year ascending.
 *
 * Use [points] for the stacked-bar chart (one bar per year, domestic at the
 * bottom + foreign on top).
 */
data class TourismTimeSeries(
    val countryCode: String,
    val countryName: String,
    val points: List<TourismDataPoint>,
)
