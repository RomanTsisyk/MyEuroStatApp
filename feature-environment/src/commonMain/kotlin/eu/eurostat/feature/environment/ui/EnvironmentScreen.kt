package eu.eurostat.feature.environment.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.eurostat.core.charts.EurostatLineChart
import eu.eurostat.core.charts.model.ChartAxis
import eu.eurostat.core.charts.model.ChartPoint
import eu.eurostat.core.charts.model.ChartSeries
import eu.eurostat.core.common.EurostatCountries
import eu.eurostat.feature.environment.domain.EnvMetric
import eu.eurostat.feature.environment.domain.EnvSector
import eu.eurostat.feature.environment.domain.EnvironmentDataPoint
import eu.eurostat.feature.environment.domain.EnvironmentTimeSeries
import eu.eurostat.ui.component.ChipRow
import eu.eurostat.ui.component.CountryChipsRow
import eu.eurostat.ui.component.CountryPickerSheet
import eu.eurostat.ui.component.EuroCard
import eu.eurostat.ui.component.MetricDropdown
import eu.eurostat.ui.component.MetricHeadline
import eu.eurostat.ui.component.ModuleAppBar
import eu.eurostat.ui.component.SourceFooter
import eu.eurostat.ui.component.StaleBanner
import eu.eurostat.ui.component.StatTile
import eu.eurostat.ui.component.YearDropdown
import eu.eurostat.ui.component.states.EmptyState
import eu.eurostat.ui.component.states.ErrorState
import eu.eurostat.ui.component.states.LoadingShimmer
import eu.eurostat.ui.format.formatDecimal
import eu.eurostat.ui.layout.adaptiveChartHeight
import eu.eurostat.ui.layout.adaptiveContentMaxWidth
import eu.eurostat.ui.theme.Euro

