package eu.eurostat.feature.trade.ui

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.common.prefs.AppPreferences
import eu.eurostat.core.common.toUserMessage
import eu.eurostat.feature.trade.domain.GetTradeTimeSeriesUseCase
import eu.eurostat.feature.trade.domain.TradeQuery
import eu.eurostat.feature.trade.domain.TradeTimeSeries
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

interface TradeComponent {
    val state: StateFlow<TradeUiState>
    fun onIntent(intent: TradeIntent)
}

class DefaultTradeComponent(
    componentContext: ComponentContext,
    private val useCase: GetTradeTimeSeriesUseCase,
    private val dispatchers: DispatcherProvider,
    private val appPreferences: AppPreferences,
) : TradeComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(SupervisorJob() + dispatchers.main)
    private val _state = MutableStateFlow<TradeUiState>(TradeUiState.Loading)
    override val state: StateFlow<TradeUiState> = _state.asStateFlow()

    private var currentQuery = TradeQuery(
        countryCodes = listOf("EU27_2020", "DE", "FR", "PL"),
        yearRange = 2010..2024,
        partner = "EU27_2020",
    )

    /** UI-only active-country selection — survives re-emissions on the same query. */
    private var selectedCountry: String = "DE"

    /** Persisted tab index for Exports/Imports/Balance — survives rotation via UiState. Default: Balance (2). */
    private var selectedTab: Int = 2

    /** UI-only year selection — null means "use latest year for active country". Reset to null when active country changes. */
    private var selectedYear: Int? = null

    /** Last successfully loaded series — used for re-rendering on active-country change. */
    private var lastSeries: List<TradeTimeSeries>? = null
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

    override fun onIntent(intent: TradeIntent) {
        when (intent) {
            is TradeIntent.SelectActiveCountry -> {
                selectedCountry = intent.code
                selectedYear = null
                rerenderFromLastData()
            }
            is TradeIntent.SelectTab -> {
                selectedTab = intent.index
                rerenderFromLastData()
            }
            is TradeIntent.SelectYear -> {
                selectedYear = intent.year
                rerenderFromLastData()
            }
            is TradeIntent.SelectCountries -> {
                currentQuery = currentQuery.copy(countryCodes = intent.codes)
                load()
            }
            is TradeIntent.ChangeYearRange -> {
                currentQuery = currentQuery.copy(yearRange = intent.range)
                load()
            }
            is TradeIntent.ChangePartner -> {
                currentQuery = currentQuery.copy(partner = intent.partner)
                load()
            }
            TradeIntent.Refresh -> {
                scope.launch { runCatching { useCase.refresh(currentQuery) }; load() }
            }
            TradeIntent.Retry -> load()
        }
    }

    private fun rerenderFromLastData() {
        val series = lastSeries ?: return
        _state.value = buildContent(series, lastStale, currentQuery)
    }

    private fun load() {
        collectJob?.cancel()
        collectJob = scope.launch {
            useCase.observe(currentQuery).collect { result ->
                _state.value = result.toUiState(currentQuery)
            }
        }
    }

    private fun Result<List<TradeTimeSeries>>.toUiState(query: TradeQuery): TradeUiState =
        when (this) {
            is Result.Loading -> TradeUiState.Loading
            is Result.Success -> {
                if (data.isEmpty()) {
                    TradeUiState.Empty(query)
                } else {
                    lastSeries = data
                    lastStale = isStale
                    buildContent(data, isStale, query)
                }
            }
            is Result.Error -> TradeUiState.Error(
                message = cause.toUserMessage(),
                canRetry = true,
            )
        }

    private fun buildContent(
        series: List<TradeTimeSeries>,
        isStale: Boolean,
        query: TradeQuery,
    ): TradeUiState.Content {
        val availableCountries = series.map { it.countryCode }
        val activeCountry = when {
            selectedCountry in availableCountries -> selectedCountry
            else -> availableCountries.firstOrNull { it != "EU27_2020" }
                ?: availableCountries.firstOrNull()
                ?: selectedCountry
        }
        selectedCountry = activeCountry

        val activeSeries = series.firstOrNull { it.countryCode == activeCountry }
        val availableYears = activeSeries?.points?.map { it.year }?.sorted() ?: emptyList()
        val resolvedYear = when {
            availableYears.isEmpty() -> query.yearRange.last
            selectedYear != null && selectedYear!! in availableYears -> selectedYear!!
            else -> availableYears.max()
        }.coerceIn(availableYears.minOrNull() ?: query.yearRange.first, availableYears.maxOrNull() ?: query.yearRange.last)

        return TradeUiState.Content(
            series = series,
            isStale = isStale,
            query = query,
            activeCountry = activeCountry,
            availableCountries = availableCountries,
            selectedTabIndex = selectedTab,
            selectedYear = resolvedYear,
            availableYears = availableYears,
        )
    }
}
