package eu.eurostat.feature.economy.data

import eu.eurostat.core.jsonstat.JsonStatParser
import eu.eurostat.core.network.EurostatApiClient
import eu.eurostat.core.network.EurostatJson
import eu.eurostat.feature.economy.domain.EconomyQuery
import eu.eurostat.feature.economy.domain.EconomyUnit
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for [EconomyApiServiceImpl].
 *
 * The service makes 3 parallel requests:
 *   1. nama_10_gdp   — primary GDP dataset
 *   2. prc_hicp_aind — HICP inflation index (secondary, failures swallowed)
 *   3. gov_10dd_edpt1 — government deficit (secondary, failures swallowed)
 *
 * MockEngine routes by URL content. Secondary datasets return minimal valid JSON-stat.
 * URL checks use [HttpRequestData.url] parameters directly to avoid encoding issues.
 */
class EconomyApiServiceImplTest {

    // ------------------------------------------------------------------------------------
    // Canned JSON-stat responses
    // ------------------------------------------------------------------------------------

    /** Primary GDP response: PL/2020/B1GQ/CP_MEUR = 523000 */
    private fun gdpJsonStat(): String = """
        {
          "id": ["geo", "time", "na_item", "unit"],
          "size": [1, 1, 1, 1],
          "dimension": {
            "geo":     { "category": { "index": { "PL": 0 }, "label": { "PL": "Poland" } } },
            "time":    { "category": { "index": { "2020": 0 }, "label": { "2020": "2020" } } },
            "na_item": { "category": { "index": { "B1GQ": 0 }, "label": { "B1GQ": "GDP" } } },
            "unit":    { "category": { "index": { "CP_MEUR": 0 }, "label": { "CP_MEUR": "Mill euro" } } }
          },
          "value": { "0": 523000 }
        }
    """.trimIndent()

