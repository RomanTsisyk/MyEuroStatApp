package eu.eurostat.feature.environment.data

import eu.eurostat.core.jsonstat.JsonStatParser
import eu.eurostat.core.network.EurostatApiClient
import eu.eurostat.core.network.EurostatJson
import eu.eurostat.feature.environment.domain.EnvironmentQuery
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for [EnvironmentApiServiceImpl].
 *
 * The service makes 3 parallel requests:
 *   1. env_air_gge  — GHG emissions (primary; failure propagates)
 *   2. nrg_bal_c    — energy consumption (primary; failure propagates)
 *   3. sdg_13_10    — SDG-13 index (secondary; failure is swallowed via safeFetch)
 *
 * MockEngine routes by URL content. Filters are checked via request parameters.
 * URL parameter checks use `HttpRequestData.url.parameters` directly to avoid
 * percent-encoding issues with underscores.
 *
 * Verified filters per CLAUDE.md:
 *   env_air_gge  → airpol=GHG, src_crf=TOTX4_MEMO/CRF1A3/CRF1A2, unit=MIO_T
 *   nrg_bal_c    → siec=TOTAL, nrg_bal=FC_E/FC_TRA_E/FC_IND_E, unit=KTOE
 *   sdg_13_10    → unit=I90
 */
class EnvironmentApiServiceImplTest {

    // ------------------------------------------------------------------------------------
    // Canned JSON-stat responses
    // ------------------------------------------------------------------------------------

    /** Minimal valid GHG response: PL/2020/TOTX4_MEMO = 400 MIO_T */
    private fun ghgJsonStat(): String = """
        {
          "id": ["geo", "time", "src_crf", "airpol", "unit"],
          "size": [1, 1, 1, 1, 1],
          "dimension": {
            "geo":     { "category": { "index": { "PL": 0 }, "label": { "PL": "Poland" } } },
            "time":    { "category": { "index": { "2020": 0 }, "label": { "2020": "2020" } } },
            "src_crf": { "category": { "index": { "TOTX4_MEMO": 0 }, "label": { "TOTX4_MEMO": "Total" } } },
            "airpol":  { "category": { "index": { "GHG": 0 }, "label": { "GHG": "GHG" } } },
            "unit":    { "category": { "index": { "MIO_T": 0 }, "label": { "MIO_T": "Million tonnes" } } }
          },
          "value": { "0": 400 }
        }
    """.trimIndent()

    /** Minimal valid Energy response: PL/2020/FC_E = 100000 KTOE */
    private fun energyJsonStat(): String = """
        {
          "id": ["geo", "time", "nrg_bal", "siec", "unit"],
          "size": [1, 1, 1, 1, 1],
          "dimension": {
            "geo":     { "category": { "index": { "PL": 0 }, "label": { "PL": "Poland" } } },
            "time":    { "category": { "index": { "2020": 0 }, "label": { "2020": "2020" } } },
            "nrg_bal": { "category": { "index": { "FC_E": 0 }, "label": { "FC_E": "Final consumption" } } },
            "siec":    { "category": { "index": { "TOTAL": 0 }, "label": { "TOTAL": "Total" } } },
            "unit":    { "category": { "index": { "KTOE": 0 }, "label": { "KTOE": "Thousand TOE" } } }
          },
          "value": { "0": 100000 }
        }
    """.trimIndent()

    /** Minimal valid SDG response (empty values — secondary dataset) */
    private fun sdgJsonStat(): String = """
        {
          "id": ["geo", "time"],
          "size": [1, 1],
          "dimension": {
            "geo":  { "category": { "index": { "PL": 0 }, "label": { "PL": "Poland" } } },
            "time": { "category": { "index": { "2020": 0 }, "label": { "2020": "2020" } } }
          },
          "value": {}
        }
    """.trimIndent()

    // ------------------------------------------------------------------------------------
    // Builder helper: routes by URL content
    // ------------------------------------------------------------------------------------

