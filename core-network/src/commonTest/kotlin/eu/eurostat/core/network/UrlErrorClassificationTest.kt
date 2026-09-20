package eu.eurostat.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the text-based `NSURLErrorDomain` classification that the iOS
 * `toAppError()` uses. Runs on the JVM because it touches no platform types.
 */
class UrlErrorClassificationTest {

    private fun darwin(code: Int, domain: String = "NSURLErrorDomain") =
        IllegalStateException(
            "Exception in http request: Error Domain=$domain Code=$code " +
                "\"The Internet connection appears to be offline.\" UserInfo={NSLocalizedDescription=x}",
        )

    @Test
    fun parses_negative_code_from_darwin_message() {
        assertEquals(-1009L, urlErrorCode(darwin(-1009).message))
    }

    @Test
    fun returns_null_for_other_domains_and_plain_messages() {
        assertNull(urlErrorCode(darwin(50, domain = "NSPOSIXErrorDomain").message))
        assertNull(urlErrorCode("boom"))
        assertNull(urlErrorCode(null))
    }

    @Test
    fun returns_null_when_the_code_has_no_digits() {
        assertNull(urlErrorCode("Error Domain=NSURLErrorDomain Code=oops"))
    }

    @Test
    fun not_connected_and_lost_connection_are_no_connectivity() {
        assertTrue(darwin(-1009).isNoConnectivityUrlError())
        assertTrue(darwin(-1005).isNoConnectivityUrlError())
        assertTrue(darwin(-1004).isNoConnectivityUrlError())
        assertTrue(darwin(-1003).isNoConnectivityUrlError())
        assertTrue(darwin(-1001).isNoConnectivityUrlError())
    }

    @Test
    fun unrelated_url_errors_are_not_no_connectivity() {
        // -1011 = bad server response, -999 = cancelled.
        assertFalse(darwin(-1011).isNoConnectivityUrlError())
        assertFalse(darwin(-999).isNoConnectivityUrlError())
    }

    @Test
    fun other_domains_are_not_no_connectivity() {
        assertFalse(darwin(-1009, domain = "SomeOtherDomain").isNoConnectivityUrlError())
    }

    @Test
    fun finds_the_error_in_the_cause_chain() {
        val wrapped = RuntimeException("request failed", darwin(-1009))
        assertTrue(wrapped.isNoConnectivityUrlError())
    }

    @Test
    fun plain_exception_is_not_no_connectivity() {
        assertFalse(IllegalStateException("boom").isNoConnectivityUrlError())
    }
}
