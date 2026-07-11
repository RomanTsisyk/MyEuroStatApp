package eu.eurostat.core.common.cache

/**
 * Storage contract for the convergent JSON-blob cache used by feature modules
 * whose domain models are too multi-dimensional for flat per-column tables
 * (age-cohort pyramids, residence-split tourism rows, seasonality grids, …).
 *
 * One row per [String] cache key; the value is an opaque JSON document plus the
 * epoch-millisecond timestamp of the network fetch that produced it. Key layout
 * is owned by the callers — by convention `"{feature}:{slice}:v{N}:{params}"`
 * so that [deleteByPrefix] can wipe a whole feature (or slice) at once and the
 * `v{N}` segment can be bumped when the serialized shape changes.
 *
 * Implementations live in `core-database` (SQLDelight-backed) and in test code
 * (in-memory fakes). All methods are `suspend` so implementations may touch
 * disk; callers are expected to invoke them from an IO dispatcher.
 */
interface BlobCacheStore {

    /** Returns the stored entry for [key], or `null` when the key is absent. */
    suspend fun get(key: String): BlobCacheEntry?

    /**
     * Inserts or replaces the entry stored under [key].
     *
     * @param dataJson serialized JSON payload; opaque to the store.
     * @param fetchedAtEpochMs epoch milliseconds of the originating network fetch,
     *   used by callers to compute staleness.
     */
    suspend fun put(key: String, dataJson: String, fetchedAtEpochMs: Long)

    /** Deletes every entry whose key starts with [prefix]. */
    suspend fun deleteByPrefix(prefix: String)
}