    /** Minimal valid HICP response (empty values — secondary dataset) */
    private fun hicpJsonStat(): String = """
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

    /** Minimal valid deficit response (empty values — secondary dataset) */
    private fun deficitJsonStat(): String = """
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
        gdpStatus: HttpStatusCode = HttpStatusCode.OK,
        onRequest: suspend (HttpRequestData) -> Unit = {},
    ): EconomyApiServiceImpl {
        val engine = MockEngine { request ->
            onRequest(request)
            val urlStr = request.url.toString()
            // Route: GDP is primary, others are secondary
            val (body, status) = when {
                urlStr.contains("hicp") || urlStr.contains("prc") -> Pair(hicpJsonStat(), HttpStatusCode.OK)
                urlStr.contains("gov") || urlStr.contains("edpt") -> Pair(deficitJsonStat(), HttpStatusCode.OK)
                else -> Pair(gdpJsonStat(), gdpStatus) // nama_10_gdp or anything else → GDP response
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
        return EconomyApiServiceImpl(apiClient)
    }

    // ------------------------------------------------------------------------------------
    // 1. GDP request targets nama_10_gdp
    // ------------------------------------------------------------------------------------

    @Test
    fun fetchEconomy_gdpRequestTargets_nama_10_gdp() = runTest {
        val recorder = RequestRecorder()
        val service = buildService(onRequest = recorder::record)

        service.fetchEconomy(EconomyQuery(listOf("PL"), 2020..2020))

        val captured = recorder.all()
        assertNotNull(captured.firstOrNull())
        val gdpReq = captured.firstOrNull { req ->
            val urlStr = req.url.toString()
            !urlStr.contains("hicp") && !urlStr.contains("prc") &&
            !urlStr.contains("gov") && !urlStr.contains("edpt")
        }
        assertNotNull(gdpReq, "GDP request (nama_10_gdp) not found among: ${captured.map { it.url }}")
        val urlStr = gdpReq!!.url.toString()
        assertTrue(
            urlStr.contains("nama") || urlStr.contains("gdp"),
            "GDP request URL must reference nama_10_gdp, got: $urlStr"
        )
    }

    // ------------------------------------------------------------------------------------
    // 2. geo filter present in GDP request
    // ------------------------------------------------------------------------------------

    @Test
    fun fetchEconomy_gdpRequest_geoParamsPresent() = runTest {
        val recorder = RequestRecorder()
        val service = buildService(onRequest = recorder::record)

        service.fetchEconomy(EconomyQuery(listOf("PL", "DE"), 2020..2020))

        val captured = recorder.all()
        // Find GDP request (primary — not HICP or deficit)
        val gdpReq = captured.firstOrNull { req ->
            val urlStr = req.url.toString()
            !urlStr.contains("hicp") && !urlStr.contains("prc") &&
            !urlStr.contains("gov") && !urlStr.contains("edpt")
        }
        assertNotNull(gdpReq, "GDP request not found")
        val geoValues = gdpReq!!.url.parameters.getAll("geo") ?: emptyList()
        assertTrue("PL" in geoValues, "geo filter must include PL")
        assertTrue("DE" in geoValues, "geo filter must include DE")
    }

    // ------------------------------------------------------------------------------------
    // 3. time filter present in GDP request
    // ------------------------------------------------------------------------------------

    @Test
    fun fetchEconomy_gdpRequest_timeParamsPresentForEachYear() = runTest {
        val recorder = RequestRecorder()
        val service = buildService(onRequest = recorder::record)

        service.fetchEconomy(EconomyQuery(listOf("PL"), 2020..2022))

        val captured = recorder.all()
        val gdpReq = captured.firstOrNull { req ->
            val urlStr = req.url.toString()
            !urlStr.contains("hicp") && !urlStr.contains("prc") &&
            !urlStr.contains("gov") && !urlStr.contains("edpt")
        }
        assertNotNull(gdpReq, "GDP request not found")
        val timeValues = gdpReq!!.url.parameters.getAll("time") ?: emptyList()
        assertTrue("2020" in timeValues)
        assertTrue("2021" in timeValues)
        assertTrue("2022" in timeValues)
    }

    // ------------------------------------------------------------------------------------
    // 4. na_item=B1GQ filter present in GDP request
    // ------------------------------------------------------------------------------------

    @Test
    fun fetchEconomy_gdpRequest_naItemIsB1GQ() = runTest {
        val recorder = RequestRecorder()
        val service = buildService(onRequest = recorder::record)

        service.fetchEconomy(EconomyQuery(listOf("PL"), 2020..2020))

        val captured = recorder.all()
        val gdpReq = captured.firstOrNull { req ->
            val urlStr = req.url.toString()
            !urlStr.contains("hicp") && !urlStr.contains("prc") &&
            !urlStr.contains("gov") && !urlStr.contains("edpt")
        }
        assertNotNull(gdpReq, "GDP request not found")
        val naItemValues = gdpReq!!.url.parameters.getAll("na_item") ?: emptyList()
        assertTrue("B1GQ" in naItemValues, "na_item must be B1GQ, got: $naItemValues")
    }

    // ------------------------------------------------------------------------------------
    // 5. unit filter in GDP request reflects query unit
    // ------------------------------------------------------------------------------------

    @Test
    fun fetchEconomy_gdpRequest_unitParamMatchesQueryUnit() = runTest {
        val recorder = RequestRecorder()
        val service = buildService(onRequest = recorder::record)

        service.fetchEconomy(EconomyQuery(listOf("PL"), 2020..2020, unit = EconomyUnit.CP_MEUR))

        val captured = recorder.all()
        val gdpReq = captured.firstOrNull { req ->
            val urlStr = req.url.toString()
            !urlStr.contains("hicp") && !urlStr.contains("prc") &&
            !urlStr.contains("gov") && !urlStr.contains("edpt")
        }
        assertNotNull(gdpReq, "GDP request not found")
        val unitValues = gdpReq!!.url.parameters.getAll("unit") ?: emptyList()
        assertTrue("CP_MEUR" in unitValues, "unit must be CP_MEUR, got: $unitValues")
    }

    // ------------------------------------------------------------------------------------
    // 6. Service makes 3 parallel requests total (GDP + HICP + deficit)
    // ------------------------------------------------------------------------------------

    @Test
    fun fetchEconomy_makeThreeParallelRequests() = runTest {
        val recorder = RequestRecorder()
        val service = buildService(onRequest = recorder::record)

        service.fetchEconomy(EconomyQuery(listOf("PL"), 2020..2020))

        val captured = recorder.all()
        assertEquals(3, captured.size, "Service must make exactly 3 requests (GDP + HICP + deficit)")
    }

    // ------------------------------------------------------------------------------------
    // 7. Response threaded through mapper — non-empty result from GDP data
    // ------------------------------------------------------------------------------------

    @Test
    fun fetchEconomy_responseThreadedThroughMapper_returnsNonEmpty() = runTest {
        val service = buildService()
        val result = service.fetchEconomy(EconomyQuery(listOf("PL"), 2020..2020))
        assertTrue(result.isNotEmpty(), "Service should return mapped EconomyTimeSeries")
    }

    // ------------------------------------------------------------------------------------
    // 8. Mapped result contains correct country, year and gdpEur from primary dataset
    // ------------------------------------------------------------------------------------

    @Test
    fun fetchEconomy_mappedResult_hasCorrectGdpValues() = runTest {
        val service = buildService()
        val result = service.fetchEconomy(EconomyQuery(listOf("PL"), 2020..2020))

        assertEquals(1, result.size)
        val ts = result[0]
        assertEquals("PL", ts.countryCode)
        assertEquals(1, ts.points.size)
        val point = ts.points[0]
        assertEquals(2020, point.year)
        assertEquals(523_000L, point.gdpEur)
    }

    // ------------------------------------------------------------------------------------
    // 9. Primary GDP failure → service throws (secondary failures are swallowed, primary is not)
    // ------------------------------------------------------------------------------------

    @Test
    fun fetchEconomy_primaryDatasetError_throws() = runTest {
        val engine = MockEngine { request ->
            // All requests fail with 500
            respond(
                content = ByteReadChannel("""{"error":"Internal Server Error"}"""),
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val httpClient = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) { json(EurostatJson) }
        }
        val apiClient = EurostatApiClient(httpClient, JsonStatParser())
        val service = EconomyApiServiceImpl(apiClient)

        assertFails("Primary GDP 500 should propagate as exception") {
            service.fetchEconomy(EconomyQuery(listOf("PL"), 2020..2020))
        }
    }

    // ------------------------------------------------------------------------------------
    // 10. Merged time series carries country code from query
    // ------------------------------------------------------------------------------------

    @Test
    fun fetchEconomy_returnsCountryFromQuery() = runTest {
        val service = buildService()
        val result = service.fetchEconomy(EconomyQuery(listOf("PL"), 2020..2020, EconomyUnit.CP_MEUR))
        assertEquals("PL", result[0].countryCode)
    }

    // ------------------------------------------------------------------------------------
    // 11. HICP request pins coicop=CP00 (NOT I15) and unit=INX_A_AVG
    //     CLAUDE.md flags coicop=I15 as a known regression — this test guards against it.
    // ------------------------------------------------------------------------------------

    @Test
    fun hicp_request_pins_coicop_cp00_not_i15() = runTest {
        val recorder = RequestRecorder()
        val service = buildService(onRequest = recorder::record)

        service.fetchEconomy(EconomyQuery(listOf("PL"), 2020..2020))

        val captured = recorder.all()
        val hicpReq = captured.firstOrNull { req ->
            val urlStr = req.url.toString()
            urlStr.contains("hicp") || urlStr.contains("prc")
        }
        assertNotNull(hicpReq, "HICP request (prc_hicp_aind) not found among: ${captured.map { it.url }}")
        val coicopValues = hicpReq!!.url.parameters.getAll("coicop") ?: emptyList()
        assertTrue("CP00" in coicopValues, "coicop must be CP00, got: $coicopValues")
        assertTrue("I15" !in coicopValues, "coicop must NOT be I15 (known regression), got: $coicopValues")

        val unitValues = hicpReq.url.parameters.getAll("unit") ?: emptyList()
        assertTrue("INX_A_AVG" in unitValues, "unit must be INX_A_AVG, got: $unitValues")
    }

    // ------------------------------------------------------------------------------------
    // 12. Deficit request pins sector=S13 and na_item=B9
    // ------------------------------------------------------------------------------------

    @Test
    fun deficit_request_pins_sector_s13_na_item_b9() = runTest {
        val recorder = RequestRecorder()
        val service = buildService(onRequest = recorder::record)

        service.fetchEconomy(EconomyQuery(listOf("PL"), 2020..2020))

        val captured = recorder.all()
        val deficitReq = captured.firstOrNull { req ->
            val urlStr = req.url.toString()
            urlStr.contains("gov") || urlStr.contains("edpt")
        }
        assertNotNull(deficitReq, "Deficit request (gov_10dd_edpt1) not found among: ${captured.map { it.url }}")
        val sectorValues = deficitReq!!.url.parameters.getAll("sector") ?: emptyList()
        assertTrue("S13" in sectorValues, "sector must be S13, got: $sectorValues")

        val naItemValues = deficitReq.url.parameters.getAll("na_item") ?: emptyList()
        assertTrue("B9" in naItemValues, "na_item must be B9, got: $naItemValues")
    }
}

/**
 * Thread-safe request recorder: MockEngine may invoke handlers concurrently on
 * different threads (the service fires 3 parallel requests), so unsynchronized
 * appends to a plain list can lose elements.
 */
private class RequestRecorder {
    private val mutex = Mutex()
    private val requests = mutableListOf<HttpRequestData>()

    /** Records one intercepted request under the mutex. */
    suspend fun record(request: HttpRequestData) {
        mutex.withLock { requests += request }
    }

    /** Returns a snapshot of all recorded requests. */
    suspend fun all(): List<HttpRequestData> = mutex.withLock { requests.toList() }
}
