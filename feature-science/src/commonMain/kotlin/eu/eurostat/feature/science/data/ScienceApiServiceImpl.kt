package eu.eurostat.feature.science.data

import eu.eurostat.core.network.EurostatApiClient
import eu.eurostat.feature.science.domain.ScienceDataPoint
import eu.eurostat.feature.science.domain.ScienceQuery
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ScienceApiServiceImpl(
    private val apiClient: EurostatApiClient,
) : ScienceApiService {

    override suspend fun fetch(query: ScienceQuery): List<ScienceDataPoint> = coroutineScope {
        val years = query.yearRange.map { it.toString() }
        val geoFilter = mapOf(
            "geo" to query.countryCodes,
            "time" to years,
        )

        val rdFilter = geoFilter + mapOf("unit" to listOf("PC_GDP"), "sectperf" to listOf("TOTAL"))
        val internetFilter = geoFilter + mapOf(
            "indic_is" to listOf("I_IU3"),
            "ind_type" to listOf("IND_TOTAL"),
            "unit" to listOf("PC_IND"),
        )
        val educFilter = geoFilter + mapOf(
            "isced11" to listOf("ED5-8"),
            "sex" to listOf("T"),
            "age" to listOf("Y25-64"),
            "unit" to listOf("PC"),
        )

        val rdDeferred = async { apiClient.fetchDataset("rd_e_gerdtot", rdFilter) }
        val internetDeferred = async { apiClient.fetchDataset("isoc_ci_ifp_iu", internetFilter) }
        val educDeferred = async { apiClient.fetchDataset("edat_lfse_03", educFilter) }

        val rdMap = ScienceCellMapper.mapRdSpend(rdDeferred.await())
        val internetMap = ScienceCellMapper.mapInternetUsage(internetDeferred.await())
        val educMap = ScienceCellMapper.mapTertiaryEduc(educDeferred.await())

        // Union of all (country, year) keys
        val allKeys = (rdMap.keys + internetMap.keys + educMap.keys).toSet()

        allKeys.map { (country, year) ->
            ScienceDataPoint(
                countryCode = country,
                year = year,
                rdSpendPctGdp = rdMap[country to year],
                internetUsagePct = internetMap[country to year],
                tertiaryEducPct = educMap[country to year],
            )
        }
    }
}
