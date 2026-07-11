package eu.eurostat.feature.transport.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import eu.eurostat.core.charts.EurostatSmallMultiples
import eu.eurostat.core.charts.SmallMultiplePanel
import eu.eurostat.core.charts.model.ChartAxis
import eu.eurostat.core.charts.model.ChartPoint
import eu.eurostat.core.charts.model.ChartSeries
import eu.eurostat.core.common.EurostatCountries
import eu.eurostat.feature.transport.domain.TransportDataPoint
import eu.eurostat.feature.transport.domain.TransportMode
import eu.eurostat.feature.transport.domain.TransportTimeSeries
import eu.eurostat.ui.component.CountryChipsRow
import eu.eurostat.ui.component.YearDropdown
import eu.eurostat.ui.component.CountryPickerSheet
import eu.eurostat.ui.component.EuroCard
import eu.eurostat.ui.component.MetricHeadline
import eu.eurostat.ui.component.ModuleAppBar
import eu.eurostat.ui.component.PillToggle
import eu.eurostat.ui.component.SourceFooter
import eu.eurostat.ui.component.StatTile
import eu.eurostat.ui.component.states.EmptyState
import eu.eurostat.ui.component.states.ErrorState
import eu.eurostat.ui.component.states.LoadingShimmer
import eu.eurostat.ui.format.formatDecimal
import eu.eurostat.ui.layout.AdaptiveTwoPane
import eu.eurostat.ui.layout.adaptiveChartHeight
import eu.eurostat.ui.theme.Euro
import kotlin.math.ln
import kotlin.math.roundToInt
import myeurostatapp.feature_transport.generated.resources.Res
import myeurostatapp.feature_transport.generated.resources.transport_empty_body
import myeurostatapp.feature_transport.generated.resources.transport_empty_headline
import myeurostatapp.feature_transport.generated.resources.transport_error_headline
import myeurostatapp.feature_transport.generated.resources.transport_footer_staleness_fresh
import myeurostatapp.feature_transport.generated.resources.transport_footer_staleness_stale
import myeurostatapp.feature_transport.generated.resources.transport_headline_label_passengers
import myeurostatapp.feature_transport.generated.resources.transport_mode_label_road
import myeurostatapp.feature_transport.generated.resources.transport_mode_toggle_air
import myeurostatapp.feature_transport.generated.resources.transport_mode_toggle_all
import myeurostatapp.feature_transport.generated.resources.transport_mode_toggle_road
import myeurostatapp.feature_transport.generated.resources.transport_module_tagline
import myeurostatapp.feature_transport.generated.resources.transport_module_title
import myeurostatapp.feature_transport.generated.resources.transport_tile_label_air
import myeurostatapp.feature_transport.generated.resources.transport_tile_label_sea
import myeurostatapp.feature_transport.generated.resources.transport_tile_sea_delta
import myeurostatapp.feature_transport.generated.resources.transport_tile_sea_value
import myeurostatapp.feature_transport.generated.resources.transport_toggle_log_label
import myeurostatapp.feature_transport.generated.resources.transport_unit_billion
import myeurostatapp.feature_transport.generated.resources.transport_unit_million
import org.jetbrains.compose.resources.stringResource

