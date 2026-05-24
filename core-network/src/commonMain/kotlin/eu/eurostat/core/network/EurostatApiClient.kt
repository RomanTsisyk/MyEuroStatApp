package eu.eurostat.core.network

import eu.eurostat.core.jsonstat.JsonStatParser
import eu.eurostat.core.jsonstat.JsonStatResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Single Ktor HttpClient shared across all feature modules.
 *
 * Created once in DI graph, injected into per-feature API services.
 * Each feature module owns its own dataset-specific calls but uses this client.
 *
 * Reusing the engine matters: Darwin/OkHttp keep connection pools, and creating
 * multiple clients leaks file descriptors on iOS.
 *
 * Base URL: https://ec.europa.eu/eurostat/api/dissemination/statistics/1.0/data/
 */
class EurostatApiClient(
    private val httpClient: HttpClient,
    private val parser: JsonStatParser,
) {
    /**
     * Fetch a Eurostat dataset by its code, with optional dimension filters.
     *
     * Eurostat REST filter syntax — pass as repeated query parameters:
     *   ?geo=PL&geo=DE&time=2020&time=2021&sex=T
     *
     * The API normalizes order, so we don't need to escape or sort.
     *
     * Throws on non-2xx or parse failures — repository wraps these into AppError.
     */
    suspend fun fetchDataset(
        datasetCode: String,
        filters: Map<String, List<String>> = emptyMap(),
    ): List<eu.eurostat.core.jsonstat.JsonStatCell> {
        val raw: JsonStatResponse = httpClient.get(datasetCode) {
            parameter("format", "JSON")
            parameter("lang", "EN")
            for ((dim, values) in filters) {
                for (v in values) parameter(dim, v)
            }
        }.body()
        return parser.parse(raw)
    }
}

/**
 * Factory for the shared HttpClient. Engine is platform-specific:
 *   - androidMain: OkHttp engine via expect/actual
 *   - iosMain:     Darwin engine via expect/actual
 *
 * Configuration here is platform-agnostic. Timeouts and retry are conservative
 * because Eurostat occasionally serves slow responses for large datasets.
 */
expect fun createHttpClientEngine(): io.ktor.client.engine.HttpClientEngineFactory<*>

fun createHttpClient(json: Json, debug: Boolean = false): HttpClient = HttpClient(createHttpClientEngine()) {
    expectSuccess = true

    defaultRequest {
        url("https://ec.europa.eu/eurostat/api/dissemination/statistics/1.0/data/")
    }

    install(ContentNegotiation) { json(json) }

    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 30_000
    }

    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = 2)
        exponentialDelay(base = 2.0, maxDelayMs = 5_000)
    }

    if (debug) {
        install(Logging) { level = io.ktor.client.plugins.logging.LogLevel.HEADERS }
    }
}

/** Single Json config — kotlinx-serialization rejects unknown fields by default, we need lenient. */
val EurostatJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}
