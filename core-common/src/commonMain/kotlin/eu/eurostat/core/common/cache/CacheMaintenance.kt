package eu.eurostat.core.common.cache

/**
 * Destructive maintenance operations on the local statistics cache.
 *
 * Lets the Settings screen offer "Clear cache" without depending on the
 * database module: the SQLDelight-backed implementation lives in
 * `core-database` (`SqlDelightCacheMaintenance`) and wipes every cache table
 * in a single transaction. User preferences are not cache and are never
 * touched by this interface.
 */
interface CacheMaintenance {

    /**
     * Deletes every cached statistics row — all per-feature flat tables plus
     * the JSON-blob store — atomically. Feature repositories transparently
     * refetch from the network on their next observation, so clearing is safe
     * at any time.
     */
    suspend fun clearAllCaches()
}
