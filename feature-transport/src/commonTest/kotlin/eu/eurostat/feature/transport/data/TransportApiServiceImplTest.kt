package eu.eurostat.feature.transport.data

import eu.eurostat.core.jsonstat.JsonStatParser
import eu.eurostat.core.network.EurostatApiClient
import eu.eurostat.core.network.EurostatJson
import eu.eurostat.feature.transport.domain.TransportMode
import eu.eurostat.feature.transport.domain.TransportQuery
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies that [TransportApiServiceImpl] fires the correct number of HTTP requests
 * and targets the right dataset codes for each [TransportMode].
 */
class TransportApiServiceImplTest {

    // A minimal valid JSON-stat response with geo=PL, time=2020
    private fun oneCell(value: Double = 0.0): String = """
        {
          "id":["geo","time"],
          "size":[1,1],
          "dimension":{
            "geo":{"category":{"index":{"PL":0},"label":{"PL":"Poland"}}},
            "time":{"category":{"index":{"2020":0},"label":{"2020":"2020"}}}
          },
          "value":{"0":$value}
        }
    """.trimIndent()

    private fun buildService(capturedUrls: MutableList<String>): TransportApiServiceImpl {
        val engine = MockEngine { request ->
            capturedUrls += request.url.toString()
            respond(
                content = ByteReadChannel(oneCell()),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(EurostatJson) }
        }
        val api = EurostatApiClient(httpClient, JsonStatParser())
        return TransportApiServiceImpl(api)
    }

    // ---------------------------------------------------------------------------
    // Mode-specific fetch: only ONE request is made
    // ---------------------------------------------------------------------------

    @Test
    fun fetch_road_mode_calls_only_road_pa_buscoa() = runTest {
        val urls = mutableListOf<String>()
        val service = buildService(urls)
        service.fetch(TransportQuery(listOf("PL"), 2020..2020, mode = TransportMode.ROAD))
        assertEquals(1, urls.size)
        assertTrue(urls[0].contains("road_pa_buscoa"), "Expected road_pa_buscoa in URL, got: ${urls[0]}")
    }

    @Test
    fun fetch_air_mode_calls_only_avia_paoc() = runTest {
        val urls = mutableListOf<String>()
        val service = buildService(urls)
        service.fetch(TransportQuery(listOf("PL"), 2020..2020, mode = TransportMode.AIR))
        assertEquals(1, urls.size)
        assertTrue(urls[0].contains("avia_paoc"), "Expected avia_paoc in URL, got: ${urls[0]}")
    }

    @Test
    fun fetch_sea_mode_fires_no_requests() = runTest {
        val urls = mutableListOf<String>()
        val service = buildService(urls)
        val result = service.fetch(TransportQuery(listOf("PL"), 2020..2020, mode = TransportMode.SEA))
        assertEquals(0, urls.size, "SEA is disabled: mar_pa_aa uses port-based dim, not geo")
        assertTrue(result.isEmpty(), "SEA fetch should return empty list")
    }

    // ---------------------------------------------------------------------------
    // ALL mode: two requests fire (SEA is disabled)
    // ---------------------------------------------------------------------------

    @Test
    fun fetch_all_mode_calls_road_and_air_endpoints() = runTest {
        val urls = mutableListOf<String>()
        val service = buildService(urls)
        service.fetch(TransportQuery(listOf("PL"), 2020..2020, mode = TransportMode.ALL))
        assertEquals(2, urls.size, "ALL mode fires 2 requests (SEA disabled), got: $urls")
        assertTrue(urls.any { it.contains("road_pa_buscoa") }, "Missing road_pa_buscoa in $urls")
        assertTrue(urls.any { it.contains("avia_paoc") },     "Missing avia_paoc in $urls")
    }

    // ---------------------------------------------------------------------------
    // URL contains geo and time query parameters
    // ---------------------------------------------------------------------------

    @Test
    fun fetch_url_contains_geo_and_time_params() = runTest {
        val urls = mutableListOf<String>()
        val service = buildService(urls)
        service.fetch(TransportQuery(listOf("PL", "DE"), 2021..2022, mode = TransportMode.ROAD))
        val url = urls[0]
        assertTrue(url.contains("geo=PL"),   "URL should contain geo=PL, got: $url")
        assertTrue(url.contains("geo=DE"),   "URL should contain geo=DE, got: $url")
        assertTrue(url.contains("time=2021"), "URL should contain time=2021, got: $url")
        assertTrue(url.contains("time=2022"), "URL should contain time=2022, got: $url")
    }

