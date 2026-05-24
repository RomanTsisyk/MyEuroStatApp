package eu.eurostat.core.common.di

import eu.eurostat.core.common.DefaultDispatcherProvider
import eu.eurostat.core.common.DispatcherProvider
import org.koin.dsl.module

fun coreCommonModule() = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
}
