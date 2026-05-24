package eu.eurostat.feature.population.data

import eu.eurostat.core.jsonstat.JsonStatParser
import eu.eurostat.core.network.EurostatApiClient
import eu.eurostat.core.network.EurostatJson
import eu.eurostat.feature.population.domain.PopulationQuery
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFails

/**
 * Unit tests for [PopulationApiServiceImpl].
 *
 * Uses Ktor MockEngine to intercept HTTP calls and assert:
 * - Correct dataset code in URL (demo_pjan)
 * - Correct filter parameters (geo, time, sex, age)
 * - Correct threading of response through cell mapper
 * - Error propagation on non-2xx responses
 *
 * Note: URL checks use [HttpRequestData.url] properties directly (parameters, pathSegments)
 * rather than [toString] to avoid percent-encoding issues with underscores.
 */
class PopulationApiServiceImplTest {

    // ------------------------------------------------------------------------------------
    // Canned JSON-stat response helper
    // ------------------------------------------------------------------------------------

    /**
     * Builds a minimal JSON-stat 2.0 payload with one observation:
     * PL/2020/T/TOTAL = 38000000.
     *
     * The parser reads category.index keys as dimension codes, matching the mapper's expectations.
     */
    private fun cannedJsonStatResponse(): String = """
        {
          "id": ["geo", "time", "sex", "age"],
          "size": [1, 1, 3, 1],
          "dimension": {
            "geo":  { "category": { "index": { "PL": 0 },                    "label": { "PL": "Poland" } } },
            "time": { "category": { "index": { "2020": 0 },                  "label": { "2020": "2020" } } },
            "sex":  { "category": { "index": { "T": 0, "M": 1, "F": 2 },    "label": { "T": "Total", "M": "Males", "F": "Females" } } },
            "age":  { "category": { "index": { "TOTAL": 0 },                 "label": { "TOTAL": "Total" } } }
          },
          "value": { "0": 38000000, "1": 18000000, "2": 20000000 }
        }
    """.trimIndent()

    // ------------------------------------------------------------------------------------
    // Builder helper: creates a MockEngine + HttpClient + EurostatApiClient
    // ------------------------------------------------------------------------------------

    private fun buildService(
        responseBody: String = cannedJsonStatResponse(),
        status: HttpStatusCode = HttpStatusCode.OK,
        onRequest: (HttpRequestData) -> Unit = {},
    ): PopulationApiServiceImpl {
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
        return PopulationApiServiceImpl(apiClient)
    }

    // ------------------------------------------------------------------------------------
    // 1. Dataset code: URL must contain "demo_pjan"
    // ------------------------------------------------------------------------------------

    @Test
    fun fetchPopulation_requestTargets_demo_pjan() = runTest {
        var captured: HttpRequestData? = null
        val service = buildService(onRequest = { captured = it })

        service.fetchPopulation(PopulationQuery(listOf("PL"), 2020..2020))

        assertNotNull(captured)
        val urlStr = captured!!.url.toString()
        assertTrue(
            urlStr.contains("demo") && urlStr.contains("pjan"),
            "URL must reference the demo_pjan dataset, got: $urlStr"
        )
    }

    // ------------------------------------------------------------------------------------
    // 2. geo filter appears in URL for each requested country
    // ------------------------------------------------------------------------------------

    @Test
    fun fetchPopulation_geoParamsPresent() = runTest {
        var captured: HttpRequestData? = null
        val service = buildService(onRequest = { captured = it })

        service.fetchPopulation(PopulationQuery(listOf("PL", "DE"), 2020..2020))

        assertNotNull(captured)
        val geoValues = captured!!.url.parameters.getAll("geo") ?: emptyList()
        assertTrue("PL" in geoValues, "geo filter must include PL, got: $geoValues")
        assertTrue("DE" in geoValues, "geo filter must include DE, got: $geoValues")
    }

    // ------------------------------------------------------------------------------------
    // 3. time filter: each year in range appears
    // ------------------------------------------------------------------------------------

    @Test
    fun fetchPopulation_timeParamsPresentForEachYear() = runTest {
        var captured: HttpRequestData? = null
        val service = buildService(onRequest = { captured = it })

        service.fetchPopulation(PopulationQuery(listOf("PL"), 2020..2022))

        assertNotNull(captured)
        val timeValues = captured!!.url.parameters.getAll("time") ?: emptyList()
        assertTrue("2020" in timeValues, "time filter must include 2020, got: $timeValues")
        assertTrue("2021" in timeValues, "time filter must include 2021, got: $timeValues")
        assertTrue("2022" in timeValues, "time filter must include 2022, got: $timeValues")
    }

