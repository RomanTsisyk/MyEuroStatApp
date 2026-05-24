package eu.eurostat.core.database.di

import eu.eurostat.core.database.createAppDatabase
import org.koin.dsl.module

fun coreDatabaseModule() = module {
    single { createAppDatabase(get()) }
}
