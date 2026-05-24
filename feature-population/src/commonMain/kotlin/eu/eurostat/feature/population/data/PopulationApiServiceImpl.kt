package eu.eurostat.feature.population.data

import eu.eurostat.core.network.EurostatApiClient
import eu.eurostat.feature.population.domain.PopulationData
import eu.eurostat.feature.population.domain.PopulationQuery

class PopulationApiServiceImpl(
    private val eurostatApiClient: EurostatApiClient,
) : PopulationApiService {

    override suspend fun fetchPopulation(query: PopulationQuery): PopulationData {
        val filters = buildFilters(query)
        // demo_pjangroup (5-year age groups) supports the cohort codes the
        // pyramid needs. demo_pjan only exposes per-year codes (Y0..Y99) so
        // it cannot serve the pyramid hero — confirmed against the live
        // metadata endpoint. demo_pjangroup also serves the `age=TOTAL`
        // headline we need for the time series, so one dataset covers both.
        val cells = eurostatApiClient.fetchDataset("demo_pjangroup", filters)
        val timeSeries = PopulationCellMapper.map(cells)
        val snapshots = if (query.includeCohorts) {
            PopulationCellMapper.mapToSnapshots(cells)
        } else {
            emptyMap()
        }
        return PopulationData(timeSeries = timeSeries, snapshots = snapshots)
    }

    private fun buildFilters(query: PopulationQuery): Map<String, List<String>> {
        val ageFilter = if (query.includeCohorts) {
            listOf("TOTAL") + COHORT_CODES
        } else {
            listOf("TOTAL")
        }
        return mapOf(
            "geo" to query.countryCodes,
            "time" to query.yearRange.map { it.toString() },
            "sex" to listOf("T", "M", "F"),
            "age" to ageFilter,
        )
    }

    private companion object {
        /** Eurostat demo_pjan 5-year cohort codes, youngest → oldest. */
        val COHORT_CODES = listOf(
            "Y_LT5", "Y5-9", "Y10-14", "Y15-19", "Y20-24",
            "Y25-29", "Y30-34", "Y35-39", "Y40-44", "Y45-49",
            "Y50-54", "Y55-59", "Y60-64", "Y65-69", "Y70-74",
            "Y75-79", "Y80-84", "Y_GE85",
        )
    }
}
