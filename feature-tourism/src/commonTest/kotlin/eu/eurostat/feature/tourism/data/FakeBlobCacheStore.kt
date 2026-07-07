package eu.eurostat.feature.tourism.data

import eu.eurostat.core.common.cache.BlobCacheEntry
import eu.eurostat.core.common.cache.BlobCacheStore

/**
 * In-memory [BlobCacheStore] backing the tourism repository tests.
 * [failReads] simulates a broken storage layer for degradation tests.
 */
class FakeBlobCacheStore : BlobCacheStore {

    private val entries = mutableMapOf<String, BlobCacheEntry>()

    /** When `true`, every [get] throws to simulate a storage failure. */
    var failReads: Boolean = false

    /** Number of stored entries. */
    val size: Int get() = entries.size

    override suspend fun get(key: String): BlobCacheEntry? {
        check(!failReads) { "simulated storage failure" }
        return entries[key]
    }

    override suspend fun put(key: String, dataJson: String, fetchedAtEpochMs: Long) {
        entries[key] = BlobCacheEntry(dataJson, fetchedAtEpochMs)
    }

    override suspend fun deleteByPrefix(prefix: String) {
        entries.keys.filter { it.startsWith(prefix) }.forEach { entries.remove(it) }
    }

    /** Seeds raw (possibly malformed) JSON directly, bypassing serialization. */
    fun seedRaw(key: String, dataJson: String, fetchedAtEpochMs: Long) {
        entries[key] = BlobCacheEntry(dataJson, fetchedAtEpochMs)
    }
}
