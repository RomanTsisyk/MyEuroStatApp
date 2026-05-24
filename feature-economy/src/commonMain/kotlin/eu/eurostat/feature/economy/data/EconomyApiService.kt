package eu.eurostat.feature.economy.data

import eu.eurostat.feature.economy.domain.EconomyQuery
import eu.eurostat.feature.economy.domain.EconomyTimeSeries

interface EconomyApiService {
    suspend fun fetchEconomy(query: EconomyQuery): List<EconomyTimeSeries>
}
