package eu.eurostat.feature.compare.domain

import eu.eurostat.core.common.formatCompactNumber
import eu.eurostat.core.common.formatGrouped
import eu.eurostat.core.common.formatPercent
import myeurostatapp.feature_compare.generated.resources.Res
import myeurostatapp.feature_compare.generated.resources.compare_indicator_air
import myeurostatapp.feature_compare.generated.resources.compare_indicator_exports
import myeurostatapp.feature_compare.generated.resources.compare_indicator_gdp
import myeurostatapp.feature_compare.generated.resources.compare_indicator_ghg
import myeurostatapp.feature_compare.generated.resources.compare_indicator_population
import myeurostatapp.feature_compare.generated.resources.compare_indicator_poverty
import myeurostatapp.feature_compare.generated.resources.compare_indicator_rd
import myeurostatapp.feature_compare.generated.resources.compare_indicator_tourism
import myeurostatapp.feature_compare.generated.resources.compare_unit_air
import myeurostatapp.feature_compare.generated.resources.compare_unit_exports
import myeurostatapp.feature_compare.generated.resources.compare_unit_gdp
import myeurostatapp.feature_compare.generated.resources.compare_unit_ghg
import myeurostatapp.feature_compare.generated.resources.compare_unit_population
import myeurostatapp.feature_compare.generated.resources.compare_unit_poverty
import myeurostatapp.feature_compare.generated.resources.compare_unit_rd
import myeurostatapp.feature_compare.generated.resources.compare_unit_tourism
import org.jetbrains.compose.resources.StringResource

/**
 * How a comparison [CompareIndicator]'s value should be rendered as text (used
 * for the chart legend's latest-value label). Kept small and orthogonal to the
 * indicator so the same three formatters serve any future indicators.
 */
enum class CompareValueFormat {
    /** Large count with a compact K/M/B suffix (e.g. population, passengers, nights). */
    Count,

    /** Grouped integer magnitude with locale thousands separators (e.g. GDP / exports in B€, GHG in Mt). */
    Grouped,

    /** Percentage with one decimal and a trailing `%` (e.g. poverty rate, R&D share of GDP). */
    Percent,
}

/**
 * The eight cross-module indicators the Compare screen can overlay — one
 * headline metric per feature module. Selecting an indicator drives which
 * repository [CompareDataSource] queries and how the resulting series are
 * labelled and coloured.
 *
 * [titleRes] / [unitRes] are Compose string resources (not plain [String]s)
 * because this enum is consumed from non-`@Composable` code
 * (`DefaultCompareComponent`); resolution happens at the render site. This
 * follows the same pattern as `feature-search`'s `SearchModule`. The resources
 * belong to feature-compare itself, since this module cannot resolve the other
 * feature modules' Compose Resources.
 *
 * Declaration order is the display order in the indicator selector (matches the
 * Overview browse grid).
 *
 * @property id stable, locale-independent identifier (analytics / diagnostics).
 * @property accentKey key passed to `Euro.moduleAccents.forModule(...)` to tint the screen.
 * @property titleRes localized indicator name shown in the selector and header.
 * @property unitRes localized short unit label shown on the y-axis / header.
 * @property datasetCode Eurostat dataset code cited by the `SourceFooter`.
 * @property valueFormat how [formatValue] renders a value in the legend.
 */
enum class CompareIndicator(
    val id: String,
    val accentKey: String,
    val titleRes: StringResource,
    val unitRes: StringResource,
    val datasetCode: String,
    val valueFormat: CompareValueFormat,
) {
    /** Total population (`demo_pjangroup`, sex=T, age=TOTAL) — count of people. */
    POPULATION(
        id = "population_total",
        accentKey = "population",
        titleRes = Res.string.compare_indicator_population,
        unitRes = Res.string.compare_unit_population,
        datasetCode = "demo_pjangroup",
        valueFormat = CompareValueFormat.Count,
    ),

    /** GDP at current prices (`nama_10_gdp`) — displayed in billions of EUR. */
    GDP(
        id = "gdp",
        accentKey = "economy",
        titleRes = Res.string.compare_indicator_gdp,
        unitRes = Res.string.compare_unit_gdp,
        datasetCode = "nama_10_gdp",
        valueFormat = CompareValueFormat.Grouped,
    ),

    /** Total greenhouse-gas emissions (`env_air_gge`, src_crf=TOTX4_MEMO) — Mt CO₂e. */
    GHG(
        id = "ghg_total",
        accentKey = "environment",
        titleRes = Res.string.compare_indicator_ghg,
        unitRes = Res.string.compare_unit_ghg,
        datasetCode = "env_air_gge",
        valueFormat = CompareValueFormat.Grouped,
    ),

    /** Intra-EU exports (`ext_lt_intratrd`, MIO_EXP_VAL) — displayed in billions of EUR. */
    EXPORTS(
        id = "intra_eu_exports",
        accentKey = "trade",
        titleRes = Res.string.compare_indicator_exports,
        unitRes = Res.string.compare_unit_exports,
        datasetCode = "ext_lt_intratrd",
        valueFormat = CompareValueFormat.Grouped,
    ),

    /** Air passengers carried (`avia_paoc`), with a road-passenger fallback — count. */
    AIR_PASSENGERS(
        id = "air_passengers",
        accentKey = "transport",
        titleRes = Res.string.compare_indicator_air,
        unitRes = Res.string.compare_unit_air,
        datasetCode = "avia_paoc",
        valueFormat = CompareValueFormat.Count,
    ),

    /** Tourism accommodation nights (`tour_occ_ninat`, c_resid=TOTAL) — count. */
    TOURISM_NIGHTS(
        id = "tourism_nights",
        accentKey = "tourism",
        titleRes = Res.string.compare_indicator_tourism,
        unitRes = Res.string.compare_unit_tourism,
        datasetCode = "tour_occ_ninat",
        valueFormat = CompareValueFormat.Count,
    ),

    /** At-risk-of-poverty rate (`ilc_li02`) — percentage. */
    POVERTY_RATE(
        id = "poverty_rate",
        accentKey = "social",
        titleRes = Res.string.compare_indicator_poverty,
        unitRes = Res.string.compare_unit_poverty,
        datasetCode = "ilc_li02",
        valueFormat = CompareValueFormat.Percent,
    ),

    /** Gross R&D expenditure (`rd_e_gerdtot`, sectperf=TOTAL) — percentage of GDP. */
    RD_SPEND(
        id = "rd_spend",
        accentKey = "science",
        titleRes = Res.string.compare_indicator_rd,
        unitRes = Res.string.compare_unit_rd,
        datasetCode = "rd_e_gerdtot",
        valueFormat = CompareValueFormat.Percent,
    ),
    ;

    /**
     * Renders [value] (already in this indicator's display unit) as a short,
     * locale-aware string for the chart legend, dispatching on [valueFormat].
     * Not `@Composable` — the delegated formatters are plain functions.
     */
    fun formatValue(value: Double): String = when (valueFormat) {
        CompareValueFormat.Count -> formatCompactNumber(value)
        CompareValueFormat.Grouped -> formatGrouped(value)
        CompareValueFormat.Percent -> formatPercent(value)
    }

    companion object {
        /** The initial indicator selected when the screen first opens. */
        val DEFAULT: CompareIndicator = GDP
    }
}