    private fun buildService(
        ghgStatus: HttpStatusCode = HttpStatusCode.OK,
        energyStatus: HttpStatusCode = HttpStatusCode.OK,
        sdgStatus: HttpStatusCode = HttpStatusCode.OK,
        onRequest: (HttpRequestData) -> Unit = {},
    ): EnvironmentApiServiceImpl {
        val engine = MockEngine { request ->
            onRequest(request)
            val urlStr = request.url.toString()
            val (body, status) = when {
                urlStr.contains("nrg_bal") || urlStr.contains("nrg") ->
                    Pair(energyJsonStat(), energyStatus)
                urlStr.contains("sdg") ->
                    Pair(sdgJsonStat(), sdgStatus)
                else ->
                    // env_air_gge or anything else → GHG response
                    Pair(ghgJsonStat(), ghgStatus)
            }
            respond(
                content = ByteReadChannel(body),
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val httpClient = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) { json(EurostatJson) }
        }
        val apiClient = EurostatApiClient(httpClient, JsonStatParser())
        return EnvironmentApiServiceImpl(apiClient)
    }

    private val defaultQuery = EnvironmentQuery(listOf("PL"), 2020..2020)

    // ------------------------------------------------------------------------------------
    // 1. Service makes exactly 3 parallel requests (GHG + Energy + SDG)
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_makesExactlyThreeRequests() = runTest {
        val captured = mutableListOf<HttpRequestData>()
        val service = buildService(onRequest = { captured += it })

        service.fetch(defaultQuery)

        assertEquals(3, captured.size, "Service must make exactly 3 requests (env_air_gge + nrg_bal_c + sdg_13_10)")
    }

    // ------------------------------------------------------------------------------------
    // 2. GHG request targets env_air_gge dataset
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_ghgRequestTargets_env_air_gge() = runTest {
        val captured = mutableListOf<HttpRequestData>()
        val service = buildService(onRequest = { captured += it })

        service.fetch(defaultQuery)

        val ghgReq = captured.firstOrNull { req ->
            val urlStr = req.url.toString()
            !urlStr.contains("nrg") && !urlStr.contains("sdg")
        }
        assertNotNull(ghgReq, "env_air_gge request not found among: ${captured.map { it.url }}")
        val urlStr = ghgReq!!.url.toString()
        assertTrue(
            urlStr.contains("air") || urlStr.contains("gge") || urlStr.contains("env"),
            "GHG request URL must reference env_air_gge, got: $urlStr"
        )
    }

    // ------------------------------------------------------------------------------------
    // 3. GHG request has correct airpol=GHG filter
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_ghgRequest_airpolIsGHG() = runTest {
        val captured = mutableListOf<HttpRequestData>()
        val service = buildService(onRequest = { captured += it })

        service.fetch(defaultQuery)

        val ghgReq = captured.firstOrNull { req ->
            val urlStr = req.url.toString()
            !urlStr.contains("nrg") && !urlStr.contains("sdg")
        }
        assertNotNull(ghgReq, "GHG request not found")
        val airpolValues = ghgReq!!.url.parameters.getAll("airpol") ?: emptyList()
        assertTrue("GHG" in airpolValues, "airpol must include GHG, got: $airpolValues")
    }

    // ------------------------------------------------------------------------------------
    // 4. GHG request has all three sector codes: TOTX4_MEMO, CRF1A3, CRF1A2
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_ghgRequest_srcCrfContainsAllThreeSectorCodes() = runTest {
        val captured = mutableListOf<HttpRequestData>()
        val service = buildService(onRequest = { captured += it })

        service.fetch(defaultQuery)

        val ghgReq = captured.firstOrNull { req ->
            val urlStr = req.url.toString()
            !urlStr.contains("nrg") && !urlStr.contains("sdg")
        }
        assertNotNull(ghgReq, "GHG request not found")
        val srcCrfValues = ghgReq!!.url.parameters.getAll("src_crf") ?: emptyList()
        assertTrue("TOTX4_MEMO" in srcCrfValues, "src_crf must include TOTX4_MEMO, got: $srcCrfValues")
        assertTrue("CRF1A3" in srcCrfValues, "src_crf must include CRF1A3, got: $srcCrfValues")
        assertTrue("CRF1A2" in srcCrfValues, "src_crf must include CRF1A2, got: $srcCrfValues")
    }

