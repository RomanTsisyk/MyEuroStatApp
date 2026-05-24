package eu.eurostat.feature.science.data

import eu.eurostat.feature.science.domain.ScienceDataPoint
import eu.eurostat.feature.science.domain.ScienceQuery

interface ScienceApiService {
    suspend fun fetch(query: ScienceQuery): List<ScienceDataPoint>
}
