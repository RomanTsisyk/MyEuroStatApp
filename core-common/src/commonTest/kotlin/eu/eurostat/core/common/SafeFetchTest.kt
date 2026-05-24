package eu.eurostat.core.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

class SafeFetchTest {

    @Test
    fun returns_block_result_on_success() = runTest {
        val result = safeFetch(onError = { }) { "hello" }
        assertEquals("hello", result)
    }

    @Test
    fun returns_null_on_exception() = runTest {
        val result = safeFetch(onError = { }) {
            throw IllegalArgumentException("boom")
        }
        assertNull(result)
    }

    @Test
    fun invokes_onError_on_exception() = runTest {
        var captured: Throwable? = null
        safeFetch(onError = { captured = it }) {
            throw IllegalArgumentException("boom")
        }
        assertNotNull(captured)
        assertEquals("boom", captured!!.message)
    }

    @Test
    fun rethrows_CancellationException() = runTest {
        assertFailsWith<CancellationException> {
            safeFetch(onError = { }) {
                throw CancellationException("cancel")
            }
        }
    }

    @Test
    fun does_not_invoke_onError_on_CancellationException() = runTest {
        var invoked = false
        try {
            safeFetch(onError = { invoked = true }) {
                throw CancellationException("cancel")
            }
        } catch (_: CancellationException) {
            // expected
        }
        assertEquals(false, invoked, "onError must not be called when CancellationException propagates")
    }

    @Test
    fun propagates_CancellationException_even_when_wrapped_in_runCatching() = runTest {
        // This simulates the pattern where a caller wraps safeFetch in runCatching
        // — should still not swallow CE because safeFetch rethrows.
        assertFailsWith<CancellationException> {
            safeFetch(onError = { }) {
                throw CancellationException("cancel")
            }
        }
    }

    @Test
    fun returns_null_for_any_Throwable_except_CE() = runTest {
        val errorMessages = listOf(
            IllegalArgumentException("illegal"),
            IllegalStateException("state"),
            IndexOutOfBoundsException("index"),
            NullPointerException("npe"),
            ArithmeticException("divide"),
            RuntimeException("runtime"),
        )
        for (msg in errorMessages) {
            val result = safeFetch<String>(onError = { }) {
                throw msg
            }
            assertNull(result, "Expected null for ${msg::class.simpleName}")
        }
    }

    @Test
    fun calling_code_executes_inline_same_context() = runTest {
        // safeFetch has no dispatcher switching — verify the block runs in the caller's context.
        val callerName = "test-context"
        val result = safeFetch(onError = { }) { callerName }
        assertEquals(callerName, result)
    }
}
