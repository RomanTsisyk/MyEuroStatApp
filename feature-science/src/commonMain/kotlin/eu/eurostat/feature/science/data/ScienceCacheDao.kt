package eu.eurostat.feature.science.data

import eu.eurostat.feature.science.domain.ScienceDataPoint
import eu.eurostat.feature.science.domain.ScienceQuery

interface ScienceCacheDao {
    suspend fun query(query: ScienceQuery): List<ScienceDataPoint>
    suspend fun upsertAll(rows: List<ScienceDataPoint>, fetchedAt: Long)
    suspend fun oldestFetchedAt(query: ScienceQuery): Long?
}
