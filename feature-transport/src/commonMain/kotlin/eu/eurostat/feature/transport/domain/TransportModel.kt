package eu.eurostat.feature.transport.domain

/**
 * Transport mode. ALL fetches all three datasets in parallel and merges.
 * Specific modes fetch only their respective dataset.
 */
enum class TransportMode(val code: String) {
    ROAD("ROAD"),
    AIR("AIR"),
    SEA("SEA"),
    ALL("ALL"),
}

/**
 * A single transport observation for one country, year, and mode.
 * For ALL mode, all three passenger columns may be filled.
 * For a specific mode, only the matching column is non-null.
 */
data class TransportDataPoint(
    val countryCode: String,
    val year: Int,
    val mode: TransportMode,
    val roadPassengers: Long?,
    val airPassengers: Long?,
    val seaPassengers: Long?,
)

/**
 * Aggregated transport time series for a single country and mode.
 */
data class TransportTimeSeries(
    val countryCode: String,
    val countryName: String,
    val mode: TransportMode,
    val points: List<TransportDataPoint>,
)
