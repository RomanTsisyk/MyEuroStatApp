package eu.eurostat.feature.economy.di

import eu.eurostat.core.navigation.ChildConfig
import eu.eurostat.core.navigation.ComponentFactory
import eu.eurostat.feature.economy.data.EconomyApiService
import eu.eurostat.feature.economy.data.EconomyApiServiceImpl
import eu.eurostat.feature.economy.data.EconomyCacheDao
import eu.eurostat.feature.economy.data.EconomyCacheDaoImpl
import eu.eurostat.feature.economy.data.EconomyRepositoryImpl
import eu.eurostat.feature.economy.domain.EconomyRepository
import eu.eurostat.feature.economy.domain.GetEconomyTimeSeriesUseCase
import eu.eurostat.feature.economy.ui.DefaultEconomyComponent
import kotlinx.datetime.Clock
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun featureEconomyModule() = module {
    factory<EconomyApiService> { EconomyApiServiceImpl(get()) }
    factory<EconomyCacheDao> { EconomyCacheDaoImpl(get()) }
    factory<Clock> { Clock.System }
    single<EconomyRepository> { EconomyRepositoryImpl(get(), get(), get(), get()) }
    factory { GetEconomyTimeSeriesUseCase(get()) }
    factory<ComponentFactory<Any>>(
        qualifier = named(ChildConfig.Economy::class.qualifiedName!!)
    ) {
        ComponentFactory { ctx -> DefaultEconomyComponent(ctx, get(), get(), get()) }
    }
}
