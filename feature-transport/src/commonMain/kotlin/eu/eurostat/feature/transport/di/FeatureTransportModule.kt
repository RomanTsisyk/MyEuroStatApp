package eu.eurostat.feature.transport.di

import eu.eurostat.core.navigation.ChildConfig
import eu.eurostat.core.navigation.ComponentFactory
import eu.eurostat.feature.transport.data.TransportApiService
import eu.eurostat.feature.transport.data.TransportApiServiceImpl
import eu.eurostat.feature.transport.data.TransportCacheDao
import eu.eurostat.feature.transport.data.TransportCacheDaoImpl
import eu.eurostat.feature.transport.data.TransportRepositoryImpl
import eu.eurostat.feature.transport.domain.GetTransportTimeSeriesUseCase
import eu.eurostat.feature.transport.domain.TransportRepository
import eu.eurostat.feature.transport.ui.DefaultTransportComponent
import kotlinx.datetime.Clock
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun featureTransportModule() = module {
    factory<TransportApiService> { TransportApiServiceImpl(get()) }
    factory<TransportCacheDao> { TransportCacheDaoImpl(get()) }
    single<TransportRepository> { TransportRepositoryImpl(get(), get(), get(), get()) }
    factory<Clock> { Clock.System }
    factory { GetTransportTimeSeriesUseCase(get()) }
    factory<ComponentFactory<Any>>(
        qualifier = named(ChildConfig.Transport::class.qualifiedName!!)
    ) {
        ComponentFactory { ctx -> DefaultTransportComponent(ctx, get(), get()) }
    }
}
