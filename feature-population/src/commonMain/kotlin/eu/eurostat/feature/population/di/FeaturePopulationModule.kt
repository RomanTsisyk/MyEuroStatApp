package eu.eurostat.feature.population.di

import eu.eurostat.core.common.cache.JsonBlobCache
import eu.eurostat.core.navigation.ChildConfig
import eu.eurostat.core.navigation.ComponentFactory
import eu.eurostat.feature.population.data.PopulationApiService
import eu.eurostat.feature.population.data.PopulationApiServiceImpl
import eu.eurostat.feature.population.data.PopulationCacheBlob
import eu.eurostat.feature.population.data.PopulationCacheDao
import eu.eurostat.feature.population.data.PopulationCacheDaoImpl
import eu.eurostat.feature.population.domain.GetPopulationTimeSeriesUseCase
import eu.eurostat.feature.population.domain.PopulationRepository
import eu.eurostat.feature.population.data.PopulationRepositoryImpl
import eu.eurostat.feature.population.ui.DefaultPopulationComponent
import kotlinx.datetime.Clock
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun featurePopulationModule() = module {
    factory<PopulationApiService> { PopulationApiServiceImpl(get()) }
    factory<PopulationCacheDao> { PopulationCacheDaoImpl(get()) }
    factory<Clock> { Clock.System }
    single<PopulationRepository> {
        PopulationRepositoryImpl(
            api = get(),
            dao = get(),
            cohortCache = JsonBlobCache(get(), PopulationCacheBlob.serializer()),
            dispatchers = get(),
            clock = get(),
        )
    }
    factory { GetPopulationTimeSeriesUseCase(get()) }
    factory<ComponentFactory<Any>>(
        qualifier = named(ChildConfig.Population::class.qualifiedName!!)
    ) {
        ComponentFactory { ctx -> DefaultPopulationComponent(ctx, get(), get(), get()) }
    }
}
