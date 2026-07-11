package eu.eurostat.feature.economy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.eurostat.core.charts.EurostatLineChart
import eu.eurostat.core.charts.model.ChartAxis
import eu.eurostat.core.charts.model.ChartPoint
import eu.eurostat.core.charts.model.ChartSeries
import eu.eurostat.core.charts.model.SeriesPalette
import eu.eurostat.core.common.EurostatCountries
import eu.eurostat.feature.economy.domain.EconomyDataPoint
import eu.eurostat.feature.economy.domain.EconomyMetric
import eu.eurostat.feature.economy.domain.EconomyTimeSeries
import eu.eurostat.ui.component.CountryChipsRow
import eu.eurostat.ui.component.CountryPickerSheet
import eu.eurostat.ui.component.EuroCard
import eu.eurostat.ui.component.MetricHeadline
import eu.eurostat.ui.component.ModuleAppBar
import eu.eurostat.ui.component.PillToggle
import eu.eurostat.ui.component.SegmentedControl
import eu.eurostat.ui.component.SourceFooter
import eu.eurostat.ui.component.StatTile
import eu.eurostat.ui.component.YearDropdown
import eu.eurostat.ui.component.YearScrubber
import eu.eurostat.ui.component.states.EmptyState
import eu.eurostat.ui.component.states.ErrorState
import eu.eurostat.ui.component.states.LoadingShimmer
import eu.eurostat.ui.format.formatDecimal
import eu.eurostat.ui.format.formatGrouped
import eu.eurostat.ui.layout.AdaptiveTwoPane
import eu.eurostat.ui.layout.adaptiveChartHeight
import eu.eurostat.ui.theme.Euro
import kotlin.math.abs
import myeurostatapp.feature_economy.generated.resources.Res
import myeurostatapp.feature_economy.generated.resources.economy_axis_deficit
import myeurostatapp.feature_economy.generated.resources.economy_axis_gdp
import myeurostatapp.feature_economy.generated.resources.economy_axis_inflation
import myeurostatapp.feature_economy.generated.resources.economy_chart_axis_index
import myeurostatapp.feature_economy.generated.resources.economy_chart_axis_year
import myeurostatapp.feature_economy.generated.resources.economy_chart_empty_body
import myeurostatapp.feature_economy.generated.resources.economy_chart_empty_headline
import myeurostatapp.feature_economy.generated.resources.economy_delta_unit_pp
import myeurostatapp.feature_economy.generated.resources.economy_empty_body
import myeurostatapp.feature_economy.generated.resources.economy_empty_headline
import myeurostatapp.feature_economy.generated.resources.economy_error_headline
import myeurostatapp.feature_economy.generated.resources.economy_footer_staleness_fresh
import myeurostatapp.feature_economy.generated.resources.economy_footer_staleness_stale
import myeurostatapp.feature_economy.generated.resources.economy_metric_deficit
import myeurostatapp.feature_economy.generated.resources.economy_metric_gdp
import myeurostatapp.feature_economy.generated.resources.economy_metric_inflation
import myeurostatapp.feature_economy.generated.resources.economy_module_tagline
import myeurostatapp.feature_economy.generated.resources.economy_module_title
import myeurostatapp.feature_economy.generated.resources.economy_subtitle_deficit
import myeurostatapp.feature_economy.generated.resources.economy_subtitle_gdp
import myeurostatapp.feature_economy.generated.resources.economy_subtitle_inflation
import myeurostatapp.feature_economy.generated.resources.economy_tile_deficit_label
import myeurostatapp.feature_economy.generated.resources.economy_tile_deficit_unit
import myeurostatapp.feature_economy.generated.resources.economy_tile_gdp_delta
import myeurostatapp.feature_economy.generated.resources.economy_tile_inflation_delta
import myeurostatapp.feature_economy.generated.resources.economy_tile_inflation_label
import myeurostatapp.feature_economy.generated.resources.economy_toggle_absolute
import myeurostatapp.feature_economy.generated.resources.economy_toggle_indexed
import myeurostatapp.feature_economy.generated.resources.economy_unit_deficit
import myeurostatapp.feature_economy.generated.resources.economy_unit_gdp
import myeurostatapp.feature_economy.generated.resources.economy_unit_inflation
import org.jetbrains.compose.resources.stringResource

private const val M_TO_B: Long = 1_000L
private const val PERCENT: Double = 100.0