/**
 * Environment screen. Renders the standard module chrome around a
 * stale-while-revalidate banner, a [MetricHeadline] (headline GHG · Total ·
 * latest year for the active country), a [MetricDropdown] for the active
 * metric paired with a sector [ChipRow] (hidden when the SDG metric is
 * selected), the hero [EurostatLineChart], two secondary [StatTile]s for the
 * other two metrics, and a [CountryChipsRow] derived from the loaded data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentScreen(component: EnvironmentComponent, onBack: () -> Unit = {}) {
    val state by component.state.collectAsState()
    val accent = Euro.moduleAccents.forModule("Environment")
    val contentState = state as? EnvironmentUiState.Content
    val appBarYear = contentState?.selectedYear
    val appBarCountry = contentState?.activeCountry?.let { code ->
        val name = EurostatCountries.byCode(code)?.name ?: code
        "$name · $code"
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Euro.colors.paper),
    ) {
        ModuleAppBar(
            title = "Environment",
            tagline = "Climate",
            accent = accent,
            onBack = onBack,
            year = appBarYear,
            country = appBarCountry,
            onSearch = {},
            onRefresh = { component.onIntent(EnvironmentIntent.Refresh) },
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            PullToRefreshBox(
                isRefreshing = state is EnvironmentUiState.Loading,
                onRefresh = { component.onIntent(EnvironmentIntent.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val s = state) {
                    EnvironmentUiState.Loading -> LoadingShimmer(
                        modifier = Modifier.padding(Euro.spacing.base),
                    )
                    is EnvironmentUiState.Empty -> EmptyState(
                        headline = "no data",
                        body = "No environment data for the selected filters.",
                    )
                    is EnvironmentUiState.Error -> ErrorState(
                        headline = "Couldn't load environment",
                        body = s.message,
                        onRetry = if (s.canRetry) {
                            { component.onIntent(EnvironmentIntent.Retry) }
                        } else {
                            null
                        },
                    )
                    is EnvironmentUiState.Content -> EnvironmentContent(
                        accent = accent,
                        content = s,
                        onSelectCountry = { code ->
                            component.onIntent(EnvironmentIntent.SelectActiveCountry(code))
                        },
                        onConfirmCountries = { codes ->
                            component.onIntent(EnvironmentIntent.SelectCountries(codes.toList()))
                        },
                        onSelectSector = { sector ->
                            component.onIntent(EnvironmentIntent.SelectSector(sector))
                        },
                        onSelectMetric = { metric ->
                            component.onIntent(EnvironmentIntent.SelectMetric(metric))
                        },
                        onSelectYear = { year ->
                            component.onIntent(EnvironmentIntent.SelectYear(year))
                        },
                    )
                }
            }
        }
        SourceFooter(
            dataset = "env_air_gge · +2",
            staleness = if ((state as? EnvironmentUiState.Content)?.isStale == true) {
                "cached"
            } else {
                "fresh"
            },
            stale = (state as? EnvironmentUiState.Content)?.isStale == true,
            modifier = Modifier
                .padding(horizontal = Euro.spacing.base)
                .navigationBarsPadding(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EnvironmentContent(
    accent: Color,
    content: EnvironmentUiState.Content,
    onSelectCountry: (String) -> Unit,
    onConfirmCountries: (Set<String>) -> Unit,
    onSelectSector: (EnvSector) -> Unit,
    onSelectMetric: (EnvMetric) -> Unit,
    onSelectYear: (Int) -> Unit,
) {
    val timeSeries = content.timeSeries
    val countries = content.availableCountries
    val activeCountry = content.activeCountry

    val sector = content.activeSector
    val metric = content.activeMetric
    var showCountryPicker by remember { mutableStateOf(false) }

    val activeSeries = remember(timeSeries, activeCountry) {
        timeSeries.firstOrNull { it.countryCode == activeCountry }
    }
    val euAccent = Euro.colors.warn

    val headlineSector = if (metric == EnvMetric.Sdg) null else sector
    val selectedYear = content.selectedYear
    val availableYears = content.availableYears
    val headline = remember(activeSeries, metric, headlineSector, selectedYear) {
        latestHeadline(activeSeries, metric, headlineSector, selectedYear)
    }

    val chartSeries = remember(timeSeries, metric, sector, activeCountry, accent, euAccent) {
        buildChartSeries(
            timeSeries = timeSeries,
            metric = metric,
            sector = sector,
            activeCountry = activeCountry,
            activeColor = accent,
            comparisonColor = euAccent,
        )
    }

    val secondaryTiles = remember(activeSeries, metric, sector, content.selectedYear) {
        secondaryTileValues(activeSeries, metric, sector, content.selectedYear)
    }

    val chartHeight = adaptiveChartHeight(compact = 180.dp, medium = 240.dp, expanded = 300.dp)
    val maxW = adaptiveContentMaxWidth()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (maxW != Dp.Unspecified) Modifier.widthIn(max = maxW) else Modifier)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Euro.spacing.base),
        verticalArrangement = Arrangement.spacedBy(Euro.spacing.m),
    ) {
        if (content.isStale) {
            StaleBanner(modifier = Modifier.padding(top = Euro.spacing.s))
        }

        Spacer(Modifier.height(Euro.spacing.xs))

        MetricHeadline(
            value = headline.value,
            unit = headline.unit,
            subtitle = headline.subtitle,
            year = headline.year,
            accent = accent,
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Euro.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Euro.spacing.xs),
        ) {
            MetricDropdown(
                label = "metric",
                value = metric.label(),
                options = EnvMetric.entries.map { it.label() },
                onSelect = { picked ->
                    val resolved = EnvMetric.entries.firstOrNull { it.label() == picked } ?: metric
                    onSelectMetric(resolved)
                },
            )
            if (availableYears.isNotEmpty()) {
                YearDropdown(
                    selectedYear = selectedYear,
                    years = availableYears,
                    onSelect = onSelectYear,
                )
            }
            if (metric != EnvMetric.Sdg) {
                ChipRow(
                    options = EnvSector.entries.map { it.label() },
                    selected = setOf(sector.label()),
                    onToggle = { picked ->
                        val resolved = EnvSector.entries.firstOrNull { it.label() == picked } ?: sector
                        onSelectSector(resolved)
                    },
                )
            }
        }

        EuroCard {
            Column {
                Text(
                    text = chartTitle(metric, if (metric == EnvMetric.Sdg) null else sector),
                    style = Euro.typography.bodySmall,
                    color = Euro.colors.muted,
                )
                Spacer(Modifier.height(Euro.spacing.xs))
                val hasData = chartSeries.any { series -> series.points.any { it.y != null } }
                if (!hasData) {
                    EmptyState(
                        headline = "no data",
                        body = "No values for the selected metric / sector combination.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight),
                    )
                } else {
                    EurostatLineChart(
                        series = chartSeries,
                        xAxis = ChartAxis(label = "year"),
                        yAxis = ChartAxis(label = yAxisLabel(metric)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight),
                    )
                    Spacer(Modifier.height(Euro.spacing.s))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Euro.spacing.base),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        chartSeries.forEach { LegendDot(color = it.color, label = it.label) }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Euro.spacing.s),
        ) {
            StatTile(
                label = secondaryTiles.first.label,
                value = secondaryTiles.first.value,
                delta = secondaryTiles.first.unit,
                bordered = true,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = secondaryTiles.second.label,
                value = secondaryTiles.second.value,
                delta = secondaryTiles.second.unit,
                bordered = true,
                modifier = Modifier.weight(1f),
            )
        }

        CountryChipsRow(
            countries = countries,
            active = setOf(activeCountry),
            onSelect = { code -> onSelectCountry(code) },
            onAdd = { showCountryPicker = true },
            modifier = Modifier.fillMaxWidth(),
        )

        if (showCountryPicker) {
            CountryPickerSheet(
                selected = countries.toSet(),
                onConfirm = { selected ->
                    showCountryPicker = false
                    onConfirmCountries(selected)
                },
                onDismiss = { showCountryPicker = false },
            )
        }

        Spacer(Modifier.height(Euro.spacing.s))
    }
    } // end Box
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.size(Euro.spacing.xs))
        Text(
            text = label,
            style = Euro.typography.bodySmall,
            color = Euro.colors.muted,
        )
    }
}

// ---------------------------------------------------------------------------
// Pure helpers (no Composable state)
// ---------------------------------------------------------------------------

/** Selector returning the per-point value for a given metric. */
private fun EnvironmentDataPoint.valueFor(metric: EnvMetric): Double? = when (metric) {
    EnvMetric.Ghg -> ghgMtCo2eq
    EnvMetric.Energy -> energyKtoe
    EnvMetric.Sdg -> sdg13Index
}

