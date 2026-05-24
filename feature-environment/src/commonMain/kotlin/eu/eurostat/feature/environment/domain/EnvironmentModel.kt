package eu.eurostat.feature.environment.domain

/**
 * Sector breakdown for GHG emissions and energy consumption.
 *
 * Maps to dataset-specific codes:
 *  - [Total]     → `src_crf=TOTX4_MEMO` (GHG) / `nrg_bal=FC_E` (energy)
 *  - [Transport] → `src_crf=CRF1A3`     (GHG) / `nrg_bal=FC_TRA_E` (energy)
 *  - [Industry]  → `src_crf=CRF1A2`     (GHG) / `nrg_bal=FC_IND_E` (energy)
 *
 * SDG-13 index is sector-less and is represented with a `null` sector on its data points.
 */
enum class EnvSector {
    Total,
    Transport,
    Industry,
}

/**
 * Metric currently surfaced by the Environment hero chart. SDG is sector-less; UI
 * hides the sector chip row when this is selected.
 */
enum class EnvMetric {
    Ghg,
    Energy,
    Sdg,
}

/**
 * A single observation for one country, year, and (optionally) sector.
 *
 * For GHG and Energy points, [sector] is non-null and the corresponding metric
 * field is populated. For SDG-13 points, [sector] is null and only [sdg13Index]
 * is populated.
 */
data class EnvironmentDataPoint(
    val countryCode: String,
    val year: Int,
    val sector: EnvSector?,
    val ghgMtCo2eq: Double? = null,
    val energyKtoe: Double? = null,
    val sdg13Index: Double? = null,
)

/**
 * Aggregated environment time series for a single country. Holds GHG, energy
 * (across all three sectors) and SDG-13 points in one place.
 *
 * Points are sorted by `(sector ordinal, year)`. SDG points (sector = null)
 * are sorted last by year.
 */
data class EnvironmentTimeSeries(
    val countryCode: String,
    val countryName: String,
    val points: List<EnvironmentDataPoint>,
)
