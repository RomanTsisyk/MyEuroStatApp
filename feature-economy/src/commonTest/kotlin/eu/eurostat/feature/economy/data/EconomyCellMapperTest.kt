package eu.eurostat.feature.economy.data

import eu.eurostat.core.jsonstat.JsonStatCell
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [EconomyCellMapper].
 *
 * Dimension values use CODES (from JSON-stat category.index keys), not labels:
 *   dimensions["geo"] = "PL", dimensions["na_item"] = "B1GQ", etc.
 *
 * The mapper does NOT filter by na_item or unit — it maps all cells with
 * valid geo+time, trusting the API filters to provide only the relevant data.
 */
class EconomyCellMapperTest {

    private fun cell(geo: String, time: String, naItem: String = "B1GQ", unit: String = "CP_MEUR", value: Double?) =
        JsonStatCell(
            dimensions = mapOf("geo" to geo, "time" to time, "na_item" to naItem, "unit" to unit),
            value = value,
        )

    @Test
    fun map_happyPath_twoCountriesTwoYears_producesCorrectTimeSeries() {
        val cells = listOf(
            cell("PL", "2020", value = 523_000.0),
            cell("PL", "2021", value = 545_000.0),
            cell("DE", "2020", value = 3_400_000.0),
            cell("DE", "2021", value = 3_570_000.0),
        )

        val result = EconomyCellMapper.map(cells)

        assertEquals(2, result.size)
        val pl = result.first { it.countryCode == "PL" }
        val de = result.first { it.countryCode == "DE" }
        assertEquals(2, pl.points.size)
        assertEquals(2, de.points.size)
    }

    @Test
    fun map_singleCell_gdpEurPopulatedCorrectly() {
        val cells = listOf(cell("PL", "2020", value = 523_000.0))
        val result = EconomyCellMapper.map(cells)
        val point = result[0].points[0]
        assertEquals("PL", point.countryCode)
        assertEquals(2020, point.year)
        assertEquals(523_000L, point.gdpEur)
    }

    @Test
    fun map_nullValueCell_gdpEurIsNull() {
        val cells = listOf(cell("PL", "2020", value = null))
        val result = EconomyCellMapper.map(cells)
        assertEquals(1, result.size)
        assertNull(result[0].points[0].gdpEur)
    }

    @Test
    fun map_hicpIndex_andDeficit_areNull_whenOnlyGdpProvided() {
        val cells = listOf(cell("PL", "2020", value = 523_000.0))
        val result = EconomyCellMapper.map(cells)
        val point = result[0].points[0]
        assertNull(point.hicpIndex)
        assertNull(point.deficitPctGdp)
    }

    @Test
    fun map_nonB1GQItem_cellIsIncluded_mapperDoesNotFilter() {
        val cells = listOf(
            cell("PL", "2020", naItem = "B1GQ", value = 523_000.0),
            cell("PL", "2021", naItem = "P31", value = 300_000.0),
        )
        val result = EconomyCellMapper.map(cells)
        val pl = result.first { it.countryCode == "PL" }
        assertEquals(2, pl.points.size)
    }

    @Test
    fun map_emptyInput_returnsEmpty() {
        val result = EconomyCellMapper.map(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun map_pointsSortedAscendingByYear() {
        val cells = listOf(
            cell("PL", "2022", value = 600_000.0),
            cell("PL", "2020", value = 523_000.0),
            cell("PL", "2021", value = 545_000.0),
        )
        val result = EconomyCellMapper.map(cells)
        assertEquals(listOf(2020, 2021, 2022), result[0].points.map { it.year })
    }

    @Test
    fun map_timeSeriesSortedByCountryCode() {
        val cells = listOf(
            cell("PL", "2020", value = 523_000.0),
            cell("AT", "2020", value = 399_000.0),
            cell("DE", "2020", value = 3_400_000.0),
        )
        val result = EconomyCellMapper.map(cells)
        assertEquals(listOf("AT", "DE", "PL"), result.map { it.countryCode })
    }

    @Test
    fun map_largeGdpValue_truncatedToLong() {
        val cells = listOf(cell("DE", "2020", value = 3_400_000.7))
        val result = EconomyCellMapper.map(cells)
        assertEquals(3_400_000L, result[0].points[0].gdpEur)
    }

    @Test
    fun mergeIntoTimeSeries_combinesAllThreeMetrics() {
        val gdp = listOf(cell("DE", "2020", value = 3_400_000.0))
        val hicp = listOf(cell("DE", "2020", naItem = "-", unit = "INX_A_AVG", value = 105.8))
        val deficit = listOf(cell("DE", "2020", naItem = "B9", unit = "PC_GDP", value = -2.5))

        val result = EconomyCellMapper.mergeIntoTimeSeries(gdp, hicp, deficit)
        val pt = result.single().points.single()
        assertEquals(3_400_000L, pt.gdpEur)
        assertEquals(105.8, pt.hicpIndex)
        assertEquals(-2.5, pt.deficitPctGdp)
    }
}
