package eu.eurostat.core.network

import eu.eurostat.core.jsonstat.JsonStatParser
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
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EurostatApiClientErrorTest {

    private val cannedJson = """
        {
          "id":["geo","time"],
          "size":[1,1],
          "dimension":{
            "geo":{"category":{"index":{"PL":0},"label":{"PL":"Poland"}}},
            "time":{"category":{"index":{"2024":0},"label":{"2024":"2024"}}}
          },
          "value":{"0":38500000}
        }
    """.trimIndent()

    private val emptyPayload = """{}""".trimIndent()

    private fun buildClient(responseBody: String, status: HttpStatusCode): EurostatApiClient {
        val engine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(responseBody),
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(EurostatJson) }
        }
        return EurostatApiClient(httpClient, JsonStatParser())
    }

    @Test
    fun success_response_returns_cells() = runTest {
        val client = buildClient(cannedJson, HttpStatusCode.OK)
        val cells = client.fetchDataset("demo_pjan", mapOf("geo" to listOf("PL")))
        assertTrue(cells.isNotEmpty(), "Should parse cells from successful response")
    }

    @Test
    fun bad_request_400_throws() = runTest {
        val client = buildClient("Bad Request", HttpStatusCode.BadRequest)
        assertFails {
            client.fetchDataset("unknown_dataset", emptyMap())
        }
    }

    @Test
    fun not_found_404_throws() = runTest {
        val client = buildClient("Not Found", HttpStatusCode.NotFound)
        assertFails {
            client.fetchDataset("nonexistent", emptyMap())
        }
    }

    @Test
    fun server_error_500_throws() = runTest {
        val client = buildClient("Internal Server Error", HttpStatusCode.InternalServerError)
        assertFails {
            client.fetchDataset("demo_pjan", emptyMap())
        }
    }

    @Test
    fun too_many_requests_429_throws() = runTest {
        val client = buildClient("Too Many Requests", HttpStatusCode.TooManyRequests)
        assertFails {
            client.fetchDataset("demo_pjan", emptyMap())
        }
    }

    @Test
    fun invalid_json_throws() = runTest {
        val client = buildClient("{invalid json!!!}", HttpStatusCode.OK)
        assertFails {
            client.fetchDataset("demo_pjan", emptyMap())
        }
    }

    @Test
    fun empty_json_object_throws() = runTest {
        val client = buildClient(emptyPayload, HttpStatusCode.OK)
        assertFails {
            client.fetchDataset("demo_pjan", emptyMap())
        }
    }

    @Test
    fun url_contains_repeated_geo_params() = runTest {
        val capturedUrls = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedUrls += request.url.toString()
            respond(
                content = ByteReadChannel(cannedJson),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(EurostatJson) }
        }
        val client = EurostatApiClient(httpClient, JsonStatParser())

        client.fetchDataset("demo_pjan", mapOf("geo" to listOf("PL", "DE", "FR")))
        val url = capturedUrls[0]
        assertTrue(url.contains("geo=PL"), "URL should contain geo=PL")
        assertTrue(url.contains("geo=DE"), "URL should contain geo=DE")
        assertTrue(url.contains("geo=FR"), "URL should contain geo=FR")
    }

    @Test
    fun url_contains_format_and_lang_params() = runTest {
        val capturedUrls = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedUrls += request.url.toString()
            respond(
                content = ByteReadChannel(cannedJson),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(EurostatJson) }
        }
        val client = EurostatApiClient(httpClient, JsonStatParser())

        client.fetchDataset("demo_pjan", emptyMap())
        val url = capturedUrls[0]
        assertTrue(url.contains("format=JSON"), "URL should contain format=JSON")
        assertTrue(url.contains("lang=EN"), "URL should contain lang=EN")
    }

    @Test
    fun repeated_params_for_compound_filters() = runTest {
        val capturedUrls = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedUrls += request.url.toString()
            respond(
                content = ByteReadChannel(cannedJson),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(EurostatJson) }
        }
        val client = EurostatApiClient(httpClient, JsonStatParser())

        client.fetchDataset("env_air_gge", mapOf(
            "src_crf" to listOf("TOTX4_MEMO", "CRF1A3", "CRF1A2"),
            "airpol" to listOf("GHG"),
            "geo" to listOf("PL"),
        ))
        val url = capturedUrls[0]
        assertTrue(url.contains("src_crf=TOTX4_MEMO"))
        assertTrue(url.contains("src_crf=CRF1A3"))
        assertTrue(url.contains("src_crf=CRF1A2"))
        assertTrue(url.contains("airpol=GHG"))
        assertTrue(url.contains("geo=PL"))
    }
}