    // ------------------------------------------------------------------------------------
    // 4. sex filter: T, M, F all present
    // ------------------------------------------------------------------------------------

    @Test
    fun fetchPopulation_sexParamsAreT_M_F() = runTest {
        var captured: HttpRequestData? = null
        val service = buildService(onRequest = { captured = it })

        service.fetchPopulation(PopulationQuery(listOf("PL"), 2020..2020))

        assertNotNull(captured)
        val sexValues = captured!!.url.parameters.getAll("sex") ?: emptyList()
        assertTrue("T" in sexValues, "sex filter must include T, got: $sexValues")
        assertTrue("M" in sexValues, "sex filter must include M, got: $sexValues")
        assertTrue("F" in sexValues, "sex filter must include F, got: $sexValues")
    }

    // ------------------------------------------------------------------------------------
    // 5. age filter: TOTAL present
    // ------------------------------------------------------------------------------------

    @Test
    fun fetchPopulation_ageParamIsTOTAL() = runTest {
        var captured: HttpRequestData? = null
        val service = buildService(onRequest = { captured = it })

        service.fetchPopulation(PopulationQuery(listOf("PL"), 2020..2020))

        assertNotNull(captured)
        val ageValues = captured!!.url.parameters.getAll("age") ?: emptyList()
        assertTrue("TOTAL" in ageValues, "age filter must include TOTAL, got: $ageValues")
    }

    // ------------------------------------------------------------------------------------
    // 6. Response is threaded through mapper — non-empty result
    // ------------------------------------------------------------------------------------

    @Test
    fun fetchPopulation_responseThreadedThroughMapper_returnsNonEmpty() = runTest {
        val service = buildService()
        val result = service.fetchPopulation(PopulationQuery(listOf("PL"), 2020..2020)).timeSeries
        assertTrue(result.isNotEmpty(), "Service should return mapped TimeSeries")
    }

    // ------------------------------------------------------------------------------------
    // 7. Mapped TimeSeries contains correct country and year
    // ------------------------------------------------------------------------------------

    @Test
    fun fetchPopulation_mappedResult_hasCorrectCountryAndYear() = runTest {
        val service = buildService()
        val result = service.fetchPopulation(PopulationQuery(listOf("PL"), 2020..2020)).timeSeries

        assertEquals(1, result.size)
        assertEquals("PL", result[0].countryCode)
        assertEquals(2020, result[0].points[0].year)
    }

    // ------------------------------------------------------------------------------------
    // 8. Mapped result has correct total population
    // ------------------------------------------------------------------------------------

    @Test
    fun fetchPopulation_mappedResult_hasCorrectTotalPopulation() = runTest {
        val service = buildService()
        val result = service.fetchPopulation(PopulationQuery(listOf("PL"), 2020..2020)).timeSeries

        val point = result[0].points[0]
        assertEquals(38_000_000L, point.totalPopulation)
        assertEquals(18_000_000L, point.malePopulation)
        assertEquals(20_000_000L, point.femalePopulation)
    }

    // ------------------------------------------------------------------------------------
    // 9. 500 error → service throws (does NOT swallow)
    // ------------------------------------------------------------------------------------

    @Test
    fun fetchPopulation_serverError_throws() = runTest {
        val service = buildService(
            responseBody = """{"error":"Internal Server Error"}""",
            status = HttpStatusCode.InternalServerError,
        )

        assertFails("Service should propagate HTTP 500 as an exception") {
            service.fetchPopulation(PopulationQuery(listOf("PL"), 2020..2020))
        }
    }

    // ------------------------------------------------------------------------------------
    // 10. format=JSON query parameter is always present
    // ------------------------------------------------------------------------------------

    @Test
    fun fetchPopulation_formatJsonParamPresent() = runTest {
        var captured: HttpRequestData? = null
        val service = buildService(onRequest = { captured = it })

        service.fetchPopulation(PopulationQuery(listOf("PL"), 2020..2020))

        assertNotNull(captured)
        val formatValues = captured!!.url.parameters.getAll("format") ?: emptyList()
        assertTrue("JSON" in formatValues, "format=JSON must be present as query parameter, got: $formatValues")
    }
}
