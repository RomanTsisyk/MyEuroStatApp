package eu.eurostat.feature.social.domain

data class SocialQuery(
    val countryCodes: List<String>,
    val yearRange: IntRange,
)