    // ---------------------------------------------------------------------------
    // Result wiring — road mode returns TransportDataPoints from road mapper
    // ---------------------------------------------------------------------------

    @Test
    fun fetch_road_mode_returns_road_data_points() = runTest {
        val urls = mutableListOf<String>()
        val engine = MockEngine { request ->
            urls += request.url.toString()
            respond(
                content = ByteReadChannel(oneCell(value = 5_000_000.0)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(EurostatJson) }
        }
        val api = EurostatApiClient(httpClient, JsonStatParser())
        val service = TransportApiServiceImpl(api)

        val result = service.fetch(TransportQuery(listOf("PL"), 2020..2020, mode = TransportMode.ROAD))
        assertEquals(1, result.size)
        assertEquals(TransportMode.ROAD, result[0].mode)
        // Input is in THS_PAS (thousands), mapper scales ×1000 to raw passenger count
        assertEquals(5_000_000_000L, result[0].roadPassengers)
    }

    // ---------------------------------------------------------------------------
    // F4 regression: road vehicle dim and air partner dim must be pinned
    // Without these, last-cell-wins in the mapper picks a random sub-total.
    // ---------------------------------------------------------------------------

    @Test
    fun fetch_road_url_does_not_contain_vehicle_dim() = runTest {
        // Eurostat road_pa_buscoa does not expose a `vehicle` dimension —
        // pinning it returns HTTP 400 INVALID_QUERY_DIMENSION. Verified
        // against the live API; see CLAUDE.md.
        val urls = mutableListOf<String>()
        val service = buildService(urls)
        service.fetch(TransportQuery(listOf("PL"), 2020..2020, mode = TransportMode.ROAD))
        val url = urls.first { it.contains("road_pa_buscoa") }
        assertTrue(!url.contains("vehicle="), "road_pa_buscoa must NOT send vehicle dim: $url")
    }

    @Test
    fun fetch_air_url_does_not_contain_partner_dim() = runTest {
        // Eurostat avia_paoc does not expose a `partner` dimension —
        // pinning it returns HTTP 400 INVALID_QUERY_DIMENSION. Verified
        // against the live API; see CLAUDE.md.
        val urls = mutableListOf<String>()
        val service = buildService(urls)
        service.fetch(TransportQuery(listOf("PL"), 2020..2020, mode = TransportMode.AIR))
        val url = urls.first { it.contains("avia_paoc") }
        assertTrue(!url.contains("partner="), "avia_paoc must NOT send partner dim: $url")
    }

    @Test
    fun fetch_all_mode_road_url_does_not_contain_vehicle_dim() = runTest {
        val urls = mutableListOf<String>()
        val service = buildService(urls)
        service.fetch(TransportQuery(listOf("PL"), 2020..2020, mode = TransportMode.ALL))
        val roadUrl = urls.first { it.contains("road_pa_buscoa") }
        assertTrue(!roadUrl.contains("vehicle="), "road_pa_buscoa in ALL mode must NOT send vehicle dim: $roadUrl")
    }

    @Test
    fun fetch_all_mode_air_url_does_not_contain_partner_dim() = runTest {
        val urls = mutableListOf<String>()
        val service = buildService(urls)
        service.fetch(TransportQuery(listOf("PL"), 2020..2020, mode = TransportMode.ALL))
        val airUrl = urls.first { it.contains("avia_paoc") }
        assertTrue(!airUrl.contains("partner="), "avia_paoc in ALL mode must NOT send partner dim: $airUrl")
    }

    @Test
    fun fetch_all_mode_merges_results_into_single_points_per_country_year() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(oneCell(value = 1_000.0)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(EurostatJson) }
        }
        val api = EurostatApiClient(httpClient, JsonStatParser())
        val service = TransportApiServiceImpl(api)

        val result = service.fetch(TransportQuery(listOf("PL"), 2020..2020, mode = TransportMode.ALL))
        // Road and air return PL-2020; SEA is disabled → 1 merged point
        assertEquals(1, result.size)
        assertEquals(TransportMode.ALL, result[0].mode)
        assertEquals("PL", result[0].countryCode)
        // Road value is in THS_PAS, scaled ×1000 in mapper; air stays raw
        assertEquals(1_000_000L, result[0].roadPassengers)
        assertEquals(1_000L, result[0].airPassengers)
        assertNull(result[0].seaPassengers)
    }
}
