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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.eurostat.core.charts.EurostatLineChart
import eu.eurostat.core.charts.model.ChartAxis
import eu.eurostat.core.charts.model.ChartPoint
import eu.eurostat.core.charts.model.ChartSeries
import eu.eurostat.core.common.EurostatCountries
import eu.eurostat.feature.economy.domain.EconomyDataPoint
import eu.eurostat.feature.economy.domain.EconomyMetric
import eu.eurostat.feature.economy.domain.EconomyTimeSeries
import eu.eurostat.ui.component.CountryChipsRow
import eu.eurostat.ui.component.CountryPickerSheet
import eu.eurostat.ui.component.EuroCard
import eu.eurostat.ui.component.MetricHeadline
import eu.eurostat.ui.component.ModuleAppBar
import eu.eurostat.ui.component.SegmentedControl
import eu.eurostat.ui.component.SourceFooter
import eu.eurostat.ui.component.StatTile
import eu.eurostat.ui.component.YearDropdown
import eu.eurostat.ui.component.YearScrubber
import eu.eurostat.ui.component.states.EmptyState
import eu.eurostat.ui.component.states.ErrorState
import eu.eurostat.ui.component.states.LoadingShimmer
import eu.eurostat.ui.layout.adaptiveChartHeight
import eu.eurostat.ui.layout.adaptiveContentMaxWidth
import eu.eurostat.ui.theme.Euro
import kotlin.math.abs

private val SeriesFr: Color = Color(0xFF7A5C46)
private val SeriesPl: Color = Color(0xFF5E6B58)

private const val M_TO_B: Double = 1_000.0
private const val PERCENT: Double = 100.0

