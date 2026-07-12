package eu.eurostat.feature.trade.ui

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import eu.eurostat.core.common.AppError
import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.common.prefs.AppPreferences
import eu.eurostat.core.common.prefs.ThemePreference
import eu.eurostat.feature.trade.domain.GetTradeTimeSeriesUseCase
import eu.eurostat.feature.trade.domain.TradeDataPoint
import eu.eurostat.feature.trade.domain.TradeQuery
import eu.eurostat.feature.trade.domain.TradeRepository
import eu.eurostat.feature.trade.domain.TradeTimeSeries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// Test doubles
// ---------------------------------------------------------------------------

private class FakeTradeRepository : TradeRepository {
    var lastQuery: TradeQuery? = null
    var refreshCallCount = 0
    var refreshThrows: Throwable? = null

    val emissions = MutableStateFlow<Result<List<TradeTimeSeries>>>(Result.Loading)

    override fun observe(query: TradeQuery): Flow<Result<List<TradeTimeSeries>>> {
        lastQuery = query
        return emissions.asStateFlow()
    }

    override suspend fun refresh(query: TradeQuery) {
        refreshCallCount++
        refreshThrows?.let { throw it }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class TestDispatcherProviderLocal(dispatcher: kotlinx.coroutines.test.TestDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

/** In-memory [AppPreferences] fake; only [defaultCountry] matters to the component. */
private class FakeAppPreferences(
    defaultCountry: String = AppPreferences.DEFAULT_COUNTRY,
) : AppPreferences {
    override val themePreference: Flow<ThemePreference> = MutableStateFlow(ThemePreference.SYSTEM)
    override val language: Flow<String> = MutableStateFlow(AppPreferences.DEFAULT_LANGUAGE)
    override val defaultCountry: Flow<String> = MutableStateFlow(defaultCountry)
    override suspend fun setThemePreference(value: ThemePreference) = Unit
    override suspend fun setLanguage(value: String) = Unit
    override suspend fun setDefaultCountry(value: String) = Unit
}

// ---------------------------------------------------------------------------
// Sample data
// ---------------------------------------------------------------------------

private fun sampleTradeSeries(country: String = "DE", partner: String = "EU27_2020"): TradeTimeSeries =
    TradeTimeSeries(
        countryCode = country,
        countryName = country,
        partner = partner,
        points = listOf(
            TradeDataPoint(
                countryCode = country,
                year = 2019,
                partner = partner,
                exportsEur = 480_000L,
                importsEur = 390_000L,
                balanceEur = 90_000L,
            ),
            TradeDataPoint(
                countryCode = country,
                year = 2020,
                partner = partner,
                exportsEur = 500_000L,
                importsEur = 400_000L,
                balanceEur = 100_000L,
            ),
        )
    )

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

@OptIn(ExperimentalCoroutinesApi::class)
class TradeComponentTest {

    private val lifecycle = LifecycleRegistry()
    private val context = DefaultComponentContext(lifecycle)

    @BeforeTest
    fun resume() {
        lifecycle.resume()
    }

    @AfterTest
    fun destroy() {
        lifecycle.destroy()
    }

    private fun buildComponent(
        repo: FakeTradeRepository,
        dispatcher: kotlinx.coroutines.test.TestDispatcher,
        preferences: AppPreferences = FakeAppPreferences(),
    ): DefaultTradeComponent {
        val useCase = GetTradeTimeSeriesUseCase(repo)
        val dispatchers = TestDispatcherProviderLocal(dispatcher)
        return DefaultTradeComponent(context, useCase, dispatchers, preferences)
    }

    @Test
    fun stored_default_country_seeds_active_country_and_first_query() = runTest {
        val repo = FakeTradeRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher, FakeAppPreferences(defaultCountry = "IT"))

        repo.emissions.value = Result.Success(
            listOf(sampleTradeSeries("IT"), sampleTradeSeries("DE")),
            isStale = false,
        )
        testScheduler.advanceUntilIdle()

        // Preference country joins the list right after the EU aggregate.
        assertEquals(listOf("EU27_2020", "IT", "DE", "FR", "PL"), repo.lastQuery?.countryCodes)

        val state = component.state.value
        assertIs<TradeUiState.Content>(state)
        assertEquals("IT", state.activeCountry)
    }

    @Test
    fun init_emits_loading_immediately() = runTest {
        val repo = FakeTradeRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        assertEquals(TradeUiState.Loading, component.state.value)
    }

    @Test
    fun emits_content_when_repo_emits_success_with_data() = runTest {
        val repo = FakeTradeRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(sampleTradeSeries()), isStale = false)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<TradeUiState.Content>(state)
        assertEquals(1, state.series.size)
        assertFalse(state.isStale)
    }

    @Test
    fun emits_empty_when_repo_emits_success_with_empty_list() = runTest {
        val repo = FakeTradeRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(emptyList(), isStale = false)
        testScheduler.advanceUntilIdle()

        assertIs<TradeUiState.Empty>(component.state.value)
    }

    @Test
    fun emits_error_with_canRetry_when_repo_emits_error() = runTest {
        val repo = FakeTradeRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Error(AppError.NoNetwork)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<TradeUiState.Error>(state)
        assertTrue(state.canRetry)
    }

    @Test
    fun stale_content_emits_isStale_true() = runTest {
        val repo = FakeTradeRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(sampleTradeSeries()), isStale = true)
        testScheduler.advanceUntilIdle()

        val state = component.state.value
        assertIs<TradeUiState.Content>(state)
        assertTrue(state.isStale)
    }

