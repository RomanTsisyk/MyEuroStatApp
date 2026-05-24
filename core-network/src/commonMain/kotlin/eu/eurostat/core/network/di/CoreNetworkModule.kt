package eu.eurostat.core.network.di

import eu.eurostat.core.jsonstat.JsonStatParser
import eu.eurostat.core.network.EurostatApiClient
import eu.eurostat.core.network.EurostatJson
import eu.eurostat.core.network.createHttpClient
import org.koin.dsl.module

fun coreNetworkModule() = module {
    single { EurostatJson }
    single { createHttpClient(get()) }
    single { JsonStatParser() }
    single { EurostatApiClient(get(), get()) }
}
