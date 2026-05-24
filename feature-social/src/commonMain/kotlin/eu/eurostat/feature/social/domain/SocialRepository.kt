package eu.eurostat.feature.social.domain

import eu.eurostat.core.common.Result
import kotlinx.coroutines.flow.Flow

interface SocialRepository {
    fun observe(query: SocialQuery): Flow<Result<List<SocialTimeSeries>>>
    suspend fun refresh(query: SocialQuery)
}

class GetSocialTimeSeriesUseCase(
    private val repository: SocialRepository,
) {
    fun observe(query: SocialQuery): Flow<Result<List<SocialTimeSeries>>> =
        repository.observe(query)

    suspend fun refresh(query: SocialQuery) = repository.refresh(query)
}
