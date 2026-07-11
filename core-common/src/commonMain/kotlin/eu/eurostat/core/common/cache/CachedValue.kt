package eu.eurostat.core.common.cache

import kotlinx.datetime.Instant

/**
 * A successfully decoded cache hit returned by [JsonBlobCache.get].
 *
 * @property value the deserialized cached value.
 * @property fetchedAt when the value was originally fetched from the network.
 *   Callers compare this against their TTL to decide whether the hit is stale
 *   (stale-while-revalidate contract).
 */
data class CachedValue<T>(
    val value: T,
    val fetchedAt: Instant,
)
