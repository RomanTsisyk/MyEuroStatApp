package eu.eurostat.core.network

import eu.eurostat.core.common.AppError
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

/**
 * Maps this [Throwable] to a typed [AppError].
 *
 * **Important:** [CancellationException] is always re-thrown before mapping so that
 * structured concurrency is preserved. A cancelled coroutine must never be silently
 * absorbed as an [AppError.Unknown] — that would leak the coroutine and suppress
 * parent-scope cancellation.
 */
actual fun Throwable.toAppError(): AppError {
    if (this is CancellationException) throw this
    return when (this) {
        is ConnectTimeoutException,
        is HttpRequestTimeoutException,
        -> AppError.NoNetwork

        is ClientRequestException -> AppError.HttpError(response.status.value, message ?: "")
        is ServerResponseException -> AppError.HttpError(response.status.value, message ?: "")

        is JsonConvertException,
        is SerializationException,
        -> AppError.ParseError(message ?: "Parse error")

        // Darwin reports a device that is really offline as an NSURLErrorDomain error
        // (e.g. -1009 "not connected to the internet"), not as a Ktor timeout.
        else -> if (isNoConnectivityUrlError()) AppError.NoNetwork else AppError.Unknown(this)
    }
}
