package eu.eurostat.core.common.cache

/**
 * Raw row returned by [BlobCacheStore.get] — the serialized payload plus the
 * timestamp of the network fetch that produced it.
 *
 * @property dataJson serialized JSON payload exactly as it was stored.
 * @property fetchedAtEpochMs epoch milliseconds of the originating network fetch.
 */
data class BlobCacheEntry(
    val dataJson: String,
    val fetchedAtEpochMs: Long,
)
