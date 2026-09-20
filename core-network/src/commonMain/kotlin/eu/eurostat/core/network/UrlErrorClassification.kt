package eu.eurostat.core.network

/**
 * `NSURLErrorDomain` codes that mean the device cannot reach the network:
 * timed out (-1001), cannot find host (-1003), cannot connect to host (-1004),
 * connection lost (-1005), DNS lookup failed (-1006), not connected to the
 * internet (-1009), roaming off (-1018), call active (-1019), data not
 * allowed (-1020).
 */
private val NO_CONNECTIVITY_URL_ERROR_CODES: Set<Long> =
    setOf(-1001L, -1003L, -1004L, -1005L, -1006L, -1009L, -1018L, -1019L, -1020L)

private const val URL_ERROR_MARKER = "NSURLErrorDomain Code="

/** Ktor wraps engine failures; this bounds how deep the cause chain is searched. */
private const val MAX_CAUSE_DEPTH = 8

/**
 * Extracts the `NSURLError` code from a Darwin failure message such as
 * `Exception in http request: Error Domain=NSURLErrorDomain Code=-1009 "The
 * Internet connection appears to be offline." UserInfo={…}`, or `null` when the
 * text does not describe an `NSURLErrorDomain` error.
 *
 * Ktor's Darwin engine reports failures as an exception whose message embeds
 * `NSError.description`. Parsing that text keeps the classification free of
 * Kotlin/Native platform types, so it can be unit-tested on the JVM.
 */
internal fun urlErrorCode(message: String?): Long? {
    val text = message ?: return null
    val markerAt = text.indexOf(URL_ERROR_MARKER)
    if (markerAt < 0) return null
    val start = markerAt + URL_ERROR_MARKER.length
    var end = start
    if (end < text.length && text[end] == '-') end++
    while (end < text.length && text[end] in '0'..'9') end++
    return text.substring(start, end).toLongOrNull()
}

/**
 * `true` when this throwable, or anything in its cause chain, is an
 * `NSURLErrorDomain` failure that means the device has no usable connection.
 */
internal fun Throwable.isNoConnectivityUrlError(): Boolean =
    generateSequence(this) { it.cause }
        .take(MAX_CAUSE_DEPTH)
        .any { urlErrorCode(it.message) in NO_CONNECTIVITY_URL_ERROR_CODES }