/**
 * Editorial Economy feature screen. Bound to [EconomyComponent.state]:
 * Loading / Empty / Error branches use the shared status views; the
 * Content branch renders the headline + GDP/Inflation/Deficit segmented
 * switcher, a multi-country line chart hero with an Absolute / Indexed-100
 * scale toggle (comparison mode), secondary KPI tiles for the inactive
 * metrics, the year scrubber and country chips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EconomyScreen(component: EconomyComponent, onBack: () -> Unit = {}) {
    val state by component.state.collectAsState()
    val accent = Euro.moduleAccents.forModule("Economy")
    val contentState = state as? EconomyUiState.Content
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
            title = stringResource(Res.string.economy_module_title),
            tagline = stringResource(Res.string.economy_module_tagline),
            accent = accent,
            onBack = onBack,
            year = appBarYear,
            country = appBarCountry,
            onSearch = {},
            onRefresh = { component.onIntent(EconomyIntent.Refresh) },
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            PullToRefreshBox(
                isRefreshing = state is EconomyUiState.Loading,
                onRefresh = { component.onIntent(EconomyIntent.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val s = state) {
                    EconomyUiState.Loading -> LoadingShimmer(
                        modifier = Modifier.padding(Euro.spacing.base),
                    )
                    is EconomyUiState.Empty -> EmptyState(
                        headline = stringResource(Res.string.economy_empty_headline),
                        body = stringResource(Res.string.economy_empty_body),
                    )
                    is EconomyUiState.Error -> ErrorState(
                        headline = stringResource(Res.string.economy_error_headline),
                        body = s.message,
                        onRetry = if (s.canRetry) {
                            { component.onIntent(EconomyIntent.Retry) }
                        } else {
                            null
                        },
                    )
                    is EconomyUiState.Content -> EconomyContent(
                        accent = accent,
                        state = s,
                        component = component,
                    )
                }
            }
        }
        SourceFooter(
            dataset = "nama_10_gdp · +2",
            staleness = if ((state as? EconomyUiState.Content)?.isStale == true) {
                stringResource(Res.string.economy_footer_staleness_stale)
            } else {
                stringResource(Res.string.economy_footer_staleness_fresh)
            },
            stale = (state as? EconomyUiState.Content)?.isStale == true,
            modifier = Modifier
                .padding(horizontal = Euro.spacing.base)
                .navigationBarsPadding(),
        )
    }
}

/**
 * Real-data content branch. All hero/chart/kpi widgets are derived from
 * the bound [state]; the segmented switcher drives which metric the
 * chart and headline render, with the two inactive metrics hinted as
 * dimmed secondary tiles.
 *
 * Active-country selection is lifted to [EconomyComponent] via
 * [EconomyIntent.SelectActiveCountry]; available countries come from
 * [EconomyUiState.Content.availableCountries].
 */
