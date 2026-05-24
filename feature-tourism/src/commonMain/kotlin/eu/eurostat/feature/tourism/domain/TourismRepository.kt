package eu.eurostat.feature.tourism.domain

import eu.eurostat.core.common.Result
import kotlinx.coroutines.flow.Flow

/**
 * Combined result emitted by the tourism repository on each observation tick.
 *
 * @param timeSeries yearly country data for the stacked-bar chart / headline.
 * @param heatmapCells monthly seasonality grid (from `tour_occ_nim`), rows=years,
 *        cols=months 1..12, values normalised to 0f..1f. Empty when unavailable.
 */
data class TourismData(
    val timeSeries: List<TourismTimeSeries>,
    val heatmapCells: List<List<Float>>,
)

interface TourismRepository {
    fun observe(query: TourismQuery): Flow<Result<TourismData>>
    suspend fun refresh(query: TourismQuery)
}

class GetTourismTimeSeriesUseCase(
    private val repository: TourismRepository,
) {
    fun observe(query: TourismQuery): Flow<Result<TourismData>> =
        repository.observe(query)

    suspend fun refresh(query: TourismQuery) = repository.refresh(query)
}
