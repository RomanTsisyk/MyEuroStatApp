package eu.eurostat.feature.compare

import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.Result
import eu.eurostat.core.common.prefs.AppPreferences
import eu.eurostat.core.common.prefs.ThemePreference
import eu.eurostat.feature.economy.domain.EconomyQuery
import eu.eurostat.feature.economy.domain.EconomyRepository
import eu.eurostat.feature.economy.domain.EconomyTimeSeries
import eu.eurostat.feature.environment.domain.EnvironmentQuery
import eu.eurostat.feature.environment.domain.EnvironmentRepository
import eu.eurostat.feature.environment.domain.EnvironmentTimeSeries
import eu.eurostat.feature.population.domain.PopulationData
import eu.eurostat.feature.population.domain.PopulationQuery
import eu.eurostat.feature.population.domain.PopulationRepository
import eu.eurostat.feature.science.domain.ScienceQuery
import eu.eurostat.feature.science.domain.ScienceRepository
import eu.eurostat.feature.science.domain.ScienceTimeSeries
import eu.eurostat.feature.social.domain.SocialQuery
import eu.eurostat.feature.social.domain.SocialRepository
import eu.eurostat.feature.social.domain.SocialTimeSeries
import eu.eurostat.feature.tourism.domain.TourismData
import eu.eurostat.feature.tourism.domain.TourismQuery
import eu.eurostat.feature.tourism.domain.TourismRepository
import eu.eurostat.feature.trade.domain.TradeQuery
import eu.eurostat.feature.trade.domain.TradeRepository
import eu.eurostat.feature.trade.domain.TradeTimeSeries
import eu.eurostat.feature.transport.domain.TransportQuery
import eu.eurostat.feature.transport.domain.TransportRepository
import eu.eurostat.feature.transport.domain.TransportTimeSeries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher

// ---------------------------------------------------------------------------
// Fake repositories — each exposes a MutableStateFlow the test drives, records
// the last query, and counts observe() subscriptions (so "no re-fetch" is
// verifiable).
// ---------------------------------------------------------------------------

internal class FakePopulationRepository : PopulationRepository {
    val emissions = MutableStateFlow<Result<PopulationData>>(Result.Loading)
    var lastQuery: PopulationQuery? = null
    var observeCount = 0
    override fun observe(query: PopulationQuery): Flow<Result<PopulationData>> {
        lastQuery = query
        observeCount++
        return emissions
    }
    var refreshCount = 0
    override suspend fun refresh(query: PopulationQuery) { refreshCount++ }
}

internal class FakeEconomyRepository : EconomyRepository {
    val emissions = MutableStateFlow<Result<List<EconomyTimeSeries>>>(Result.Loading)
    var lastQuery: EconomyQuery? = null
    var observeCount = 0
    override fun observe(query: EconomyQuery): Flow<Result<List<EconomyTimeSeries>>> {
        lastQuery = query
        observeCount++
        return emissions
    }
    var refreshCount = 0
    override suspend fun refresh(query: EconomyQuery) { refreshCount++ }
}

internal class FakeEnvironmentRepository : EnvironmentRepository {
    val emissions = MutableStateFlow<Result<List<EnvironmentTimeSeries>>>(Result.Loading)
    var lastQuery: EnvironmentQuery? = null
    var observeCount = 0
    override fun observe(query: EnvironmentQuery): Flow<Result<List<EnvironmentTimeSeries>>> {
        lastQuery = query
        observeCount++
        return emissions
    }
    var refreshCount = 0
    override suspend fun refresh(query: EnvironmentQuery) { refreshCount++ }
}

internal class FakeTradeRepository : TradeRepository {
    val emissions = MutableStateFlow<Result<List<TradeTimeSeries>>>(Result.Loading)
    var lastQuery: TradeQuery? = null
    var observeCount = 0
    override fun observe(query: TradeQuery): Flow<Result<List<TradeTimeSeries>>> {
        lastQuery = query
        observeCount++
        return emissions
    }
    var refreshCount = 0
    override suspend fun refresh(query: TradeQuery) { refreshCount++ }
}

internal class FakeTransportRepository : TransportRepository {
    val emissions = MutableStateFlow<Result<List<TransportTimeSeries>>>(Result.Loading)
    var lastQuery: TransportQuery? = null
    var observeCount = 0
    override fun observe(query: TransportQuery): Flow<Result<List<TransportTimeSeries>>> {
        lastQuery = query
        observeCount++
        return emissions
    }
    var refreshCount = 0
    override suspend fun refresh(query: TransportQuery) { refreshCount++ }
}

internal class FakeTourismRepository : TourismRepository {
    val emissions = MutableStateFlow<Result<TourismData>>(Result.Loading)
    var lastQuery: TourismQuery? = null
    var observeCount = 0
    override fun observe(query: TourismQuery): Flow<Result<TourismData>> {
        lastQuery = query
        observeCount++
        return emissions
    }
    var refreshCount = 0
    override suspend fun refresh(query: TourismQuery) { refreshCount++ }
}

internal class FakeSocialRepository : SocialRepository {
    val emissions = MutableStateFlow<Result<List<SocialTimeSeries>>>(Result.Loading)
    var lastQuery: SocialQuery? = null
    var observeCount = 0
    override fun observe(query: SocialQuery): Flow<Result<List<SocialTimeSeries>>> {
        lastQuery = query
        observeCount++
        return emissions
    }
    var refreshCount = 0
    override suspend fun refresh(query: SocialQuery) { refreshCount++ }
}

internal class FakeScienceRepository : ScienceRepository {
    val emissions = MutableStateFlow<Result<List<ScienceTimeSeries>>>(Result.Loading)
    var lastQuery: ScienceQuery? = null
    var observeCount = 0
    override fun observe(query: ScienceQuery): Flow<Result<List<ScienceTimeSeries>>> {
        lastQuery = query
        observeCount++
        return emissions
    }
    var refreshCount = 0
    override suspend fun refresh(query: ScienceQuery) { refreshCount++ }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class TestDispatchers(dispatcher: TestDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

/** In-memory [AppPreferences] fake; only [defaultCountry] matters to the component. */
internal class FakeAppPreferences(
    defaultCountry: String = AppPreferences.DEFAULT_COUNTRY,
) : AppPreferences {
    override val themePreference: Flow<ThemePreference> = MutableStateFlow(ThemePreference.SYSTEM)
    override val language: Flow<String> = MutableStateFlow(AppPreferences.DEFAULT_LANGUAGE)
    override val defaultCountry: Flow<String> = MutableStateFlow(defaultCountry)
    override suspend fun setThemePreference(value: ThemePreference) = Unit
    override suspend fun setLanguage(value: String) = Unit
    override suspend fun setDefaultCountry(value: String) = Unit
}
