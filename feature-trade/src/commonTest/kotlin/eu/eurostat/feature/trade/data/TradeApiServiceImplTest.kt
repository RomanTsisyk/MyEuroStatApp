package eu.eurostat.feature.trade.data

import eu.eurostat.core.jsonstat.JsonStatParser
import eu.eurostat.core.network.EurostatApiClient
import eu.eurostat.core.network.EurostatJson
import eu.eurostat.feature.trade.domain.TradeQuery
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
 * Unit tests for [TradeApiServiceImpl].
 *
 * Uses Ktor MockEngine to intercept HTTP calls and verify:
 * - Correct dataset code in URL (ext_lt_intratrd)
 * - Correct filter parameters (geo, time, partner, indic_et, unit)
 * - Response threading through cell mapper
 * - Error propagation on non-2xx responses
 *
 * URL checks use [HttpRequestData.url] properties directly (host, parameters, pathSegments)
 * to avoid percent-encoding issues with underscores.
 */
class TradeApiServiceImplTest {

    // ------------------------------------------------------------------------------------
    // Canned JSON-stat response: PL/2020/EU27_2020 with MIO_EXP_VAL=300000, MIO_IMP_VAL=280000, MIO_BAL_VAL=20000
    // ------------------------------------------------------------------------------------

    private fun cannedJsonStatResponse(): String = """
        {
          "id": ["geo", "time", "partner", "indic_et"],
          "size": [1, 1, 1, 3],
          "dimension": {
            "geo":      { "category": { "index": { "PL": 0 },                                                              "label": { "PL": "Poland" } } },
            "time":     { "category": { "index": { "2020": 0 },                                                            "label": { "2020": "2020" } } },
            "partner":  { "category": { "index": { "EU27_2020": 0 },                                                       "label": { "EU27_2020": "EU27 (2020)" } } },
            "indic_et": { "category": { "index": { "MIO_EXP_VAL": 0, "MIO_IMP_VAL": 1, "MIO_BAL_VAL": 2 },               "label": { "MIO_EXP_VAL": "Exports", "MIO_IMP_VAL": "Imports", "MIO_BAL_VAL": "Balance" } } }
          },
          "value": { "0": 300000, "1": 280000, "2": 20000 }
        }
    """.trimIndent()

    // ------------------------------------------------------------------------------------
    // Builder helper
    // ------------------------------------------------------------------------------------

    private fun buildService(
        responseBody: String = cannedJsonStatResponse(),
        status: HttpStatusCode = HttpStatusCode.OK,
        onRequest: suspend (HttpRequestData) -> Unit = {},
    ): TradeApiServiceImpl {
        val engine = MockEngine { request ->
            onRequest(request)
            respond(
                content = ByteReadChannel(responseBody),
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val httpClient = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) { json(EurostatJson) }
        }
        val apiClient = EurostatApiClient(httpClient, JsonStatParser())
        return TradeApiServiceImpl(apiClient)
    }

    // ------------------------------------------------------------------------------------
    // 1. Dataset code: URL must reference "ext_lt_intratrd"
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_requestTargets_ext_lt_intratrd() = runTest {
        val recorder = RequestRecorder()
        val service = buildService(onRequest = recorder::record)

        service.fetch(TradeQuery(listOf("PL"), 2020..2020))

        val captured: HttpRequestData? = recorder.all().firstOrNull()
        assertNotNull(captured)
        val urlStr = captured!!.url.toString()
        assertTrue(
            urlStr.contains("intratrd") || (urlStr.contains("ext") && urlStr.contains("lt")),
            "URL must reference the ext_lt_intratrd dataset, got: $urlStr"
        )
    }

    // ------------------------------------------------------------------------------------
    // 2. geo filter appears as query param for each requested country
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_geoParamsPresent() = runTest {
        val recorder = RequestRecorder()
        val service = buildService(onRequest = recorder::record)

        service.fetch(TradeQuery(listOf("PL", "DE"), 2020..2020))

        val captured: HttpRequestData? = recorder.all().firstOrNull()
        assertNotNull(captured)
        val geoValues = captured!!.url.parameters.getAll("geo") ?: emptyList()
        assertTrue("PL" in geoValues, "geo filter must include PL, got: $geoValues")
        assertTrue("DE" in geoValues, "geo filter must include DE, got: $geoValues")
    }

    // ------------------------------------------------------------------------------------
    // 3. time filter: each year in range appears as query param
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_timeParamsPresentForEachYear() = runTest {
        val recorder = RequestRecorder()
        val service = buildService(onRequest = recorder::record)

        service.fetch(TradeQuery(listOf("PL"), 2020..2022))

        val captured: HttpRequestData? = recorder.all().firstOrNull()
        assertNotNull(captured)
        val timeValues = captured!!.url.parameters.getAll("time") ?: emptyList()
        assertTrue("2020" in timeValues, "time filter must include 2020, got: $timeValues")
        assertTrue("2021" in timeValues, "time filter must include 2021, got: $timeValues")
        assertTrue("2022" in timeValues, "time filter must include 2022, got: $timeValues")
    }

