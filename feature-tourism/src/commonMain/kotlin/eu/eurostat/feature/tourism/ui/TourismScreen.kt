package eu.eurostat.feature.tourism.ui

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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import eu.eurostat.core.charts.EurostatHeatmapChart
import eu.eurostat.core.charts.EurostatStackedBarChart
import eu.eurostat.core.charts.StackedBarRow
import eu.eurostat.ui.layout.AdaptiveTwoPane
import eu.eurostat.ui.layout.adaptiveChartHeight
import eu.eurostat.core.charts.model.ColorScale
import eu.eurostat.core.common.EurostatCountries
import eu.eurostat.feature.tourism.domain.TourismDataPoint
import eu.eurostat.feature.tourism.domain.TourismResidence
import eu.eurostat.ui.component.ChipRow
import eu.eurostat.ui.component.CountryChipsRow
import eu.eurostat.ui.component.EuroCard
import eu.eurostat.ui.component.MetricHeadline
import eu.eurostat.ui.component.ModuleAppBar
import eu.eurostat.ui.component.SourceFooter
import eu.eurostat.ui.component.CountryPickerSheet
import eu.eurostat.ui.component.YearDropdown
import eu.eurostat.ui.component.states.EmptyState
import eu.eurostat.ui.component.states.ErrorState
import eu.eurostat.ui.component.states.LoadingShimmer
import eu.eurostat.ui.component.states.localizedMessage
import eu.eurostat.ui.format.formatLargeNumberParts
import eu.eurostat.ui.theme.Euro
import kotlin.math.abs
import myeurostatapp.feature_tourism.generated.resources.Res
import myeurostatapp.feature_tourism.generated.resources.tourism_active_empty_body
import myeurostatapp.feature_tourism.generated.resources.tourism_bar_caption
import myeurostatapp.feature_tourism.generated.resources.tourism_chip_domestic
import myeurostatapp.feature_tourism.generated.resources.tourism_chip_foreign
import myeurostatapp.feature_tourism.generated.resources.tourism_chip_total
import myeurostatapp.feature_tourism.generated.resources.tourism_empty_body
import myeurostatapp.feature_tourism.generated.resources.tourism_empty_headline
import myeurostatapp.feature_tourism.generated.resources.tourism_error_headline
import myeurostatapp.feature_tourism.generated.resources.tourism_footer_staleness_fresh
import myeurostatapp.feature_tourism.generated.resources.tourism_footer_staleness_stale
import myeurostatapp.feature_tourism.generated.resources.tourism_headline_subtitle_domestic
import myeurostatapp.feature_tourism.generated.resources.tourism_headline_subtitle_foreign
import myeurostatapp.feature_tourism.generated.resources.tourism_headline_subtitle_total
import myeurostatapp.feature_tourism.generated.resources.tourism_heatmap_caption
import myeurostatapp.feature_tourism.generated.resources.tourism_legend_domestic
import myeurostatapp.feature_tourism.generated.resources.tourism_legend_foreign
import myeurostatapp.feature_tourism.generated.resources.tourism_module_title
import myeurostatapp.feature_tourism.generated.resources.tourism_month_dec
import myeurostatapp.feature_tourism.generated.resources.tourism_month_jan
import myeurostatapp.feature_tourism.generated.resources.tourism_month_jul
import myeurostatapp.feature_tourism.generated.resources.tourism_tagline
import org.jetbrains.compose.resources.stringResource

