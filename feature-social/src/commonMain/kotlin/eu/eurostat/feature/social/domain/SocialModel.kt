package eu.eurostat.feature.social.domain

/**
 * One year of social-indicator values for a single country. All metrics are
 * percentages; any of them may be `null` when a particular series has no
 * observation for that (country, year) cell.
 */
data class SocialDataPoint(
    val countryCode: String,
    val year: Int,
    val povertyRate: Double? = null,
    val atRiskRate: Double? = null,
    val healthSatisfaction: Double? = null,
)

/**
 * Time series of [SocialDataPoint]s for one country, with points sorted by
 * year ascending.
 */
data class SocialTimeSeries(
    val countryCode: String,
    val countryName: String,
    val points: List<SocialDataPoint>,
)
