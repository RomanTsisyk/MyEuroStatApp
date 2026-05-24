package eu.eurostat.feature.transport.domain

data class TransportQuery(
    val countryCodes: List<String>,
    val yearRange: IntRange,
    val mode: TransportMode = TransportMode.ALL,
)
