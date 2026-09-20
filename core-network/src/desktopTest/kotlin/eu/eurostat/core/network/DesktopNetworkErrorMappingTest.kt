package eu.eurostat.core.network

import eu.eurostat.core.common.AppError
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.test.Test
import kotlin.test.assertIs

/**
 * Locks down the JVM connectivity branches of [toAppError]: a device that is
 * genuinely offline throws plain `java.net` exceptions rather than Ktor timeout
 * types, and must surface as [AppError.NoNetwork] ("Check your connection")
 * instead of the generic [AppError.Unknown].
 */
class DesktopNetworkErrorMappingTest {

    @Test
    fun unknown_host_maps_to_no_network() {
        assertIs<AppError.NoNetwork>(UnknownHostException("ec.europa.eu").toAppError())
    }

    @Test
    fun connection_refused_maps_to_no_network() {
        assertIs<AppError.NoNetwork>(ConnectException("Connection refused").toAppError())
    }

    @Test
    fun no_route_to_host_maps_to_no_network() {
        assertIs<AppError.NoNetwork>(NoRouteToHostException("No route to host").toAppError())
    }

    @Test
    fun socket_timeout_maps_to_no_network() {
        assertIs<AppError.NoNetwork>(SocketTimeoutException("timeout").toAppError())
    }

    @Test
    fun unrelated_io_exception_stays_unknown() {
        assertIs<AppError.Unknown>(IOException("disk full").toAppError())
    }
}
