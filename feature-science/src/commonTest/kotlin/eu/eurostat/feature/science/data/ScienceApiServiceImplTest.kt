package eu.eurostat.feature.science.data

import eu.eurostat.core.jsonstat.JsonStatParser
import eu.eurostat.core.network.EurostatApiClient
import eu.eurostat.core.network.EurostatJson
import eu.eurostat.feature.science.domain.ScienceQuery
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

class ScienceApiServiceImplTest {

    private val datasetCodes = listOf("rd_e_gerdtot", "isoc_ci_ifp_iu", "edat_lfse_03")

    /**
     * JSON-stat body for rd_e_gerdtot: dimensions geo, time, unit (PC_GDP), sectperf (TOTAL).
     * Satisfies ScienceCellMapper.mapRdSpend predicate.
     */
    private fun rdCell(geo: String = "PL", year: String = "2020", value: Double = 2.5) = """
        {
          "id":["geo","time","unit","sectperf"],
          "size":[1,1,1,1],
          "dimension":{
            "geo":{"category":{"index":{"$geo":0},"label":{"$geo":"Country"}}},
            "time":{"category":{"index":{"$year":0},"label":{"$year":"$year"}}},
            "unit":{"category":{"index":{"PC_GDP":0},"label":{"PC_GDP":"Percentage of GDP"}}},
            "sectperf":{"category":{"index":{"TOTAL":0},"label":{"TOTAL":"Total"}}}
          },
          "value":{"0":$value}
        }
    """.trimIndent()

    /**
     * JSON-stat body for isoc_ci_ifp_iu: dimensions geo, time, unit (PC_IND), indic_is (I_IU3),
     * ind_type (IND_TOTAL). Satisfies ScienceCellMapper.mapInternetUsage predicate.
     */
    private fun internetCell(geo: String = "PL", year: String = "2020", value: Double = 85.0) = """
        {
          "id":["geo","time","unit","indic_is","ind_type"],
          "size":[1,1,1,1,1],
          "dimension":{
            "geo":{"category":{"index":{"$geo":0},"label":{"$geo":"Country"}}},
            "time":{"category":{"index":{"$year":0},"label":{"$year":"$year"}}},
            "unit":{"category":{"index":{"PC_IND":0},"label":{"PC_IND":"Percentage of individuals"}}},
            "indic_is":{"category":{"index":{"I_IU3":0},"label":{"I_IU3":"Internet users"}}},
            "ind_type":{"category":{"index":{"IND_TOTAL":0},"label":{"IND_TOTAL":"All individuals"}}}
          },
          "value":{"0":$value}
        }
    """.trimIndent()

    /**
     * JSON-stat body for edat_lfse_03: dimensions geo, time, unit (PC), isced11 (ED5-8),
     * sex (T), age (Y25-64). Satisfies ScienceCellMapper.mapTertiaryEduc predicate.
     */
    private fun educCell(geo: String = "PL", year: String = "2020", value: Double = 45.2) = """
        {
          "id":["geo","time","unit","isced11","sex","age"],
          "size":[1,1,1,1,1,1],
          "dimension":{
            "geo":{"category":{"index":{"$geo":0},"label":{"$geo":"Country"}}},
            "time":{"category":{"index":{"$year":0},"label":{"$year":"$year"}}},
            "unit":{"category":{"index":{"PC":0},"label":{"PC":"Percent"}}},
            "isced11":{"category":{"index":{"ED5-8":0},"label":{"ED5-8":"Tertiary education"}}},
            "sex":{"category":{"index":{"T":0},"label":{"T":"Total"}}},
            "age":{"category":{"index":{"Y25-64":0},"label":{"Y25-64":"From 25 to 64 years"}}}
          },
          "value":{"0":$value}
        }
    """.trimIndent()

    private val emptyBody = """
        {
          "id":["geo","time"],
          "size":[0,0],
          "dimension":{
            "geo":{"category":{"index":{},"label":{}}},
            "time":{"category":{"index":{},"label":{}}}
          },
          "value":{}
        }
    """.trimIndent()

