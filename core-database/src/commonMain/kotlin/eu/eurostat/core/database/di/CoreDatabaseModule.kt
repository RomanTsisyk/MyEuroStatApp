package eu.eurostat.core.database.di

import eu.eurostat.core.common.cache.BlobCacheStore
import eu.eurostat.core.common.cache.CacheMaintenance
import eu.eurostat.core.common.prefs.AppPreferences
import eu.eurostat.core.database.SqlDelightAppPreferences
import eu.eurostat.core.database.SqlDelightBlobCacheStore
import eu.eurostat.core.database.SqlDelightCacheMaintenance
import eu.eurostat.core.database.createAppDatabase
import org.koin.dsl.module

fun coreDatabaseModule() = module {
    single { createAppDatabase(get()) }
    single<BlobCacheStore> { SqlDelightBlobCacheStore(get()) }
    single<AppPreferences> { SqlDelightAppPreferences(get(), get()) }
    single<CacheMaintenance> { SqlDelightCacheMaintenance(get(), get()) }
}
