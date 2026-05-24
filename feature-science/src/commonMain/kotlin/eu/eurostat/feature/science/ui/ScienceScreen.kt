package eu.eurostat.feature.science.ui

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import eu.eurostat.ui.layout.adaptiveChartHeight
import eu.eurostat.ui.layout.adaptiveContentMaxWidth
import eu.eurostat.core.charts.EurostatRadarChart
import eu.eurostat.core.common.EurostatCountries
import eu.eurostat.core.charts.RadarSeries
import eu.eurostat.core.charts.model.ChartAxis
import eu.eurostat.core.charts.model.ChartPoint
import eu.eurostat.core.charts.model.ChartSeries
import eu.eurostat.feature.science.domain.ScienceDataPoint
import eu.eurostat.feature.science.domain.ScienceTimeSeries
import eu.eurostat.ui.component.CountryChipsRow
import eu.eurostat.ui.component.CountryPickerSheet
import eu.eurostat.ui.component.EuroCard
import eu.eurostat.ui.component.MetricHeadline
import eu.eurostat.ui.component.ModuleAppBar
import eu.eurostat.ui.component.SourceFooter
import eu.eurostat.ui.component.StaleBanner
import eu.eurostat.ui.component.YearDropdown
import eu.eurostat.ui.component.states.EmptyState
import eu.eurostat.ui.component.states.ErrorState
import eu.eurostat.ui.component.states.LoadingShimmer
import eu.eurostat.ui.theme.Euro

/**
 * Editorial Science feature screen. Renders the R&D / Internet / Tertiary
 * radar hero (top two countries overlaid), three sparkline tiles for each
 * metric for the active country, and a country chip row, surrounded by the
 * standard module chrome.
 *
 * No metric switcher — the radar already presents all three normalized %
 * metrics at once.
 */
@Composable
fun ScienceScreen(component: ScienceComponent, onBack: () -> Unit = {}) {
    val state by component.state.collectAsState()
    val accent = Euro.moduleAccents.forModule("Science")
    val contentState = state as? ScienceUiState.Content
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
            title = "Science",
            tagline = "Innovation",
            accent = accent,
            onBack = onBack,
            year = appBarYear,
            country = appBarCountry,
            onSearch = {},
            onRefresh = { component.onIntent(ScienceIntent.Refresh) },
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val s = state) {
                ScienceUiState.Loading -> LoadingShimmer(
                    modifier = Modifier.padding(Euro.spacing.base),
                )
                is ScienceUiState.Empty -> EmptyState(
                    headline = "no data",
                    body = "No science indicators for the selected filters.",
                )
                is ScienceUiState.Error -> ErrorState(
                    headline = "Couldn't load science",
                    body = s.message,
                    onRetry = if (s.canRetry) {
                        { component.onIntent(ScienceIntent.Retry) }
                    } else {
                        null
                    },
                )
                is ScienceUiState.Content -> ScienceContent(
                    accent = accent,
                    timeSeries = s.series,
                    isStale = s.isStale,
                    activeCountry = s.activeCountry,
                    availableCountries = s.availableCountries,
                    selectedYear = s.selectedYear,
                    availableYears = s.availableYears,
                    onSelectActiveCountry = { code ->
                        component.onIntent(ScienceIntent.SelectActiveCountry(code))
                    },
                    onSelectCountries = { codes ->
                        component.onIntent(ScienceIntent.SelectCountries(codes))
                    },
                    onSelectYear = { year ->
                        component.onIntent(ScienceIntent.SelectYear(year))
                    },
                )
            }
        }
        SourceFooter(
            dataset = "rd_e_gerdtot · +2",
            staleness = "fresh",
            stale = (state as? ScienceUiState.Content)?.isStale == true,
            modifier = Modifier
                .padding(horizontal = Euro.spacing.base)
                .navigationBarsPadding(),
        )
    }
}

