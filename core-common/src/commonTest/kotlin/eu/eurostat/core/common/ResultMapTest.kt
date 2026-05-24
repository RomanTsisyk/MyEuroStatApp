package eu.eurostat.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ResultMapTest {
    @Test fun map_success_transforms_value() {
        val r: Result<Int> = Result.Success(2, isStale = false)
        val mapped = r.map { it * 10 }
        val s = assertIs<Result.Success<Int>>(mapped)
        assertEquals(20, s.data)
        assertEquals(false, s.isStale)
    }

    @Test fun map_loading_stays_loading() {
        val r: Result<Int> = Result.Loading
        assertIs<Result.Loading>(r.map { it * 10 })
    }

    @Test fun map_error_propagates() {
        val err = AppError.NoNetwork
        val r: Result<Int> = Result.Error(err)
        val e = assertIs<Result.Error>(r.map { it * 10 })
        assertEquals(err, e.cause)
    }
}
