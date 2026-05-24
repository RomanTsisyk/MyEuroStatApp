package eu.eurostat.feature.trade.domain

data class TradeQuery(
    val countryCodes: List<String>,
    val yearRange: IntRange,
    val partner: String = "EU27_2020",
)
