package eu.eurostat.feature.trade.domain

/**
 * A single trade observation for one country, year, and trading partner.
 *
 * Monetary values are denominated in **millions of EUR** (M€) as returned by the
 * Eurostat `ext_lt_intratrd` indicators `MIO_EXP_VAL`, `MIO_IMP_VAL`, `MIO_BAL_VAL`.
 * Divide by 1000 to display as billions (B €).
 *
 * @property exportsEur Exports in M€ (may be null for missing observations).
 * @property importsEur Imports in M€ (may be null for missing observations).
 * @property balanceEur Trade balance = exports - imports, in M€. Can be negative.
 */
data class TradeDataPoint(
    val countryCode: String,
    val year: Int,
    val partner: String,
    val exportsEur: Long?,
    val importsEur: Long?,
    val balanceEur: Long?,
)

/**
 * Aggregated trade time series for a single country and partner relationship.
 */
data class TradeTimeSeries(
    val countryCode: String,
    val countryName: String,
    val partner: String,
    val points: List<TradeDataPoint>,
)
