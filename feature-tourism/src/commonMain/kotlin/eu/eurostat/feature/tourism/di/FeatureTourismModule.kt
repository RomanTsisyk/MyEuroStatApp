package eu.eurostat.feature.tourism.di

import eu.eurostat.core.navigation.ChildConfig
import eu.eurostat.core.navigation.ComponentFactory
import eu.eurostat.feature.tourism.data.TourismApiService
import eu.eurostat.feature.tourism.data.TourismApiServiceImpl
import eu.eurostat.feature.tourism.data.TourismCacheDao
import eu.eurostat.feature.tourism.data.TourismCacheDaoImpl
import eu.eurostat.feature.tourism.data.TourismRepositoryImpl
import eu.eurostat.feature.tourism.domain.GetTourismTimeSeriesUseCase
import eu.eurostat.feature.tourism.domain.TourismRepository
import eu.eurostat.feature.tourism.ui.DefaultTourismComponent
import kotlinx.datetime.Clock
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun featureTourismModule() = module {
    factory<TourismApiService> { TourismApiServiceImpl(get()) }
    factory<TourismCacheDao> { TourismCacheDaoImpl(get()) }
    single<TourismRepository> { TourismRepositoryImpl(get(), get(), get(), get()) }
    factory<Clock> { Clock.System }
    factory { GetTourismTimeSeriesUseCase(get()) }
    factory<ComponentFactory<Any>>(qualifier = named(ChildConfig.Tourism::class.qualifiedName!!)) {
        ComponentFactory { ctx -> DefaultTourismComponent(ctx, get(), get()) }
    }
}