    @Test
    fun on_select_countries_updates_query_and_reloads() = runTest {
        val repo = FakeTradeRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(TradeIntent.SelectCountries(listOf("IT", "ES")))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("IT", "ES"), repo.lastQuery?.countryCodes)
    }

    @Test
    fun on_change_year_range_updates_query_and_reloads() = runTest {
        val repo = FakeTradeRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(TradeIntent.ChangeYearRange(2015..2023))
        testScheduler.advanceUntilIdle()

        assertEquals(2015..2023, repo.lastQuery?.yearRange)
    }

    @Test
    fun on_change_partner_updates_query_and_reloads() = runTest {
        val repo = FakeTradeRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(TradeIntent.ChangePartner("US"))
        testScheduler.advanceUntilIdle()

        assertEquals("US", repo.lastQuery?.partner)
    }

    @Test
    fun on_refresh_calls_repository_refresh() = runTest {
        val repo = FakeTradeRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        component.onIntent(TradeIntent.Refresh)
        testScheduler.advanceUntilIdle()

        assertEquals(1, repo.refreshCallCount)
    }

    @Test
    fun on_retry_triggers_reload() = runTest {
        val repo = FakeTradeRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)
        testScheduler.advanceUntilIdle()

        repo.emissions.value = Result.Success(listOf(sampleTradeSeries()), isStale = false)
        component.onIntent(TradeIntent.Retry)
        testScheduler.advanceUntilIdle()

        assertIs<TradeUiState.Content>(component.state.value)
    }

    @Test
    fun select_year_updates_selectedYear_without_re_fetching() = runTest {
        val repo = FakeTradeRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val component = buildComponent(repo, dispatcher)

        repo.emissions.value = Result.Success(listOf(sampleTradeSeries()), isStale = false)
        testScheduler.advanceUntilIdle()

        val initialContent = component.state.value as TradeUiState.Content
        assertEquals(2020, initialContent.selectedYear)
        assertEquals(listOf(2019, 2020), initialContent.availableYears)

        val queryCountBefore = repo.lastQuery
        component.onIntent(TradeIntent.SelectYear(2019))
        testScheduler.advanceUntilIdle()

        val updatedContent = component.state.value as TradeUiState.Content
        assertEquals(2019, updatedContent.selectedYear)
        assertEquals(queryCountBefore, repo.lastQuery, "SelectYear must not trigger a new network query")
    }
}
