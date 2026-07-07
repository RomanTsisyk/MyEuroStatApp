package eu.eurostat.feature.trade.ui

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import eu.eurostat.core.charts.DivergingBarRow
import eu.eurostat.core.charts.EurostatDivergingBarChart
import eu.eurostat.core.common.EurostatCountries
import eu.eurostat.feature.trade.domain.TradeTimeSeries
import eu.eurostat.ui.component.CountryChipsRow
import eu.eurostat.ui.component.CountryPickerSheet
import eu.eurostat.ui.component.EuroCard
import eu.eurostat.ui.component.MetricHeadline
import eu.eurostat.ui.component.ModuleAppBar
import eu.eurostat.ui.component.SourceFooter
import eu.eurostat.ui.component.StatTile
import eu.eurostat.ui.component.UnderlineTabs
import eu.eurostat.ui.component.YearDropdown
import eu.eurostat.ui.component.states.EmptyState
import eu.eurostat.ui.component.states.ErrorState
import eu.eurostat.ui.component.states.LoadingShimmer
import eu.eurostat.ui.layout.AdaptiveTwoPane
import eu.eurostat.ui.layout.adaptiveChartHeight
import eu.eurostat.ui.theme.Euro
import kotlin.math.absoluteValue

private val TradeTabs = listOf("Exports", "Imports", "Balance")
private const val MAX_VISIBLE_YEARS = 8

/**
 * Trade module screen. Renders module headline, switcher tabs, a diverging-bar
 * exports-vs-imports hero, two compact KPI tiles for exports and imports,
 * country chips and a dataset footer.
 *
 * Binds to [TradeComponent.state]. Chip taps dispatch [TradeIntent.SelectActiveCountry]
 * (no re-fetch). The "+" chip opens a [CountryPickerSheet] whose "Apply" dispatches
 * [TradeIntent.SelectCountries], triggering a re-fetch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeScreen(component: TradeComponent, onBack: () -> Unit = {}) {
    val state by component.state.collectAsState()
    val accent = Euro.moduleAccents.forModule("Trade")

    val contentState = state as? TradeUiState.Content
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
            title = "Trade",
            tagline = "Flows",
            accent = accent,
            onBack = onBack,
            year = appBarYear,
            country = appBarCountry,
            onRefresh = { component.onIntent(TradeIntent.Refresh) },
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            PullToRefreshBox(
                isRefreshing = state is TradeUiState.Loading,
                onRefresh = { component.onIntent(TradeIntent.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val s = state) {
                    is TradeUiState.Loading -> LoadingBody()
                    is TradeUiState.Error -> ErrorState(
                        headline = "Could not load Trade",
                        body = s.message,
                        onRetry = if (s.canRetry) {
                            { component.onIntent(TradeIntent.Retry) }
                        } else {
                            null
                        },
                    )
                    is TradeUiState.Empty -> ErrorState(
                        headline = "No data",
                        body = "No data for the selected filters.",
                    )
                    is TradeUiState.Content -> ContentBody(
                        accent = accent,
                        series = s.series,
                        isStale = s.isStale,
                        activeCountry = s.activeCountry,
                        availableCountries = s.availableCountries,
                        selectedTabIndex = s.selectedTabIndex,
                        selectedYear = s.selectedYear,
                        availableYears = s.availableYears,
                        onSelectTab = { index ->
                            component.onIntent(TradeIntent.SelectTab(index))
                        },
                        onSelectActiveCountry = { code ->
                            component.onIntent(TradeIntent.SelectActiveCountry(code))
                        },
                        onSelectCountries = { codes ->
                            component.onIntent(TradeIntent.SelectCountries(codes))
                        },
                        onSelectYear = { year ->
                            component.onIntent(TradeIntent.SelectYear(year))
                        },
                    )
                }
            }
        }
    }
}

/** Shimmer placeholder body shown while [TradeUiState.Loading] is emitted. */
@Composable
private fun LoadingBody() {
    val chartH = adaptiveChartHeight(compact = 180.dp, medium = 240.dp, expanded = 300.dp)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Euro.spacing.base),
        verticalArrangement = Arrangement.spacedBy(Euro.spacing.m),
    ) {
        LoadingShimmer(height = 64.dp)
        LoadingShimmer(height = 36.dp)
        LoadingShimmer(height = chartH)
        LoadingShimmer(height = 72.dp)
    }
}

/**
 * Successful-state body. [activeCountry] and [availableCountries] come from
 * [TradeUiState.Content] and are owned by the component. Chip taps call
 * [onSelectActiveCountry] (no re-fetch). The picker "Apply" button calls
 * [onSelectCountries] with the full new country set, triggering a re-fetch.
 */
