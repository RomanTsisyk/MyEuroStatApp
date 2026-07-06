package eu.eurostat.feature.social.data

import eu.eurostat.core.network.EurostatApiClient
import eu.eurostat.feature.social.domain.SocialDataPoint
import eu.eurostat.feature.social.domain.SocialQuery
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Fetches the three Social-module datasets in parallel and merges them by
 * (country, year) into a flat list of [SocialDataPoint]s.
 *
 * Dataset filters follow CLAUDE.md exactly:
 *  - `ilc_li02`     → `unit=PC, statinfo=MED_EI, rskpovth=B_60, sex=T, age=TOTAL` → povertyRate
 *  - `ilc_peps01n`  → `unit=PC, sex=T, age=TOTAL` (new AROPE def; old ilc_peps01 frozen @2020) → atRiskRate
 *  - `hlth_silc_01` → `levels=VGOOD, sex=T, age=Y_GE16, wstatus=POP, unit=PC` → healthSatisfaction
 */
class SocialApiServiceImpl(
    private val apiClient: EurostatApiClient,
) : SocialApiService {

    override suspend fun fetch(query: SocialQuery): List<SocialDataPoint> = coroutineScope {
        val years = query.yearRange.map { it.toString() }
        val baseFilter = mapOf(
            "geo" to query.countryCodes,
            "time" to years,
        )

        val povertyDeferred = async {
            // Eurostat restructured ilc_li02: the old `indic_il` dimension is
            // gone (pinning it now returns HTTP 400). The at-risk-of-poverty
            // rate is now sliced as statinfo=MED_EI (median equivalised income)
            // + rskpovth=B_60 (below 60% of the median). Both must be pinned —
            // statinfo has 2 categories and rskpovth has 8 — else the mapper
            // last-cell-wins across thresholds.
            apiClient.fetchDataset(
                datasetCode = "ilc_li02",
                filters = baseFilter + mapOf(
                    "unit" to listOf("PC"),
                    "statinfo" to listOf("MED_EI"),
                    "rskpovth" to listOf("B_60"),
                    "sex" to listOf("T"),
                    "age" to listOf("TOTAL"),
                ),
            )
        }
        val atRiskDeferred = async {
            // ilc_peps01n is the new AROPE definition (the legacy ilc_peps01 is
            // frozen at 2020 and returns an empty value{} for later years).
            // Like the old dataset it exposes no `indic_il` dimension — the
            // indicator is implicit — so slice only the remaining dims (sex +
            // age) so the mapper sees one cell per (country, year) and doesn't
            // last-cell-win across sub-populations.
            apiClient.fetchDataset(
                datasetCode = "ilc_peps01n",
                filters = baseFilter + mapOf(
                    "unit" to listOf("PC"),
                    "sex" to listOf("T"),
                    "age" to listOf("TOTAL"),
                ),
            )
        }
        val healthDeferred = async {
            apiClient.fetchDataset(
                datasetCode = "hlth_silc_01",
                filters = baseFilter + mapOf(
                    "unit" to listOf("PC"),
                    "levels" to listOf("VGOOD"),
                    "sex" to listOf("T"),
                    "age" to listOf("Y_GE16"),
                    "wstatus" to listOf("POP"),
                ),
            )
        }

        val povertyMap = SocialCellMapper.mapToValueMap(povertyDeferred.await())
        val atRiskMap = SocialCellMapper.mapToValueMap(atRiskDeferred.await())
        val healthMap = SocialCellMapper.mapHealthToValueMap(healthDeferred.await())

        // Union of all (country, year) keys across the three series.
        val allKeys = povertyMap.keys + atRiskMap.keys + healthMap.keys

        allKeys.map { (country, year) ->
            SocialDataPoint(
                countryCode = country,
                year = year,
                povertyRate = povertyMap[country to year],
                atRiskRate = atRiskMap[country to year],
                healthSatisfaction = healthMap[country to year],
            )
        }
    }
}