/**
 * Tourism feature screen. Renders the editorial Eurostat layout: hero metric +
 * residence chip row + stacked-bar nights chart + monthly seasonality heatmap +
 * country chip row + dataset footer.
 *
 * All data — including the seasonality heatmap — is driven by real Eurostat
 * API responses from [TourismComponent.state]. The heatmap uses `tour_occ_nim`
 * (monthly accommodation nights); a shape-preserving placeholder is shown while
 * data is loading or when the fetch fails.
 *
 * @param component the Decompose component driving this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourismScreen(component: TourismComponent, onBack: () -> Unit = {}) {
    val state by component.state.collectAsState()
    val accent: Color = Euro.moduleAccents.forModule("Tourism")

    val contentState = state as? TourismUiState.Content
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
            title = stringResource(Res.string.tourism_module_title),
            tagline = stringResource(Res.string.tourism_tagline),
            accent = accent,
            onBack = onBack,
            year = appBarYear,
            country = appBarCountry,
            onRefresh = { component.onIntent(TourismIntent.Refresh) },
        )

        Box(modifier = Modifier.weight(1f)) {
            PullToRefreshBox(
                isRefreshing = state is TourismUiState.Loading,
                onRefresh = { component.onIntent(TourismIntent.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val s = state) {
                    is TourismUiState.Loading -> LoadingShimmer(
                        modifier = Modifier.padding(Euro.spacing.base),
                    )

                    is TourismUiState.Error -> ErrorState(
                        headline = stringResource(Res.string.tourism_error_headline),
                        body = s.error.localizedMessage(),
                        onRetry = if (s.canRetry) {
                            { component.onIntent(TourismIntent.Retry) }
                        } else {
                            null
                        },
                    )

                    is TourismUiState.Empty -> EmptyState(
                        headline = stringResource(Res.string.tourism_empty_headline),
                        body = stringResource(Res.string.tourism_empty_body),
                    )

                    is TourismUiState.Content -> TourismContent(
                        content = s,
                        accent = accent,
                        onResidenceToggle = { component.onIntent(TourismIntent.HighlightResidence(it)) },
                        onCountrySelect = { component.onIntent(TourismIntent.SelectActiveCountry(it)) },
                        onConfirmCountries = { codes ->
                            component.onIntent(TourismIntent.SelectCountries(codes))
                        },
                        onYearSelect = { component.onIntent(TourismIntent.SelectYear(it)) },
                    )
                }
            }
        }

        val isStale = contentState?.isStale == true
        SourceFooter(
            dataset = "tour_occ_ninat · tour_dem_tttot · tour_occ_nim",
            staleness = if (isStale) {
                stringResource(Res.string.tourism_footer_staleness_stale)
            } else {
                stringResource(Res.string.tourism_footer_staleness_fresh)
            },
            stale = isStale,
            modifier = Modifier
                .padding(horizontal = Euro.spacing.base)
                .navigationBarsPadding(),
        )
    }
}

/**
 * Scrollable content rendered when the component reports [TourismUiState.Content].
 * Reads the active country's points to drive the headline and stacked-bar chart;
 * the seasonality heatmap is driven by real `tour_occ_nim` data when available,
 * falling back to a shape-preserving placeholder while loading.
 */