@Composable
private fun ScienceContent(
    accent: Color,
    timeSeries: List<ScienceTimeSeries>,
    isStale: Boolean,
    activeCountry: String,
    availableCountries: List<String>,
    selectedYear: Int?,
    availableYears: List<Int>,
    onSelectActiveCountry: (String) -> Unit,
    onSelectCountries: (List<String>) -> Unit,
    onSelectYear: (Int) -> Unit,
) {
    val warn = Euro.colors.warn

    val activeSeries = remember(timeSeries, activeCountry) {
        timeSeries.firstOrNull { it.countryCode == activeCountry }
    }

    // Data point per country at the selected year, used for the radar.
    val pointAtSelectedYear: Map<String, ScienceDataPoint> = remember(timeSeries, selectedYear) {
        if (selectedYear == null) emptyMap()
        else timeSeries.mapNotNull { ts ->
            ts.points.firstOrNull { it.year == selectedYear }?.let { ts.countryCode to it }
        }.toMap()
    }

    // Headline value: R&D for the active country at the selected year, if available.
    val headlineRd: Double? = remember(activeSeries, selectedYear) {
        if (selectedYear == null) null
        else activeSeries?.points
            ?.firstOrNull { it.year == selectedYear }
            ?.rdSpendPctGdp
    }

    val maxW = adaptiveContentMaxWidth()
    val radarHeight = adaptiveChartHeight(compact = 260.dp, medium = 320.dp, expanded = 380.dp)

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (maxW != Dp.Unspecified) Modifier.widthIn(max = maxW) else Modifier)
            .align(Alignment.TopCenter)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Euro.spacing.base),
        verticalArrangement = Arrangement.spacedBy(Euro.spacing.m),
    ) {
        if (isStale) {
            StaleBanner(modifier = Modifier.padding(top = Euro.spacing.s))
        }

        Spacer(Modifier.height(Euro.spacing.xs))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MetricHeadline(
                value = headlineRd?.formatPct() ?: "—",
                unit = "%",
                subtitle = buildString {
                    append("R&D spend · % of GDP")
                    if (selectedYear != null) append(" · ").append(selectedYear)
                },
                year = selectedYear?.toString().orEmpty(),
                accent = accent,
                modifier = Modifier.weight(1f),
            )
            if (availableYears.isNotEmpty() && selectedYear != null) {
                Spacer(Modifier.width(Euro.spacing.s))
                YearDropdown(
                    selectedYear = selectedYear,
                    years = availableYears,
                    onSelect = onSelectYear,
                )
            }
        }

        EuroCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                // Label clarifies: active country (primary) vs. best-available comparison (DE/FR/EU27).
                Text(
                    text = buildString {
                        append("radar · selected country vs. top peer")
                        if (selectedYear != null) append(" · ").append(selectedYear)
                    },
                    style = Euro.typography.bodySmall,
                    color = Euro.colors.muted,
                )
                Spacer(Modifier.height(Euro.spacing.s))

                val radarSeries = buildRadarSeries(
                    pointAtYear = pointAtSelectedYear,
                    activeCountry = activeCountry,
                    primaryColor = accent,
                    secondaryColor = warn,
                )
                EurostatRadarChart(
                    axes = listOf("R&D %GDP", "Internet %", "Tertiary %"),
                    series = radarSeries,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(radarHeight),
                )
                Spacer(Modifier.height(Euro.spacing.s))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    radarSeries.forEachIndexed { i, rs ->
                        if (i > 0) Spacer(Modifier.width(Euro.spacing.m))
                        LegendDot(color = rs.color, label = rs.label)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Euro.spacing.s),
        ) {
            SparkTile(
                label = "R&D",
                unit = "%GDP",
                accent = accent,
                series = activeSeries?.toSparkSeries(accent) { it.rdSpendPctGdp },
                modifier = Modifier.weight(1f),
            )
            SparkTile(
                label = "Internet",
                unit = "%ind",
                accent = accent,
                series = activeSeries?.toSparkSeries(accent) { it.internetUsagePct },
                modifier = Modifier.weight(1f),
            )
            SparkTile(
                label = "Tertiary",
                unit = "%25-64",
                accent = accent,
                series = activeSeries?.toSparkSeries(accent) { it.tertiaryEducPct },
                modifier = Modifier.weight(1f),
            )
        }

        if (availableCountries.isNotEmpty()) {
            var showCountryPicker by remember { mutableStateOf(false) }
            CountryChipsRow(
                countries = availableCountries,
                active = setOf(activeCountry),
                onSelect = { onSelectActiveCountry(it) },
                onAdd = { showCountryPicker = true },
                modifier = Modifier.fillMaxWidth(),
            )
            if (showCountryPicker) {
                CountryPickerSheet(
                    selected = availableCountries.toSet(),
                    onConfirm = { selected ->
                        onSelectCountries(selected.toList())
                        showCountryPicker = false
                    },
                    onDismiss = { showCountryPicker = false },
                )
            }
        }

        Spacer(Modifier.height(Euro.spacing.s))
    }
    } // end Box
}