@Composable
private fun ContentBody(
    accent: Color,
    series: List<TradeTimeSeries>,
    isStale: Boolean,
    activeCountry: String,
    availableCountries: List<String>,
    selectedTabIndex: Int,
    selectedYear: Int,
    availableYears: List<Int>,
    onSelectTab: (Int) -> Unit,
    onSelectActiveCountry: (String) -> Unit,
    onSelectCountries: (List<String>) -> Unit,
    onSelectYear: (Int) -> Unit,
) {
    var showCountryPicker by remember { mutableStateOf(false) }

    val activeSeries = remember(series, activeCountry) {
        series.firstOrNull { it.countryCode == activeCountry }
    }

    if (activeSeries == null) {
        EmptyState(
            headline = "No data for $activeCountry",
            body = "Select a different country from the chips below.",
        )
        return
    }

    val visiblePoints = remember(activeSeries) {
        activeSeries.points.takeLast(MAX_VISIBLE_YEARS)
    }
    val latest = activeSeries.points.firstOrNull { it.year == selectedYear }

    val rows: List<DivergingBarRow> = remember(visiblePoints) {
        visiblePoints.map { p ->
            DivergingBarRow(
                label = p.year.toString(),
                exports = (p.exportsEur ?: 0L).toFloat(),
                imports = (p.importsEur ?: 0L).toFloat(),
            )
        }
    }

    val balanceText = latest?.balanceEur?.let(::formatSignedBillions) ?: "—"
    val exportsText = latest?.exportsEur?.let { "${formatBillions(it)} B €" } ?: "—"
    val importsText = latest?.importsEur?.let { "${formatBillions(it)} B €" } ?: "—"
    val latestYear = selectedYear.toString()
    val chartH = adaptiveChartHeight(compact = 180.dp, medium = 240.dp, expanded = 300.dp)

    // Sections shared between the compact (phone) ordering and the ≥840dp
    // two-pane split. Purely structural — all state stays on the component.
    val headlineSection: @Composable () -> Unit = {
        MetricHeadline(
            value = balanceText,
            unit = "B €",
            subtitle = subtitleFor(selectedTabIndex, activeCountry),
            year = latestYear,
            accent = accent,
        )
    }
    val tabsSection: @Composable () -> Unit = {
        UnderlineTabs(
            tabs = TradeTabs,
            selectedIndex = selectedTabIndex,
            onSelect = { onSelectTab(it) },
            activeColor = accent,
        )
    }
    val yearSection: @Composable () -> Unit = {
        if (availableYears.isNotEmpty()) {
            YearDropdown(
                selectedYear = selectedYear,
                years = availableYears,
                onSelect = onSelectYear,
            )
        }
    }
    val chartSection: @Composable () -> Unit = {
        EuroCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Euro.spacing.s),
            ) {
                Text(
                    text = "exports vs imports · €M",
                    style = Euro.typography.bodySmall,
                    color = Euro.colors.muted,
                )
                val exportColor = when (selectedTabIndex) {
                    0 -> accent
                    1 -> accent.copy(alpha = 0.4f)
                    else -> accent
                }
                val importColor = when (selectedTabIndex) {
                    0 -> Euro.colors.warn.copy(alpha = 0.4f)
                    1 -> Euro.colors.warn
                    else -> Euro.colors.warn
                }
                EurostatDivergingBarChart(
                    data = rows,
                    exportColor = exportColor,
                    importColor = importColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartH),
                )
                LegendRow(accent = accent, warn = Euro.colors.warn)
            }
        }
    }
    val tilesSection: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Euro.spacing.s),
        ) {
            StatTile(
                label = "exports",
                value = exportsText,
                bordered = true,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "imports",
                value = importsText,
                bordered = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
    val countriesSection: @Composable () -> Unit = {
        CountryChipsRow(
            countries = availableCountries,
            active = setOf(activeCountry),
            onSelect = { code -> onSelectActiveCountry(code) },
            onAdd = { showCountryPicker = true },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AdaptiveTwoPane(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(
                start = Euro.spacing.base,
                end = Euro.spacing.base,
                top = Euro.spacing.s,
                bottom = Euro.spacing.base,
            ),
            controls = {
                tabsSection()
                yearSection()
                countriesSection()
                Spacer(Modifier.size(Euro.spacing.s))
            },
            content = {
                headlineSection()
                chartSection()
                tilesSection()
                Spacer(Modifier.size(Euro.spacing.s))
            },
            compact = {
                headlineSection()
                tabsSection()
                yearSection()
                chartSection()
                tilesSection()
                countriesSection()
                Spacer(Modifier.size(Euro.spacing.s))
            },
        )

        SourceFooter(
            dataset = "ext_lt_intratrd",
            staleness = if (isStale) "stale" else "fresh",
            stale = isStale,
            modifier = Modifier
                .padding(horizontal = Euro.spacing.base)
                .navigationBarsPadding(),
        )
    }

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

/** Inline color-dot legend used under the diverging-bar hero. */
@Composable
private fun LegendRow(accent: Color, warn: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Euro.spacing.m),
    ) {
        LegendItem(label = "exports", dot = accent)
        LegendItem(label = "imports", dot = warn)
    }
}

@Composable
private fun LegendItem(label: String, dot: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dot, CircleShape),
        )
        Spacer(Modifier.width(Euro.spacing.xs))
        Text(
            text = label,
            style = Euro.typography.bodySmall,
            color = Euro.colors.muted,
        )
    }
}

/**
 * Format a millions-of-EUR value as a signed billions string with a leading
 * ASCII sign character (e.g. `+89`, `-12`). Uses truncating integer billions
 * (not rounded), dropping fractional digits for headline compactness.
 */
private fun formatSignedBillions(valueMEur: Long): String {
    val billions = valueMEur / 1000L
    val sign = if (billions >= 0) "+" else "-"
    return "$sign${billions.absoluteValue}"
}

/**
 * Format a millions-of-EUR value as an unsigned billions string. Negative
 * values are rendered as their absolute magnitude — used by the export/import
 * KPI tiles where direction is implied by the label.
 */
private fun formatBillions(valueMEur: Long): String {
    val billions = valueMEur / 1000L
    return billions.absoluteValue.toString()
}

private fun subtitleFor(tabIndex: Int, country: String): String {
    val metric = TradeTabs.getOrNull(tabIndex)?.lowercase() ?: "balance"
    return "$metric · partner EU27 · $country"
}
