package eu.eurostat.feature.science.di

import eu.eurostat.core.navigation.ChildConfig
import eu.eurostat.core.navigation.ComponentFactory
import eu.eurostat.feature.science.data.ScienceApiService
import eu.eurostat.feature.science.data.ScienceApiServiceImpl
import eu.eurostat.feature.science.data.ScienceCacheDao
import eu.eurostat.feature.science.data.ScienceCacheDaoImpl
import eu.eurostat.feature.science.data.ScienceRepositoryImpl
import eu.eurostat.feature.science.domain.GetScienceTimeSeriesUseCase
import eu.eurostat.feature.science.domain.ScienceRepository
import eu.eurostat.feature.science.ui.DefaultScienceComponent
import kotlinx.datetime.Clock
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun featureScienceModule() = module {
    factory<ScienceApiService> { ScienceApiServiceImpl(get()) }
    factory<ScienceCacheDao> { ScienceCacheDaoImpl(get()) }
    single<ScienceRepository> { ScienceRepositoryImpl(get(), get(), get(), get()) }
    factory<Clock> { Clock.System }
    factory { GetScienceTimeSeriesUseCase(get()) }
    factory<ComponentFactory<Any>>(qualifier = named(ChildConfig.Science::class.qualifiedName!!)) {
        ComponentFactory { ctx -> DefaultScienceComponent(ctx, get(), get()) }
    }
}
