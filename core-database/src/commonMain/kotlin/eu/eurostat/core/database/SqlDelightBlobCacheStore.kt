package eu.eurostat.core.database

import eu.eurostat.core.common.cache.BlobCacheEntry
import eu.eurostat.core.common.cache.BlobCacheStore
import eu.eurostat.core.database.generated.AppDatabase

/**
 * SQLDelight-backed [BlobCacheStore] persisting entries in the
 * `MultiDimCacheEntity` table (see `MultiDimCache.sq`).
 *
 * Bound as the [BlobCacheStore] singleton in
 * [eu.eurostat.core.database.di.coreDatabaseModule]; feature modules wrap it in
 * a typed `JsonBlobCache` with their own serializer.
 */
class SqlDelightBlobCacheStore(
    private val db: AppDatabase,
) : BlobCacheStore {

    override suspend fun get(key: String): BlobCacheEntry? =
        db.multiDimCacheQueries.selectByKey(key).executeAsOneOrNull()?.let { entity ->
            BlobCacheEntry(
                dataJson = entity.data_json,
                fetchedAtEpochMs = entity.fetched_at_epoch_ms,
            )
        }

    override suspend fun put(key: String, dataJson: String, fetchedAtEpochMs: Long) {
        db.multiDimCacheQueries.upsert(
            cache_key = key,
            data_json = dataJson,
            fetched_at_epoch_ms = fetchedAtEpochMs,
        )
    }

    override suspend fun deleteByPrefix(prefix: String) {
        db.multiDimCacheQueries.deleteByKeyPrefix(prefix)
    }
}
