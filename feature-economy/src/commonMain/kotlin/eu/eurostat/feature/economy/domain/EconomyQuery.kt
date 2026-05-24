package eu.eurostat.feature.economy.domain

data class EconomyQuery(
    val countryCodes: List<String>,
    val yearRange: IntRange,
    val unit: EconomyUnit = EconomyUnit.CP_MEUR,
)
