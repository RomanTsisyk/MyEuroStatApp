package eu.eurostat.feature.social.ui

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
import androidx.compose.foundation.layout.widthIn
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
import eu.eurostat.core.charts.EurostatMultiLineHighlighted
import eu.eurostat.core.charts.model.ChartPoint
import eu.eurostat.core.charts.model.ChartSeries
import eu.eurostat.core.common.EurostatCountries
import eu.eurostat.feature.social.domain.SocialDataPoint
import eu.eurostat.feature.social.domain.SocialTimeSeries
import eu.eurostat.ui.component.CountryChipsRow
import eu.eurostat.ui.component.CountryPickerSheet
import eu.eurostat.ui.component.EuroCard
import eu.eurostat.ui.component.KpiTile
import eu.eurostat.ui.component.KpiTileSelector
import eu.eurostat.ui.component.MetricHeadline
import eu.eurostat.ui.component.ModuleAppBar
import eu.eurostat.ui.component.SourceFooter
import eu.eurostat.ui.component.StaleBanner
import eu.eurostat.ui.component.YearDropdown
import eu.eurostat.ui.component.YearScrubber
import eu.eurostat.ui.component.states.EmptyState
import eu.eurostat.ui.component.states.ErrorState
import eu.eurostat.ui.component.states.LoadingShimmer
import eu.eurostat.ui.layout.adaptiveChartHeight
import eu.eurostat.ui.layout.adaptiveContentMaxWidth
import eu.eurostat.ui.theme.Euro

private const val TILE_POVERTY = "poverty"
private const val TILE_AT_RISK = "atrisk"
private const val TILE_HEALTH = "health"

/**
 * Editorial Social feature screen. Renders three % indicator series
 * (at-risk-of-poverty, at-risk-of-poverty-or-social-exclusion, very-good
 * self-perceived health) as a multi-line highlighted chart. The active KPI
 * tile determines which series is emphasised.
 *
 * All values are sourced from the live [SocialComponent] state — no mock
 * data path remains.
 */
@Composable
fun SocialScreen(component: SocialComponent, onBack: () -> Unit = {}) {
    val state by component.state.collectAsState()
    val accent = Euro.moduleAccents.forModule("Social")
    val contentState = state as? SocialUiState.Content
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
            title = "Social",
            tagline = "Wellbeing",
            accent = accent,
            onBack = onBack,
            year = appBarYear,
            country = appBarCountry,
            onSearch = {},
            onRefresh = { component.onIntent(SocialIntent.Refresh) },
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val s = state) {
                SocialUiState.Loading -> LoadingShimmer(
                    modifier = Modifier.padding(Euro.spacing.base),
                )
                is SocialUiState.Empty -> EmptyState(
                    headline = "no data",
                    body = "No social indicators for the selected filters.",
                )
                is SocialUiState.Error -> ErrorState(
                    headline = "Couldn't load social data",
                    body = s.message,
                    onRetry = if (s.canRetry) {
                        { component.onIntent(SocialIntent.Retry) }
                    } else {
                        null
                    },
                )
                is SocialUiState.Content -> SocialContent(
                    accent = accent,
                    content = s,
                    onIntent = component::onIntent,
                )
            }
        }
        SourceFooter(
            dataset = "ilc_li02 · +2",
            staleness = if ((state as? SocialUiState.Content)?.isStale == true) "stale" else "fresh",
            stale = (state as? SocialUiState.Content)?.isStale == true,
            modifier = Modifier
                .padding(horizontal = Euro.spacing.base)
                .navigationBarsPadding(),
        )
    }
}

/**
 * Real-data body for the [SocialUiState.Content] branch. Picks the active
 * country's time series, derives KPI tile values and the highlighted line
 * chart, and reports user gestures back through [onIntent].
 *
 * [content.activeCountry] and [content.availableCountries] are sourced from
 * the component state — no local `remember` holds the active country.
 */
