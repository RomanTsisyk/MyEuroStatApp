package eu.eurostat.core.network

import eu.eurostat.core.jsonstat.JsonStatParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class EurostatApiClientTest {
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

    @Test
    fun fetchDataset_returns_cells_from_mock_engine() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(cannedJson),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(EurostatJson) }
        }
        val api = EurostatApiClient(client, JsonStatParser())
        val cells = api.fetchDataset("demo_pjan", mapOf("geo" to listOf("PL"), "time" to listOf("2024")))
        assertTrue(cells.isNotEmpty())
    }
}