private fun EnvMetric.label(): String = when (this) {
    EnvMetric.Ghg -> "GHG"
    EnvMetric.Energy -> "Energy"
    EnvMetric.Sdg -> "SDG"
}

private fun EnvSector.label(): String = when (this) {
    EnvSector.Total -> "TOTAL"
    EnvSector.Transport -> "TRANSPORT"
    EnvSector.Industry -> "INDUSTRY"
}

private fun chartTitle(metric: EnvMetric, sector: EnvSector?): String = when (metric) {
    EnvMetric.Ghg -> "GHG emissions · Mt CO₂-eq" + (sector?.let { " · ${it.label()}" } ?: "")
    EnvMetric.Energy -> "Final energy consumption · ktoe" + (sector?.let { " · ${it.label()}" } ?: "")
    EnvMetric.Sdg -> "SDG 13 climate index · 1990=100"
}

private fun yAxisLabel(metric: EnvMetric): String = when (metric) {
    EnvMetric.Ghg -> "Mt CO₂-eq"
    EnvMetric.Energy -> "ktoe"
    EnvMetric.Sdg -> "index"
}

private data class Headline(
    val value: String,
    val unit: String,
    val subtitle: String,
    val year: String,
)

/**
 * Pick the observation for [selectedYear] for the active country/metric/sector
 * combination and format it for the [MetricHeadline]. Falls back to the most
 * recent point only if no point exists for [selectedYear]. For SDG, [sector] is
 * null and we read sector-less points.
 */