@Composable
private fun EconomyContent(
    accent: Color,
    state: EconomyUiState.Content,
    component: EconomyComponent,
) {
    val timeSeries = state.timeSeries
    // stringResource is @Composable — resolved here (not inside remember{}) and
    // combined into a plain List<String> for the segmented control.
    val metricLabels = listOf(
        stringResource(Res.string.economy_metric_gdp),
        stringResource(Res.string.economy_metric_inflation),
        stringResource(Res.string.economy_metric_deficit),
    )
    val metrics = remember { listOf(EconomyMetric.Gdp, EconomyMetric.Inflation, EconomyMetric.Deficit) }

    // selectedMetric and displayYearRange are owned by the component — survives rotation.
    val selectedMetric = state.selectedMetric
    val selectedIndex = metrics.indexOf(selectedMetric).coerceAtLeast(0)

    // Active country and available countries are owned by the component; read from state.
    val activeCountry = state.activeCountry
    val availableCountries = state.availableCountries

    // Year range derived from the union of all observation years.
    val (minYear, maxYear) = remember(timeSeries) {
        val years = timeSeries.flatMap { it.points }.map { it.year }
        if (years.isEmpty()) 2010 to 2024 else years.min() to years.max()
    }
    val yearRange = state.displayYearRange ?: (minYear..maxYear)

    val activeSeries = remember(timeSeries, activeCountry) {
        timeSeries.firstOrNull { it.countryCode == activeCountry }
    }
    val sortedPoints = activeSeries?.points?.sortedBy { it.year } ?: emptyList()
    val latestPoint: EconomyDataPoint? = sortedPoints.firstOrNull { it.year == state.selectedYear }
    val prevPoint: EconomyDataPoint? = sortedPoints.firstOrNull { it.year == state.selectedYear - 1 }

    var showCountryPicker by remember { mutableStateOf(false) }

    val chartHeight = adaptiveChartHeight(compact = 160.dp, medium = 220.dp, expanded = 280.dp)

    // Sections shared between the compact (phone) ordering and the ≥840dp
    // two-pane split. Purely structural — all state stays on the component.
    val yearSection: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Euro.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.availableYears.isNotEmpty()) {
                YearDropdown(
                    selectedYear = state.selectedYear,
                    years = state.availableYears,
                    onSelect = { component.onIntent(EconomyIntent.SelectYear(it)) },
                )
            }
        }
    }
    val headlineSection: @Composable () -> Unit = {
        HeadlineForMetric(
            metric = selectedMetric,
            latest = latestPoint,
            previous = prevPoint,
            accent = accent,
        )
    }
    val metricSwitcherSection: @Composable () -> Unit = {
        SegmentedControl(
            options = metricLabels,
            selectedIndex = selectedIndex,
            onSelect = { component.onIntent(EconomyIntent.SelectMetric(metrics[it])) },
            activeColor = accent,
        )
    }
    val chartSection: @Composable () -> Unit = {
        EuroCard {
            Column {
                PillToggle(
                    options = listOf(
                        stringResource(Res.string.economy_toggle_absolute),
                        stringResource(Res.string.economy_toggle_indexed),
                    ),
                    selectedIndex = if (state.normalized) 1 else 0,
                    onSelect = { component.onIntent(EconomyIntent.SetNormalized(it == 1)) },
                    activeColor = accent,
                )
                Spacer(Modifier.height(Euro.spacing.s))
                val chartSeries = remember(timeSeries, selectedMetric, yearRange, state.normalized) {
                    val absolute = buildChartSeries(timeSeries, selectedMetric, yearRange)
                    if (state.normalized) rebaseToIndex(absolute) else absolute
                }
                if (chartSeries.isEmpty() || chartSeries.all { it.points.isEmpty() }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            headline = stringResource(Res.string.economy_chart_empty_headline),
                            body = stringResource(Res.string.economy_chart_empty_body),
                        )
                    }
                } else {
                    EurostatLineChart(
                        series = chartSeries,
                        xAxis = ChartAxis(label = stringResource(Res.string.economy_chart_axis_year)),
                        yAxis = ChartAxis(
                            label = if (state.normalized) {
                                stringResource(Res.string.economy_chart_axis_index)
                            } else {
                                yAxisLabelFor(selectedMetric)
                            },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight),
                    )
                }
                Spacer(Modifier.height(Euro.spacing.s))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Euro.spacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    chartSeries.forEach { s ->
                        LegendDot(color = s.color, label = s.label)
                    }
                }
            }
        }
    }
    val tilesSection: @Composable () -> Unit = {
        SecondaryMetricTiles(
            selected = selectedMetric,
            latest = latestPoint,
        )
    }
    val scrubberSection: @Composable () -> Unit = {
        YearScrubber(
            min = minYear,
            max = maxYear,
            value = yearRange,
            onValueChange = { component.onIntent(EconomyIntent.SetDisplayYearRange(it)) },
        )
    }
    val countriesSection: @Composable () -> Unit = {
        CountryChipsRow(
            countries = availableCountries,
            active = setOf(activeCountry),
            onSelect = { component.onIntent(EconomyIntent.SelectActiveCountry(it)) },
            onAdd = { showCountryPicker = true },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    AdaptiveTwoPane(
        modifier = Modifier.fillMaxSize(),
        controls = {
            Spacer(Modifier.height(Euro.spacing.xs))
            yearSection()
            metricSwitcherSection()
            scrubberSection()
            countriesSection()
            Spacer(Modifier.height(Euro.spacing.s))
        },
        content = {
            Spacer(Modifier.height(Euro.spacing.xs))
            headlineSection()
            chartSection()
            tilesSection()
            Spacer(Modifier.height(Euro.spacing.s))
        },
        compact = {
            Spacer(Modifier.height(Euro.spacing.xs))
            yearSection()
            headlineSection()
            metricSwitcherSection()
            chartSection()
            tilesSection()
            scrubberSection()
            countriesSection()
            Spacer(Modifier.height(Euro.spacing.s))
        },
    )

    if (showCountryPicker) {
        CountryPickerSheet(
            selected = availableCountries.toSet(),
            onConfirm = { newSet ->
                component.onIntent(EconomyIntent.SelectCountries(newSet.toList()))
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false },
        )
    }
}

/**
 * Renders the headline KPI for the currently selected metric, with a
 * year-over-year delta computed from the active country's last two points.
 */
@Composable
private fun HeadlineForMetric(
    metric: EconomyMetric,
    latest: EconomyDataPoint?,
    previous: EconomyDataPoint?,
    accent: Color,
) {
    val (valueText, unitText, baseSubtitle) = when (metric) {
        EconomyMetric.Gdp -> Triple(
            latest?.gdpEur?.let { formatBillions(it) } ?: "—",
            stringResource(Res.string.economy_unit_gdp),
            stringResource(Res.string.economy_subtitle_gdp),
        )
        EconomyMetric.Inflation -> Triple(
            latest?.hicpIndex?.let { formatDecimal(it, 1) } ?: "—",
            stringResource(Res.string.economy_unit_inflation),
            stringResource(Res.string.economy_subtitle_inflation),
        )
        EconomyMetric.Deficit -> Triple(
            latest?.deficitPctGdp?.let { formatSignedPercent(it) } ?: "—",
            stringResource(Res.string.economy_unit_deficit),
            stringResource(Res.string.economy_subtitle_deficit),
        )
    }
    // yoyDeltaText is a plain (non-@Composable) helper — its unit suffixes are
    // resolved here, at the composable call site, and passed in.
    val deltaText = yoyDeltaText(
        metric = metric,
        latest = latest,
        previous = previous,
        idxUnit = stringResource(Res.string.economy_unit_inflation),
        ppUnit = stringResource(Res.string.economy_delta_unit_pp),
    )
    val subtitle = if (deltaText != null) "$baseSubtitle · $deltaText" else baseSubtitle
    val yearLabel = latest?.year?.toString() ?: "—"
    MetricHeadline(
        value = valueText,
        unit = unitText,
        subtitle = subtitle,
        year = yearLabel,
        accent = accent,
    )
}

/**
 * Renders the two inactive metrics as dimmed secondary tiles using the
 * active country's most recent observation.
 */
@Composable
private fun SecondaryMetricTiles(
    selected: EconomyMetric,
    latest: EconomyDataPoint?,
) {
    val others = EconomyMetric.entries.filter { it != selected }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Euro.spacing.s),
    ) {
        others.forEach { metric ->
            when (metric) {
                EconomyMetric.Gdp -> StatTile(
                    label = stringResource(Res.string.economy_metric_gdp),
                    value = latest?.gdpEur?.let { formatBillions(it) } ?: "—",
                    delta = stringResource(Res.string.economy_tile_gdp_delta),
                    bordered = true,
                    modifier = Modifier.weight(1f).alpha(0.55f),
                )
                EconomyMetric.Inflation -> StatTile(
                    label = stringResource(Res.string.economy_tile_inflation_label),
                    value = latest?.hicpIndex?.let { formatDecimal(it, 1) } ?: "—",
                    delta = stringResource(Res.string.economy_tile_inflation_delta),
                    bordered = true,
                    modifier = Modifier.weight(1f).alpha(0.55f),
                )
                EconomyMetric.Deficit -> DeficitTile(
                    value = latest?.deficitPctGdp,
                    modifier = Modifier.weight(1f).alpha(0.55f),
                )
            }
        }
    }
}

