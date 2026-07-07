package eu.eurostat.core.common.cache

import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Typed façade over a [BlobCacheStore]: serializes values of [T] to JSON on
 * write and deserializes them on read.
 *
 * Deserialization failures never cross the data/domain boundary — a corrupted
 * or shape-incompatible payload (e.g. after an app update that changed the DTO)
 * is treated as a plain cache miss and [get] returns `null`. The stale row is
 * then naturally overwritten by the next successful network fetch.
 *
 * @param T the (serializable) cached value type — typically a small DTO owned
 *   by the feature's data layer, not the domain model itself.
 * @property store the underlying key/JSON/timestamp storage.
 * @property serializer strategy used to encode and decode [T].
 * @property json JSON configuration. The default ignores unknown keys so that
 *   payloads written by a newer app version with additive fields still decode.
 */
class JsonBlobCache<T>(
    private val store: BlobCacheStore,
    private val serializer: KSerializer<T>,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {

    /**
     * Returns the decoded value stored under [key] together with its fetch
     * timestamp, or `null` when the key is absent **or** the stored JSON can
     * no longer be decoded into [T] (corruption / schema drift — treated as a
     * cache miss rather than an error).
     */
    suspend fun get(key: String): CachedValue<T>? {
        val entry = store.get(key) ?: return null
        val value = try {
            json.decodeFromString(serializer, entry.dataJson)
        } catch (_: IllegalArgumentException) {
            // SerializationException extends IllegalArgumentException; both mean
            // "this payload is not (or no longer) a valid T" -> cache miss.
            return null
        }
        return CachedValue(
            value = value,
            fetchedAt = Instant.fromEpochMilliseconds(entry.fetchedAtEpochMs),
        )
    }

    /** Serializes [value] and stores it under [key] with the given [fetchedAt] timestamp. */
    suspend fun put(key: String, value: T, fetchedAt: Instant) {
        store.put(
            key = key,
            dataJson = json.encodeToString(serializer, value),
            fetchedAtEpochMs = fetchedAt.toEpochMilliseconds(),
        )
    }

    /** Deletes every entry whose key starts with [prefix]. */
    suspend fun deleteByPrefix(prefix: String) {
        store.deleteByPrefix(prefix)
    }
}
