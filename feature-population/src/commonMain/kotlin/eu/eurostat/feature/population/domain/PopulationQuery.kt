package eu.eurostat.feature.population.domain

/**
 * Query parameters for the population dataset.
 *
 * @property countryCodes Eurostat `geo` codes (e.g. "DE", "EU27_2020").
 * @property yearRange Inclusive year range mapped to the `time` dimension.
 * @property includeCohorts When `true`, the API also requests all 18 five-year
 *   age cohorts so the repository can build [PopulationSnapshot]s for the
 *   demographic pyramid. When `false`, only `age=TOTAL` is requested
 *   (smaller payload — useful for trend-only consumers).
 */
data class PopulationQuery(
    val countryCodes: List<String>,
    val yearRange: IntRange,
    val includeCohorts: Boolean = true,
)
