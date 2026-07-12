package eu.eurostat.feature.environment.ui

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.common.prefs.AppPreferences
import eu.eurostat.feature.environment.domain.EnvMetric
import eu.eurostat.feature.environment.domain.EnvSector
import eu.eurostat.feature.environment.domain.EnvironmentQuery
import eu.eurostat.feature.environment.domain.EnvironmentTimeSeries
import eu.eurostat.feature.environment.domain.GetEnvironmentTimeSeriesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Decompose component for the Environment screen. */
interface EnvironmentComponent {
    val state: StateFlow<EnvironmentUiState>
    fun onIntent(intent: EnvironmentIntent)
}

/**
 * Default implementation. Holds the current [EnvironmentQuery] and the
 * actively-highlighted country code, maps repository [Result] emissions to
 * [EnvironmentUiState], and exposes Refresh/Retry/SelectActiveCountry/SelectCountries.
 */
class DefaultEnvironmentComponent(
    componentContext: ComponentContext,
    private val useCase: GetEnvironmentTimeSeriesUseCase,
    private val dispatchers: DispatcherProvider,
    private val appPreferences: AppPreferences,
) : EnvironmentComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(SupervisorJob() + dispatchers.main)
    private val _state = MutableStateFlow<EnvironmentUiState>(EnvironmentUiState.Loading)
    override val state: StateFlow<EnvironmentUiState> = _state.asStateFlow()
    private var collectJob: Job? = null

    private var currentQuery = EnvironmentQuery(
        countryCodes = listOf("EU27_2020", "DE", "FR", "PL"),
        yearRange = 2010..2024,
    )

    /** Highlighted country — updated without triggering a network reload. */
    private var activeCountry: String = defaultActiveCountry(currentQuery.countryCodes)

    /** Sector selection, persisted in UiState.Content. */
    private var activeSector: EnvSector = EnvSector.Total

    /** Metric selection, persisted in UiState.Content. */
    private var activeMetric: EnvMetric = EnvMetric.Ghg

    /** Headline year selection — null means "latest available". */
    private var activeYear: Int? = null

    init {
        scope.launch {
            applyDefaultCountryPreference()
            load()
        }
    }

    /**
     * Seeds the active country and initial query from the persisted
     * default-country preference ([AppPreferences.defaultCountry]).
     *
     * Read exactly once, before the first load, so the very first query already
     * targets the preferred country (no double fetch, no flash of the wrong
     * country). Changing the preference mid-session therefore takes effect on
     * the next app start; live re-querying is intentionally out of scope.
     * When the preference is unset (the [AppPreferences.DEFAULT_COUNTRY] EU
     * aggregate), the historical defaults are kept unchanged.
     */
    private suspend fun applyDefaultCountryPreference() {
        val preferred = appPreferences.defaultCountry.first()
        if (preferred.isBlank() || preferred == AppPreferences.DEFAULT_COUNTRY) return
        activeCountry = preferred
        if (preferred !in currentQuery.countryCodes) {
            val codes = currentQuery.countryCodes.toMutableList()
            codes.add(if (codes.firstOrNull() == "EU27_2020") 1 else 0, preferred)
            currentQuery = currentQuery.copy(countryCodes = codes)
        }
    }

    override fun onIntent(intent: EnvironmentIntent) {
        when (intent) {
            is EnvironmentIntent.SelectActiveCountry -> {
                activeCountry = intent.code
                // No network reload needed — just reproject current data into state.
                // If state is currently Loading, the intent is applied immediately to
                // the component field; it will be picked up when data arrives.
                reprojectContent()
            }
            is EnvironmentIntent.SelectSector -> {
                activeSector = intent.sector
                reprojectContent()
            }
            is EnvironmentIntent.SelectMetric -> {
                activeMetric = intent.metric
                reprojectContent()
            }
            is EnvironmentIntent.SelectYear -> {
                activeYear = intent.year
                reprojectContent()
            }
            is EnvironmentIntent.SelectCountries -> {
                currentQuery = currentQuery.copy(countryCodes = intent.codes)
                activeCountry = defaultActiveCountry(intent.codes)
                load()
            }
            is EnvironmentIntent.ChangeYearRange -> {
                currentQuery = currentQuery.copy(yearRange = intent.range)
                load()
            }
            EnvironmentIntent.Refresh -> {
                scope.launch {
                    runCatching { useCase.refresh(currentQuery) }
                    load()
                }
            }
            EnvironmentIntent.Retry -> load()
        }
    }

    private fun load() {
        collectJob?.cancel()
        collectJob = scope.launch {
            useCase.observe(currentQuery).collect { result ->
                _state.value = result.toUiState(currentQuery, activeCountry, activeSector, activeMetric, activeYear)
                // Keep activeCountry and activeYear in sync when data arrives.
                if (result is Result.Success && result.data.isNotEmpty()) {
                    val codes = result.data.map { it.countryCode }
                    if (activeCountry !in codes) activeCountry = defaultActiveCountry(codes)
                    val resolved = result.data.firstOrNull { it.countryCode == activeCountry }
                    val availableYears = resolved?.points?.map { it.year }?.distinct()?.sorted() ?: emptyList()
                    if (availableYears.isNotEmpty() && activeYear != null && activeYear !in availableYears) {
                        activeYear = null
                    }
                }
            }
        }
    }

    /**
     * Re-emit the current Content state with the latest [activeCountry],
     * [activeSector], and [activeMetric] without reloading data.
     * If the current state is Loading or Error, the fields are stored in the
     * component and will be applied when content arrives via [load].
     */
    private fun reprojectContent() {
        val current = _state.value
        if (current is EnvironmentUiState.Content) {
            // availableYears must be recomputed against the (possibly new) activeCountry,
            // not read from current.availableYears which reflects the previous country.
            val availableYears = current.timeSeries
                .firstOrNull { it.countryCode == activeCountry }
                ?.points?.map { it.year }?.distinct()?.sorted()
                .orEmpty()
            val resolvedYear = if (availableYears.isEmpty()) {
                current.selectedYear
            } else {
                activeYear?.coerceIn(availableYears.first(), availableYears.last())
                    ?: availableYears.last()
            }
            _state.value = current.copy(
                activeCountry = activeCountry,
                activeSector = activeSector,
                activeMetric = activeMetric,
                selectedYear = resolvedYear,
                availableYears = availableYears,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Choose the default highlighted country from a list: prefer "DE" if present,
 * else the first non-aggregate, else the first available.
 */
private fun defaultActiveCountry(codes: List<String>): String =
    codes.firstOrNull { it == "DE" }
        ?: codes.firstOrNull { it != "EU27_2020" }
        ?: codes.firstOrNull().orEmpty()

private fun Result<List<EnvironmentTimeSeries>>.toUiState(
    query: EnvironmentQuery,
    activeCountry: String,
    activeSector: EnvSector,
    activeMetric: EnvMetric,
    activeYear: Int?,
): EnvironmentUiState = when (this) {
    is Result.Loading -> EnvironmentUiState.Loading
    is Result.Success -> if (data.isEmpty()) {
        EnvironmentUiState.Empty(query)
    } else {
        val countries = data.map { it.countryCode }
        val resolvedActive = if (activeCountry in countries) activeCountry
        else defaultActiveCountry(countries)
        val activeSeries = data.firstOrNull { it.countryCode == resolvedActive }
        val availableYears = activeSeries?.points
            ?.map { it.year }
            ?.distinct()
            ?.sorted()
            ?: emptyList()
        val resolvedYear = if (availableYears.isEmpty()) {
            0
        } else {
            activeYear?.coerceIn(availableYears.first(), availableYears.last())
                ?: availableYears.last()
        }
        EnvironmentUiState.Content(
            timeSeries = data,
            isStale = isStale,
            query = query,
            activeCountry = resolvedActive,
            availableCountries = countries,
            activeSector = activeSector,
            activeMetric = activeMetric,
            selectedYear = resolvedYear,
            availableYears = availableYears,
        )
    }
    is Result.Error -> EnvironmentUiState.Error(
        error = cause,
        canRetry = true,
    )
}
