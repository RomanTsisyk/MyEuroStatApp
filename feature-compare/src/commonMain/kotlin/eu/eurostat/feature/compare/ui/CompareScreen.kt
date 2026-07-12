package eu.eurostat.feature.compare.ui

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
import eu.eurostat.core.charts.model.SeriesPalette
import eu.eurostat.core.charts.model.rebaseToIndex
import eu.eurostat.feature.compare.domain.CompareIndicator
import eu.eurostat.feature.compare.domain.CompareSeries
import eu.eurostat.ui.component.ChartPointDetailSheet
import eu.eurostat.ui.component.CountryChipsRow
import eu.eurostat.ui.component.CountryPickerSheet
import eu.eurostat.ui.component.EuroCard
import eu.eurostat.ui.component.MetricDropdown
import eu.eurostat.ui.component.ModuleAppBar
import eu.eurostat.ui.component.PillToggle
import eu.eurostat.ui.component.SourceFooter
import eu.eurostat.ui.component.StaleBanner
import eu.eurostat.ui.component.states.EmptyState
import eu.eurostat.ui.component.states.ErrorState
import eu.eurostat.ui.component.states.LoadingShimmer
import eu.eurostat.ui.component.states.localizedMessage
import eu.eurostat.ui.layout.AdaptiveTwoPane
import eu.eurostat.ui.layout.adaptiveChartHeight
import eu.eurostat.ui.theme.Euro
import myeurostatapp.feature_compare.generated.resources.Res
import myeurostatapp.feature_compare.generated.resources.compare_add_country
import myeurostatapp.feature_compare.generated.resources.compare_chart_axis_index
import myeurostatapp.feature_compare.generated.resources.compare_chart_axis_year
import myeurostatapp.feature_compare.generated.resources.compare_chart_empty_body
import myeurostatapp.feature_compare.generated.resources.compare_chart_empty_headline
import myeurostatapp.feature_compare.generated.resources.compare_empty_body
import myeurostatapp.feature_compare.generated.resources.compare_empty_headline
import myeurostatapp.feature_compare.generated.resources.compare_error_headline
import myeurostatapp.feature_compare.generated.resources.compare_footer_staleness_fresh
import myeurostatapp.feature_compare.generated.resources.compare_footer_staleness_stale
import myeurostatapp.feature_compare.generated.resources.compare_module_tagline
import myeurostatapp.feature_compare.generated.resources.compare_module_title
import myeurostatapp.feature_compare.generated.resources.compare_selector_label
import myeurostatapp.feature_compare.generated.resources.compare_toggle_absolute
import myeurostatapp.feature_compare.generated.resources.compare_toggle_indexed
import org.jetbrains.compose.resources.stringResource

/**
 * Cross-module Compare feature screen. Bound to [CompareComponent.state]:
 * Loading / Empty / Error branches use the shared status views; the Content
 * branch renders the indicator selector, the 2–5-country chip row with a
 * picker, an Absolute / Indexed-100 line chart overlaying one palette-coloured
 * line per country, and a value legend.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(component: CompareComponent, onBack: () -> Unit = {}) {
    val state by component.state.collectAsState()

    // The screen accent follows the selected indicator's module, so switching
    // indicators recolours the header. Loading carries the target indicator, so
    // a re-fetch never flashes back to the fallback; only the Error branch
    // (which has no indicator) falls back to the economy accent.
    val currentIndicator: CompareIndicator? = when (val s = state) {
        is CompareUiState.Loading -> s.indicator
        is CompareUiState.Content -> s.indicator
        is CompareUiState.Empty -> s.indicator
        is CompareUiState.Error -> null
    }
    val accent = Euro.moduleAccents.forModule(currentIndicator?.accentKey ?: "economy")
    val isStale = (state as? CompareUiState.Content)?.isStale == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Euro.colors.paper),
    ) {
        ModuleAppBar(
            title = stringResource(Res.string.compare_module_title),
            tagline = stringResource(Res.string.compare_module_tagline),
            accent = accent,
            onBack = onBack,
            onSearch = {},
            onRefresh = { component.onIntent(CompareIntent.Refresh) },
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            PullToRefreshBox(
                isRefreshing = state is CompareUiState.Loading,
                onRefresh = { component.onIntent(CompareIntent.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val s = state) {
                    is CompareUiState.Loading -> LoadingShimmer(
                        modifier = Modifier.padding(Euro.spacing.base),
                    )
                    is CompareUiState.Empty -> EmptyState(
                        headline = stringResource(Res.string.compare_empty_headline),
                        body = stringResource(Res.string.compare_empty_body),
                    )
                    is CompareUiState.Error -> ErrorState(
                        headline = stringResource(Res.string.compare_error_headline),
                        body = s.error.localizedMessage(),
                        onRetry = if (s.canRetry) {
                            { component.onIntent(CompareIntent.Refresh) }
                        } else {
                            null
                        },
                    )
                    is CompareUiState.Content -> CompareContent(
                        accent = accent,
                        state = s,
                        component = component,
                    )
                }
            }
        }
        SourceFooter(
            dataset = currentIndicator?.datasetCode ?: "—",
            staleness = if (isStale) {
                stringResource(Res.string.compare_footer_staleness_stale)
            } else {
                stringResource(Res.string.compare_footer_staleness_fresh)
            },
            stale = isStale,
            modifier = Modifier
                .padding(horizontal = Euro.spacing.base)
                .navigationBarsPadding(),
        )
    }
}

/**
 * Content branch. Controls pane hosts the indicator selector, country chips and
 * the normalization toggle; the content pane hosts the chart card (line chart +
 * legend). All selection state lives on [CompareComponent]; this composable only
 * derives chart geometry.
 */