/**
 * Editorial Economy feature screen. Bound to [EconomyComponent.state]:
 * Loading / Empty / Error branches use the shared status views; the
 * Content branch renders the headline + GDP/Inflation/Deficit segmented
 * switcher, a multi-country line chart hero, secondary KPI tiles for the
 * inactive metrics, the year scrubber and country chips.
 */
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
            title = "Economy",
            tagline = "Macro",
            accent = accent,
            onBack = onBack,
            year = appBarYear,
            country = appBarCountry,
            onSearch = {},
            onRefresh = { component.onIntent(EconomyIntent.Refresh) },
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val s = state) {
                EconomyUiState.Loading -> LoadingShimmer(
                    modifier = Modifier.padding(Euro.spacing.base),
                )
                is EconomyUiState.Empty -> EmptyState(
                    headline = "no data",
                    body = "No economy data for the selected filters.",
                )
                is EconomyUiState.Error -> ErrorState(
                    headline = "Couldn't load economy",
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
        SourceFooter(
            dataset = "nama_10_gdp · +2",
            staleness = if ((state as? EconomyUiState.Content)?.isStale == true) "stale" else "fresh",
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
    val metricLabels = remember { listOf("GDP", "Inflation", "Deficit") }
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
    val maxW = adaptiveContentMaxWidth()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .then(
                    if (maxW != Dp.Unspecified) Modifier.widthIn(max = maxW) else Modifier,
                )
                .padding(horizontal = Euro.spacing.base),
            verticalArrangement = Arrangement.spacedBy(Euro.spacing.m),
        ) {
            Spacer(Modifier.height(Euro.spacing.xs))

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

            HeadlineForMetric(
                metric = selectedMetric,
                latest = latestPoint,
                previous = prevPoint,
                accent = accent,
            )

            SegmentedControl(
                options = metricLabels,
                selectedIndex = selectedIndex,
                onSelect = { component.onIntent(EconomyIntent.SelectMetric(metrics[it])) },
                activeColor = accent,
            )

            EuroCard {
                Column {
                    val chartSeries = remember(timeSeries, selectedMetric, accent, yearRange) {
                        buildChartSeries(timeSeries, selectedMetric, accent, yearRange)
                    }
                    if (chartSeries.isEmpty() || chartSeries.all { it.points.isEmpty() }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(chartHeight),
                            contentAlignment = Alignment.Center,
                        ) {
                            EmptyState(
                                headline = "no data",
                                body = "No series available for the selected metric and year range.",
                            )
                        }
                    } else {
                        EurostatLineChart(
                            series = chartSeries,
                            xAxis = ChartAxis(label = "Year"),
                            yAxis = ChartAxis(label = yAxisLabelFor(selectedMetric)),
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

            SecondaryMetricTiles(
                selected = selectedMetric,
                latest = latestPoint,
            )

            YearScrubber(
                min = minYear,
                max = maxYear,
                value = yearRange,
                onValueChange = { component.onIntent(EconomyIntent.SetDisplayYearRange(it)) },
            )

            CountryChipsRow(
                countries = availableCountries,
                active = setOf(activeCountry),
                onSelect = { component.onIntent(EconomyIntent.SelectActiveCountry(it)) },
                onAdd = { showCountryPicker = true },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(Euro.spacing.s))
        }
    }

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
            "B €",
            "GDP · current prices",
        )
        EconomyMetric.Inflation -> Triple(
            latest?.hicpIndex?.let { formatDecimal(it, 1) } ?: "—",
            "idx",
            "HICP · 2015 = 100",
        )
        EconomyMetric.Deficit -> Triple(
            latest?.deficitPctGdp?.let { formatSignedPercent(it) } ?: "—",
            "% GDP",
            "Gov. net lending / borrowing",
        )
    }
    val deltaText = yoyDeltaText(metric, latest, previous)
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
                    label = "GDP",
                    value = latest?.gdpEur?.let { formatBillions(it) } ?: "—",
                    delta = "B € · current prices",
                    bordered = true,
                    modifier = Modifier.weight(1f).alpha(0.55f),
                )
                EconomyMetric.Inflation -> StatTile(
                    label = "HICP infl.",
                    value = latest?.hicpIndex?.let { formatDecimal(it, 1) } ?: "—",
                    delta = "idx · 2015=100",
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
                text = "deficit",
                style = Euro.typography.bodySmall,
                color = Euro.colors.muted,
            )
            Text(
                text = value?.let { formatSignedPercent(it) } ?: "—",
                style = Euro.typography.tabularNumLarge,
                color = Euro.colors.warn,
            )
            Text(
                text = "% of GDP",
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
 * given [metric], filtered to [yearRange] and using the editorial color
 * palette (DE = module accent, FR = sienna, PL = olive, EU27_* = muted).
 */
private fun buildChartSeries(
    timeSeries: List<EconomyTimeSeries>,
    metric: EconomyMetric,
    accent: Color,
    yearRange: IntRange,
): List<ChartSeries> = timeSeries.map { ts ->
    val points = ts.points
        .filter { it.year in yearRange }
        .mapNotNull { p ->
            p.fieldFor(metric)?.let { v -> ChartPoint(x = p.year.toDouble(), y = v) }
        }
    ChartSeries(
        label = ts.countryCode,
        color = colorFor(ts.countryCode, accent),
        points = points,
    )
}

private fun colorFor(countryCode: String, accent: Color): Color = when (countryCode) {
    "DE" -> accent
    "FR" -> SeriesFr
    "PL" -> SeriesPl
    else -> if (countryCode.startsWith("EU")) Color(0xFFA39A8D) else accent
}

private fun yAxisLabelFor(metric: EconomyMetric): String = when (metric) {
    EconomyMetric.Gdp -> "GDP M€"
    EconomyMetric.Inflation -> "HICP idx"
    EconomyMetric.Deficit -> "% GDP"
}

/** Year-over-year delta string, e.g. `"+6.2%"`, `"+1.2 idx"`, `"−0.4 pp"`. */
private fun yoyDeltaText(
    metric: EconomyMetric,
    latest: EconomyDataPoint?,
    previous: EconomyDataPoint?,
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
            "${signed(l - p)} idx"
        }
        EconomyMetric.Deficit -> {
            val l = latest.deficitPctGdp ?: return null
            val p = previous.deficitPctGdp ?: return null
            "${signed(l - p)} pp"
        }
    }
}

/** Million-EUR → billion-EUR, formatted with non-breaking thin spaces (e.g. `3 451`). */
private fun formatBillions(millionEur: Long): String {
    val billions = millionEur / M_TO_B
    return formatThousands(billions.toLong())
}

private fun formatThousands(n: Long): String {
    val raw = abs(n).toString()
    val parts = mutableListOf<String>()
    var i = raw.length
    while (i > 0) {
        val start = maxOf(0, i - 3)
        parts.add(0, raw.substring(start, i))
        i = start
    }
    val joined = parts.joinToString(" ")
    return if (n < 0) "-$joined" else joined
}

private fun formatDecimal(v: Double, digits: Int): String {
    val factor = pow10(digits)
    val rounded = kotlin.math.round(v * factor) / factor
    val whole = rounded.toLong()
    val frac = abs((rounded - whole) * factor).toLong()
    return if (digits == 0) whole.toString() else "$whole.${frac.toString().padStart(digits, '0')}"
}

private fun formatSignedPercent(v: Double): String {
    val formatted = formatDecimal(abs(v), 1)
    return when {
        v > 0 -> "+$formatted%"
        v < 0 -> "−$formatted%"
        else -> "0.0%"
    }
}

private fun signed(v: Double): String {
    val formatted = formatDecimal(abs(v), 1)
    return when {
        v > 0 -> "+$formatted"
        v < 0 -> "−$formatted"
        else -> "0.0"
    }
}

private fun pow10(n: Int): Double {
    var r = 1.0
    repeat(n) { r *= 10.0 }
    return r
}