    // ------------------------------------------------------------------------------------
    // 5. GHG request has unit=MIO_T filter
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_ghgRequest_unitIsMIO_T() = runTest {
        val captured = mutableListOf<HttpRequestData>()
        val service = buildService(onRequest = { captured += it })

        service.fetch(defaultQuery)

        val ghgReq = captured.firstOrNull { req ->
            val urlStr = req.url.toString()
            !urlStr.contains("nrg") && !urlStr.contains("sdg")
        }
        assertNotNull(ghgReq, "GHG request not found")
        val unitValues = ghgReq!!.url.parameters.getAll("unit") ?: emptyList()
        assertTrue("MIO_T" in unitValues, "unit must include MIO_T, got: $unitValues")
    }

    // ------------------------------------------------------------------------------------
    // 6. Energy request has siec=TOTAL, nrg_bal codes FC_E/FC_TRA_E/FC_IND_E, unit=KTOE
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_energyRequest_hasCorrectFilters() = runTest {
        val captured = mutableListOf<HttpRequestData>()
        val service = buildService(onRequest = { captured += it })

        service.fetch(defaultQuery)

        val energyReq = captured.firstOrNull { req -> req.url.toString().contains("nrg") }
        assertNotNull(energyReq, "nrg_bal_c request not found among: ${captured.map { it.url }}")

        val params = energyReq!!.url.parameters
        val siecValues = params.getAll("siec") ?: emptyList()
        assertTrue("TOTAL" in siecValues, "siec must include TOTAL, got: $siecValues")

        val nrgBalValues = params.getAll("nrg_bal") ?: emptyList()
        assertTrue("FC_E" in nrgBalValues, "nrg_bal must include FC_E, got: $nrgBalValues")
        assertTrue("FC_TRA_E" in nrgBalValues, "nrg_bal must include FC_TRA_E, got: $nrgBalValues")
        assertTrue("FC_IND_E" in nrgBalValues, "nrg_bal must include FC_IND_E, got: $nrgBalValues")

        val unitValues = params.getAll("unit") ?: emptyList()
        assertTrue("KTOE" in unitValues, "unit must include KTOE, got: $unitValues")
    }

    // ------------------------------------------------------------------------------------
    // 7. SDG request has unit=I90 filter
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_sdgRequest_unitIsI90() = runTest {
        val captured = mutableListOf<HttpRequestData>()
        val service = buildService(onRequest = { captured += it })

        service.fetch(defaultQuery)

        val sdgReq = captured.firstOrNull { req -> req.url.toString().contains("sdg") }
        assertNotNull(sdgReq, "sdg_13_10 request not found among: ${captured.map { it.url }}")
        val unitValues = sdgReq!!.url.parameters.getAll("unit") ?: emptyList()
        assertTrue("I90" in unitValues, "SDG unit must be I90, got: $unitValues")
    }

    // ------------------------------------------------------------------------------------
    // 8. geo filter appears as query param for each requested country
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_ghgRequest_geoParamsPresent() = runTest {
        val captured = mutableListOf<HttpRequestData>()
        val service = buildService(onRequest = { captured += it })
        val query = EnvironmentQuery(listOf("PL", "DE"), 2020..2020)

        service.fetch(query)

        val ghgReq = captured.firstOrNull { req ->
            val urlStr = req.url.toString()
            !urlStr.contains("nrg") && !urlStr.contains("sdg")
        }
        assertNotNull(ghgReq, "GHG request not found")
        val geoValues = ghgReq!!.url.parameters.getAll("geo") ?: emptyList()
        assertTrue("PL" in geoValues, "geo filter must include PL, got: $geoValues")
        assertTrue("DE" in geoValues, "geo filter must include DE, got: $geoValues")
    }

