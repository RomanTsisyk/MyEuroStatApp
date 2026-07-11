package eu.eurostat.feature.transport.ui

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.common.prefs.AppPreferences
import eu.eurostat.feature.transport.domain.GetTransportTimeSeriesUseCase
import eu.eurostat.feature.transport.domain.TransportMode
import eu.eurostat.feature.transport.domain.TransportQuery
import eu.eurostat.feature.transport.domain.TransportTimeSeries
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

interface TransportComponent {
    val state: StateFlow<TransportUiState>
    fun onIntent(intent: TransportIntent)
}

class DefaultTransportComponent(
    componentContext: ComponentContext,
    private val useCase: GetTransportTimeSeriesUseCase,
    private val dispatchers: DispatcherProvider,
    private val appPreferences: AppPreferences,
) : TransportComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(SupervisorJob() + dispatchers.main)
    private val _state = MutableStateFlow<TransportUiState>(TransportUiState.Loading)
    override val state: StateFlow<TransportUiState> = _state.asStateFlow()

    private var currentQuery = TransportQuery(
        countryCodes = listOf("EU27_2020", "DE", "FR", "PL"),
        yearRange = 2010..2024,
        mode = TransportMode.ALL,
    )

    /** UI-only selection — survives across emissions on the same query. */
    private var selectedCountry: String = "DE"
    private var displayPanelMode: TransportPanelMode = TransportPanelMode.ALL
    private var logScale: Boolean = false
    /** Null means "use latest available year for active country". */
    private var selectedYear: Int? = null

    private var lastSeries: List<TransportTimeSeries>? = null
    private var lastStale: Boolean = false

    private var collectJob: Job? = null

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
        selectedCountry = preferred
        if (preferred !in currentQuery.countryCodes) {
            val codes = currentQuery.countryCodes.toMutableList()
            codes.add(if (codes.firstOrNull() == "EU27_2020") 1 else 0, preferred)
            currentQuery = currentQuery.copy(countryCodes = codes)
        }
    }

    override fun onIntent(intent: TransportIntent) {
        when (intent) {
            is TransportIntent.SelectActiveCountry -> {
                selectedCountry = intent.code
                selectedYear = null
                rerenderFromLast()
            }
            is TransportIntent.SelectCountries -> {
                currentQuery = currentQuery.copy(countryCodes = intent.codes)
                load()
            }
            is TransportIntent.ChangeYearRange -> {
                currentQuery = currentQuery.copy(yearRange = intent.range)
                load()
            }
            is TransportIntent.ChangeMode -> {
                currentQuery = currentQuery.copy(mode = intent.mode)
                load()
            }
            is TransportIntent.SelectPanelMode -> {
                displayPanelMode = intent.mode
                rerenderFromLast()
            }
            TransportIntent.ToggleLogScale -> {
                logScale = !logScale
                rerenderFromLast()
            }
            is TransportIntent.SelectYear -> {
                selectedYear = intent.year
                rerenderFromLast()
            }
            TransportIntent.Refresh -> {
                scope.launch { runCatching { useCase.refresh(currentQuery) }; load() }
            }
            TransportIntent.Retry -> load()
        }
    }

    private fun load() {
        collectJob?.cancel()
        collectJob = scope.launch {
            useCase.observe(currentQuery).collect { result ->
                _state.value = result.toUiState(currentQuery)
            }
        }
    }

    private fun rerenderFromLast() {
        val series = lastSeries ?: return
        _state.value = buildContent(series, lastStale, currentQuery)
    }

    private fun Result<List<TransportTimeSeries>>.toUiState(query: TransportQuery): TransportUiState =
        when (this) {
            is Result.Loading -> TransportUiState.Loading
            is Result.Success -> if (data.isEmpty()) {
                TransportUiState.Empty(query)
            } else {
                lastSeries = data
                lastStale = isStale
                buildContent(data, isStale, query)
            }
            is Result.Error -> TransportUiState.Error(
                error = cause,
                canRetry = true,
            )
        }

    private fun buildContent(
        series: List<TransportTimeSeries>,
        isStale: Boolean,
        query: TransportQuery,
    ): TransportUiState.Content {
        val available = series.map { it.countryCode }.distinct()
        val active = when {
            selectedCountry in available -> selectedCountry
            else -> available.firstOrNull { it != "EU27_2020" }
                ?: available.firstOrNull()
                ?: selectedCountry
        }
        selectedCountry = active

        val activeSeries = series.filter { it.countryCode == active }
        val availableYears = activeSeries
            .flatMap { it.points }
            .filter { it.roadPassengers != null || it.airPassengers != null }
            .map { it.year }
            .distinct()
            .sorted()

        val resolvedYear = when {
            availableYears.isEmpty() -> query.yearRange.last
            selectedYear != null && selectedYear!! in availableYears -> selectedYear!!
            else -> availableYears.last()
        }
        selectedYear = resolvedYear

        return TransportUiState.Content(
            series = series,
            isStale = isStale,
            query = query,
            activeCountry = active,
            availableCountries = available,
            displayPanelMode = displayPanelMode,
            logScale = logScale,
            selectedYear = resolvedYear,
            availableYears = availableYears,
        )
    }
}
