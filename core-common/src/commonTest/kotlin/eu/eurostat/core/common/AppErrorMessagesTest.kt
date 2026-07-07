package eu.eurostat.core.common

import kotlin.test.Test
import kotlin.test.assertEquals

class AppErrorMessagesTest {
    @Test fun noNetwork_mapsToConnectionMessage() {
        assertEquals(
            "No connection — check your network",
            AppError.NoNetwork.toUserMessage(),
        )
    }

    @Test fun httpError_serverError_mapsToTemporarilyUnavailableMessage() {
        assertEquals(
            "Eurostat servers are temporarily unavailable (HTTP 503)",
            AppError.HttpError(503, "Service Unavailable").toUserMessage(),
        )
    }

    @Test fun httpError_clientError_mapsToRequestFailedMessage() {
        assertEquals(
            "Eurostat request failed (HTTP 404)",
            AppError.HttpError(404, "Not Found").toUserMessage(),
        )
    }

    @Test fun parseError_mapsToUnexpectedDataMessage() {
        assertEquals(
            "Got unexpected data from Eurostat",
            AppError.ParseError("boom").toUserMessage(),
        )
    }

    @Test fun cacheEmpty_mapsToNoCachedDataMessage() {
        assertEquals(
            "No cached data yet",
            AppError.CacheEmpty.toUserMessage(),
        )
    }

    @Test fun unknown_mapsToSomethingWentWrongMessage() {
        assertEquals(
            "Something went wrong",
            AppError.Unknown(RuntimeException("oops")).toUserMessage(),
        )
    }
}
