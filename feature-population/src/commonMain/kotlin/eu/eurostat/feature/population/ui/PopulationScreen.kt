package eu.eurostat.feature.population.ui

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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.eurostat.core.charts.EurostatPyramidChart
import eu.eurostat.core.charts.PyramidRow
import eu.eurostat.core.common.EurostatCountries
import eu.eurostat.feature.population.domain.PopulationSnapshot
import eu.eurostat.ui.component.CountryChipsRow
import eu.eurostat.ui.component.CountryPickerSheet
import eu.eurostat.ui.component.EuroCard
import eu.eurostat.ui.component.MetricHeadline
import eu.eurostat.ui.component.ModuleAppBar
import eu.eurostat.ui.component.SegmentedControl
import eu.eurostat.ui.component.SourceFooter
import eu.eurostat.ui.component.StaleBanner
import eu.eurostat.ui.component.states.EmptyState
import eu.eurostat.ui.component.states.ErrorState
import eu.eurostat.ui.component.states.LoadingShimmer
import eu.eurostat.ui.format.formatDecimal
import eu.eurostat.ui.format.formatLargeNumber
import eu.eurostat.ui.format.formatLargeNumberParts
import eu.eurostat.ui.layout.adaptiveChartHeight
import eu.eurostat.ui.layout.adaptiveContentMaxWidth
import eu.eurostat.ui.theme.Euro
import eu.eurostat.ui.theme.EuroWindowWidth
import eu.eurostat.ui.theme.LocalEuroWindowWidth
import kotlin.math.abs

/** Number of discrete steps reserved for the year slider. Slider needs steps + 2 = count. */
private const val SLIDER_STEP_PADDING = 2

/**
 * Editorial Population feature screen — wires real Eurostat `demo_pjan` data
 * into a demographic pyramid plus headline total, country chips, year scrubber,
 * and a Total/Men/Women segmented control.
 */
@Composable
fun PopulationScreen(component: PopulationComponent, onBack: () -> Unit = {}) {
    val state by component.state.collectAsState()
    val accent = Euro.moduleAccents.forModule("Population")
    val appBarYear = (state as? PopulationUiState.Content)?.selectedYear
    val appBarCountry = (state as? PopulationUiState.Content)?.selectedCountry?.let { code ->
        val name = EurostatCountries.byCode(code)?.name ?: code
        "$name · $code"
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Euro.colors.paper),
    ) {
        ModuleAppBar(
            title = "Population",
            tagline = "Demography",
            accent = accent,
            onBack = onBack,
            year = appBarYear,
            country = appBarCountry,
            onSearch = {},
            onRefresh = { component.onIntent(PopulationIntent.Refresh) },
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val s = state) {
                PopulationUiState.Loading -> LoadingShimmer(
                    modifier = Modifier.padding(Euro.spacing.base),
                )
                is PopulationUiState.Empty -> EmptyState(
                    headline = "no data",
                    body = "No population data for the selected filters.",
                )
                is PopulationUiState.Error -> ErrorState(
                    headline = "Couldn't load population",
                    body = s.message,
                    onRetry = if (s.canRetry) {
                        { component.onIntent(PopulationIntent.Retry) }
                    } else {
                        null
                    },
                )
                is PopulationUiState.Content -> PopulationContent(
                    state = s,
                    accent = accent,
                    onIntent = component::onIntent,
                )
            }
        }
        SourceFooter(
            dataset = "demo_pjangroup",
            staleness = "fresh",
            stale = (state as? PopulationUiState.Content)?.isStale == true,
            modifier = Modifier
                .padding(horizontal = Euro.spacing.base)
                .navigationBarsPadding(),
        )
    }
}