/**
 * Transport feature screen — wired to live Eurostat data.
 *
 * The Content branch reads [TransportUiState.Content.series] and renders the
 * editorial Transport layout:
 *
 *  - [MetricHeadline] hero — latest road-passenger value for the active
 *    country, formatted as `"X.Y bn"`.
 *  - [PillToggle] (ROAD / AIR / ALL) controlling which small-multiple panels
 *    are shown, plus a log-scale [Switch].
 *  - [EurostatSmallMultiples] panels: ROAD (formatted in billions, accent
 *    color) and AIR (formatted in millions, warn color). With log enabled,
 *    each value `v` is plotted as `ln(v + 1)`.
 *  - Three [StatTile]s — road (latest, formatted), air (latest, formatted),
 *    and a sea tile that is always `"n/a · port-based"` because the sea
 *    dataset is keyed by port not country and is intentionally disabled.
 *  - [CountryChipsRow] derived from the country codes present in [series].
 *  - [SourceFooter] citing the underlying datasets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportScreen(component: TransportComponent, onBack: () -> Unit = {}) {
    val state by component.state.collectAsState()
    val accent = Euro.moduleAccents.forModule("Transport")

    val contentState = state as? TransportUiState.Content
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
            title = stringResource(Res.string.transport_module_title),
            tagline = stringResource(Res.string.transport_module_tagline),
            accent = accent,
            onBack = onBack,
            year = appBarYear,
            country = appBarCountry,
            onRefresh = { component.onIntent(TransportIntent.Refresh) },
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            PullToRefreshBox(
                isRefreshing = state is TransportUiState.Loading,
                onRefresh = { component.onIntent(TransportIntent.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val s = state) {
                    is TransportUiState.Loading -> LoadingBody()
                    is TransportUiState.Error -> ErrorState(
                        headline = stringResource(Res.string.transport_error_headline),
                        body = s.message,
                        onRetry = if (s.canRetry) {
                            { component.onIntent(TransportIntent.Retry) }
                        } else {
                            null
                        },
                    )
                    is TransportUiState.Empty -> EmptyState(
                        headline = stringResource(Res.string.transport_empty_headline),
                        body = stringResource(Res.string.transport_empty_body),
                    )
                    is TransportUiState.Content -> ContentBody(
                        series = s.series,
                        isStale = s.isStale,
                        accent = accent,
                        activeCountry = s.activeCountry,
                        availableCountries = s.availableCountries,
                        panelMode = s.displayPanelMode,
                        logScale = s.logScale,
                        selectedYear = s.selectedYear,
                        availableYears = s.availableYears,
                        onSelectCountry = { code ->
                            component.onIntent(TransportIntent.SelectActiveCountry(code))
                        },
                        onSelectCountries = { codes ->
                            component.onIntent(TransportIntent.SelectCountries(codes))
                        },
                        onPanelModeChange = { mode ->
                            component.onIntent(TransportIntent.SelectPanelMode(mode))
                        },
                        onLogScaleToggle = {
                            component.onIntent(TransportIntent.ToggleLogScale)
                        },
                        onSelectYear = { year ->
                            component.onIntent(TransportIntent.SelectYear(year))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingBody() {
    val chartH = adaptiveChartHeight(compact = 220.dp, medium = 280.dp, expanded = 340.dp)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Euro.spacing.base),
        verticalArrangement = Arrangement.spacedBy(Euro.spacing.base),
    ) {
        LoadingShimmer(height = 72.dp)
        LoadingShimmer(height = chartH)
        LoadingShimmer(height = 96.dp)
    }
}

@Composable
private fun ContentBody(
    series: List<TransportTimeSeries>,
    isStale: Boolean,
    accent: Color,
    activeCountry: String,
    availableCountries: List<String>,
    panelMode: TransportPanelMode,
    logScale: Boolean,
    selectedYear: Int,
    availableYears: List<Int>,
    onSelectCountry: (String) -> Unit,
    onSelectCountries: (List<String>) -> Unit,
    onPanelModeChange: (TransportPanelMode) -> Unit,
    onLogScaleToggle: () -> Unit,
    onSelectYear: (Int) -> Unit,
) {
    val seriesCountries = remember(series) { series.map { it.countryCode }.distinct() }
    val countries = availableCountries.ifEmpty { seriesCountries }
    var showCountryPicker by remember { mutableStateOf(false) }

    val active = series.firstOrNull { it.countryCode == activeCountry }
    val selectedPoint = active?.points?.firstOrNull { it.year == selectedYear }
    val roadValue = selectedPoint?.roadPassengers
    val airValue = selectedPoint?.airPassengers
    val headlineValue = roadValue?.let { formatBillionsValue(it) } ?: "—"

    // stringResource is @Composable — resolve every label once here, then pass
    // the resolved Strings down into the section lambdas, ModeAndLogRow, and
    // buildPanels()/formatBillions()/formatMillions() (which are either plain
    // lambdas capturing these as closures, or non-composable helpers that take
    // them as parameters).
    val roadModeLabel = stringResource(Res.string.transport_mode_toggle_road)
    val airModeLabel = stringResource(Res.string.transport_mode_toggle_air)
    val allModeLabel = stringResource(Res.string.transport_mode_toggle_all)
    val billionUnit = stringResource(Res.string.transport_unit_billion)
    val millionUnit = stringResource(Res.string.transport_unit_million)
    val roadLabelLower = stringResource(Res.string.transport_mode_label_road)
    val passengersLabel = stringResource(Res.string.transport_headline_label_passengers)
    val airTileLabel = stringResource(Res.string.transport_tile_label_air)
    val seaTileLabel = stringResource(Res.string.transport_tile_label_sea)
    val seaValueText = stringResource(Res.string.transport_tile_sea_value)
    val seaDeltaText = stringResource(Res.string.transport_tile_sea_delta)
    val logLabel = stringResource(Res.string.transport_toggle_log_label)
    val freshLabel = stringResource(Res.string.transport_footer_staleness_fresh)
    val staleLabel = stringResource(Res.string.transport_footer_staleness_stale)
    // "ROAD · bn" / "AIR · M" — small-multiples panel captions built from the
    // same mode words as the PillToggle plus the localized unit suffix.
    val roadPanelTitle = "$roadModeLabel · $billionUnit"
    val airPanelTitle = "$airModeLabel · $millionUnit"

    // Sections shared between the compact (phone) ordering and the ≥840dp
    // two-pane split. Purely structural — all state stays on the component.
    val headlineSection: @Composable () -> Unit = {
        MetricHeadline(
            value = headlineValue,
            unit = billionUnit,
            subtitle = "$roadLabelLower · $passengersLabel · ${active?.countryName ?: activeCountry}",
            year = selectedYear.toString(),
            accent = accent,
            modifier = Modifier.padding(top = Euro.spacing.s),
        )
    }
    val yearSection: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Euro.spacing.s),
        ) {
            if (availableYears.isNotEmpty()) {
                YearDropdown(
                    selectedYear = selectedYear,
                    years = availableYears,
                    onSelect = onSelectYear,
                )
            }
        }
    }
    val modeSection: @Composable () -> Unit = {
        ModeAndLogRow(
            mode = panelMode,
            onModeChange = onPanelModeChange,
            logScale = logScale,
            onLogChange = { onLogScaleToggle() },
            accent = accent,
            roadLabel = roadModeLabel,
            airLabel = airModeLabel,
            allLabel = allModeLabel,
            logLabel = logLabel,
        )
    }
    val panelsSection: @Composable () -> Unit = {
        EuroCard(modifier = Modifier.fillMaxWidth()) {
            val panels = buildPanels(
                series = active,
                mode = panelMode,
                logScale = logScale,
                accent = accent,
                warn = Euro.colors.warn,
                roadTitle = roadPanelTitle,
                airTitle = airPanelTitle,
            )
            EurostatSmallMultiples(
                panels = panels,
                columns = if (panels.size >= 2) 2 else 1,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    val tilesSection: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(Euro.spacing.s)) {
            StatTile(
                label = roadLabelLower,
                value = roadValue?.let { formatBillions(it, billionUnit) } ?: "—",
                bordered = true,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = airTileLabel,
                value = airValue?.let { formatMillions(it, millionUnit) } ?: "—",
                bordered = true,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = seaTileLabel,
                value = seaValueText,
                delta = seaDeltaText,
                bordered = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
    val countriesSection: @Composable () -> Unit = {
        if (countries.isNotEmpty()) {
            CountryChipsRow(
                countries = countries,
                active = setOf(activeCountry),
                onSelect = { code ->
                    onSelectCountry(code)
                },
                onAdd = { showCountryPicker = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AdaptiveTwoPane(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            controls = {
                Spacer(Modifier.height(Euro.spacing.xs))
                yearSection()
                modeSection()
                countriesSection()
            },
            content = {
                headlineSection()
                panelsSection()
                tilesSection()
            },
            compact = {
                headlineSection()
                yearSection()
                modeSection()
                panelsSection()
                tilesSection()
                countriesSection()
            },
        )

        SourceFooter(
            dataset = "road_pa_buscoa · avia_paoc",
            staleness = if (isStale) staleLabel else freshLabel,
            stale = isStale,
            modifier = Modifier
                .padding(horizontal = Euro.spacing.base)
                .navigationBarsPadding(),
        )
    }

    if (showCountryPicker) {
        CountryPickerSheet(
            selected = countries.toSet(),
            onConfirm = { selected ->
                onSelectCountries(selected.toList())
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false },
        )
    }
}

@Composable
private fun ModeAndLogRow(
    mode: TransportPanelMode,
    onModeChange: (TransportPanelMode) -> Unit,
    logScale: Boolean,
    onLogChange: (Boolean) -> Unit,
    accent: Color,
    roadLabel: String,
    airLabel: String,
    allLabel: String,
    logLabel: String,
) {
    val options = listOf(roadLabel, airLabel, allLabel)
    val selectedIdx = when (mode) {
        TransportPanelMode.ROAD -> 0
        TransportPanelMode.AIR -> 1
        TransportPanelMode.ALL -> 2
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        PillToggle(
            options = options,
            selectedIndex = selectedIdx,
            onSelect = {
                onModeChange(
                    when (it) {
                        0 -> TransportPanelMode.ROAD
                        1 -> TransportPanelMode.AIR
                        else -> TransportPanelMode.ALL
                    }
                )
            },
            activeColor = accent,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = logLabel,
                style = Euro.typography.bodySmall,
                color = Euro.colors.muted,
                modifier = Modifier.padding(end = Euro.spacing.xs),
            )
            Switch(
                checked = logScale,
                onCheckedChange = onLogChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Euro.colors.paper,
                    checkedTrackColor = accent,
                    checkedBorderColor = Euro.colors.ink,
                    uncheckedThumbColor = Euro.colors.paper,
                    uncheckedTrackColor = Euro.colors.surface2,
                    uncheckedBorderColor = Euro.colors.ink,
                ),
            )
        }
    }
}

/**
 * Build the small-multiple panels for the given mode and active series.
 * ROAD/ALL include a billions-scaled road panel, AIR/ALL an air panel
 * scaled to millions. The optional [logScale] applies `ln(v + 1)` per point.
 */