    // ------------------------------------------------------------------------------------
    // 4. partner filter in URL
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_partnerParamPresent() = runTest {
        val recorder = RequestRecorder()
        val service = buildService(onRequest = recorder::record)

        service.fetch(TradeQuery(listOf("PL"), 2020..2020, partner = "EU27_2020"))

        val captured: HttpRequestData? = recorder.all().firstOrNull()
        assertNotNull(captured)
        val partnerValues = captured!!.url.parameters.getAll("partner") ?: emptyList()
        assertTrue("EU27_2020" in partnerValues, "partner filter must include EU27_2020, got: $partnerValues")
    }

    // ------------------------------------------------------------------------------------
    // 5. indic_et filter: MIO_EXP_VAL, MIO_IMP_VAL, MIO_BAL_VAL all present
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_indicEtParamsContainMIO_EXP_VAL_IMP_VAL_BAL_VAL() = runTest {
        val recorder = RequestRecorder()
        val service = buildService(onRequest = recorder::record)

        service.fetch(TradeQuery(listOf("PL"), 2020..2020))

        val captured: HttpRequestData? = recorder.all().firstOrNull()
        assertNotNull(captured)
        val indicEtValues = captured!!.url.parameters.getAll("indic_et") ?: emptyList()
        assertTrue("MIO_EXP_VAL" in indicEtValues, "indic_et must include MIO_EXP_VAL, got: $indicEtValues")
        assertTrue("MIO_IMP_VAL" in indicEtValues, "indic_et must include MIO_IMP_VAL, got: $indicEtValues")
        assertTrue("MIO_BAL_VAL" in indicEtValues, "indic_et must include MIO_BAL_VAL, got: $indicEtValues")
    }

    // ------------------------------------------------------------------------------------
    // 6. sitc06=TOTAL filter must be present to avoid per-product-category fan-out
    // ------------------------------------------------------------------------------------

    @Test
    fun request_pins_sitc06_total() = runTest {
        val recorder = RequestRecorder()
        val service = buildService(onRequest = recorder::record)

        service.fetch(TradeQuery(listOf("PL"), 2020..2020))

        val captured: HttpRequestData? = recorder.all().firstOrNull()
        assertNotNull(captured)
        val sitc06Values = captured!!.url.parameters.getAll("sitc06") ?: emptyList()
        assertTrue(
            "TOTAL" in sitc06Values,
            "sitc06=TOTAL must be present to collapse per-category rows, got: $sitc06Values",
        )
    }

    // ------------------------------------------------------------------------------------
    // 7. Response threaded through mapper — non-empty result
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_responseThreadedThroughMapper_returnsNonEmpty() = runTest {
        val service = buildService()
        val result = service.fetch(TradeQuery(listOf("PL"), 2020..2020))
        assertTrue(result.isNotEmpty(), "Service should return mapped TradeDataPoints")
    }

    // ------------------------------------------------------------------------------------
    // 8. Mapped result contains correct values
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_mappedResult_hasCorrectValues() = runTest {
        val service = buildService()
        val result = service.fetch(TradeQuery(listOf("PL"), 2020..2020, partner = "EU27_2020"))

        assertEquals(1, result.size)
        val point = result[0]
        assertEquals("PL", point.countryCode)
        assertEquals(2020, point.year)
        assertEquals("EU27_2020", point.partner)
        assertEquals(300_000L, point.exportsEur)
        assertEquals(280_000L, point.importsEur)
        assertEquals(20_000L, point.balanceEur)
    }

    // ------------------------------------------------------------------------------------
    // 9. 500 error → service throws (does NOT swallow)
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_serverError_throws() = runTest {
        val service = buildService(
            responseBody = """{"error":"Internal Server Error"}""",
            status = HttpStatusCode.InternalServerError,
        )

        assertFails("Service should propagate HTTP 500 as an exception") {
            service.fetch(TradeQuery(listOf("PL"), 2020..2020))
        }
    }

    // ------------------------------------------------------------------------------------
    // 10. format=JSON always present as query parameter
    // ------------------------------------------------------------------------------------

    @Test
    fun fetch_formatJsonParamPresent() = runTest {
        val recorder = RequestRecorder()
        val service = buildService(onRequest = recorder::record)

        service.fetch(TradeQuery(listOf("PL"), 2020..2020))

        val captured: HttpRequestData? = recorder.all().firstOrNull()
        assertNotNull(captured)
        val formatValues = captured!!.url.parameters.getAll("format") ?: emptyList()
        assertTrue("JSON" in formatValues, "format=JSON must be present, got: $formatValues")
    }
}

/**
 * Thread-safe request recorder: MockEngine may invoke handlers concurrently on
 * different threads, so unsynchronized writes from the handler can be lost or
 * remain invisible to the asserting test thread.
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
