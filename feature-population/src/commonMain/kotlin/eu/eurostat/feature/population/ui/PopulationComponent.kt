package eu.eurostat.feature.population.ui

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.common.toUserMessage
import eu.eurostat.feature.population.domain.GetPopulationTimeSeriesUseCase
import eu.eurostat.feature.population.domain.PopulationData
import eu.eurostat.feature.population.domain.PopulationQuery
import eu.eurostat.feature.population.domain.PopulationSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface PopulationComponent {
    val state: StateFlow<PopulationUiState>
    fun onIntent(intent: PopulationIntent)
}

class DefaultPopulationComponent(
    componentContext: ComponentContext,
    private val useCase: GetPopulationTimeSeriesUseCase,
    private val dispatchers: DispatcherProvider,
) : PopulationComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(SupervisorJob() + dispatchers.main)
    private val _state = MutableStateFlow<PopulationUiState>(PopulationUiState.Loading)
    override val state: StateFlow<PopulationUiState> = _state.asStateFlow()

    private var currentQuery = PopulationQuery(
        countryCodes = listOf("EU27_2020", "DE", "FR", "PL"),
        yearRange = 2010..2024,
        includeCohorts = true,
    )
    /** UI-only selection — survives across emissions on the same query. */
    private var selectedCountry: String = "DE"
    private var selectedYear: Int? = null
    private var selectedMetric: Int = 0

    private var collectJob: Job? = null

    init {
        load()
    }

    override fun onIntent(intent: PopulationIntent) {
        when (intent) {
            is PopulationIntent.SelectCountries -> {
                currentQuery = currentQuery.copy(countryCodes = intent.codes)
                load()
            }
            is PopulationIntent.ChangeYearRange -> {
                currentQuery = currentQuery.copy(yearRange = intent.range)
                selectedYear = null
                load()
            }
            is PopulationIntent.SelectActiveCountry -> {
                selectedCountry = intent.code
                rerenderFromLastData()
            }
            is PopulationIntent.SelectYear -> {
                selectedYear = intent.year
                rerenderFromLastData()
            }
            is PopulationIntent.SelectMetric -> {
                selectedMetric = intent.index
                rerenderFromLastData()
            }
            PopulationIntent.Refresh -> scope.launch {
                runCatching { useCase.refresh(currentQuery) }
                load()
            }
            PopulationIntent.Retry -> load()
        }
    }

    private var lastData: PopulationData? = null
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

    private fun Result<PopulationData>.toUiState(query: PopulationQuery): PopulationUiState =
        when (this) {
            is Result.Loading -> PopulationUiState.Loading
            is Result.Success -> if (data.timeSeries.isEmpty()) {
                PopulationUiState.Empty(query)
            } else {
                lastData = data
                lastStale = isStale
                buildContent(data, isStale, query)
            }
            is Result.Error -> PopulationUiState.Error(
                message = cause.toUserMessage(),
                canRetry = true,
            )
        }

    private fun buildContent(
        data: PopulationData,
        isStale: Boolean,
        query: PopulationQuery,
    ): PopulationUiState.Content {
        val availableCountries = data.timeSeries.map { it.countryCode }
        val activeCountry = when {
            selectedCountry in availableCountries -> selectedCountry
            else -> availableCountries.firstOrNull { it != "EU27_2020" }
                ?: availableCountries.firstOrNull()
                ?: selectedCountry
        }
        selectedCountry = activeCountry

        val seriesForCountry = data.timeSeries.firstOrNull { it.countryCode == activeCountry }
        val availableYears = seriesForCountry?.points?.map { it.year }?.sorted().orEmpty()
        val activeYear = when {
            selectedYear != null && selectedYear in availableYears -> selectedYear ?: 0
            availableYears.isNotEmpty() -> availableYears.last()
            else -> query.yearRange.last
        }
        selectedYear = activeYear

        val snapshot: PopulationSnapshot? = data.snapshots[activeCountry to activeYear]

        return PopulationUiState.Content(
            timeSeries = data.timeSeries,
            snapshot = snapshot,
            selectedCountry = activeCountry,
            selectedYear = activeYear,
            availableYears = availableYears,
            availableCountries = availableCountries,
            selectedMetric = selectedMetric,
            isStale = isStale,
            query = query,
        )
    }
}
