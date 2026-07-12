package eu.eurostat.feature.social.ui

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.common.prefs.AppPreferences
import eu.eurostat.feature.social.domain.GetSocialTimeSeriesUseCase
import eu.eurostat.feature.social.domain.SocialQuery
import eu.eurostat.feature.social.domain.SocialTimeSeries
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

interface SocialComponent {
    val state: StateFlow<SocialUiState>
    fun onIntent(intent: SocialIntent)
}

class DefaultSocialComponent(
    componentContext: ComponentContext,
    private val useCase: GetSocialTimeSeriesUseCase,
    private val dispatchers: DispatcherProvider,
    private val appPreferences: AppPreferences,
) : SocialComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(SupervisorJob() + dispatchers.main)
    private val _state = MutableStateFlow<SocialUiState>(SocialUiState.Loading)
    override val state: StateFlow<SocialUiState> = _state.asStateFlow()
    private var currentQuery = SocialQuery(
        countryCodes = listOf("EU27_2020", "DE", "FR", "PL"),
        yearRange = 2010..2024,
    )

    /** The country code currently highlighted; defaults to the first in the query list. */
    private var activeCountry: String = currentQuery.countryCodes.firstOrNull() ?: "EU27_2020"

    /** The KPI tile currently selected; persists across data reloads. */
    private var selectedKpiKey: SocialKpiKey = SocialKpiKey.POVERTY

    /** Year selected via YearDropdown; null means "use latest available". */
    private var selectedYear: Int? = null

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
        activeCountry = preferred
        if (preferred !in currentQuery.countryCodes) {
            val codes = currentQuery.countryCodes.toMutableList()
            codes.add(if (codes.firstOrNull() == "EU27_2020") 1 else 0, preferred)
            currentQuery = currentQuery.copy(countryCodes = codes)
        }
    }

    override fun onIntent(intent: SocialIntent) {
        when (intent) {
            is SocialIntent.SelectCountries -> {
                currentQuery = currentQuery.copy(countryCodes = intent.codes)
                // Keep activeCountry valid after country list changes.
                if (activeCountry !in intent.codes) {
                    activeCountry = intent.codes.firstOrNull() ?: activeCountry
                }
                load()
            }
            is SocialIntent.SelectActiveCountry -> {
                activeCountry = intent.code
                selectedYear = null
                // Re-emit current state with updated activeCountry without re-fetching.
                val current = _state.value
                if (current is SocialUiState.Content) {
                    val availableYears = current.series
                        .firstOrNull { it.countryCode == activeCountry }
                        ?.points?.map { it.year }?.sorted() ?: emptyList()
                    val resolvedYear = availableYears.maxOrNull() ?: current.selectedYear
                    _state.value = current.copy(
                        activeCountry = activeCountry,
                        availableYears = availableYears,
                        selectedYear = resolvedYear,
                    )
                }
            }
            is SocialIntent.ChangeYearRange -> {
                currentQuery = currentQuery.copy(yearRange = intent.range)
                load()
            }
            is SocialIntent.SelectKpiTile -> {
                selectedKpiKey = intent.key
                // Re-emit current Content state with updated tile selection without re-fetching.
                val current = _state.value
                if (current is SocialUiState.Content) {
                    _state.value = current.copy(selectedKpiKey = selectedKpiKey)
                }
            }
            is SocialIntent.SelectYear -> {
                val current = _state.value
                if (current is SocialUiState.Content) {
                    val clamped = intent.year.coerceIn(
                        current.availableYears.minOrNull() ?: intent.year,
                        current.availableYears.maxOrNull() ?: intent.year,
                    )
                    selectedYear = clamped
                    _state.value = current.copy(selectedYear = clamped)
                }
            }
            SocialIntent.Refresh -> {
                scope.launch {
                    runCatching { useCase.refresh(currentQuery) }
                    load()
                }
            }
            SocialIntent.Retry -> load()
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

    private fun Result<List<SocialTimeSeries>>.toUiState(query: SocialQuery): SocialUiState =
        when (this) {
            is Result.Loading -> SocialUiState.Loading
            is Result.Success -> if (data.isEmpty()) {
                SocialUiState.Empty(query)
            } else {
                val available = data.map { it.countryCode }
                // Ensure activeCountry is valid; prefer first non-EU aggregate.
                if (activeCountry !in available) {
                    activeCountry = available.firstOrNull { it != "EU27_2020" }
                        ?: available.firstOrNull()
                        ?: activeCountry
                }
                // Derive display year range from live data so the scrubber
                // re-anchors when the dataset bounds change.
                val points = data.flatMap { it.points }
                val dataMin = points.minOfOrNull { it.year } ?: query.yearRange.first
                val dataMax = points.maxOfOrNull { it.year } ?: query.yearRange.last
                val availableYears = data
                    .firstOrNull { it.countryCode == activeCountry }
                    ?.points?.map { it.year }?.sorted() ?: emptyList()
                val resolvedYear = availableYears.let { years ->
                    val target = selectedYear
                    if (target != null && target in years) target else years.maxOrNull() ?: dataMax
                }
                SocialUiState.Content(
                    series = data,
                    isStale = isStale,
                    query = query,
                    activeCountry = activeCountry,
                    availableCountries = available,
                    selectedKpiKey = selectedKpiKey,
                    displayYearRange = dataMin..dataMax,
                    selectedYear = resolvedYear,
                    availableYears = availableYears,
                )
            }
            is Result.Error -> SocialUiState.Error(
                error = cause,
                canRetry = true,
            )
        }
}