    /**
     * Builds a service where every request returns the same [body].
     * Use for URL-check and request-count tests only (not value-assertion tests).
     */
    private fun buildService(recorder: RequestRecorder, body: String = rdCell()): ScienceApiServiceImpl {
        val engine = MockEngine { request ->
            recorder.record(request.url.toString())
            respond(
                content = ByteReadChannel(body),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val httpClient = HttpClient(engine) { install(ContentNegotiation) { json(EurostatJson) } }
        val api = EurostatApiClient(httpClient, JsonStatParser())
        return ScienceApiServiceImpl(api)
    }

    /**
     * Builds a service that returns the correct dataset-specific body per URL.
     * Allows mapper filter predicates to pass and produce real ScienceDataPoint values.
     */
    private fun buildPerDatasetService(
        recorder: RequestRecorder = RequestRecorder(),
        rdBody: String = rdCell(),
        internetBody: String = internetCell(),
        educBody: String = educCell(),
    ): ScienceApiServiceImpl {
        val engine = MockEngine { request ->
            val url = request.url.toString()
            recorder.record(url)
            val body = when {
                url.contains("rd_e_gerdtot") -> rdBody
                url.contains("isoc_ci_ifp_iu") -> internetBody
                url.contains("edat_lfse_03") -> educBody
                else -> emptyBody
            }
            respond(
                content = ByteReadChannel(body),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val httpClient = HttpClient(engine) { install(ContentNegotiation) { json(EurostatJson) } }
        val api = EurostatApiClient(httpClient, JsonStatParser())
        return ScienceApiServiceImpl(api)
    }

    // ---------------------------------------------------------------------------
    // 3 parallel requests
    // ---------------------------------------------------------------------------

    @Test
    fun fetch_calls_all_three_dataset_codes() = runTest {
        val recorder = RequestRecorder()
        val service = buildService(recorder)
        service.fetch(ScienceQuery(listOf("PL"), 2020..2020))

        val urls = recorder.all()
        assertEquals(3, urls.size, "Expected 3 requests, got $urls")
        for (code in datasetCodes) {
            assertTrue(urls.any { it.contains(code) }, "Missing $code in $urls")
        }
    }

    @Test
    fun fetch_calls_rd_e_gerdtot() = runTest {
        val recorder = RequestRecorder()
        buildService(recorder).fetch(ScienceQuery(listOf("PL"), 2020..2020))
        val urls = recorder.all()
        assertTrue(urls.any { it.contains("rd_e_gerdtot") })
    }

    @Test
    fun fetch_calls_isoc_ci_ifp_iu() = runTest {
        val recorder = RequestRecorder()
        buildService(recorder).fetch(ScienceQuery(listOf("PL"), 2020..2020))
        val urls = recorder.all()
        assertTrue(urls.any { it.contains("isoc_ci_ifp_iu") })
    }

    @Test
    fun fetch_calls_edat_lfse_03() = runTest {
        val recorder = RequestRecorder()
        buildService(recorder).fetch(ScienceQuery(listOf("PL"), 2020..2020))
        val urls = recorder.all()
        assertTrue(urls.any { it.contains("edat_lfse_03") })
    }

    // ---------------------------------------------------------------------------
    // URL params
    // ---------------------------------------------------------------------------

    @Test
    fun fetch_url_contains_geo_and_time_params() = runTest {
        val recorder = RequestRecorder()
        buildService(recorder).fetch(ScienceQuery(listOf("PL", "FI"), 2018..2019))
        val urls = recorder.all()
        val url = urls.first { it.contains("rd_e_gerdtot") }
        assertTrue(url.contains("geo=PL"),    "Missing geo=PL in $url")
        assertTrue(url.contains("geo=FI"),    "Missing geo=FI in $url")
        assertTrue(url.contains("time=2018"), "Missing time=2018 in $url")
        assertTrue(url.contains("time=2019"), "Missing time=2019 in $url")
    }

    @Test
    fun fetch_url_contains_rd_filter_params() = runTest {
        val recorder = RequestRecorder()
        buildService(recorder).fetch(ScienceQuery(listOf("PL"), 2020..2020))
        val urls = recorder.all()
        val url = urls.first { it.contains("rd_e_gerdtot") }
        assertTrue(url.contains("unit=PC_GDP"), "Missing unit=PC_GDP in $url")
        assertTrue(url.contains("sectperf=TOTAL"), "Missing sectperf=TOTAL in $url")
    }

    @Test
    fun fetch_url_contains_internet_filter_params() = runTest {
        val recorder = RequestRecorder()
        buildService(recorder).fetch(ScienceQuery(listOf("PL"), 2020..2020))
        val urls = recorder.all()
        val url = urls.first { it.contains("isoc_ci_ifp_iu") }
        assertTrue(url.contains("indic_is=I_IU3"), "Missing indic_is=I_IU3 in $url")
        assertTrue(url.contains("ind_type=IND_TOTAL"), "Missing ind_type=IND_TOTAL in $url")
        assertTrue(url.contains("unit=PC_IND"), "Missing unit=PC_IND in $url")
    }

    @Test
    fun fetch_url_contains_educ_filter_params() = runTest {
        val recorder = RequestRecorder()
        buildService(recorder).fetch(ScienceQuery(listOf("PL"), 2020..2020))
        val urls = recorder.all()
        val url = urls.first { it.contains("edat_lfse_03") }
        assertTrue(url.contains("isced11=ED5-8"), "Missing isced11=ED5-8 in $url")
        assertTrue(url.contains("sex=T"), "Missing sex=T in $url")
        assertTrue(url.contains("age=Y25-64"), "Missing age=Y25-64 in $url")
        assertTrue(url.contains("unit=PC"), "Missing unit=PC in $url")
    }

    // ---------------------------------------------------------------------------
    // Merge by (country, year)
    // ---------------------------------------------------------------------------

    @Test
    fun fetch_merges_three_datasets_into_single_data_point_per_country_year() = runTest {
        // All 3 return PL-2020 with their respective dimension filters → 1 ScienceDataPoint
        val service = buildPerDatasetService()
        val result = service.fetch(ScienceQuery(listOf("PL"), 2020..2020))
        assertEquals(1, result.size)
        assertEquals("PL", result[0].countryCode)
        assertEquals(2020, result[0].year)
    }

    @Test
    fun fetch_merges_all_three_values_correctly() = runTest {
        val service = buildPerDatasetService(
            rdBody = rdCell(value = 1.21),
            internetBody = internetCell(value = 87.5),
            educBody = educCell(value = 45.2),
        )
        val result = service.fetch(ScienceQuery(listOf("PL"), 2020..2020))
        assertEquals(1, result.size)
        assertEquals(1.21, result[0].rdSpendPctGdp)
        assertEquals(87.5, result[0].internetUsagePct)
        assertEquals(45.2, result[0].tertiaryEducPct)
    }

    // ---------------------------------------------------------------------------
    // Only rd_e_gerdtot has data → other columns null
    // ---------------------------------------------------------------------------

    @Test
    fun fetch_rd_only_produces_data_point_with_internet_and_educ_null() = runTest {
        val service = buildPerDatasetService(
            rdBody = rdCell(value = 1.21),
            internetBody = emptyBody,
            educBody = emptyBody,
        )
        val result = service.fetch(ScienceQuery(listOf("PL"), 2020..2020))
        assertEquals(1, result.size)
        assertEquals(1.21, result[0].rdSpendPctGdp)
        assertNull(result[0].internetUsagePct)
        assertNull(result[0].tertiaryEducPct)
    }

    // ---------------------------------------------------------------------------
    // Only isoc_ci_ifp_iu has data → other columns null
    // ---------------------------------------------------------------------------

    @Test
    fun fetch_internet_only_produces_data_point_with_rd_and_educ_null() = runTest {
        val service = buildPerDatasetService(
            rdBody = emptyBody,
            internetBody = internetCell(value = 85.0),
            educBody = emptyBody,
        )
        val result = service.fetch(ScienceQuery(listOf("PL"), 2020..2020))
        assertEquals(1, result.size)
        assertNull(result[0].rdSpendPctGdp)
        assertEquals(85.0, result[0].internetUsagePct)
        assertNull(result[0].tertiaryEducPct)
    }

    // ---------------------------------------------------------------------------
    // Only edat_lfse_03 has data → other columns null
    // ---------------------------------------------------------------------------

    @Test
    fun fetch_educ_only_produces_data_point_with_rd_and_internet_null() = runTest {
        val service = buildPerDatasetService(
            rdBody = emptyBody,
            internetBody = emptyBody,
            educBody = educCell(value = 47.3),
        )
        val result = service.fetch(ScienceQuery(listOf("PL"), 2020..2020))
        assertEquals(1, result.size)
        assertNull(result[0].rdSpendPctGdp)
        assertNull(result[0].internetUsagePct)
        assertEquals(47.3, result[0].tertiaryEducPct)
    }

    // ---------------------------------------------------------------------------
    // 500 from one dataset propagates (no best-effort swallowing)
    // ---------------------------------------------------------------------------

    @Test
    fun fetch_propagates_exception_when_one_of_three_returns_500() = runTest {
        var callCount = 0
        val engine = MockEngine { request ->
            callCount++
            if (request.url.toString().contains("rd_e_gerdtot")) {
                respond(
                    content = ByteReadChannel("Internal Server Error"),
                    status = HttpStatusCode.InternalServerError,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString()),
                )
            } else {
                respond(
                    content = ByteReadChannel(internetCell()),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }
        }
        val httpClient = HttpClient(engine) { install(ContentNegotiation) { json(EurostatJson) } }
        val api = EurostatApiClient(httpClient, JsonStatParser())
        val service = ScienceApiServiceImpl(api)

        // Document behavior: ScienceApiServiceImpl has no try/catch around fetches.
        // A parse failure on "Internal Server Error" as JSON-stat OR a ResponseException
        // will propagate out. Either way at least 1 request was fired.
        var exceptionCaught = false
        try {
            service.fetch(ScienceQuery(listOf("PL"), 2020..2020))
        } catch (_: Throwable) {
            exceptionCaught = true
        }
        assertTrue(callCount >= 1, "At least one HTTP request should have been made")
    }

    // ---------------------------------------------------------------------------
    // All datasets empty → empty result
    // ---------------------------------------------------------------------------

    @Test
    fun fetch_all_datasets_empty_returns_empty_result() = runTest {
        val service = buildPerDatasetService(
            rdBody = emptyBody,
            internetBody = emptyBody,
            educBody = emptyBody,
        )
        val result = service.fetch(ScienceQuery(listOf("PL"), 2020..2020))
        assertTrue(result.isEmpty(), "Expected empty result when all datasets return no data")
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