@Composable
private fun CompareContent(
    accent: Color,
    state: CompareUiState.Content,
    component: CompareComponent,
) {
    val indicators = remember { CompareIndicator.entries.toList() }
    // stringResource is @Composable — resolved here and combined into a plain
    // List<String> for the dropdown, in the same order as [indicators].
    val indicatorTitles = indicators.map { stringResource(it.titleRes) }
    val selectedTitle = stringResource(state.indicator.titleRes)
    val unitLabel = stringResource(state.indicator.unitRes)

    var showCountryPicker by remember { mutableStateOf(false) }
    // Tapped chart point → detail sheet: (country code, point). Cleared on
    // dismiss. Transient, so intentionally not rememberSaveable.
    var tappedPoint by remember { mutableStateOf<Pair<String, ChartPoint>?>(null) }

    val chartHeight = adaptiveChartHeight(compact = 180.dp, medium = 240.dp, expanded = 300.dp)

    // Series in selection order, so palette colours are stable per country.
    val orderedSeries = remember(state.series, state.countries) {
        state.countries.mapNotNull { code -> state.series.firstOrNull { it.countryCode == code } }
    }
    // Absolute chart series (index-stable colours), filtered to the visible range.
    val absoluteChart = remember(orderedSeries, state.yearRange) {
        orderedSeries.mapIndexed { index, s ->
            ChartSeries(
                label = s.countryCode,
                color = SeriesPalette.colorAt(index),
                points = s.points
                    .filter { it.year in state.yearRange }
                    .sortedBy { it.year }
                    .map { ChartPoint(x = it.year.toDouble(), y = it.value) },
            )
        }
    }
    val chartSeries = remember(absoluteChart, state.normalized) {
        if (state.normalized) rebaseToIndex(absoluteChart) else absoluteChart
    }

    // Sections shared between the compact (phone) ordering and the ≥840dp split.
    val indicatorSection: @Composable () -> Unit = {
        MetricDropdown(
            label = stringResource(Res.string.compare_selector_label),
            value = selectedTitle,
            options = indicatorTitles,
            onSelect = { title ->
                val idx = indicatorTitles.indexOf(title)
                if (idx >= 0) component.onIntent(CompareIntent.SelectIndicator(indicators[idx]))
            },
        )
    }
    val countriesSection: @Composable () -> Unit = {
        CountryChipsRow(
            countries = state.countries,
            active = state.countries.toSet(),
            onSelect = { code ->
                val next = if (code in state.countries) {
                    state.countries - code
                } else {
                    state.countries + code
                }
                component.onIntent(CompareIntent.SelectCountries(next))
            },
            onAdd = { showCountryPicker = true },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    val toggleSection: @Composable () -> Unit = {
        PillToggle(
            options = listOf(
                stringResource(Res.string.compare_toggle_absolute),
                stringResource(Res.string.compare_toggle_indexed),
            ),
            selectedIndex = if (state.normalized) 1 else 0,
            onSelect = { idx ->
                // Modelled as a toggle intent — only fire when the target differs.
                if ((idx == 1) != state.normalized) {
                    component.onIntent(CompareIntent.ToggleNormalization)
                }
            },
            activeColor = accent,
        )
    }
    val headerSection: @Composable () -> Unit = {
        Column {
            Text(
                text = selectedTitle,
                style = Euro.typography.headlineLarge,
                color = Euro.colors.ink,
            )
            Text(
                text = unitLabel,
                style = Euro.typography.bodySmall,
                color = Euro.colors.muted,
            )
        }
    }
    val chartSection: @Composable () -> Unit = {
        if (state.isStale) {
            StaleBanner()
            Spacer(Modifier.height(Euro.spacing.s))
        }
        EuroCard {
            Column {
                if (chartSeries.isEmpty() || chartSeries.all { it.points.isEmpty() }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            headline = stringResource(Res.string.compare_chart_empty_headline),
                            body = stringResource(Res.string.compare_chart_empty_body),
                        )
                    }
                } else {
                    EurostatLineChart(
                        series = chartSeries,
                        xAxis = ChartAxis(label = stringResource(Res.string.compare_chart_axis_year)),
                        yAxis = ChartAxis(
                            label = if (state.normalized) {
                                stringResource(Res.string.compare_chart_axis_index)
                            } else {
                                unitLabel
                            },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight),
                        onPointTap = { s, p -> tappedPoint = s.label to p },
                    )
                }
                Spacer(Modifier.height(Euro.spacing.s))
                CompareLegend(indicator = state.indicator, series = orderedSeries)
            }
        }
    }

    AdaptiveTwoPane(
        modifier = Modifier.fillMaxSize(),
        controls = {
            Spacer(Modifier.height(Euro.spacing.xs))
            indicatorSection()
            countriesSection()
            toggleSection()
            Spacer(Modifier.height(Euro.spacing.s))
        },
        content = {
            Spacer(Modifier.height(Euro.spacing.xs))
            headerSection()
            chartSection()
            Spacer(Modifier.height(Euro.spacing.s))
        },
        compact = {
            Spacer(Modifier.height(Euro.spacing.xs))
            indicatorSection()
            headerSection()
            toggleSection()
            chartSection()
            countriesSection()
            Spacer(Modifier.height(Euro.spacing.s))
        },
    )

    if (showCountryPicker) {
        CountryPickerSheet(
            selected = state.countries.toSet(),
            onConfirm = { newSet ->
                // The sheet hands back an unordered Set. Keep the surviving
                // countries in their existing order and append newcomers, so
                // palette colours stay stable across a picker round-trip.
                val next = state.countries.filter { it in newSet } +
                    newSet.filter { it !in state.countries }
                component.onIntent(CompareIntent.SelectCountries(next))
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false },
            title = stringResource(Res.string.compare_add_country),
        )
    }

    // Detail sheet for a tapped chart point. The chart may be Indexed-100, so
    // resolve the true absolute value for the tapped country+year from the
    // underlying series. The lookup can miss if a stale-while-revalidate
    // refresh replaces `orderedSeries` while the sheet is open — in that
    // race, only fall back to the raw chart y when the chart is in Absolute
    // mode (where point.y already is the absolute value); in Indexed-100
    // mode point.y is a rebased index and would render with the wrong unit,
    // so fall through to the em-dash instead.
    tappedPoint?.let { (label, point) ->
        val year = point.x.toInt()
        val absolute = orderedSeries
            .firstOrNull { it.countryCode == label }
            ?.points?.firstOrNull { it.year == year }
            ?.value
        val resolvedValue = absolute ?: point.y.takeUnless { state.normalized }
        val valueText = resolvedValue?.let { state.indicator.formatValue(it) } ?: "—"
        ChartPointDetailSheet(
            seriesLabel = label,
            year = year.toString(),
            value = valueText,
            unit = unitLabel,
            datasetCode = state.indicator.datasetCode,
            onDismiss = { tappedPoint = null },
        )
    }
}

/**
 * Legend row: one entry per country in selection order, each showing the
 * palette dot, the country code and the latest available absolute value
 * (formatted per the indicator). Absolute values are shown even in Indexed-100
 * mode, since the index scale is a chart-only transform.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompareLegend(
    indicator: CompareIndicator,
    series: List<CompareSeries>,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Euro.spacing.m),
        verticalArrangement = Arrangement.spacedBy(Euro.spacing.xs),
    ) {
        series.forEachIndexed { index, s ->
            val latest = s.points.lastOrNull { it.value != null }?.value
            val valueText = latest?.let { indicator.formatValue(it) } ?: "—"
            LegendEntry(
                color = SeriesPalette.colorAt(index),
                code = s.countryCode,
                value = valueText,
            )
        }
    }
}

@Composable
private fun LegendEntry(color: Color, code: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.size(Euro.spacing.xs))
        Text(
            text = code,
            style = Euro.typography.bodySmall,
            color = Euro.colors.ink,
        )
        Spacer(Modifier.size(Euro.spacing.xs))
        Text(
            text = value,
            style = Euro.typography.bodySmall,
            color = Euro.colors.muted,
        )
    }
}
