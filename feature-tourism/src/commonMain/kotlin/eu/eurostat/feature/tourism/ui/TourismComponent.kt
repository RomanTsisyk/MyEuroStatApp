package eu.eurostat.feature.tourism.ui

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.common.prefs.AppPreferences
import eu.eurostat.feature.tourism.domain.GetTourismTimeSeriesUseCase
import eu.eurostat.feature.tourism.domain.TourismData
import eu.eurostat.feature.tourism.domain.TourismQuery
import eu.eurostat.feature.tourism.domain.TourismResidence
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Decompose component for the tourism screen. Owns the active query
 * (countries, year range), the user-selected active country, and the
 * currently highlighted residence chip. Renders [TourismUiState] into
 * [state] and accepts intents via [onIntent].
 */
interface TourismComponent {
    val state: StateFlow<TourismUiState>
    fun onIntent(intent: TourismIntent)
}

class DefaultTourismComponent(
    componentContext: ComponentContext,
    private val useCase: GetTourismTimeSeriesUseCase,
    private val dispatchers: DispatcherProvider,
    private val appPreferences: AppPreferences,
) : TourismComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(SupervisorJob() + dispatchers.main)
    private val _state = MutableStateFlow<TourismUiState>(TourismUiState.Loading)
    override val state: StateFlow<TourismUiState> = _state.asStateFlow()

    private var currentQuery = TourismQuery(
        countryCodes = listOf("EU27_2020", "DE", "FR", "PL", "IT", "ES"),
        yearRange = 2010..2024,
    )
    private var activeCountry: String = "DE"
    private var highlightedResidence: TourismResidence = TourismResidence.Foreign

    /** null means "auto-resolve to latest"; set explicitly when the user picks a year. */
    private var selectedYear: Int? = null

    /** True when the last manual refresh failed; reset by every other [load]. */
    private var refreshFailed: Boolean = false

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

    override fun onIntent(intent: TourismIntent) {
        when (intent) {
            is TourismIntent.SelectCountries -> {
                currentQuery = currentQuery.copy(countryCodes = intent.codes)
                if (activeCountry !in intent.codes && intent.codes.isNotEmpty()) {
                    activeCountry = intent.codes.first()
                }
                load()
            }
            is TourismIntent.ChangeYearRange -> {
                currentQuery = currentQuery.copy(yearRange = intent.range)
                load()
            }
            is TourismIntent.SelectActiveCountry -> {
                activeCountry = intent.code
                // Reset year selection when the active country changes so the
                // latest year for the new country is auto-selected.
                selectedYear = null
                // No reload required — just rerender from existing data.
                _state.value = projectState(currentResult)
            }
            is TourismIntent.HighlightResidence -> {
                highlightedResidence = intent.residence
                _state.value = projectState(currentResult)
            }
            is TourismIntent.SelectYear -> {
                selectedYear = intent.year
                // No reload required — reproject without touching the network.
                _state.value = projectState(currentResult)
            }
            TourismIntent.Refresh -> {
                scope.launch {
                    val failed = try {
                        useCase.refresh(currentQuery)
                        false
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        true
                    }
                    load(refreshFailed = failed)
                }
            }
            TourismIntent.Retry -> load()
        }
    }

    private var currentResult: Result<TourismData> = Result.Loading
    private var collectJob: Job? = null

    /**
     * (Re)starts observing [currentQuery].
     *
     * @param refreshFailed true only when called right after a failed manual
     *   refresh; stored before the collector launches so the first emission
     *   already carries it. Every other caller keeps the default and thereby
     *   clears the hint.
     */
    private fun load(refreshFailed: Boolean = false) {
        this.refreshFailed = refreshFailed
        collectJob?.cancel()
        collectJob = scope.launch {
            useCase.observe(currentQuery).collect { result ->
                // A stale emission is already covered by the stale UI, and a later
                // successful revalidation must not leave the failure hint up. Done
                // here (not in projectState) so UI-only re-renders never clear it.
                // Qualified because the load() parameter shadows the property here.
                if (result is Result.Success && result.isStale) {
                    this@DefaultTourismComponent.refreshFailed = false
                }
                currentResult = result
                _state.value = projectState(result)
            }
        }
    }

    private fun projectState(result: Result<TourismData>): TourismUiState {
        return when (result) {
            is Result.Loading -> TourismUiState.Loading
            is Result.Success -> if (result.data.timeSeries.isEmpty()) {
                TourismUiState.Empty
            } else {
                val series = result.data.timeSeries
                // Ensure active country exists in the data; prefer first non-EU aggregate.
                // series is non-empty at this point, so firstOrNull() is always non-null.
                if (series.none { it.countryCode == activeCountry }) {
                    activeCountry = series.firstOrNull { it.countryCode != "EU27_2020" }?.countryCode
                        ?: series.firstOrNull()?.countryCode
                        ?: return TourismUiState.Empty
                }

                // Compute years where any residence has data for the active country.
                val activePoints = series.firstOrNull { it.countryCode == activeCountry }?.points
                    ?: emptyList()
                val availableYears = activePoints
                    .filter { p ->
                        p.domesticNights != null || p.foreignNights != null || p.totalNights != null
                    }
                    .map { it.year }
                    .sorted()

                // Resolve the displayed year: honour user selection if valid, else latest.
                val resolvedYear = if (availableYears.isEmpty()) {
                    selectedYear ?: 0
                } else {
                    val preferred = selectedYear
                    if (preferred != null && preferred in availableYears) {
                        preferred
                    } else {
                        availableYears.last()
                    }
                }

                TourismUiState.Content(
                    timeSeries = series,
                    isStale = result.isStale,
                    activeCountry = activeCountry,
                    highlightedResidence = highlightedResidence,
                    heatmapCells = result.data.heatmapCells,
                    selectedYear = resolvedYear,
                    availableYears = availableYears,
                    refreshFailed = refreshFailed,
                )
            }
            is Result.Error -> TourismUiState.Error(
                error = result.cause,
                canRetry = true,
            )
        }
    }
}
