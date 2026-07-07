package eu.eurostat.feature.economy.ui

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.common.toUserMessage
import eu.eurostat.feature.economy.domain.EconomyMetric
import eu.eurostat.feature.economy.domain.EconomyQuery
import eu.eurostat.feature.economy.domain.EconomyTimeSeries
import eu.eurostat.feature.economy.domain.GetEconomyTimeSeriesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface EconomyComponent {
    val state: StateFlow<EconomyUiState>
    fun onIntent(intent: EconomyIntent)
}

class DefaultEconomyComponent(
    componentContext: ComponentContext,
    private val useCase: GetEconomyTimeSeriesUseCase,
    private val dispatchers: DispatcherProvider,
) : EconomyComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(SupervisorJob() + dispatchers.main)
    private val _state = MutableStateFlow<EconomyUiState>(EconomyUiState.Loading)
    override val state: StateFlow<EconomyUiState> = _state.asStateFlow()

    private var currentQuery = EconomyQuery(
        countryCodes = listOf("EU27_2020", "DE", "FR", "PL"),
        yearRange = 2010..2024,
    )

    /** UI-only selections — survive across emissions on the same query. */
    private var selectedCountry: String = "DE"
    private var selectedMetric: EconomyMetric = EconomyMetric.Gdp
    private var displayYearRange: IntRange? = null
    private var selectedYear: Int? = null
    private var normalized: Boolean = false

    private var collectJob: Job? = null

    init {
        load()
    }

    override fun onIntent(intent: EconomyIntent) {
        when (intent) {
            is EconomyIntent.SelectActiveCountry -> {
                selectedCountry = intent.code
                selectedYear = null
                rerenderFromLastData()
            }
            is EconomyIntent.SelectCountries -> {
                currentQuery = currentQuery.copy(countryCodes = intent.codes)
                load()
            }
            is EconomyIntent.ChangeYearRange -> {
                currentQuery = currentQuery.copy(yearRange = intent.range)
                load()
            }
            is EconomyIntent.ChangeUnit -> {
                currentQuery = currentQuery.copy(unit = intent.unit)
                load()
            }
            EconomyIntent.Refresh -> scope.launch {
                runCatching { useCase.refresh(currentQuery) }
                load()
            }
            EconomyIntent.Retry -> load()
            is EconomyIntent.SelectMetric -> {
                selectedMetric = intent.metric
                rerenderFromLastData()
            }
            is EconomyIntent.SetDisplayYearRange -> {
                displayYearRange = intent.range
                rerenderFromLastData()
            }
            is EconomyIntent.SelectYear -> {
                selectedYear = intent.year
                rerenderFromLastData()
            }
            is EconomyIntent.SetNormalized -> {
                normalized = intent.normalized
                rerenderFromLastData()
            }
        }
    }

    private var lastData: List<EconomyTimeSeries>? = null
    private var lastStale: Boolean = false

    private fun load() {
        collectJob?.cancel()
        collectJob = scope.launch {
            useCase.observe(currentQuery).collect { result ->
                _state.value = result.toUiState(currentQuery)
            }
        }
    }

    private fun rerenderFromLastData() {
        val data = lastData ?: return
        _state.value = buildContent(data, lastStale, currentQuery)
    }

    private fun Result<List<EconomyTimeSeries>>.toUiState(query: EconomyQuery): EconomyUiState =
        when (this) {
            is Result.Loading -> EconomyUiState.Loading
            is Result.Success -> if (data.isEmpty()) {
                EconomyUiState.Empty(query)
            } else {
                lastData = data
                lastStale = isStale
                buildContent(data, isStale, query)
            }
            is Result.Error -> EconomyUiState.Error(
                message = cause.toUserMessage(),
                canRetry = true,
            )
        }

    private fun buildContent(
        data: List<EconomyTimeSeries>,
        isStale: Boolean,
        query: EconomyQuery,
    ): EconomyUiState.Content {
        val availableCountries = data.map { it.countryCode }
        val activeCountry = when {
            selectedCountry in availableCountries -> selectedCountry
            else -> availableCountries.firstOrNull { !it.startsWith("EU") }
                ?: availableCountries.firstOrNull()
                ?: selectedCountry
        }
        selectedCountry = activeCountry

        val seriesForCountry = data.firstOrNull { it.countryCode == activeCountry }
        val availableYears = seriesForCountry?.points?.map { it.year }?.sorted().orEmpty()
        val activeYear = when {
            availableYears.isEmpty() -> query.yearRange.last
            selectedYear != null && selectedYear in availableYears -> selectedYear!!
            else -> availableYears.last()
        }.coerceIn(availableYears.firstOrNull() ?: query.yearRange.first, availableYears.lastOrNull() ?: query.yearRange.last)
        selectedYear = activeYear

        return EconomyUiState.Content(
            timeSeries = data,
            isStale = isStale,
            query = query,
            activeCountry = activeCountry,
            availableCountries = availableCountries,
            selectedMetric = selectedMetric,
            displayYearRange = displayYearRange,
            selectedYear = activeYear,
            availableYears = availableYears,
            normalized = normalized,
        )
    }
}