@Composable
private fun PopulationContent(
    state: PopulationUiState.Content,
    accent: Color,
    onIntent: (PopulationIntent) -> Unit,
) {
    val femaleColor = Euro.colors.warn
    val snapshot = state.snapshot

    // Side-emphasis driven by segmented control.
    val maleAlpha = when (state.selectedMetric) {
        2 -> 0.5f      // Women selected: dim men.
        else -> 1f
    }
    val femaleAlpha = when (state.selectedMetric) {
        1 -> 0.5f      // Men selected: dim women.
        else -> 1f
    }

    val cohorts: List<PyramidRow> = snapshot?.cohorts
        ?.asReversed() // chart wants oldest at top, youngest at bottom.
        ?.map { c ->
            PyramidRow(
                label = c.ageLabel,
                male = c.male.toFloat(),
                female = c.female.toFloat(),
            )
        }
        .orEmpty()

    val headlineCountryName = snapshot?.countryName
        ?: state.timeSeries.firstOrNull { it.countryCode == state.selectedCountry }?.countryName
        ?: state.selectedCountry
    val headlineYoY = computeYoY(state, snapshot)
    val (headlineValue, headlineUnit) = headlineParts(snapshot?.total ?: fallbackTotal(state))

    val windowWidth = LocalEuroWindowWidth.current
    val pyramidHeight = adaptiveChartHeight(compact = 220.dp, medium = 320.dp, expanded = 420.dp)
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
            if (state.isStale) {
                StaleBanner(modifier = Modifier.padding(top = Euro.spacing.s))
            }

            Spacer(Modifier.height(Euro.spacing.xs))

            MetricHeadline(
                value = headlineValue,
                unit = headlineUnit,
                subtitle = buildString {
                    append("total · ")
                    append(headlineCountryName)
                    if (headlineYoY != null) {
                        append(" · ")
                        append(headlineYoY)
                    }
                },
                year = state.selectedYear.toString(),
                accent = accent,
            )

            SegmentedControl(
                options = listOf("Total", "Men", "Women"),
                selectedIndex = state.selectedMetric,
                onSelect = { onIntent(PopulationIntent.SelectMetric(it)) },
                activeColor = accent,
            )

            if (windowWidth == EuroWindowWidth.Expanded) {
                // Two-pane: pyramid on the left, country/year controls on the right.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Euro.spacing.m),
                ) {
                    EuroCard(modifier = Modifier.weight(1f)) {
                        PyramidCardContent(
                            cohorts = cohorts,
                            accent = accent,
                            maleAlpha = maleAlpha,
                            femaleAlpha = femaleAlpha,
                            femaleColor = femaleColor,
                            snapshot = snapshot,
                            pyramidHeight = pyramidHeight,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Euro.spacing.m),
                    ) {
                        if (state.availableYears.isNotEmpty()) {
                            YearSlider(state = state, accent = accent, onIntent = onIntent)
                        }
                        CountrySectionExpanded(state = state, onIntent = onIntent)
                    }
                }
            } else {
                EuroCard {
                    PyramidCardContent(
                        cohorts = cohorts,
                        accent = accent,
                        maleAlpha = maleAlpha,
                        femaleAlpha = femaleAlpha,
                        femaleColor = femaleColor,
                        snapshot = snapshot,
                        pyramidHeight = pyramidHeight,
                    )
                }

                if (state.availableYears.isNotEmpty()) {
                    YearSlider(state = state, accent = accent, onIntent = onIntent)
                }

                if (state.availableCountries.isNotEmpty()) {
                    var showCountryPicker by remember { mutableStateOf(false) }
                    CountryChipsRow(
                        countries = state.availableCountries,
                        active = setOf(state.selectedCountry),
                        onSelect = { onIntent(PopulationIntent.SelectActiveCountry(it)) },
                        onAdd = { showCountryPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (showCountryPicker) {
                        CountryPickerSheet(
                            selected = state.availableCountries.toSet(),
                            onConfirm = { selected ->
                                onIntent(PopulationIntent.SelectCountries(selected.toList()))
                                showCountryPicker = false
                            },
                            onDismiss = { showCountryPicker = false },
                        )
                    }
                }
            }

            Spacer(Modifier.height(Euro.spacing.s))
        }
    }
}

@Composable
private fun PyramidCardContent(
    cohorts: List<PyramidRow>,
    accent: Color,
    maleAlpha: Float,
    femaleAlpha: Float,
    femaleColor: Color,
    snapshot: PopulationSnapshot?,
    pyramidHeight: Dp,
) {
    Column {
        if (cohorts.isEmpty()) {
            // `snapshot` is null on the first (cached) emission — cohorts arrive with
            // the subsequent network response. Show a shimmer while we wait.
            // If `snapshot` is non-null but `cohorts` is still empty, the API returned
            // a snapshot with no cohort rows — show a proper empty state instead of a
            // misleading "loading" label.
            Box(modifier = Modifier.fillMaxWidth().height(pyramidHeight)) {
                if (snapshot == null) {
                    LoadingShimmer(height = pyramidHeight)
                } else {
                    EmptyState(
                        headline = "No cohort data",
                        body = "No 5-year age cohort data available for ${snapshot.countryName} ${snapshot.year}.",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            EurostatPyramidChart(
                cohorts = cohorts,
                maleColor = accent.copy(alpha = maleAlpha),
                femaleColor = femaleColor.copy(alpha = femaleAlpha),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pyramidHeight),
            )
        }
        Spacer(Modifier.height(Euro.spacing.s))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "5-yr cohorts",
                style = Euro.typography.bodySmall,
                color = Euro.colors.muted,
            )
            LegendDot(
                color = accent.copy(alpha = maleAlpha),
                label = "men ${formatLargeNumber(snapshot?.totalMale ?: 0L)}",
            )
            LegendDot(
                color = femaleColor.copy(alpha = femaleAlpha),
                label = "women ${formatLargeNumber(snapshot?.totalFemale ?: 0L)}",
            )
        }
    }
}