/**
 * Build up to two [RadarSeries] from data points at the selected year per country.
 * Each axis is normalized to [0, 1] against the max across all supplied
 * countries (not just the two rendered) so that the chart reads consistently
 * even when more countries are loaded than displayed.
 *
 * If a country has no observation for the selected year its axes render as 0
 * (matching the existing behavior for null values via [safeNormalize]).
 */
private fun buildRadarSeries(
    pointAtYear: Map<String, ScienceDataPoint>,
    activeCountry: String,
    primaryColor: Color,
    secondaryColor: Color,
): List<RadarSeries> {
    if (pointAtYear.isEmpty()) return emptyList()

    val maxR = pointAtYear.values.maxOfOrNull { it.rdSpendPctGdp ?: 0.0 } ?: 0.0
    val maxI = pointAtYear.values.maxOfOrNull { it.internetUsagePct ?: 0.0 } ?: 0.0
    val maxT = pointAtYear.values.maxOfOrNull { it.tertiaryEducPct ?: 0.0 } ?: 0.0

    // Render the actively selected country FIRST (primary color), then one
    // comparison country (prefer DE -> FR -> EU27_2020 -> first available
    // non-active country). This way the radar re-shapes when the user
    // switches the country chip below the chart.
    val comparisonCandidates = listOf("DE", "FR", "EU27_2020") +
        pointAtYear.keys.toList()
    val comparison = comparisonCandidates.firstOrNull {
        it != activeCountry && it in pointAtYear.keys
    }
    val ordered = listOfNotNull(activeCountry.takeIf { it in pointAtYear.keys }, comparison)
        .distinct()
        .take(2)

    return ordered.mapIndexedNotNull { i, code ->
        val pt = pointAtYear[code] ?: return@mapIndexedNotNull null
        RadarSeries(
            label = code,
            color = if (i == 0) primaryColor else secondaryColor,
            values = listOf(
                safeNormalize(pt.rdSpendPctGdp, maxR),
                safeNormalize(pt.internetUsagePct, maxI),
                safeNormalize(pt.tertiaryEducPct, maxT),
            ),
        )
    }
}

private fun safeNormalize(value: Double?, max: Double): Float {
    if (value == null || max <= 0.0) return 0f
    return (value / max).coerceIn(0.0, 1.0).toFloat()
}

/**
 * Materialize a [ChartSeries] from this country's points for a single metric
 * selected by [selector]. Null values are dropped (the sparkline collapses
 * to the contiguous run of known observations).
 */
private fun ScienceTimeSeries.toSparkSeries(
    accent: Color,
    selector: (ScienceDataPoint) -> Double?,
): ChartSeries {
    val pts = points.mapNotNull { p ->
        val y = selector(p) ?: return@mapNotNull null
        ChartPoint(x = p.year.toDouble(), y = y)
    }
    return ChartSeries(label = "spark", color = accent, points = pts)
}

/**
 * Compact sparkline KPI tile: small label, tabular value + unit, and a tiny
 * line chart underneath with axes hidden.
 *
 * When [series] is null or empty, the value shows a dash and the chart area
 * collapses to an empty box.
 */
@Composable
private fun SparkTile(
    label: String,
    unit: String,
    accent: Color,
    series: ChartSeries?,
    modifier: Modifier = Modifier,
) {
    val displayValue = series?.points?.maxByOrNull { it.x }?.y?.formatPct() ?: "—"
    EuroCard(modifier = modifier) {
        Column {
            Text(
                text = label,
                style = Euro.typography.bodySmall,
                color = Euro.colors.muted,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = displayValue,
                    style = Euro.typography.tabularNumLarge,
                    color = Euro.colors.ink,
                )
                Spacer(Modifier.width(Euro.spacing.xs))
                Text(
                    text = unit,
                    style = Euro.typography.bodySmall,
                    color = Euro.colors.muted,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Spacer(Modifier.height(Euro.spacing.xs))
            EurostatLineChart(
                series = if (series != null && series.points.isNotEmpty()) listOf(series) else emptyList(),
                xAxis = ChartAxis(label = ""),
                yAxis = ChartAxis(label = ""),
                hideAxis = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
            )
            // Mark accent as intentionally referenced; the series color already
            // carries the accent hue.
            @Suppress("UNUSED_EXPRESSION") accent
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

/** Format a percentage-like Double with two decimals when small, one otherwise. */
private fun Double.formatPct(): String {
    val rounded = (this * 100.0).toLong() / 100.0
    val whole = rounded.toLong()
    val frac = ((rounded - whole) * 100).toLong()
    val absFrac = if (frac < 0) -frac else frac
    val fracStr = absFrac.toString().padStart(2, '0')
    return "$whole.$fracStr"
}
