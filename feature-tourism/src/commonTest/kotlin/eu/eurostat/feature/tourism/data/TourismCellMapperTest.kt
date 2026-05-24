package eu.eurostat.feature.tourism.data

import eu.eurostat.core.jsonstat.JsonStatCell
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TourismCellMapperTest {

    // ---------------------------------------------------------------------------
    // mergeToDataPoints — nights split by c_resid
    // ---------------------------------------------------------------------------

    @Test
    fun mergeToDataPoints_splits_nights_by_residence() {
        val cells = listOf(
            JsonStatCell(mapOf("geo" to "PL", "time" to "2020", "c_resid" to "DOM"), value = 1_500_000.0),
            JsonStatCell(mapOf("geo" to "PL", "time" to "2020", "c_resid" to "FOR"), value = 2_000_000.0),
            JsonStatCell(mapOf("geo" to "PL", "time" to "2020", "c_resid" to "TOTAL"), value = 3_500_000.0),
        )
        val result = TourismCellMapper.mergeToDataPoints(cells, emptyList())
        assertEquals(1, result.size)
        val point = result[0]
        assertEquals(1_500_000L, point.domesticNights)
        assertEquals(2_000_000L, point.foreignNights)
        assertEquals(3_500_000L, point.totalNights)
        assertNull(point.trips)
    }

    @Test
    fun mergeToDataPoints_sums_nights_across_nace_subcategories() {
        val cells = (1..3).map { i ->
            JsonStatCell(
                mapOf("geo" to "ES", "time" to "2021", "c_resid" to "FOR", "nace_r2" to "I55$i"),
                value = 1_000_000.0,
            )
        }
        val result = TourismCellMapper.mergeToDataPoints(cells, emptyList())
        assertEquals(1, result.size)
        assertEquals(3_000_000L, result[0].foreignNights)
    }

    @Test
    fun mergeToDataPoints_skips_missing_geo_or_time() {
        val cells = listOf(
            JsonStatCell(mapOf("time" to "2020", "c_resid" to "DOM"), value = 1.0),
            JsonStatCell(mapOf("geo" to "PL", "c_resid" to "DOM"), value = 1.0),
            JsonStatCell(mapOf("geo" to "PL", "time" to "Q1-2020", "c_resid" to "DOM"), value = 1.0),
        )
        assertTrue(TourismCellMapper.mergeToDataPoints(cells, emptyList()).isEmpty())
    }

    @Test
    fun mergeToDataPoints_skips_null_values() {
        val cells = listOf(
            JsonStatCell(mapOf("geo" to "PL", "time" to "2020", "c_resid" to "DOM"), value = null),
        )
        assertTrue(TourismCellMapper.mergeToDataPoints(cells, emptyList()).isEmpty())
    }

    @Test
    fun mergeToDataPoints_layers_trips_onto_matching_row() {
        val nights = listOf(
            JsonStatCell(mapOf("geo" to "PL", "time" to "2020", "c_resid" to "DOM"), value = 1_000_000.0),
        )
        val trips = listOf(
            JsonStatCell(mapOf("geo" to "PL", "time" to "2020"), value = 50_000.0),
        )
        val result = TourismCellMapper.mergeToDataPoints(nights, trips)
        assertEquals(1, result.size)
        assertEquals(50_000L, result[0].trips)
        assertEquals(1_000_000L, result[0].domesticNights)
    }

    @Test
    fun mergeToDataPoints_creates_trips_only_row_when_nights_missing() {
        val trips = listOf(
            JsonStatCell(mapOf("geo" to "PL", "time" to "2020"), value = 50_000.0),
        )
        val result = TourismCellMapper.mergeToDataPoints(emptyList(), trips)
        assertEquals(1, result.size)
        assertEquals(50_000L, result[0].trips)
        assertNull(result[0].domesticNights)
    }

    @Test
    fun mergeToDataPoints_multiple_years_same_country() {
        val cells = (2018..2022).map { year ->
            JsonStatCell(
                mapOf("geo" to "FR", "time" to year.toString(), "c_resid" to "TOTAL"),
                value = year.toDouble() * 1000,
            )
        }
        val result = TourismCellMapper.mergeToDataPoints(cells, emptyList())
        assertEquals(5, result.size)
        val pl2020 = result.first { it.year == 2020 }
        assertEquals(2_020_000L, pl2020.totalNights)
    }

    @Test
    fun mergeToDataPoints_empty_inputs_return_empty() {
        assertTrue(TourismCellMapper.mergeToDataPoints(emptyList(), emptyList()).isEmpty())
    }

    // ---------------------------------------------------------------------------
    // buildCountryLabels
    // ---------------------------------------------------------------------------

    @Test
    fun buildCountryLabels_uses_dimensionLabels_geo() {
        val cells = listOf(
            JsonStatCell(
                dimensions = mapOf("geo" to "DE", "time" to "2020"),
                dimensionLabels = mapOf("geo" to "Germany"),
                value = 1.0,
            ),
        )
        val labels = TourismCellMapper.buildCountryLabels(cells)
        assertEquals("Germany", labels["DE"])
    }

    @Test
    fun toTimeSeries_groups_and_sorts_by_year() {
        val points = listOf(
            eu.eurostat.feature.tourism.domain.TourismDataPoint("PL", 2022, totalNights = 3L),
            eu.eurostat.feature.tourism.domain.TourismDataPoint("PL", 2020, totalNights = 1L),
            eu.eurostat.feature.tourism.domain.TourismDataPoint("PL", 2021, totalNights = 2L),
        )
        val series = TourismCellMapper.toTimeSeries(points, mapOf("PL" to "Poland"))
        assertEquals(1, series.size)
        val pl = series[0]
        assertEquals("Poland", pl.countryName)
        assertEquals(listOf(2020, 2021, 2022), pl.points.map { it.year })
        assertNotNull(pl.points[0].totalNights)
    }
}
