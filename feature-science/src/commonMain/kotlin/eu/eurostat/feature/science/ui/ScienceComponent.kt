package eu.eurostat.feature.science.ui

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.common.toUserMessage
import eu.eurostat.feature.science.domain.GetScienceTimeSeriesUseCase
import eu.eurostat.feature.science.domain.ScienceQuery
import eu.eurostat.feature.science.domain.ScienceTimeSeries
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface ScienceComponent {
    val state: StateFlow<ScienceUiState>
    fun onIntent(intent: ScienceIntent)
}

class DefaultScienceComponent(
    componentContext: ComponentContext,
    private val useCase: GetScienceTimeSeriesUseCase,
    private val dispatchers: DispatcherProvider,
) : ScienceComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(SupervisorJob() + dispatchers.main)
    private val _state = MutableStateFlow<ScienceUiState>(ScienceUiState.Loading)
    override val state: StateFlow<ScienceUiState> = _state.asStateFlow()
    private var collectJob: Job? = null
    private var currentQuery = ScienceQuery(
        countryCodes = listOf("EU27_2020", "DE", "FR", "PL"),
        yearRange = 2010..2024,
    )

    /** Persisted across re-fetches so chip selection survives data refresh. */
    private var activeCountry: String = currentQuery.countryCodes.firstOrNull() ?: DEFAULT_COUNTRY

    /**
     * User-selected year for the headline and radar. Null means "resolve to the
     * latest available year for the active country" on the next [toUiState] call.
     * Reset to null whenever [activeCountry] changes so the default (latest) re-applies.
     */
    private var selectedYear: Int? = null

    init { load() }

    override fun onIntent(intent: ScienceIntent) {
        when (intent) {
            is ScienceIntent.SelectCountries -> {
                // Guard: never allow an empty country list; keep at least one country.
                val codes = intent.codes.ifEmpty { listOf(DEFAULT_COUNTRY) }
                currentQuery = currentQuery.copy(countryCodes = codes)
                // Keep activeCountry valid; fall back to canonical default.
                if (activeCountry !in codes) {
                    activeCountry = codes.firstOrNull() ?: DEFAULT_COUNTRY
                }
                load()
            }
            is ScienceIntent.SelectActiveCountry -> {
                activeCountry = intent.code
                // Reset year selection so the new country defaults to its latest year.
                selectedYear = null
                // Re-publish state so the UI picks up the new active country
                // without a network round-trip.
                val current = _state.value
                if (current is ScienceUiState.Content) {
                    _state.value = current.copy(
                        activeCountry = intent.code,
                        selectedYear = resolveYear(null, current.series, intent.code),
                        availableYears = availableYearsFor(current.series, intent.code),
                    )
                }
            }
            is ScienceIntent.SelectYear -> {
                // Reproject state without a network call; guard that year is in the list.
                val current = _state.value
                if (current is ScienceUiState.Content) {
                    val clamped = intent.year.coerceIn(
                        current.availableYears.minOrNull() ?: intent.year,
                        current.availableYears.maxOrNull() ?: intent.year,
                    )
                    selectedYear = clamped
                    _state.value = current.copy(selectedYear = clamped)
                }
            }
            is ScienceIntent.ChangeYearRange -> {
                currentQuery = currentQuery.copy(yearRange = intent.range)
                load()
            }
            ScienceIntent.Refresh -> {
                scope.launch {
                    runCatching { useCase.refresh(currentQuery) }
                    load()
                }
            }
            ScienceIntent.Retry -> load()
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

    private companion object {
        /** Canonical fallback country used when no country is explicitly active. */
        const val DEFAULT_COUNTRY = "EU27_2020"
    }

    /** Returns years with observations for [country] across all series, sorted ascending. */
    private fun availableYearsFor(series: List<ScienceTimeSeries>, country: String): List<Int> =
        series.firstOrNull { it.countryCode == country }
            ?.points
            ?.map { it.year }
            ?.distinct()
            ?.sorted()
            ?: emptyList()

    /**
     * Resolves the effective selected year. If [preferred] is in the country's available years,
     * keep it; otherwise default to the maximum (latest) year. Returns null when no data.
     */
    private fun resolveYear(preferred: Int?, series: List<ScienceTimeSeries>, country: String): Int? {
        val years = availableYearsFor(series, country)
        if (years.isEmpty()) return null
        return if (preferred != null && preferred in years) preferred else years.max()
    }

    private fun Result<List<ScienceTimeSeries>>.toUiState(query: ScienceQuery): ScienceUiState =
        when (this) {
            is Result.Loading -> ScienceUiState.Loading
            is Result.Success -> if (data.isEmpty()) {
                ScienceUiState.Empty(query)
            } else {
                val countries = data.map { it.countryCode }
                // Ensure activeCountry remains valid; prefer first non-EU aggregate.
                if (activeCountry !in countries) {
                    activeCountry = countries.firstOrNull { it != "EU27_2020" }
                        ?: countries.firstOrNull()
                        ?: DEFAULT_COUNTRY
                }
                val years = availableYearsFor(data, activeCountry)
                val year = resolveYear(selectedYear, data, activeCountry)
                // Persist so subsequent SelectYear intents remain coherent.
                selectedYear = year
                ScienceUiState.Content(
                    series = data,
                    isStale = isStale,
                    query = query,
                    activeCountry = activeCountry,
                    availableCountries = countries,
                    selectedYear = year,
                    availableYears = years,
                )
            }
            is Result.Error -> ScienceUiState.Error(
                message = cause.toUserMessage(),
                canRetry = true,
            )
        }
}