@Composable
private fun CountrySectionExpanded(
    state: PopulationUiState.Content,
    onIntent: (PopulationIntent) -> Unit,
) {
    if (state.availableCountries.isNotEmpty()) {
        var showCountryPicker by remember { mutableStateOf(false) }
        CountryChipsRow(
            countries = state.availableCountries,
            active = setOf(state.selectedCountry),
            onSelect = { onIntent(PopulationIntent.SelectActiveCountry(it)) },
            onAdd = { showCountryPicker = true },
            modifier = Modifier.fillMaxWidth(),
        )
        if (showCountryPicker) {
            CountryPickerSheet(
                selected = state.availableCountries.toSet(),
                onConfirm = { selected ->
                    onIntent(PopulationIntent.SelectCountries(selected.toList()))
                    showCountryPicker = false
                },
                onDismiss = { showCountryPicker = false },
            )
        }
    }
}

@Composable
private fun YearSlider(
    state: PopulationUiState.Content,
    accent: Color,
    onIntent: (PopulationIntent) -> Unit,
) {
    val years = state.availableYears
    val minYear = years.first()
    val maxYear = years.last()
    val span = (maxYear - minYear).coerceAtLeast(1)
    val current = state.selectedYear.coerceIn(minYear, maxYear)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Euro.spacing.s),
    ) {
        Text(
            text = "year",
            style = Euro.typography.bodySmall,
            color = Euro.colors.muted,
        )
        Slider(
            value = current.toFloat(),
            onValueChange = { onIntent(PopulationIntent.SelectYear(it.toInt())) },
            valueRange = minYear.toFloat()..maxYear.toFloat(),
            steps = (span - 1).coerceAtLeast(0).coerceAtMost(span + SLIDER_STEP_PADDING),
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Euro.colors.mutedAlt,
            ),
        )
        Text(
            text = current.toString(),
            style = Euro.typography.tabularNumSmall,
            color = Euro.colors.ink,
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

/** Fallback when no snapshot: read the headline from the time series. */
private fun fallbackTotal(state: PopulationUiState.Content): Long {
    val series = state.timeSeries.firstOrNull { it.countryCode == state.selectedCountry }
    val point = series?.points?.firstOrNull { it.year == state.selectedYear }
        ?: series?.points?.lastOrNull()
    return point?.totalPopulation ?: 0L
}

/**
 * Year-over-year percent change for the selected (country, year), or null
 * when not computable. Uses an ASCII `+`/`-` sign (not the Unicode minus
 * used elsewhere) to match this screen's existing subtitle typography;
 * magnitude rounding delegates to the shared [formatDecimal].
 */
private fun computeYoY(
    state: PopulationUiState.Content,
    snapshot: PopulationSnapshot?,
): String? {
    val series = state.timeSeries.firstOrNull { it.countryCode == state.selectedCountry } ?: return null
    val current = snapshot?.total
        ?: series.points.firstOrNull { it.year == state.selectedYear }?.totalPopulation
        ?: return null
    val previous = series.points.firstOrNull { it.year == state.selectedYear - 1 }?.totalPopulation
        ?: return null
    if (previous == 0L) return null
    val pct = (current - previous).toDouble() / previous.toDouble() * 100.0
    val sign = if (pct >= 0) "+" else "-"
    return "${sign}${formatDecimal(abs(pct), 1)}% YoY"
}

/**
 * Splits a population count into a (value, unit) pair for [MetricHeadline].
 * 83_200_000 -> "83.2" + "M". 1_500_000_000 -> "1.5" + "B". Delegates to the
 * shared [formatLargeNumberParts], preserving this screen's "non-positive
 * total = no data" convention.
 */
private fun headlineParts(value: Long): Pair<String, String> {
    if (value <= 0L) return "—" to ""
    return formatLargeNumberParts(value)
}
