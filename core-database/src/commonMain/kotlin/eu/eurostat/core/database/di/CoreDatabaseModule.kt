package eu.eurostat.core.database.di

import eu.eurostat.core.common.cache.BlobCacheStore
import eu.eurostat.core.database.SqlDelightBlobCacheStore
import eu.eurostat.core.database.createAppDatabase
import org.koin.dsl.module

fun coreDatabaseModule() = module {
    single { createAppDatabase(get()) }
    single<BlobCacheStore> { SqlDelightBlobCacheStore(get()) }
}
