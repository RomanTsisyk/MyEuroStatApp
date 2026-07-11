package eu.eurostat.core.common.cache

/**
 * In-memory [BlobCacheStore] for tests. Also usable as a seeding vehicle:
 * tests can call [put] with hand-written (including malformed) JSON.
 */
class InMemoryBlobCacheStore : BlobCacheStore {

    private val entries = mutableMapOf<String, BlobCacheEntry>()

    /** Number of stored entries — handy for delete assertions. */
    val size: Int get() = entries.size

    override suspend fun get(key: String): BlobCacheEntry? = entries[key]

    override suspend fun put(key: String, dataJson: String, fetchedAtEpochMs: Long) {
        entries[key] = BlobCacheEntry(dataJson, fetchedAtEpochMs)
    }

    override suspend fun deleteByPrefix(prefix: String) {
        entries.keys.filter { it.startsWith(prefix) }.forEach { entries.remove(it) }
    }
}
