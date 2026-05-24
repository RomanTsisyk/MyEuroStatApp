package eu.eurostat.feature.environment.data

import eu.eurostat.feature.environment.domain.EnvironmentQuery
import eu.eurostat.feature.environment.domain.EnvironmentTimeSeries

/**
 * Fake implementation of [EnvironmentApiService] for tests.
 *
 * Configure [willReturn] to set the data to return, or [throwable] to simulate failures.
 * Use [assertNotNull] on captured fields rather than `!!` to satisfy the type checker.
 */
class FakeEnvironmentApiService : EnvironmentApiService {
    var willReturn: List<EnvironmentTimeSeries> = emptyList()
    var throwable: Throwable? = null
    var callCount: Int = 0
    var lastQuery: EnvironmentQuery? = null

    override suspend fun fetch(query: EnvironmentQuery): List<EnvironmentTimeSeries> {
        callCount++
        lastQuery = query
        throwable?.let { throw it }
        return willReturn
    }
}