    // ------------------------------------------------------------------------------------
    // 9. time filter: each year in range appears as query param
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_ghgRequest_timeParamsPresentForEachYear() = runTest {
        val captured = mutableListOf<HttpRequestData>()
        val service = buildService(onRequest = { captured += it })
        val query = EnvironmentQuery(listOf("PL"), 2020..2022)

        service.fetch(query)

        val ghgReq = captured.firstOrNull { req ->
            val urlStr = req.url.toString()
            !urlStr.contains("nrg") && !urlStr.contains("sdg")
        }
        assertNotNull(ghgReq, "GHG request not found")
        val timeValues = ghgReq!!.url.parameters.getAll("time") ?: emptyList()
        assertTrue("2020" in timeValues, "time filter must include 2020, got: $timeValues")
        assertTrue("2021" in timeValues, "time filter must include 2021, got: $timeValues")
        assertTrue("2022" in timeValues, "time filter must include 2022, got: $timeValues")
    }

    // ------------------------------------------------------------------------------------
    // 10. SDG failure is swallowed: GHG + Energy data still flows through
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_sdgFailure_airAndEnergyDataStillFlows() = runTest {
        val service = buildService(sdgStatus = HttpStatusCode.InternalServerError)

        // Should NOT throw — SDG failure is swallowed by safeFetch
        val result = service.fetch(defaultQuery)

        // GHG + Energy data should still produce time series
        assertTrue(result.isNotEmpty(), "Result must be non-empty even when SDG fails")
        val ts = result[0]
        val hasGhgOrEnergy = ts.points.any { it.ghgMtCo2eq != null || it.energyKtoe != null }
        assertTrue(hasGhgOrEnergy, "Time series must contain GHG/Energy data even when SDG fails")
    }

    // ------------------------------------------------------------------------------------
    // 11. GHG primary failure propagates (not swallowed)
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_ghgPrimaryFailure_throws() = runTest {
        val service = buildService(ghgStatus = HttpStatusCode.InternalServerError)

        assertFails("GHG primary failure must propagate as exception") {
            service.fetch(defaultQuery)
        }
    }

    // ------------------------------------------------------------------------------------
    // 12. Energy primary failure propagates (not swallowed)
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_energyPrimaryFailure_throws() = runTest {
        val service = buildService(energyStatus = HttpStatusCode.InternalServerError)

        assertFails("Energy primary failure must propagate as exception") {
            service.fetch(defaultQuery)
        }
    }

    // ------------------------------------------------------------------------------------
    // 13. format=JSON always present as query parameter
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_formatJsonParamPresent() = runTest {
        val captured = mutableListOf<HttpRequestData>()
        val service = buildService(onRequest = { captured += it })

        service.fetch(defaultQuery)

        assertNotNull(captured.firstOrNull())
        val ghgReq = captured.first { req ->
            val urlStr = req.url.toString()
            !urlStr.contains("nrg") && !urlStr.contains("sdg")
        }
        val formatValues = ghgReq.url.parameters.getAll("format") ?: emptyList()
        assertTrue("JSON" in formatValues, "format=JSON must be present, got: $formatValues")
    }

    // ------------------------------------------------------------------------------------
    // 14. Response threaded through mapper — non-empty result from primary datasets
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_responseThreadedThroughMapper_returnsNonEmpty() = runTest {
        val service = buildService()
        val result = service.fetch(defaultQuery)
        assertTrue(result.isNotEmpty(), "Service should return mapped EnvironmentTimeSeries")
    }

    // ------------------------------------------------------------------------------------
    // 15. Mapped result contains correct country code from query
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_mappedResult_containsCountryFromQuery() = runTest {
        val service = buildService()
        val result = service.fetch(defaultQuery)
        assertEquals("PL", result[0].countryCode)
    }
}
