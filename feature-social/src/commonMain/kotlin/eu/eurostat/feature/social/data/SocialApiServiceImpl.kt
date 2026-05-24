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
 *  - `ilc_li02`     → `unit=PC, indic_il=LI_R_MD60, sex=T, age=TOTAL`      → povertyRate
 *  - `ilc_peps01`   → `unit=PC, sex=T, age=TOTAL` (no indic_il — dim absent)  → atRiskRate
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
            apiClient.fetchDataset(
                datasetCode = "ilc_li02",
                filters = baseFilter + mapOf(
                    "unit" to listOf("PC"),
                    "indic_il" to listOf("LI_R_MD60"),
                    "sex" to listOf("T"),
                    "age" to listOf("TOTAL"),
                ),
            )
        }
        val atRiskDeferred = async {
            // ilc_peps01 (AROPE) does not expose an `indic_il` dimension — the
            // indicator is implicit. Slice the remaining dims that DO exist on
            // this dataset (sex + age) so the mapper sees one cell per
            // (country, year) and doesn't last-cell-win across sub-populations.
            apiClient.fetchDataset(
                datasetCode = "ilc_peps01",
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
