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
import androidx.compose.foundation.shape.CircleShape
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
import eu.eurostat.ui.layout.AdaptiveTwoPane
import eu.eurostat.ui.layout.adaptiveChartHeight
import eu.eurostat.ui.theme.Euro
import myeurostatapp.feature_environment.generated.resources.Res
import myeurostatapp.feature_environment.generated.resources.environment_chart_empty_body
import myeurostatapp.feature_environment.generated.resources.environment_chart_title_energy
import myeurostatapp.feature_environment.generated.resources.environment_chart_title_ghg
import myeurostatapp.feature_environment.generated.resources.environment_chart_title_sdg
import myeurostatapp.feature_environment.generated.resources.environment_empty_body
import myeurostatapp.feature_environment.generated.resources.environment_empty_headline
import myeurostatapp.feature_environment.generated.resources.environment_error_headline
import myeurostatapp.feature_environment.generated.resources.environment_footer_staleness_fresh
import myeurostatapp.feature_environment.generated.resources.environment_footer_staleness_stale
import myeurostatapp.feature_environment.generated.resources.environment_headline_subtitle_energy
import myeurostatapp.feature_environment.generated.resources.environment_headline_subtitle_ghg
import myeurostatapp.feature_environment.generated.resources.environment_headline_subtitle_sdg
import myeurostatapp.feature_environment.generated.resources.environment_headline_subtitle_sector
import myeurostatapp.feature_environment.generated.resources.environment_headline_unit_energy
import myeurostatapp.feature_environment.generated.resources.environment_headline_unit_ghg
import myeurostatapp.feature_environment.generated.resources.environment_headline_unit_sdg
import myeurostatapp.feature_environment.generated.resources.environment_metric_dropdown_label
import myeurostatapp.feature_environment.generated.resources.environment_metric_energy
import myeurostatapp.feature_environment.generated.resources.environment_metric_ghg
import myeurostatapp.feature_environment.generated.resources.environment_metric_sdg
import myeurostatapp.feature_environment.generated.resources.environment_module_tagline
import myeurostatapp.feature_environment.generated.resources.environment_module_title
import myeurostatapp.feature_environment.generated.resources.environment_sector_industry
import myeurostatapp.feature_environment.generated.resources.environment_sector_total
import myeurostatapp.feature_environment.generated.resources.environment_sector_transport
import myeurostatapp.feature_environment.generated.resources.environment_tile_label_energy
import myeurostatapp.feature_environment.generated.resources.environment_tile_label_ghg
import myeurostatapp.feature_environment.generated.resources.environment_tile_label_sdg
import myeurostatapp.feature_environment.generated.resources.environment_tile_unit_energy
import myeurostatapp.feature_environment.generated.resources.environment_tile_unit_ghg
import myeurostatapp.feature_environment.generated.resources.environment_tile_unit_sdg
import org.jetbrains.compose.resources.stringResource

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
            title = stringResource(Res.string.environment_module_title),
            tagline = stringResource(Res.string.environment_module_tagline),
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
                        headline = stringResource(Res.string.environment_empty_headline),
                        body = stringResource(Res.string.environment_empty_body),
                    )
                    is EnvironmentUiState.Error -> ErrorState(
                        headline = stringResource(Res.string.environment_error_headline),
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
                stringResource(Res.string.environment_footer_staleness_stale)
            } else {
                stringResource(Res.string.environment_footer_staleness_fresh)
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

    // Resolved once per composition — stringResource() is @Composable and
    // cannot be called from the onSelect/onToggle callbacks below, from
    // remember{} blocks, or from the plain (non-composable) helper functions
    // at the bottom of this file, so every localized string this screen needs
    // is looked up here and threaded through as plain values/maps instead.
    val metricLabels = mapOf(
        EnvMetric.Ghg to stringResource(Res.string.environment_metric_ghg),
        EnvMetric.Energy to stringResource(Res.string.environment_metric_energy),
        EnvMetric.Sdg to stringResource(Res.string.environment_metric_sdg),
    )
    val sectorLabels = mapOf(
        EnvSector.Total to stringResource(Res.string.environment_sector_total),
        EnvSector.Transport to stringResource(Res.string.environment_sector_transport),
        EnvSector.Industry to stringResource(Res.string.environment_sector_industry),
    )
    val headlineUnits = mapOf(
        EnvMetric.Ghg to stringResource(Res.string.environment_headline_unit_ghg),
        EnvMetric.Energy to stringResource(Res.string.environment_headline_unit_energy),
        EnvMetric.Sdg to stringResource(Res.string.environment_headline_unit_sdg),
    )
    val headlineSubtitleBases = mapOf(
        EnvMetric.Ghg to stringResource(Res.string.environment_headline_subtitle_ghg),
        EnvMetric.Energy to stringResource(Res.string.environment_headline_subtitle_energy),
        EnvMetric.Sdg to stringResource(Res.string.environment_headline_subtitle_sdg),
    )
    val chartTitles = mapOf(
        EnvMetric.Ghg to stringResource(Res.string.environment_chart_title_ghg),
        EnvMetric.Energy to stringResource(Res.string.environment_chart_title_energy),
        EnvMetric.Sdg to stringResource(Res.string.environment_chart_title_sdg),
    )
    val tileLabels = mapOf(
        EnvMetric.Ghg to stringResource(Res.string.environment_tile_label_ghg),
        EnvMetric.Energy to stringResource(Res.string.environment_tile_label_energy),
        EnvMetric.Sdg to stringResource(Res.string.environment_tile_label_sdg),
    )
    val tileUnits = mapOf(
        EnvMetric.Ghg to stringResource(Res.string.environment_tile_unit_ghg),
        EnvMetric.Energy to stringResource(Res.string.environment_tile_unit_energy),
        EnvMetric.Sdg to stringResource(Res.string.environment_tile_unit_sdg),
    )
    val metricDropdownLabel = stringResource(Res.string.environment_metric_dropdown_label)
    val chartEmptyHeadline = stringResource(Res.string.environment_empty_headline)
    val chartEmptyBody = stringResource(Res.string.environment_chart_empty_body)

    val activeSeries = remember(timeSeries, activeCountry) {
        timeSeries.firstOrNull { it.countryCode == activeCountry }
    }
    val euAccent = Euro.colors.warn

    val headlineSector = if (metric == EnvMetric.Sdg) null else sector
    val selectedYear = content.selectedYear
    val availableYears = content.availableYears

    // Pure (string-resource-free) selection of the raw value/year — safe to
    // memoize in remember{}. Unit/subtitle localization happens right below,
    // in this composable's own scope.
    val headlineValueYear = remember(activeSeries, metric, headlineSector, selectedYear) {
        selectHeadlineValueAndYear(activeSeries, metric, headlineSector, selectedYear)
    }
    val headlineUnit = headlineUnits.getValue(metric)
    val headlineSubtitleBase = headlineSubtitleBases.getValue(metric)
    val headlineSubtitle = headlineSector?.let {
        stringResource(
            Res.string.environment_headline_subtitle_sector,
            headlineSubtitleBase,
            sectorLabels.getValue(it),
        )
    } ?: headlineSubtitleBase

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
    val resolvedChartTitle = chartTitles.getValue(metric) +
        (headlineSector?.let { " · ${sectorLabels.getValue(it)}" } ?: "")

    // Pure (string-resource-free) values for the two metrics OTHER than the
    // active one — safe to memoize in remember{}. Label/unit localization is
    // resolved above (tileLabels / tileUnits) and applied in tilesSection.
    val secondaryTiles = remember(activeSeries, metric, sector, content.selectedYear) {
        secondaryMetricValues(activeSeries, metric, sector, content.selectedYear)
    }

    val chartHeight = adaptiveChartHeight(compact = 180.dp, medium = 240.dp, expanded = 300.dp)

    // Sections shared between the compact (phone) ordering and the ≥840dp
    // two-pane split. Purely structural — all state stays on the component.
    val headlineSection: @Composable () -> Unit = {
        MetricHeadline(
            value = headlineValueYear.first,
            unit = headlineUnit,
            subtitle = headlineSubtitle,
            year = headlineValueYear.second,
            accent = accent,
        )
    }
    val metricDropdown: @Composable () -> Unit = {
        MetricDropdown(
            label = metricDropdownLabel,
            value = metricLabels.getValue(metric),
            options = EnvMetric.entries.map { metricLabels.getValue(it) },
            onSelect = { picked ->
                val resolved = EnvMetric.entries.firstOrNull { metricLabels[it] == picked } ?: metric
                onSelectMetric(resolved)
            },
        )
    }
    val yearDropdown: @Composable () -> Unit = {
        if (availableYears.isNotEmpty()) {
            YearDropdown(
                selectedYear = selectedYear,
                years = availableYears,
                onSelect = onSelectYear,
            )
        }
    }
    val sectorChips: @Composable () -> Unit = {
        if (metric != EnvMetric.Sdg) {
            ChipRow(
                options = EnvSector.entries.map { sectorLabels.getValue(it) },
                selected = setOf(sectorLabels.getValue(sector)),
                onToggle = { picked ->
                    val resolved = EnvSector.entries.firstOrNull { sectorLabels[it] == picked } ?: sector
                    onSelectSector(resolved)
                },
            )
        }
    }
    val chartSection: @Composable () -> Unit = {
        EuroCard {
            Column {
                Text(
                    text = resolvedChartTitle,
                    style = Euro.typography.bodySmall,
                    color = Euro.colors.muted,
                )
                Spacer(Modifier.height(Euro.spacing.xs))
                val hasData = chartSeries.any { series -> series.points.any { it.y != null } }
                if (!hasData) {
                    EmptyState(
                        headline = chartEmptyHeadline,
                        body = chartEmptyBody,
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
    }
    val tilesSection: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Euro.spacing.s),
        ) {
            secondaryTiles.forEach { (otherMetric, value) ->
                StatTile(
                    label = tileLabels.getValue(otherMetric),
                    value = value,
                    delta = tileUnits.getValue(otherMetric),
                    bordered = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
    val countriesSection: @Composable () -> Unit = {
        CountryChipsRow(
            countries = countries,
            active = setOf(activeCountry),
            onSelect = { code -> onSelectCountry(code) },
            onAdd = { showCountryPicker = true },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    AdaptiveTwoPane(
        modifier = Modifier.fillMaxSize(),
        controls = {
            Spacer(Modifier.height(Euro.spacing.xs))
            metricDropdown()
            yearDropdown()
            sectorChips()
            countriesSection()
            Spacer(Modifier.height(Euro.spacing.s))
        },
        content = {
            if (content.isStale) {
                StaleBanner(modifier = Modifier.padding(top = Euro.spacing.s))
            }
            Spacer(Modifier.height(Euro.spacing.xs))
            headlineSection()
            chartSection()
            tilesSection()
            Spacer(Modifier.height(Euro.spacing.s))
        },
        compact = {
            if (content.isStale) {
                StaleBanner(modifier = Modifier.padding(top = Euro.spacing.s))
            }
            Spacer(Modifier.height(Euro.spacing.xs))
            headlineSection()
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Euro.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Euro.spacing.xs),
            ) {
                metricDropdown()
                yearDropdown()
                sectorChips()
            }
            chartSection()
            tilesSection()
            countriesSection()
            Spacer(Modifier.height(Euro.spacing.s))
        },
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
// Pure helpers (no Composable state, no string resources — safe to call from
// remember{} blocks and onSelect/onToggle callbacks)
// ---------------------------------------------------------------------------

/** Selector returning the per-point value for a given metric. */
private fun EnvironmentDataPoint.valueFor(metric: EnvMetric): Double? = when (metric) {
    EnvMetric.Ghg -> ghgMtCo2eq
    EnvMetric.Energy -> energyKtoe
    EnvMetric.Sdg -> sdg13Index
}

/**
 * Axis title metadata — currently unused for display: [ChartAxis.label] is
 * documented as "rendered by callers, not by charts themselves" and no caller
 * in this codebase renders it, so this stays plain (unlocalized) English.
 */
private fun yAxisLabel(metric: EnvMetric): String = when (metric) {
    EnvMetric.Ghg -> "Mt CO₂-eq"
    EnvMetric.Energy -> "ktoe"
    EnvMetric.Sdg -> "index"
}

/** Format a metric value for compact display in the headline / tiles. */
private fun formatValue(value: Double, metric: EnvMetric): String = when (metric) {
    EnvMetric.Ghg -> formatDecimal(value, decimals = 0)
    EnvMetric.Energy -> formatDecimal(value, decimals = 0)
    EnvMetric.Sdg -> formatDecimal(value, decimals = 1)
}

/**
 * Pick the observation for [selectedYear] for the active country/metric/sector
 * combination and format its value for the [MetricHeadline]. Falls back to an
 * em dash when nothing matches. For SDG, [sector] is null and we read
 * sector-less points. Localized unit/subtitle text is resolved by the
 * composable caller ([EnvironmentContent]) — this helper only returns the
 * (value, year) pair and must stay resource-free (it backs a remember{} call).
 */
private fun selectHeadlineValueAndYear(
    series: EnvironmentTimeSeries?,
    metric: EnvMetric,
    sector: EnvSector?,
    selectedYear: Int,
): Pair<String, String> {
    val filtered = series?.points
        ?.filter { it.sector == sector }
        ?.mapNotNull { point -> point.valueFor(metric)?.let { v -> point.year to v } }
        ?: emptyList()

    val selected = filtered.firstOrNull { it.first == selectedYear }

    val value = selected?.second?.let { formatValue(it, metric) } ?: "—"
    val year = selected?.first?.toString() ?: if (selectedYear != 0) selectedYear.toString() else ""
    return value to year
}

/**
 * Numeric (metric, formatted value) pairs for the two metrics OTHER than the
 * currently selected one, at the active country/sector/year. Localized
 * label/unit text is resolved by the composable caller from a Map — this
 * helper stays resource-free (it backs a remember{} call).
 */
private fun secondaryMetricValues(
    series: EnvironmentTimeSeries?,
    metric: EnvMetric,
    sector: EnvSector,
    selectedYear: Int,
): List<Pair<EnvMetric, String>> {
    val others = EnvMetric.entries.filter { it != metric }
    return others.map { otherMetric ->
        val effectiveSector: EnvSector? = if (otherMetric == EnvMetric.Sdg) null else sector
        // Show the other metric's value at the same year the headline is showing,
        // not the metric's own latest — keeps the year display consistent across tiles.
        val selectedValue = series?.points
            ?.filter { it.sector == effectiveSector && it.year == selectedYear }
            ?.firstNotNullOfOrNull { p -> p.valueFor(otherMetric) }
        otherMetric to (selectedValue?.let { formatValue(it, otherMetric) } ?: "—")
    }
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