@Composable
private fun TourismContent(
    content: TourismUiState.Content,
    accent: Color,
    onResidenceToggle: (TourismResidence) -> Unit,
    onCountrySelect: (String) -> Unit,
    onConfirmCountries: (List<String>) -> Unit,
    onYearSelect: (Int) -> Unit,
) {
    val active = content.timeSeries.firstOrNull { it.countryCode == content.activeCountry }
        ?: content.timeSeries.firstOrNull()
    if (active == null) {
        EmptyState(
            headline = stringResource(Res.string.tourism_empty_headline),
            body = stringResource(Res.string.tourism_active_empty_body),
        )
        return
    }
    val barRows = active.points
        .takeLast(BAR_ROWS_VISIBLE)
        .map { point ->
            StackedBarRow(
                label = point.year.toString(),
                bottom = (point.domesticNights ?: 0L).toFloat(),
                top = (point.foreignNights ?: 0L).toFloat(),
            )
        }
    // Headline point: look up the exact selected year — no silent fallback to
    // a different year. If the active residence has no data for the chosen year,
    // the value shows "—" so that missing data is visually apparent.
    val headlinePoint = active.points.firstOrNull { it.year == content.selectedYear && headlineMetric(it, content.highlightedResidence) != null }
    val (headlineValue, headlineUnit) = formatNights(
        headlinePoint?.let { headlineMetric(it, content.highlightedResidence) },
    )
    // stringResource is @Composable — resolve the per-residence subtitle here,
    // at the composable call site, rather than in a non-composable helper.
    val headlineSubtitle = when (content.highlightedResidence) {
        TourismResidence.Domestic -> stringResource(Res.string.tourism_headline_subtitle_domestic)
        TourismResidence.Foreign -> stringResource(Res.string.tourism_headline_subtitle_foreign)
        TourismResidence.Total -> stringResource(Res.string.tourism_headline_subtitle_total)
    }
    val headlineYear = if (content.selectedYear != 0) content.selectedYear.toString() else "—"

    val barChartHeight = adaptiveChartHeight(compact = 200.dp, medium = 260.dp, expanded = 320.dp)
    val heatmapHeight = adaptiveChartHeight(compact = 160.dp, medium = 220.dp, expanded = 280.dp)

    // Localized chip labels keyed by residence, resolved here so both the
    // ChipRow options and the reverse (tapped label -> residence) lookup use
    // the same localized strings — matching against the English literal
    // would break once PL/UK translations render different chip text.
    val residenceLabels: Map<TourismResidence, String> = mapOf(
        TourismResidence.Domestic to stringResource(Res.string.tourism_chip_domestic),
        TourismResidence.Foreign to stringResource(Res.string.tourism_chip_foreign),
        TourismResidence.Total to stringResource(Res.string.tourism_chip_total),
    )

    var showCountryPicker by remember { mutableStateOf(false) }

    // Sections shared between the compact (phone) ordering and the ≥840dp
    // two-pane split. Purely structural — all state stays on the component.
    val headlineSection: @Composable () -> Unit = {
        MetricHeadline(
            value = headlineValue,
            unit = headlineUnit,
            subtitle = "$headlineSubtitle · ${active.countryName}",
            year = headlineYear,
            accent = accent,
            modifier = Modifier.padding(top = Euro.spacing.s),
        )
    }
    val residenceChips: @Composable (Modifier) -> Unit = { modifier ->
        ChipRow(
            options = residenceLabels.values.toList(),
            selected = setOf(requireNotNull(residenceLabels[content.highlightedResidence])),
            onToggle = { label ->
                residenceLabels.entries.firstOrNull { it.value == label }?.key?.let(onResidenceToggle)
            },
            modifier = modifier,
        )
    }
    val yearDropdown: @Composable () -> Unit = {
        if (content.availableYears.isNotEmpty()) {
            YearDropdown(
                selectedYear = content.selectedYear,
                years = content.availableYears,
                onSelect = onYearSelect,
            )
        }
    }
    val barChartSection: @Composable () -> Unit = {
        EuroCard {
            Column(verticalArrangement = Arrangement.spacedBy(Euro.spacing.s)) {
                Text(
                    text = stringResource(Res.string.tourism_bar_caption),
                    style = Euro.typography.bodySmall,
                    color = Euro.colors.muted,
                )
                EurostatStackedBarChart(
                    data = barRows,
                    bottomColor = accent,
                    topColor = accent.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barChartHeight),
                )
                StackedBarLegend(accent = accent)
            }
        }
    }
    val heatmapSection: @Composable () -> Unit = {
        val heatmapData = content.heatmapCells.ifEmpty { placeholderHeatmapCells() }
        EuroCard {
            Column(verticalArrangement = Arrangement.spacedBy(Euro.spacing.s)) {
                Text(
                    text = stringResource(Res.string.tourism_heatmap_caption),
                    style = Euro.typography.bodySmall,
                    color = Euro.colors.muted,
                )
                EurostatHeatmapChart(
                    cells = heatmapData,
                    colorScale = ColorScale(
                        domain = 0f..1f,
                        from = Euro.colors.surface2,
                        to = accent,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heatmapHeight),
                )
                MonthAxisLabels()
            }
        }
    }
    val countriesSection: @Composable () -> Unit = {
        CountryChipsRow(
            countries = content.timeSeries.map { it.countryCode },
            active = setOf(content.activeCountry),
            onSelect = onCountrySelect,
            onAdd = { showCountryPicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        )
    }

    AdaptiveTwoPane(
        modifier = Modifier.fillMaxSize(),
        sectionSpacing = Euro.spacing.base,
        controls = {
            Spacer(Modifier.height(Euro.spacing.s))
            residenceChips(Modifier.fillMaxWidth())
            yearDropdown()
            countriesSection()
            Spacer(Modifier.height(Euro.spacing.base))
        },
        content = {
            Spacer(Modifier.height(Euro.spacing.s))
            headlineSection()
            barChartSection()
            heatmapSection()
            Spacer(Modifier.height(Euro.spacing.base))
        },
        compact = {
            Spacer(Modifier.height(Euro.spacing.s))
            headlineSection()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Euro.spacing.s),
                modifier = Modifier.fillMaxWidth(),
            ) {
                residenceChips(Modifier.weight(1f))
                yearDropdown()
            }
            barChartSection()
            heatmapSection()
            countriesSection()
            Spacer(Modifier.height(Euro.spacing.base))
        },
    )

    if (showCountryPicker) {
        CountryPickerSheet(
            selected = content.timeSeries.map { it.countryCode }.toSet(),
            onConfirm = { selected ->
                onConfirmCountries(selected.toList())
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false },
        )
    }
}

