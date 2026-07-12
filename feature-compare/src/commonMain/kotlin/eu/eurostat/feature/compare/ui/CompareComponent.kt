package eu.eurostat.feature.compare.ui

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.common.prefs.AppPreferences
import eu.eurostat.feature.compare.data.CompareDataSource
import eu.eurostat.feature.compare.domain.CompareIndicator
import eu.eurostat.feature.compare.domain.CompareSeries
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Component for the cross-module Compare screen: overlays one headline
 * indicator across 2–5 countries on a single line chart.
 *
 * Exposes an immutable [state] stream and accepts user actions through
 * [onIntent]. The indicator and country selection are owned here (so they
 * survive recomposition and configuration changes); the actual data comes from
 * [CompareDataSource], which fans the choice out to the right feature
 * repository.
 */
interface CompareComponent {

    /** The current screen state. */
    val state: StateFlow<CompareUiState>

    /** Handles a user action (indicator/country change, normalization toggle, refresh). */
    fun onIntent(intent: CompareIntent)
}

/**
 * Default [CompareComponent].
 *
 * The initial country selection is seeded from the persisted default-country
 * preference (read exactly once, before the first fetch — mirroring
 * `DefaultOverviewComponent`) plus two fixed defaults, so the very first query
 * already targets the user's preferred country. The initial indicator is
 * [CompareIndicator.DEFAULT] (GDP).
 *
 * Changing the indicator or country set cancels the in-flight collection and
 * re-observes; toggling normalization only flips a flag on the current
 * [CompareUiState.Content] without re-fetching.
 */
class DefaultCompareComponent(
    componentContext: ComponentContext,
    private val dataSource: CompareDataSource,
    private val dispatchers: DispatcherProvider,
    private val appPreferences: AppPreferences,
) : CompareComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(SupervisorJob() + dispatchers.main)

    private val _state = MutableStateFlow<CompareUiState>(CompareUiState.Loading(CompareIndicator.DEFAULT))
    override val state: StateFlow<CompareUiState> = _state.asStateFlow()

    private var indicator: CompareIndicator = CompareIndicator.DEFAULT
    private var countries: List<String> = DEFAULT_COUNTRIES
    private var normalized: Boolean = false
    private val years: IntRange = 2010..2024

    private var collectJob: Job? = null

    init {
        scope.launch {
            applyDefaultCountryPreference()
            start()
        }
    }

    /**
     * Seeds [countries] with the persisted default country ([AppPreferences.defaultCountry])
     * as the first entry, followed by the fixed defaults, de-duplicated and
     * clamped to [MAX_COUNTRIES].
     *
     * Read once, before the first load, so the first queries already target the
     * preferred country. When the preference is unset (the EU aggregate) the
     * fixed defaults are kept unchanged. Changing the preference mid-session
     * therefore only takes effect on the next launch — matching the other
     * components.
     */
    private suspend fun applyDefaultCountryPreference() {
        val preferred = appPreferences.defaultCountry.first()
        if (preferred.isBlank() || preferred == AppPreferences.DEFAULT_COUNTRY) return
        countries = (listOf(preferred) + DEFAULT_COUNTRIES).distinct().take(MAX_COUNTRIES)
    }

    override fun onIntent(intent: CompareIntent) {
        when (intent) {
            is CompareIntent.SelectIndicator -> {
                if (intent.indicator == indicator) return
                indicator = intent.indicator
                start()
            }
            is CompareIntent.SelectCountries -> {
                val next = normalizeSelection(intent.codes)
                if (next.size < MIN_COUNTRIES || next == countries) return
                countries = next
                start()
            }
            CompareIntent.ToggleNormalization -> toggleNormalization()
            CompareIntent.Refresh -> refresh()
        }
    }

    /** Flips the normalization flag; updates the current Content in place, no re-fetch. */
    private fun toggleNormalization() {
        normalized = !normalized
        val current = _state.value
        if (current is CompareUiState.Content) {
            _state.value = current.copy(normalized = normalized)
        }
    }

    private fun start() {
        collectJob?.cancel()
        _state.value = CompareUiState.Loading(indicator)
        collectJob = scope.launch {
            observeCurrent()
        }
    }

    /**
     * Forces a network refetch, then re-observes. A bare re-subscription is not
     * enough: the repositories are stale-while-revalidate, so a within-TTL cache
     * is re-served without touching the network. A failed refresh is swallowed —
     * re-observing surfaces either the cache or the repository's own error.
     */
    private fun refresh() {
        collectJob?.cancel()
        _state.value = CompareUiState.Loading(indicator)
        collectJob = scope.launch {
            runCatching { dataSource.refresh(indicator, countries, years) }
            observeCurrent()
        }
    }

    /** Collects the current selection's stream into [state] until cancelled. */
    private suspend fun observeCurrent() {
        dataSource.observe(indicator, countries, years).collect { result ->
            _state.value = result.toUiState()
        }
    }

    /** Reduces one repository [Result] into a [CompareUiState] for the current selection. */
    private fun Result<List<CompareSeries>>.toUiState(): CompareUiState = when (this) {
        is Result.Loading -> CompareUiState.Loading(indicator)
        is Result.Error -> CompareUiState.Error(cause, canRetry = true)
        is Result.Success -> if (data.isEffectivelyEmpty()) {
            CompareUiState.Empty(indicator, countries)
        } else {
            CompareUiState.Content(
                indicator = indicator,
                countries = countries,
                yearRange = data.yearRange(),
                normalized = normalized,
                series = data,
                isStale = isStale,
            )
        }
    }

    /**
     * Normalizes a chip/picker selection: de-duplicated and clamped to
     * [MAX_COUNTRIES], but — deliberately — kept in the order the user built it.
     * `SeriesPalette` assigns colours by list position, so preserving selection
     * order keeps every untouched country's colour stable while others are added
     * or removed (the palette contract: colour by stable index, never by code).
     */
    private fun normalizeSelection(codes: List<String>): List<String> =
        codes.distinct().take(MAX_COUNTRIES)

    companion object {
        /** Fixed fallback selection when no default-country preference is set. */
        val DEFAULT_COUNTRIES: List<String> = listOf("DE", "FR", "PL")

        /** Minimum countries for a meaningful comparison; smaller selections are ignored. */
        const val MIN_COUNTRIES: Int = 2

        /** Maximum number of countries the chart overlays (matches the picker limit). */
        const val MAX_COUNTRIES: Int = 5
    }
}

/** True when nothing chartable is present: no series, or every series is all-gaps. */
private fun List<CompareSeries>.isEffectivelyEmpty(): Boolean =
    isEmpty() || all { series -> series.points.all { it.value == null } }

/** The min..max span of all observation years, or a single-year fallback when empty. */
private fun List<CompareSeries>.yearRange(): IntRange {
    val allYears = flatMap { it.points }.map { it.year }
    return if (allYears.isEmpty()) {
        val y = 2024
        y..y
    } else {
        allYears.min()..allYears.max()
    }
}
