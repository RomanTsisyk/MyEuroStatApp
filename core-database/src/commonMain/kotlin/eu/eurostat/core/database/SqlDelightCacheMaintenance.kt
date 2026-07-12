package eu.eurostat.core.database

import eu.eurostat.core.common.DispatcherProvider
import eu.eurostat.core.common.cache.CacheMaintenance
import eu.eurostat.core.database.generated.AppDatabase
import kotlinx.coroutines.withContext

/**
 * SQLDelight-backed [CacheMaintenance]: wipes every cache table — the seven
 * per-feature flat tables plus the JSON-blob store (`MultiDimCacheEntity`) —
 * inside one transaction, so a failed clear never leaves the cache
 * half-emptied. `PreferenceEntity` holds user data, not cache, and is
 * deliberately untouched.
 *
 * Bound as the [CacheMaintenance] singleton in
 * [eu.eurostat.core.database.di.coreDatabaseModule].
 */
class SqlDelightCacheMaintenance(
    private val db: AppDatabase,
    private val dispatchers: DispatcherProvider,
) : CacheMaintenance {

    override suspend fun clearAllCaches() = withContext(dispatchers.io) {
        db.transaction {
            db.populationCacheQueries.clearAll()
            db.economyCacheQueries.clearAll()
            db.environmentCacheQueries.clearAll()
            db.tradeCacheQueries.clearAll()
            db.transportCacheQueries.clearAll()
            db.socialCacheQueries.clearAll()
            db.scienceCacheQueries.clearAll()
            db.multiDimCacheQueries.clearAll()
        }
    }
}