@Composable
private fun SocialContent(
    accent: Color,
    content: SocialUiState.Content,
    onIntent: (SocialIntent) -> Unit,
) {
    val timeSeries: List<SocialTimeSeries> = content.series
    val countries: List<String> = content.availableCountries
    val activeCountry: String = content.activeCountry

    // selectedTile is driven by UiState so it survives recomposition and
    // data reloads without local remember state.
    val selectedKpiKey: SocialKpiKey = content.selectedKpiKey
    val selectedTile: String = when (selectedKpiKey) {
        SocialKpiKey.POVERTY -> TILE_POVERTY
        SocialKpiKey.AT_RISK -> TILE_AT_RISK
        SocialKpiKey.HEALTH -> TILE_HEALTH
    }

    val activeSeries: SocialTimeSeries? =
        timeSeries.firstOrNull { it.countryCode == activeCountry }
            ?: timeSeries.firstOrNull()
    val points: List<SocialDataPoint> = activeSeries?.points.orEmpty()

    // displayYearRange is derived in the component from live data, so the
    // scrubber re-anchors when dataset bounds change.
    val derivedMin = content.displayYearRange.first
    val derivedMax = content.displayYearRange.last
    // Local var still needed for immediate scrubber feedback (optimistic UI).
    var yearRange by remember(derivedMin, derivedMax) { mutableStateOf(derivedMin..derivedMax) }

    val latest: SocialDataPoint? = points.firstOrNull { it.year == content.selectedYear }
    val mutedAlt: Color = Euro.colors.mutedAlt

    val povertyStr = latest?.povertyRate.formatPct()
    val atRiskStr = latest?.atRiskRate.formatPct()
    val healthStr = latest?.healthSatisfaction.formatPct()

    val (headlineValue, headlineSubtitle, highlightIdx) = when (selectedTile) {
        TILE_AT_RISK -> Triple(
            atRiskStr,
            "at-risk-of-poverty-or-social-exclusion",
            1,
        )
        TILE_HEALTH -> Triple(
            healthStr,
            "very-good self-perceived health",
            2,
        )
        else -> Triple(
            povertyStr,
            "at-risk-of-poverty",
            0,
        )
    }
    val headlineYear: String = content.selectedYear.toString()

    val series: List<ChartSeries> = remember(points, accent, mutedAlt) {
        buildChartSeries(points = points, accent = accent, mutedAlt = mutedAlt)
    }

    val chartHeight = adaptiveChartHeight(compact = 200.dp, medium = 260.dp, expanded = 320.dp)
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
            value = headlineValue,
            unit = "%",
            subtitle = headlineSubtitle,
            year = headlineYear,
            accent = accent,
        )

        if (content.availableYears.isNotEmpty()) {
            YearDropdown(
                selectedYear = content.selectedYear,
                years = content.availableYears,
                onSelect = { onIntent(SocialIntent.SelectYear(it)) },
            )
        }

        KpiTileSelector(
            tiles = listOf(
                KpiTile(
                    key = TILE_POVERTY,
                    label = "poverty",
                    value = povertyStr,
                    unit = "% · ilc_li02",
                ),
                KpiTile(
                    key = TILE_AT_RISK,
                    label = "at-risk",
                    value = atRiskStr,
                    unit = "% · peps01",
                ),
                KpiTile(
                    key = TILE_HEALTH,
                    label = "health",
                    value = healthStr,
                    unit = "% · silc_01",
                ),
            ),
            selectedKey = selectedTile,
            onSelect = { key ->
                val kpiKey = when (key) {
                    TILE_AT_RISK -> SocialKpiKey.AT_RISK
                    TILE_HEALTH -> SocialKpiKey.HEALTH
                    else -> SocialKpiKey.POVERTY
                }
                onIntent(SocialIntent.SelectKpiTile(kpiKey))
            },
            accent = accent,
        )

        EuroCard {
            Column {
                val highlightLabel = series.getOrNull(highlightIdx)?.label ?: ""
                Text(
                    text = if (highlightLabel.isNotEmpty()) "three % series · highlighted = $highlightLabel"
                           else "three % series",
                    style = Euro.typography.bodySmall,
                    color = Euro.colors.muted,
                )
                Spacer(Modifier.height(Euro.spacing.s))
                EurostatMultiLineHighlighted(
                    series = series,
                    highlightIndex = highlightIdx,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight),
                )
                if (series.any { it.points.isNotEmpty() }) {
                    Spacer(Modifier.height(Euro.spacing.s))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Euro.spacing.m),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        series.forEachIndexed { idx, s ->
                            val isHi = idx == highlightIdx
                            LegendDot(
                                color = if (isHi) accent else mutedAlt,
                                label = s.label,
                                muted = !isHi,
                            )
                        }
                    }
                }
            }
        }

        YearScrubber(
            min = derivedMin,
            max = derivedMax,
            value = yearRange,
            onValueChange = {
                yearRange = it
                onIntent(SocialIntent.ChangeYearRange(it))
            },
            modifier = Modifier.fillMaxWidth(),
        )

        if (countries.isNotEmpty()) {
            var showCountryPicker by remember { mutableStateOf(false) }
            CountryChipsRow(
                countries = countries,
                active = setOf(activeCountry),
                onSelect = { onIntent(SocialIntent.SelectActiveCountry(it)) },
                onAdd = { showCountryPicker = true },
                modifier = Modifier.fillMaxWidth(),
            )
            if (showCountryPicker) {
                CountryPickerSheet(
                    selected = countries.toSet(),
                    onConfirm = { selected ->
                        onIntent(SocialIntent.SelectCountries(selected.toList()))
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

@Composable
private fun LegendDot(color: Color, label: String, muted: Boolean) {
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
            color = if (muted) Euro.colors.muted else Euro.colors.ink,
        )
    }
}

/**
 * Build the three Social indicator [ChartSeries] from a country's points.
 *  - index 0 → povertyRate, painted with the module [accent].
 *  - index 1 → atRiskRate, painted with [mutedAlt] (chart dims further).
 *  - index 2 → healthSatisfaction, painted with [mutedAlt].
 *
 * Each null metric becomes a `ChartPoint(x, y = null)` so the chart can skip
 * the data gap without re-aligning the x-axis.
 */
private fun buildChartSeries(
    points: List<SocialDataPoint>,
    accent: Color,
    mutedAlt: Color,
): List<ChartSeries> {
    val sorted = points.sortedBy { it.year }
    val poverty = sorted.map { ChartPoint(x = it.year.toDouble(), y = it.povertyRate) }
    val atRisk = sorted.map { ChartPoint(x = it.year.toDouble(), y = it.atRiskRate) }
    val health = sorted.map { ChartPoint(x = it.year.toDouble(), y = it.healthSatisfaction) }
    return listOf(
        ChartSeries(label = "poverty", color = accent, points = poverty),
        ChartSeries(label = "at-risk", color = mutedAlt, points = atRisk),
        ChartSeries(label = "health", color = mutedAlt, points = health),
    )
}

/**
 * Format a percentage value as "15.0", or "—" when the value is absent.
 * Multiplatform-friendly: avoids `String.format` which is JVM-only.
 */
private fun Double?.formatPct(): String {
    val v = this ?: return "—"
    val rounded = kotlin.math.round(v * 10.0) / 10.0
    val whole = rounded.toLong()
    val frac = kotlin.math.round((rounded - whole) * 10.0).toLong().let {
        if (it < 0L) -it else it
    }
    val sign = if (rounded < 0.0 && whole == 0L) "-" else ""
    return "$sign$whole.$frac"
}
