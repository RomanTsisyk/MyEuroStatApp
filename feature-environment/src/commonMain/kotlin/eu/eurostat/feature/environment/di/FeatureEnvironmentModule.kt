package eu.eurostat.feature.environment.di

import eu.eurostat.core.navigation.ChildConfig
import eu.eurostat.core.navigation.ComponentFactory
import eu.eurostat.feature.environment.data.EnvironmentApiService
import eu.eurostat.feature.environment.data.EnvironmentApiServiceImpl
import eu.eurostat.feature.environment.data.EnvironmentCacheDao
import eu.eurostat.feature.environment.data.EnvironmentCacheDaoImpl
import eu.eurostat.feature.environment.data.EnvironmentRepositoryImpl
import eu.eurostat.feature.environment.domain.EnvironmentRepository
import eu.eurostat.feature.environment.domain.GetEnvironmentTimeSeriesUseCase
import eu.eurostat.feature.environment.ui.DefaultEnvironmentComponent
import kotlinx.datetime.Clock
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun featureEnvironmentModule() = module {
    factory<EnvironmentApiService> { EnvironmentApiServiceImpl(get()) }
    factory<EnvironmentCacheDao> { EnvironmentCacheDaoImpl(get()) }
    factory<Clock> { Clock.System }
    single<EnvironmentRepository> { EnvironmentRepositoryImpl(get(), get(), get(), get()) }
    factory { GetEnvironmentTimeSeriesUseCase(get()) }
    factory<ComponentFactory<Any>>(
        qualifier = named(ChildConfig.Environment::class.qualifiedName!!)
    ) {
        ComponentFactory { ctx -> DefaultEnvironmentComponent(ctx, get(), get()) }
    }
}
