package eu.eurostat.feature.environment.domain

/**
 * Query for environment data. All three sectors (Total / Transport / Industry)
 * are fetched in a single request per dataset; the active sector is selected
 * client-side at render time.
 */
data class EnvironmentQuery(
    val countryCodes: List<String>,
    val yearRange: IntRange,
)
