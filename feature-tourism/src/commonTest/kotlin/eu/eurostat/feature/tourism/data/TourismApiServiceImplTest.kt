package eu.eurostat.feature.tourism.data

import eu.eurostat.core.jsonstat.JsonStatParser
import eu.eurostat.core.network.EurostatApiClient
import eu.eurostat.core.network.EurostatJson
import eu.eurostat.feature.tourism.domain.TourismQuery
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TourismApiServiceImplTest {

    private fun oneCell(geoCode: String = "PL", year: String = "2020", value: Double = 1_000.0) = """
        {
          "id":["geo","time"],
          "size":[1,1],
          "dimension":{
            "geo":{"category":{"index":{"$geoCode":0},"label":{"$geoCode":"Country"}}},
            "time":{"category":{"index":{"$year":0},"label":{"$year":"$year"}}}
          },
          "value":{"0":$value}
        }
    """.trimIndent()

    private fun buildServiceWithCapture(
        recorder: RequestRecorder,
        respondWith: String = oneCell(),
    ): TourismApiServiceImpl {
        val engine = MockEngine { request ->
            recorder.record(request.url.toString())
            respond(
                content = ByteReadChannel(respondWith),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val httpClient = HttpClient(engine) { install(ContentNegotiation) { json(EurostatJson) } }
        val api = EurostatApiClient(httpClient, JsonStatParser())
        return TourismApiServiceImpl(api)
    }

    @Test
    fun fetch_calls_all_three_datasets() = runTest {
        val recorder = RequestRecorder()
        val service = buildServiceWithCapture(recorder)
        service.fetch(TourismQuery(listOf("PL"), 2020..2020))
        val urls = recorder.all()

        assertEquals(3, urls.size, "Expected 3 requests (nights + trips + seasonality), got $urls")
        assertTrue(urls.any { it.contains("tour_occ_ninat") }, "Missing tour_occ_ninat in $urls")
        assertTrue(urls.any { it.contains("tour_dem_tttot") }, "Missing tour_dem_tttot in $urls")
        assertTrue(urls.any { it.contains("tour_occ_nim") }, "Missing tour_occ_nim in $urls")
    }

    @Test
    fun fetch_nights_url_includes_all_three_c_resid_categories() = runTest {
        val recorder = RequestRecorder()
        val service = buildServiceWithCapture(recorder)
        service.fetch(TourismQuery(listOf("PL"), 2020..2020))
        val urls = recorder.all()
        val nightsUrl = urls.first { it.contains("tour_occ_ninat") }
        assertTrue(nightsUrl.contains("c_resid=DOM"), "Expected c_resid=DOM in: $nightsUrl")
        assertTrue(nightsUrl.contains("c_resid=FOR"), "Expected c_resid=FOR in: $nightsUrl")
        assertTrue(nightsUrl.contains("c_resid=TOTAL"), "Expected c_resid=TOTAL in: $nightsUrl")
    }

    @Test
    fun fetch_trips_url_omits_c_resid_filter() = runTest {
        val recorder = RequestRecorder()
        val service = buildServiceWithCapture(recorder)
        service.fetch(TourismQuery(listOf("PL"), 2020..2020))
        val urls = recorder.all()
        val tripsUrl = urls.first { it.contains("tour_dem_tttot") }
        assertTrue(!tripsUrl.contains("c_resid="), "tour_dem_tttot must not carry c_resid: $tripsUrl")
    }

    // F2 regression: slice filters that pin tour_dem_tttot to one cell per (geo, year)
    // Without these, the mapper sums ~30 cells and produces 5-20x inflated trip counts.

    @Test
    fun fetch_trips_url_contains_purpose_TOTAL() = runTest {
        val recorder = RequestRecorder()
        val service = buildServiceWithCapture(recorder)
        service.fetch(TourismQuery(listOf("PL"), 2020..2020))
        val urls = recorder.all()
        val tripsUrl = urls.first { it.contains("tour_dem_tttot") }
        assertTrue(tripsUrl.contains("purpose=TOTAL"), "tour_dem_tttot must pin purpose=TOTAL to avoid multi-counting: $tripsUrl")
    }

    @Test
    fun fetch_trips_url_contains_duration_N_GE1() = runTest {
        val recorder = RequestRecorder()
        val service = buildServiceWithCapture(recorder)
        service.fetch(TourismQuery(listOf("PL"), 2020..2020))
        val urls = recorder.all()
        val tripsUrl = urls.first { it.contains("tour_dem_tttot") }
        assertTrue(tripsUrl.contains("duration=N_GE1"), "tour_dem_tttot must pin duration=N_GE1 (overnight trips) to avoid multi-counting: $tripsUrl")
    }

    // CRIT-2 regression: tour_dem_tttot uses `c_dest` for destination, NOT `partner`.
    // Sending `partner=WORLD` returns HTTP 400 INVALID_QUERY_DIMENSION from the live API.
    @Test
    fun fetch_trips_url_contains_c_dest_WORLD_not_partner() = runTest {
        val recorder = RequestRecorder()
        val service = buildServiceWithCapture(recorder)
        service.fetch(TourismQuery(listOf("PL"), 2020..2020))
        val urls = recorder.all()
        val tripsUrl = urls.first { it.contains("tour_dem_tttot") }
        assertTrue(
            tripsUrl.contains("c_dest=WORLD"),
            "tour_dem_tttot must use c_dest=WORLD (not partner) for destination filter: $tripsUrl",
        )
        assertTrue(
            !tripsUrl.contains("partner="),
            "tour_dem_tttot must NOT contain partner= (that dim is for trade datasets): $tripsUrl",
        )
    }

    @Test
    fun fetch_nights_url_includes_nace_r2_accommodation_subcategories() = runTest {
        val recorder = RequestRecorder()
        val service = buildServiceWithCapture(recorder)
        service.fetch(TourismQuery(listOf("PL"), 2020..2020))
        val urls = recorder.all()
        val nightsUrl = urls.first { it.contains("tour_occ_ninat") }
        assertTrue(nightsUrl.contains("nace_r2=I551"), "Missing nace_r2=I551 in $nightsUrl")
        assertTrue(nightsUrl.contains("nace_r2=I552"), "Missing nace_r2=I552 in $nightsUrl")
        assertTrue(nightsUrl.contains("nace_r2=I553"), "Missing nace_r2=I553 in $nightsUrl")
    }

    @Test
    fun fetch_url_contains_geo_and_time_params() = runTest {
        val recorder = RequestRecorder()
        val service = buildServiceWithCapture(recorder)
        service.fetch(TourismQuery(listOf("PL", "DE"), 2021..2022))
        val urls = recorder.all()
        val nightsUrl = urls.first { it.contains("tour_occ_ninat") }
        assertTrue(nightsUrl.contains("geo=PL"),    "Missing geo=PL in $nightsUrl")
        assertTrue(nightsUrl.contains("geo=DE"),    "Missing geo=DE in $nightsUrl")
        assertTrue(nightsUrl.contains("time=2021"), "Missing time=2021 in $nightsUrl")
        assertTrue(nightsUrl.contains("time=2022"), "Missing time=2022 in $nightsUrl")
    }

    @Test
    fun fetch_nights_succeed_when_trips_returns_500() = runTest {
        val engine = MockEngine { request ->
            if (request.url.toString().contains("tour_dem_tttot")) {
                respond(
                    content = ByteReadChannel("Server Error"),
                    status = HttpStatusCode.InternalServerError,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString()),
                )
            } else {
                respond(
                    content = ByteReadChannel(oneCell()),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }
        }
        val httpClient = HttpClient(engine) { install(ContentNegotiation) { json(EurostatJson) } }
        val api = EurostatApiClient(httpClient, JsonStatParser())
        val service = TourismApiServiceImpl(api)

        // Should NOT throw — trips failure is swallowed (best-effort)
        val result = service.fetch(TourismQuery(listOf("PL"), 2020..2020))
        // nights point exists, trips is null.
        assertEquals(1, result.points.size, "Expected 1 nights result even though trips failed")
        // The mock nights JSON carries no c_resid dimension, so all three nights
        // columns remain null — what matters here is that trips stayed null.
        assertNull(result.points[0].trips)
    }
}

/**
 * Thread-safe request-URL recorder: MockEngine may invoke handlers concurrently on
 * different threads (the service fires 3 parallel requests), so unsynchronized
 * appends to a plain list can lose elements.
 */
private class RequestRecorder {
    private val mutex = Mutex()
    private val urls = mutableListOf<String>()

    /** Records one intercepted request URL under the mutex. */
    suspend fun record(url: String) {
        mutex.withLock { urls += url }
    }

    /** Returns a snapshot of all recorded request URLs. */
    suspend fun all(): List<String> = mutex.withLock { urls.toList() }
}