/**
 * Warn-tinted deficit tile rendered inline because [StatTile] lacks a
 * value-color override.
 */
@Composable
private fun DeficitTile(value: Double?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(0.dp)) {
        Column(modifier = Modifier.padding(Euro.spacing.m)) {
            Text(
                text = stringResource(Res.string.economy_tile_deficit_label),
                style = Euro.typography.bodySmall,
                color = Euro.colors.muted,
            )
            Text(
                text = value?.let { formatSignedPercent(it) } ?: "—",
                style = Euro.typography.tabularNumLarge,
                color = Euro.colors.warn,
            )
            Text(
                text = stringResource(Res.string.economy_tile_deficit_unit),
                style = Euro.typography.bodySmall,
                color = Euro.colors.muted,
            )
        }
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

// ---------- pure helpers (no Compose state) ----------

/**
 * Project the merged [EconomyTimeSeries] list into chart series for the
 * given [metric], filtered to [yearRange]. Series colors are assigned by
 * stable index over the rendered series list via [SeriesPalette], so any
 * combination of picked countries stays visually distinct (cycling only
 * past 8 series).
 */
private fun buildChartSeries(
    timeSeries: List<EconomyTimeSeries>,
    metric: EconomyMetric,
    yearRange: IntRange,
): List<ChartSeries> = timeSeries.mapIndexed { index, ts ->
    val points = ts.points
        .filter { it.year in yearRange }
        .mapNotNull { p ->
            p.fieldFor(metric)?.let { v -> ChartPoint(x = p.year.toDouble(), y = v) }
        }
    ChartSeries(
        label = ts.countryCode,
        color = SeriesPalette.colorAt(index),
        points = points,
    )
}

@Composable
private fun yAxisLabelFor(metric: EconomyMetric): String = when (metric) {
    EconomyMetric.Gdp -> stringResource(Res.string.economy_axis_gdp)
    EconomyMetric.Inflation -> stringResource(Res.string.economy_axis_inflation)
    EconomyMetric.Deficit -> stringResource(Res.string.economy_axis_deficit)
}

/**
 * Year-over-year delta string, e.g. `"+6.2%"`, `"+1.2 idx"`, `"−0.4 pp"`. Not
 * composable — [idxUnit] and [ppUnit] are resolved via stringResource at the
 * composable call site (see [HeadlineForMetric]) and passed in.
 */
private fun yoyDeltaText(
    metric: EconomyMetric,
    latest: EconomyDataPoint?,
    previous: EconomyDataPoint?,
    idxUnit: String,
    ppUnit: String,
): String? {
    if (latest == null || previous == null) return null
    return when (metric) {
        EconomyMetric.Gdp -> {
            val l = latest.gdpEur?.toDouble() ?: return null
            val p = previous.gdpEur?.toDouble() ?: return null
            if (p == 0.0) return null
            val pct = (l - p) / p * PERCENT
            formatSignedPercent(pct)
        }
        EconomyMetric.Inflation -> {
            val l = latest.hicpIndex ?: return null
            val p = previous.hicpIndex ?: return null
            "${signed(l - p)} $idxUnit"
        }
        EconomyMetric.Deficit -> {
            val l = latest.deficitPctGdp ?: return null
            val p = previous.deficitPctGdp ?: return null
            "${signed(l - p)} $ppUnit"
        }
    }
}

/**
 * Million-EUR → billion-EUR, with locale-aware thousands grouping (e.g.
 * `"3,451"` in en, `"3.451"` in de). Uses truncating integer billions (not
 * rounded), matching this tile's original compactness convention.
 */
private fun formatBillions(millionEur: Long): String =
    formatGrouped(millionEur / M_TO_B)

/**
 * Signed percent-point delta, e.g. `"+6.2%"`, `"−0.4%"`. Exact zero renders
 * as bare `"0.0%"` with no sign character — this screen's original
 * convention, distinct from the `"+0.0"` used elsewhere (e.g. Population's
 * YoY badge).
 */
private fun formatSignedPercent(v: Double): String {
    val formatted = formatDecimal(abs(v), 1)
    return when {
        v > 0 -> "+$formatted%"
        v < 0 -> "−$formatted%"
        // Route zero through formatDecimal too, so the separator stays
        // locale-consistent with the non-zero branches.
        else -> "${formatDecimal(0.0, 1)}%"
    }
}

/**
 * Signed decimal delta with no unit suffix, e.g. `"+1.2"`, `"−0.4"`. Exact
 * zero renders as bare `"0.0"` — see [formatSignedPercent] for why this
 * screen special-cases zero rather than always showing a sign.
 */
private fun signed(v: Double): String {
    val formatted = formatDecimal(abs(v), 1)
    return when {
        v > 0 -> "+$formatted"
        v < 0 -> "−$formatted"
        // Route zero through formatDecimal too, so the separator stays
        // locale-consistent with the non-zero branches.
        else -> formatDecimal(0.0, 1)
    }
}
