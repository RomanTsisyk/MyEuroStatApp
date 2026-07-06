package eu.eurostat.core.network

import eu.eurostat.core.common.AppError
import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

/**
 * Tests the platform-independent contract of [toAppError].
 *
 * The Ktor-specific branches (timeouts, HTTP status, parse errors) require
 * constructing real Ktor exceptions and are covered in the client-level tests;
 * here we lock down the two branches that must behave identically on every
 * platform: the [CancellationException] re-throw and the [AppError.Unknown]
 * fallback.
 */
class ErrorMappingTest {

    @Test
    fun generic_throwable_maps_to_unknown_wrapping_the_original() {
        val boom = IllegalStateException("boom")
        val error = boom.toAppError()
        val unknown = assertIs<AppError.Unknown>(error)
        assertSame(boom, unknown.throwable)
    }

    @Test
    fun runtime_exception_maps_to_unknown() {
        assertIs<AppError.Unknown>(RuntimeException("x").toAppError())
    }

    @Test
    fun cancellation_exception_is_rethrown_not_swallowed() {
        // Structured concurrency must be preserved — a cancelled coroutine's
        // CancellationException must never become an AppError.
        assertFailsWith<CancellationException> {
            CancellationException("cancelled").toAppError()
        }
    }
}
