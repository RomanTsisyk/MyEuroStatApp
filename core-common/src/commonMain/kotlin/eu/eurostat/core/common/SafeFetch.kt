package eu.eurostat.core.common

import kotlinx.coroutines.CancellationException

/**
 * Runs [block] and returns its result, or null on failure.
 *
 * Re-throws [CancellationException] to preserve structured concurrency — if the
 * surrounding coroutine is cancelled, this function must not swallow the cancellation.
 *
 * Any other [Throwable] is reported via [onError] and the function returns null.
 */
suspend inline fun <T> safeFetch(
    onError: (Throwable) -> Unit,
    block: () -> T,
): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (t: Throwable) {
    onError(t)
    null
}
