package eu.eurostat.feature.science.domain

data class ScienceQuery(
    val countryCodes: List<String>,
    val yearRange: IntRange,
)