private fun buildPanels(
    series: TransportTimeSeries?,
    mode: TransportPanelMode,
    logScale: Boolean,
    accent: Color,
    warn: Color,
    roadTitle: String,
    airTitle: String,
): List<SmallMultiplePanel> {
    if (series == null) return emptyList()
    val panels = mutableListOf<SmallMultiplePanel>()
    if (mode == TransportPanelMode.ROAD || mode == TransportPanelMode.ALL) {
        val pts = series.points.toRoadChartPoints(logScale, scale = 1_000_000_000.0)
        if (pts.isNotEmpty()) {
            panels += SmallMultiplePanel(roadTitle) {
                PanelChart(
                    series = ChartSeries(label = roadTitle, color = accent, points = pts),
                )
            }
        }
    }
    if (mode == TransportPanelMode.AIR || mode == TransportPanelMode.ALL) {
        val pts = series.points.toAirChartPoints(logScale, scale = 1_000_000.0)
        if (pts.isNotEmpty()) {
            panels += SmallMultiplePanel(airTitle) {
                PanelChart(
                    series = ChartSeries(label = airTitle, color = warn, points = pts),
                )
            }
        }
    }
    return panels
}

@Composable
private fun PanelChart(series: ChartSeries) {
    val panelH = adaptiveChartHeight(compact = 220.dp, medium = 280.dp, expanded = 340.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelH),
    ) {
        EurostatLineChart(
            series = listOf(series),
            xAxis = ChartAxis(label = ""),
            yAxis = ChartAxis(label = ""),
            hideAxis = true,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun List<TransportDataPoint>.toRoadChartPoints(
    log: Boolean,
    scale: Double,
): List<ChartPoint> =
    mapNotNull { p ->
        val v = p.roadPassengers ?: return@mapNotNull null
        ChartPoint(x = p.year.toDouble(), y = transform(v.toDouble() / scale, log))
    }.sortedBy { it.x }

private fun List<TransportDataPoint>.toAirChartPoints(
    log: Boolean,
    scale: Double,
): List<ChartPoint> =
    mapNotNull { p ->
        val v = p.airPassengers ?: return@mapNotNull null
        ChartPoint(x = p.year.toDouble(), y = transform(v.toDouble() / scale, log))
    }.sortedBy { it.x }

private fun transform(value: Double, log: Boolean): Double =
    if (log) ln(value + 1.0) else value

/**
 * Format an absolute count into the StatTile string e.g. `"4.1 bn"` or
 * `"57.8 M"`. [unit] is the localized unit suffix (resolved at the composable
 * call site — this helper itself is not composable).
 */
private fun formatBillions(value: Long, unit: String): String = "${formatBillionsValue(value)} $unit"

private fun formatBillionsValue(value: Long): String =
    formatDecimal(value.toDouble() / 1_000_000_000.0, decimals = 1)

/**
 * Millions count with 1 decimal, e.g. `"57.8 M"` — but once the magnitude
 * reaches 100 M or more, drops the decimal to a whole number (e.g. `"142 M"`)
 * to keep the StatTile compact. [unit] is the localized unit suffix (resolved
 * at the composable call site — this helper itself is not composable).
 */
private fun formatMillions(value: Long, unit: String): String {
    val v = value.toDouble() / 1_000_000.0
    return if (v >= 100.0) {
        "${v.roundToInt()} $unit"
    } else {
        "${formatDecimal(v, decimals = 1)} $unit"
    }
}