private fun latestHeadline(
    series: EnvironmentTimeSeries?,
    metric: EnvMetric,
    sector: EnvSector?,
    selectedYear: Int,
): Headline {
    val unit = when (metric) {
        EnvMetric.Ghg -> "Mt"
        EnvMetric.Energy -> "ktoe"
        EnvMetric.Sdg -> "idx"
    }
    val subtitleBase = when (metric) {
        EnvMetric.Ghg -> "GHG · CO₂-eq"
        EnvMetric.Energy -> "Final energy"
        EnvMetric.Sdg -> "SDG 13 · 1990=100"
    }
    val subtitle = if (sector != null) "$subtitleBase · ${sector.label()} sector" else subtitleBase

    val filtered = series?.points
        ?.filter { it.sector == sector }
        ?.mapNotNull { point -> point.valueFor(metric)?.let { v -> point.year to v } }
        ?: emptyList()

    val selected = filtered.firstOrNull { it.first == selectedYear }

    return Headline(
        value = selected?.second?.let { formatValue(it, metric) } ?: "—",
        unit = unit,
        subtitle = subtitle,
        year = selected?.first?.toString() ?: if (selectedYear != 0) selectedYear.toString() else "",
    )
}

/** Format a metric value for compact display in the headline / tiles. */
private fun formatValue(value: Double, metric: EnvMetric): String = when (metric) {
    EnvMetric.Ghg -> formatDecimal(value, decimals = 0)
    EnvMetric.Energy -> formatDecimal(value, decimals = 0)
    EnvMetric.Sdg -> formatDecimal(value, decimals = 1)
}

private data class TilePair(val first: TileValue, val second: TileValue)
private data class TileValue(val label: String, val value: String, val unit: String)

/**
 * Produce the 2-up secondary tiles, showing the OTHER two metrics' latest
 * values for the active country (and active sector where applicable).
 */
private fun secondaryTileValues(
    series: EnvironmentTimeSeries?,
    metric: EnvMetric,
    sector: EnvSector,
    selectedYear: Int,
): TilePair {
    val others = EnvMetric.entries.filter { it != metric }
    val tiles = others.map { otherMetric ->
        val effectiveSector: EnvSector? = if (otherMetric == EnvMetric.Sdg) null else sector
        // Show the other metric's value at the same year the headline is showing,
        // not the metric's own latest — keeps the year display consistent across tiles.
        val selectedValue = series?.points
            ?.filter { it.sector == effectiveSector && it.year == selectedYear }
            ?.firstNotNullOfOrNull { p -> p.valueFor(otherMetric) }
        TileValue(
            label = tileLabel(otherMetric),
            value = selectedValue?.let { formatValue(it, otherMetric) } ?: "—",
            unit = tileUnit(otherMetric),
        )
    }
    val placeholder = TileValue(label = "—", value = "—", unit = "")
    return TilePair(
        first = tiles.getOrNull(0) ?: placeholder,
        second = tiles.getOrNull(1) ?: placeholder,
    )
}

private fun tileLabel(metric: EnvMetric): String = when (metric) {
    EnvMetric.Ghg -> "GHG"
    EnvMetric.Energy -> "energy use"
    EnvMetric.Sdg -> "SDG 13 idx"
}

private fun tileUnit(metric: EnvMetric): String = when (metric) {
    EnvMetric.Ghg -> "Mt CO₂-eq"
    EnvMetric.Energy -> "ktoe"
    EnvMetric.Sdg -> "1990=100"
}

/**
 * Build [ChartSeries] for the line chart: the active country always renders;
 * one comparison country (first non-active) renders in the warn accent so the
 * chart isn't a single line.
 */
private fun buildChartSeries(
    timeSeries: List<EnvironmentTimeSeries>,
    metric: EnvMetric,
    sector: EnvSector,
    activeCountry: String,
    activeColor: Color,
    comparisonColor: Color,
): List<ChartSeries> {
    val effectiveSector: EnvSector? = if (metric == EnvMetric.Sdg) null else sector

    fun pointsFor(series: EnvironmentTimeSeries): List<ChartPoint> =
        series.points
            .filter { it.sector == effectiveSector }
            .sortedBy { it.year }
            .map { ChartPoint(it.year.toDouble(), it.valueFor(metric)) }

    val active = timeSeries.firstOrNull { it.countryCode == activeCountry }
    val comparison = timeSeries.firstOrNull { it.countryCode != activeCountry }

    return listOfNotNull(
        active?.let { ChartSeries(label = it.countryCode, color = activeColor, points = pointsFor(it)) },
        comparison?.let { ChartSeries(label = it.countryCode, color = comparisonColor, points = pointsFor(it)) },
    )
}