private const val BAR_ROWS_VISIBLE = 9

private fun headlineMetric(point: TourismDataPoint, residence: TourismResidence): Long? =
    when (residence) {
        TourismResidence.Domestic -> point.domesticNights
        TourismResidence.Foreign -> point.foreignNights
        TourismResidence.Total -> point.totalNights
    }

/**
 * Format a raw nights count as a short value+unit pair, e.g. 31_700_000 -> "31.7" + "M".
 * Returns ("—", "") for null. Delegates the magnitude bucketing + rounding to
 * the shared [formatLargeNumberParts].
 */
private fun formatNights(value: Long?): Pair<String, String> {
    if (value == null) return "—" to ""
    return formatLargeNumberParts(value)
}

/**
 * Two-item legend used below the stacked bar chart: a filled dot for the
 * domestic series and an outlined circle for the foreign series.
 */
@Composable
private fun StackedBarLegend(accent: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Euro.spacing.base),
    ) {
        LegendItem(label = stringResource(Res.string.tourism_legend_domestic), filled = true, color = accent)
        LegendItem(label = stringResource(Res.string.tourism_legend_foreign), filled = false, color = accent)
    }
}

@Composable
private fun LegendItem(label: String, filled: Boolean, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (filled) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .drawBehind {
                        drawCircle(
                            color = color,
                            radius = (size.minDimension / 2f) - (0.75.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx()),
                        )
                    },
            )
        }
        Spacer(Modifier.width(Euro.spacing.xs))
        Text(
            text = label,
            style = Euro.typography.bodySmall,
            color = Euro.colors.muted,
        )
    }
}

/** Three-anchor month axis (jan / jul / dec) rendered below the heatmap. */
@Composable
private fun MonthAxisLabels() {
    val labels = listOf(
        stringResource(Res.string.tourism_month_jan),
        stringResource(Res.string.tourism_month_jul),
        stringResource(Res.string.tourism_month_dec),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                style = Euro.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = Euro.colors.muted,
            )
        }
    }
}

/**
 * Generates a 4-row × 12-column heatmap with mid-year (summer) peaks.
 * Used as a loading placeholder when real `tour_occ_nim` data is not yet
 * available (e.g. during the initial network fetch). Fully deterministic —
 * no random number generator is required.
 */
@Suppress("MagicNumber")
private fun placeholderHeatmapCells(): List<List<Float>> =
    List(4) { row ->
        List(12) { month ->
            val seasonal = 1f - abs(month - 6) / 6f
            (seasonal * 0.85f + 0.1f + row * 0.02f).coerceIn(0f, 1f)
        }
    }
